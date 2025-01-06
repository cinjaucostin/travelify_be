package com.costin.travelify.service;

import com.costin.travelify.dto.arcgis_dto.ArcgisRouteResponseDTO;
import com.costin.travelify.dto.arcgis_dto.GeoLocationDTO;
import com.costin.travelify.dto.request_dto.GenerateTripDTO;
import com.costin.travelify.dto.request_dto.LocationTagDTO;
import com.costin.travelify.dto.request_dto.UpdatePlannificationDTO;
import com.costin.travelify.dto.response_dto.*;
import com.costin.travelify.entities.*;
import com.costin.travelify.exceptions.*;
import com.costin.travelify.repository.*;
import com.costin.travelify.service.apis.ArcgisService;
import com.costin.travelify.utils.Constants;
import com.costin.travelify.utils.TSPGreedy;
import com.costin.travelify.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.costin.travelify.utils.Utils.createListOfDatesBetweenFirstDayAndLastDay;

@Service
public class TripService {
    @Autowired
    private ArcgisService arcgisService;
    @Autowired
    private DestinationService destinationService;
    @Autowired
    private LocationService locationService;
    @Autowired
    private LocationTagService locationTagService;
    @Autowired
    private TripRepository tripRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ReviewService reviewService;
    @Autowired
    private TripPlannificationDayRepository tripPlannificationDayRepository;
    @Autowired
    private TripDayObjectiveRepository tripDayObjectiveRepository;
    @Autowired
    private RecommendationRepository recommendationRepository;

    /*
        Idei recomandari in cadrul trip plannification:
            0. Also matching your preferences, but lower popularity:
                - sunt tot locatii care fac match cu categoriile selectate de user doar ca planul ar fi depasit
                numarul maxim de obiective daca le luam in considerare, asa ca le-am luat in considerare pe cele
                care fac match si au popularitate mai mare, si acestea care fac match si au popularitate mai mica o sa le
                dam drept recomandari de acest tip.
            1. You may also like:
                - iau tag-urile de la locatiile care fac match si incerc pe baza lor sa gasesc si alte locatii care au tag-uri in comun
                dar care totusi nu aveau tag-ul initial pe care l-a dorit userul.
            2. Things you've never experienced:
                - iau tag-urile tuturor locatiilor(nu doar cele care fac match cu dorintele utilizatorului) din destinatia respectiva
                si ma uit la toate tag-urile din trip-urile lui precedente si le iau pe cele de la locatia curenta care nu se afla in
                trip-urile lui anterioare.
            3. Related to your latest trips:
                - iau tag-urile locatiilor din trip-urile anterioare si pentru destinatia curenta incerc sa selectez locatiile
                care au tag-uri asemanatoare cu cele din trip-urile precedente.
        Cum selectez locurile pentru trip-ul curent:
            - in functie de numarul de zile: ma gandesc la ceva gen 3 obiective pe zi(dintre care un restaurant care sa fie la mijloc)
            - si o sa am un numar de 3 x numarul de zile de obiective per tot trip-ul.
            - iau toate locatiile care fac match cu tag-urile selectate de user le ordonez descrescator dupa rating:
                - la rating fac o combinatie intre 50% * ratingTripAdvisor + 50% * ratingTravelify.

        !:
        Trebuie sa am grija sa nu mai adaug LocationTag Attractions.
        De asemenea cu Activites/Outdoor activities
     */

    public List<Trip> searchTrips(String query) {
        return this.tripRepository.findByNameOrDestinationContainsQuery(query);
    }

    public ResponseEntity<Trip> generateTrip(GenerateTripDTO generateTripDTO, Principal principal)
            throws InsufficientPostDataException, ResourceNotFoundException, UserNotFoundException, ParseException {
        Optional<User> userOptional = this.userRepository.findByEmail(principal.getName());
        if(userOptional.isEmpty()) {
            throw new UserNotFoundException("User not found");
        }

        validateGenerateTripDTO(generateTripDTO);

        String periodType = generateTripDTO.getPeriodType();
        if(periodType.equals(Constants.FIXED_PERIOD_TYPE)) {
            Trip trip = this.generateTripForFixedPeriodType(generateTripDTO, userOptional.get());
            if(trip == null) {
                return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
            }
            return new ResponseEntity<>(trip, HttpStatus.OK);
        } else if(periodType.equals(Constants.INDICATIVE_PERIOD_TYPE)) {
            Trip trip = this.generateTripForIndicativePeriodType(generateTripDTO, userOptional.get());
            if(trip == null) {
                return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
            }
            return new ResponseEntity<>(trip, HttpStatus.OK);
        }

        return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
    }

    private Trip generateTripForIndicativePeriodType(GenerateTripDTO generateTripDTO, User user)
            throws ResourceNotFoundException {
        int destinationId = generateTripDTO.getDestinationId();
        List<LocationTagDTO> tripTags = generateTripDTO.getTripTags();
        Destination destination = this.destinationService.findDestinationById(destinationId);

        List<String> daysNames = Utils.createListOfDaysNames(generateTripDTO.getNumberOfDays());

        Trip trip = new Trip();
        trip.setUser(user);
        trip.setDestination(destination);
        trip.setMonth(Utils.getLocalDateFromMonthName(generateTripDTO.getMonth()).getMonth().name());
        if(generateTripDTO.getMonth() != null && !generateTripDTO.getMonth().isEmpty()) {
            trip.setName(STR."Trip to \{destination.getName()} for \{generateTripDTO.getNumberOfDays()} days in \{generateTripDTO.getMonth()}");
        } else {
            trip.setName(STR."Trip to \{destination.getName()} for \{generateTripDTO.getNumberOfDays()} days");
        }
        trip.setPlannificationDays(new ArrayList<>());
        trip.setRecommendations(new ArrayList<>());
        trip.setNumberOfDays(generateTripDTO.getNumberOfDays());
        trip.setTripObjectivesTags(this.locationTagService.getAssociatedLocationTagsWithGivenDTO(generateTripDTO.getTripTags()));
        trip.setCreatedTimestamp(LocalDateTime.now());

        int maxNrOfObjectives = trip.getNumberOfDays() * generateTripDTO.getMaxNrOfObjectivesPerDay();
        Map<Location, List<LocationTag>> matchingLocations = getLocationsFromDestinationWhichMatchesTags(destination, tripTags);
        Map<String, List<Location>> locationsSplit = splitMatchingLocations(destination, matchingLocations, trip.getNumberOfDays(), maxNrOfObjectives);
        List<Location> attractionsToConsider = getOptimizedTour(destination, locationsSplit.get("matching_consider"));
        Map<Location, Boolean> bestRestaurantsSelection = createRestaurantsSelection(locationsSplit.get("best_restaurants"));
        Map<Integer, List<Integer>> dividingByDays = divideObjectivesBetweenDays(attractionsToConsider.size(), trip.getNumberOfDays());

        daysNames.forEach(day -> {
            int index = daysNames.indexOf(day);
            int firstLocationIndex = dividingByDays.get(index).getFirst();
            int lastLocationIndex = dividingByDays.get(index).getLast();

            TripPlannificationDay tripDay = new TripPlannificationDay();
            tripDay.setTrip(trip);
            tripDay.setName(day);
            tripDay.setLocations(new ArrayList<>());
            Date recommendedStartTime = getRecommendedStartTime(destination);
            Date recommendedEndTime = getRecommendedEndTime(destination);
            tripDay.setRecommendedTimeToStart(recommendedStartTime);
            tripDay.setRecommendedTimeToEnd(recommendedEndTime);
            trip.getPlannificationDays().add(tripDay);

            addObjectivesToTripDay(tripDay,
                    attractionsToConsider.subList(firstLocationIndex, lastLocationIndex),
                    bestRestaurantsSelection,
                    matchingLocations
            );

            computeObjectivesTimingsForTripDay(tripDay, false);
        });

        attractionsToConsider.forEach(matchingLocation -> addLocationTagsToTrip(trip, matchingLocation));

        getRecommendationsWhichAlsoMatching(trip, locationsSplit.get("matching_limited"), matchingLocations);

        getRecommendationsBasedOnPickedLocationsAssociation(trip, destination, attractionsToConsider, 5);

        getRecommendationsBasedOnPreviousTrips(trip, user, destination, attractionsToConsider, 5);

        getRecommendationsBasedOnNeverVisitedTag(trip, user, destination, attractionsToConsider, 5);

        return this.tripRepository.save(trip);
    }

    public Trip generateTripForFixedPeriodType(GenerateTripDTO generateTripDTO, User user)
            throws ResourceNotFoundException, ParseException {
        int destinationId = generateTripDTO.getDestinationId();
        List<LocationTagDTO> tripTags = generateTripDTO.getTripTags();
        Destination destination = this.destinationService.findDestinationById(destinationId);

        List<LocalDate> dates = createListOfDatesBetweenFirstDayAndLastDay(generateTripDTO.getFirstDay(),
                generateTripDTO.getLastDay());

        Trip trip = new Trip();
        trip.setUser(user);
        trip.setDestination(destination);
        trip.setMonth(dates.getFirst().getMonth().name());
        trip.setFirstDay(dates.getFirst());
        trip.setLastDay(dates.getLast());
        trip.setName(STR."Trip to \{destination.getName()}: \{dates.getFirst()} - \{dates.getLast()}");
        trip.setPlannificationDays(new ArrayList<>());
        trip.setRecommendations(new ArrayList<>());
        int numberOfDays = (int) ChronoUnit.DAYS.between(dates.getFirst(), dates.getLast()) + 1;
        trip.setNumberOfDays(numberOfDays);
        trip.setTripObjectivesTags(this.locationTagService.getAssociatedLocationTagsWithGivenDTO(generateTripDTO.getTripTags()));
        trip.setCreatedTimestamp(LocalDateTime.now());

        int maxNrOfObjectives = numberOfDays * generateTripDTO.getMaxNrOfObjectivesPerDay();

        Map<Location, List<LocationTag>> matchingLocations = getLocationsFromDestinationWhichMatchesTags(destination, tripTags);
        if(matchingLocations.isEmpty()) {
            return null;
        }
        Map<String, List<Location>> locationsSplit = splitMatchingLocations(destination, matchingLocations, numberOfDays, maxNrOfObjectives);
        List<Location> attractionsToConsider = getOptimizedTour(destination, locationsSplit.get("matching_consider"));
        Map<Location, Boolean> bestRestaurantsSelection = createRestaurantsSelection(locationsSplit.get("best_restaurants"));
        Map<Integer, List<Integer>> dividingByDays = divideObjectivesBetweenDays(attractionsToConsider.size(), numberOfDays);

        dates.forEach(date -> {
            int index = dates.indexOf(date);
            int firstLocationIndex = dividingByDays.get(index).getFirst();
            int lastLocationIndex = dividingByDays.get(index).getLast();

            TripPlannificationDay tripDay = new TripPlannificationDay();
            tripDay.setTrip(trip);
            tripDay.setName(STR."Day \{index + 1}");
            tripDay.setDate(date);
            tripDay.setLocations(new ArrayList<>());
            Date recommendedStartTime = getRecommendedStartTime(destination);
            Date recommendedEndTime = getRecommendedEndTime(destination);
            tripDay.setRecommendedTimeToStart(recommendedStartTime);
            tripDay.setRecommendedTimeToEnd(recommendedEndTime);
            trip.getPlannificationDays().add(tripDay);

            addObjectivesToTripDay(tripDay,
                    attractionsToConsider.subList(firstLocationIndex, lastLocationIndex),
                    bestRestaurantsSelection,
                    matchingLocations
            );

            computeObjectivesTimingsForTripDay(tripDay, false);
        });

        attractionsToConsider.forEach(matchingLocation -> addLocationTagsToTrip(trip, matchingLocation));

        getRecommendationsWhichAlsoMatching(trip, locationsSplit.get("matching_limited"), matchingLocations);

        getRecommendationsBasedOnPickedLocationsAssociation(trip, destination, attractionsToConsider, 5);

        getRecommendationsBasedOnPreviousTrips(trip, user, destination, attractionsToConsider, 5);

        getRecommendationsBasedOnNeverVisitedTag(trip, user, destination, attractionsToConsider, 5);

        return this.tripRepository.save(trip);
    }

    public Date getRecommendedStartTime(Destination destination) {
        if(destination.getSunrise() != null) {
            return Utils.addHoursToDate(destination.getSunrise(), 3);
        }
        return Utils.getDateObjectForHour(10, 30);
    }

    public Date getRecommendedEndTime(Destination destination) {
        if(destination.getSunset() != null) {
            return Utils.addHoursToDate(destination.getSunset(), -2);
        }
        return Utils.getDateObjectForHour(19, 30);
    }

    public List<Location> getOptimizedTour(Destination destination, List<Location> attractions) {
        if(attractions.size() <= 2) {
            return attractions;
        }

        Location startingLocation = getClosestLocationToCenter(destination, attractions);
        List<Location> attractionsReordered = new ArrayList<>();

        TSPGreedy tspGreedy = new TSPGreedy(attractions.indexOf(startingLocation), getMatrixCostForAttractions(attractions));
        tspGreedy.solve();

        tspGreedy.getTour().subList(0, tspGreedy.getTour().size() - 1).forEach(locationIndex -> {
            attractionsReordered.add(attractions.get(locationIndex));
        });

        return attractionsReordered;
    }

    public double[][] getMatrixCostForAttractions(List<Location> attractions) {
        int n = attractions.size();
        double[][] costMatrix = new double[n][n];
        for(int i = 0; i < n; i++) {
            for(int j = 0; j < n; j++) {
                if(i != j) {
                    costMatrix[i][j] = this.locationService
                            .getDistanceBetweenLocations(attractions.get(i), attractions.get(j));
                } else {
                    costMatrix[i][j] = 10000;
                }
            }
        }
        return costMatrix;
    }

    public Location getClosestLocationToCenter(Destination destination, List<Location> locations) {
        Map<Location, Double> locationsDistancesToCenter = new HashMap<>();
        locations.forEach(location ->
            locationsDistancesToCenter.put(location, Utils.haversineDistance(destination.getLatitude(),
                    destination.getLongitude(),
                    location.getLatitude(),
                    location.getLongitude()))
        );

        return locations
                .stream()
                .sorted(Comparator.comparingDouble(location -> locationsDistancesToCenter.get(location)))
                .toList()
                .getFirst();
    }

    public Map<Location, Boolean> createRestaurantsSelection(List<Location> restaurants) {
        Map<Location, Boolean> restaurantsSelection = new HashMap<>();
        restaurants.forEach(restaurant -> restaurantsSelection.put(restaurant, true));
        return restaurantsSelection;
    }

    public Map<Integer, List<Integer>> divideObjectivesBetweenDays(int numberOfObjectives, int numberOfDays) {
        Map<Integer, List<Integer>> schedule = new HashMap<>();

        for(int i = 0; i < numberOfDays; i++) {
            int firstLocationIndex = (int) (i * (double) numberOfObjectives / numberOfDays);
            int lastLocationIndex = (int) (Math.min(
                    (i + 1) * (double) numberOfObjectives / numberOfDays, numberOfObjectives)
            );
            schedule.put(i, List.of(firstLocationIndex, lastLocationIndex));
        }

        return schedule;
    }

    public Map<String, List<Location>> splitMatchingLocations(Destination destination,
                                                              Map<Location, List<LocationTag>> matchingLocations,
                                                              int nrOfDays,
                                                              int maxNrOfObjectives) {
        Map<String, List<Location>> destinationFilteredLocationsByPreferences = new HashMap<>();
        List<Location> matchingLocationsList = matchingLocations.keySet().stream().toList();
        Map<Location, LocationPopularityDTO> attractionsPopularity = this.locationService
                .getLocationsPopularityForDestination(matchingLocationsList);

        List<Location> matchingAttractionsSortedByPopularity = matchingLocationsList
                .stream()
                .filter(location -> location.getLocationType().getType().equalsIgnoreCase(Constants.ATTRACTION_TYPE))
                .sorted(Comparator.comparingDouble(location -> attractionsPopularity.get(location).getPopularityScore()).reversed())
                .toList();

        List<Location> locationsToConsider = limitListOfLocation(matchingAttractionsSortedByPopularity, maxNrOfObjectives);
        List<Location> matchingLocationsWithLowerPopularity = matchingAttractionsSortedByPopularity.subList(
            matchingAttractionsSortedByPopularity.indexOf(locationsToConsider.getLast()) + 1,
            matchingAttractionsSortedByPopularity.size()
        );

        destinationFilteredLocationsByPreferences.put("matching_consider", locationsToConsider);
        destinationFilteredLocationsByPreferences.put("matching_limited", matchingLocationsWithLowerPopularity);

        List<Location> destinationRestaurants = destination.getLocations()
                .stream()
                .filter(location -> location.getLocationType().getType().equalsIgnoreCase(Constants.RESTAURANT_TYPE))
                .toList();
        Map<Location, LocationPopularityDTO> restaurantsPopularity = this.locationService
                .getLocationsPopularityForDestination(destinationRestaurants);

        List<Location> restaurantsSortedByPopularity = destinationRestaurants
                .stream()
                .sorted(Comparator.comparingDouble(location -> restaurantsPopularity.get(location).getPopularityScore()).reversed())
                .toList();

        List<Location> bestRestaurants = limitListOfLocation(restaurantsSortedByPopularity, nrOfDays);
        destinationFilteredLocationsByPreferences.put("best_restaurants", bestRestaurants);

        return destinationFilteredLocationsByPreferences;
    }
    public void computeObjectivesTimingsForTripDay(TripPlannificationDay tripDay, boolean saveOption) {
        if(tripDay.getLocations() == null) {
            return;
        }

        if(tripDay.getLocations().isEmpty()) {
            return;
        }

        List<TripDayObjective> objectives = tripDay.getLocations()
                .stream()
                .sorted(Comparator.comparingInt(TripDayObjective::getPriority))
                .toList();

        if(objectives.size() == 1) {
            TripDayObjective currentObjective = objectives.getFirst();
            Date objectiveStartTime = Utils.addMinutesToDate(tripDay.getRecommendedTimeToStart(), Constants.TIMING_ERROR);
            Date objectiveEndTime = Utils.addMinutesToDate(objectiveStartTime, currentObjective.getMinutesPlanned());
            currentObjective.setStartTime(objectiveStartTime);
            currentObjective.setEndTime(objectiveEndTime);
            currentObjective.setNextObjectiveName("");
            currentObjective.setTravelLengthToNextObjective(0);
            currentObjective.setTravelTimeToNextObjective(0);
        } else {
            for (int i = 0; i < objectives.size(); i++) {
                TripDayObjective currentObjective = objectives.get(i);
                if (i == 0) {
                    TripDayObjective nextObjective = objectives.get(1);
                    Date objectiveStartTime = Utils.addMinutesToDate(tripDay.getRecommendedTimeToStart(), Constants.TIMING_ERROR);
                    Date objectiveEndTime = Utils.addMinutesToDate(objectiveStartTime, currentObjective.getMinutesPlanned());
                    currentObjective.setStartTime(objectiveStartTime);
                    currentObjective.setEndTime(objectiveEndTime);
                    TwoPointsRouteDTO route = getRoutingDetailsBetweenTwoPoints(currentObjective, nextObjective);
                    currentObjective.setTravelTimeToNextObjective(route.getTime());
                    currentObjective.setTravelLengthToNextObjective(route.getDistance());
                    currentObjective.setNextObjectiveName(nextObjective.getLocation().getName());
                    continue;
                }

                if (i != objectives.size() - 1) {
                    TripDayObjective prevObjective = objectives.get(i - 1);
                    TripDayObjective nextObjective = objectives.get(i + 1);
                    Date objectiveStartTime = Utils.addMinutesToDate(prevObjective.getEndTime(), Constants.TIMING_ERROR +
                            prevObjective.getTravelTimeToNextObjective());
                    Date objectiveEndTime = Utils.addMinutesToDate(objectiveStartTime, currentObjective.getMinutesPlanned());
                    currentObjective.setStartTime(objectiveStartTime);
                    currentObjective.setEndTime(objectiveEndTime);
                    TwoPointsRouteDTO route = getRoutingDetailsBetweenTwoPoints(currentObjective, nextObjective);
                    currentObjective.setTravelTimeToNextObjective(route.getTime());
                    currentObjective.setTravelLengthToNextObjective(route.getDistance());
                    currentObjective.setNextObjectiveName(nextObjective.getLocation().getName());
                    continue;
                }

                if (i == objectives.size() - 1) {
                    TripDayObjective prevObjective = objectives.get(i - 1);
                    Date objectiveStartTime = Utils.addMinutesToDate(prevObjective.getEndTime(), Constants.TIMING_ERROR +
                            prevObjective.getTravelTimeToNextObjective());
                    Date objectiveEndTime = Utils.addMinutesToDate(objectiveStartTime, currentObjective.getMinutesPlanned());
                    currentObjective.setStartTime(objectiveStartTime);
                    currentObjective.setEndTime(objectiveEndTime);
                    currentObjective.setTravelTimeToNextObjective(0);
                    currentObjective.setTravelLengthToNextObjective(0);
                    currentObjective.setNextObjectiveName("");
                }
            }
        }

        if(saveOption) {
            objectives.forEach(tripDayObjective -> this.tripDayObjectiveRepository.save(tripDayObjective));
        }
    }

    public TwoPointsRouteDTO getRoutingDetailsBetweenTwoPoints(TripDayObjective firstObjective, TripDayObjective secondObjective) {
        GeoLocationDTO currentObjectiveLoc = new GeoLocationDTO();
        currentObjectiveLoc.setLatitude(firstObjective.getLocation().getLatitude());
        currentObjectiveLoc.setLongitude(firstObjective.getLocation().getLongitude());

        GeoLocationDTO nextObjectiveLoc = new GeoLocationDTO();
        nextObjectiveLoc.setLatitude(secondObjective.getLocation().getLatitude());
        nextObjectiveLoc.setLongitude(secondObjective.getLocation().getLongitude());

        Optional<ArcgisRouteResponseDTO> routeBetweenMultiplePointsOptional = this.arcgisService
                .getRoutingBetweenLocations(List.of(currentObjectiveLoc, nextObjectiveLoc));

        TwoPointsRouteDTO routeDTO = new TwoPointsRouteDTO();
        routeBetweenMultiplePointsOptional.ifPresent(routeBetweenMultiplePoints -> {
            Double travelTime = routeBetweenMultiplePoints.getRoutes()
                    .getFeatures().getFirst().getAttributes().getTotalTravelTime();
            Double travelDistance = routeBetweenMultiplePoints.getRoutes()
                    .getFeatures().getFirst().getAttributes().getTotalKilometers();
            if(travelTime != null) {
                routeDTO.setTime(travelTime.intValue());
            }

            if(travelDistance != null) {
                routeDTO.setDistance(travelDistance.intValue());
            }
        });

        return routeDTO;
    }

    public void addObjectivesToTripDay(TripPlannificationDay tripDay,
                                       List<Location> locations,
                                       Map<Location, Boolean> bestRestaurantsSelection,
                                       Map<Location, List<LocationTag>> matchingLocations) {
        if(locations.isEmpty()) {
            Location firstRestaurantAvailable = bestRestaurantsSelection
                    .keySet()
                    .stream()
                    .filter(bestRestaurantsSelection::get)
                    .toList()
                    .getFirst();
            bestRestaurantsSelection.replace(firstRestaurantAvailable, false);
            TripDayObjective restaurantObjective = createRestaurantTripDayObjective(tripDay, firstRestaurantAvailable);
            tripDay.getLocations().add(restaurantObjective);
        } else {
            locations.forEach(matchingLocation -> {
                TripDayObjective dayObjective = new TripDayObjective();
                dayObjective.setTripDay(tripDay);
                dayObjective.setType(matchingLocation.getLocationType().getName());
                dayObjective.setPriority(tripDay.getLocations().size());
                dayObjective.setReasonForChoosing(STR."Because you wanted to experience \{this.locationTagService
                        .extractNamesFromListOfLocationTags(matchingLocations.get(matchingLocation))}");
                dayObjective.setLocation(matchingLocation);
                if (matchingLocation.getLocationType().getType().equalsIgnoreCase(Constants.ATTRACTION_TYPE)) {
                    dayObjective.setMinutesPlanned(Constants.ATTRACTION_MINUTES_ALLOCATED);
                } else if (matchingLocation.getLocationType().getType().equalsIgnoreCase(Constants.RESTAURANT_TYPE)) {
                    dayObjective.setMinutesPlanned(Constants.RESTAURANT_MINUTES_ALLOCATED);
                } else {
                    dayObjective.setMinutesPlanned(30);
                }
                tripDay.getLocations().add(dayObjective);

                if(dayObjective.getPriority() == 0) {
                    List<Location> restaurantsAvailable = bestRestaurantsSelection
                            .keySet()
                            .stream()
                            .filter(bestRestaurantsSelection::get)
                            .toList();

                    Location closestRestaurantToFirstObjective = this.locationService
                            .getClosestLocationFromList(dayObjective.getLocation(), restaurantsAvailable);
                    if(closestRestaurantToFirstObjective != null) {
                        bestRestaurantsSelection.replace(closestRestaurantToFirstObjective, false);
                        TripDayObjective restaurantObjective = createRestaurantTripDayObjective(tripDay,
                                closestRestaurantToFirstObjective);
                        tripDay.getLocations().add(restaurantObjective);
                    }
                }
            });
        }

    }

    public TripDayObjective createRestaurantTripDayObjective(TripPlannificationDay tripDay, Location firstRestaurantAvailable) {
        TripDayObjective restaurantObjective = new TripDayObjective();
        restaurantObjective.setTripDay(tripDay);
        restaurantObjective.setType(firstRestaurantAvailable.getLocationType().getName());
        restaurantObjective.setPriority(tripDay.getLocations().size());
        if(firstRestaurantAvailable.getCuisine() != null) {
            restaurantObjective.setReasonForChoosing(STR."Lunch break with \{firstRestaurantAvailable.getCuisine()} specific");
        } else {
            restaurantObjective.setReasonForChoosing("Lunch break");
        }
        restaurantObjective.setLocation(firstRestaurantAvailable);
        restaurantObjective.setMinutesPlanned(Constants.RESTAURANT_MINUTES_ALLOCATED);
        return restaurantObjective;
    }

    public void getRecommendationsWhichAlsoMatching(Trip trip,
                                                       List<Location> locationsMatchingPreferences,
                                                       Map<Location, List<LocationTag>> matchingLocations) {
        locationsMatchingPreferences
                .forEach(recommendedLocation -> {
                    Recommendation recommendation = new Recommendation();
                    recommendation.setTrip(trip);
                    recommendation.setLocation(recommendedLocation);
                    recommendation.setType(Constants.MATCHING_CATEGORIES_RECOMMENDATION_TYPE);
                    List<String> tagsMatching = matchingLocations.get(recommendedLocation)
                        .stream()
                        .map(LocationTag::getName)
                        .toList();
                    recommendation.setRecommendationReason(STR."Also matching your preferences: \{Utils.joinListOfStrings(tagsMatching,
                            ", ")}, but has lower popularity.");
                    trip.getRecommendations().add(recommendation);
                });
    }

    public void getRecommendationsBasedOnNeverVisitedTag(Trip trip, User user, Destination destination,
                                                         List<Location> pickedLocations, int limit) {
        Set<Integer> alreadyPlannedLocationsIds = pickedLocations
                .stream()
                .map(Location::getId)
                .collect(Collectors.toSet());
        Map<Location, Set<LocationTag>> recommendations = new HashMap<>();
        Set<LocationTag> destinationTags = destination.getLocations()
                .stream()
                .flatMap(location -> location.getLocationTags().stream())
                .collect(Collectors.toSet());

        Set<LocationTag> visitedTags = user.getTrips()
                .stream()
                .filter(previousTrip -> previousTrip.getId() != trip.getId())
                .flatMap(previousTrip -> previousTrip.getTripObjectivesTags().stream())
                .collect(Collectors.toSet());

        Set<LocationTag> neverVisitedTags = new HashSet<>(destinationTags);
        neverVisitedTags.removeAll(visitedTags);

        destination.getLocations().stream().sorted(Comparator.comparing(Location::getPopularity))
                .forEach(location -> {
                    if(recommendations.size() > limit) return;

                    if(this.locationService.checkIfLocationIsHotelOrRestaurant(location)) {
                        return;
                    }

                    if(!alreadyPlannedLocationsIds.contains(location.getId())) {
                        if(!recommendations.containsKey(location)) {
                            Set<LocationTag> locationTagsSet = new HashSet<>(location.getLocationTags());
                            locationTagsSet.retainAll(neverVisitedTags);
                            if(!locationTagsSet.isEmpty()) {
                                recommendations.put(location, locationTagsSet);
                            }
                        }
                    }
                });

        recommendations.keySet()
                .forEach(recommendedLocation -> {
                    Recommendation recommendation = new Recommendation();
                    recommendation.setTrip(trip);
                    recommendation.setLocation(recommendedLocation);
                    recommendation.setType(Constants.LOCATION_TAG_NEVER_EXPERIENCED_RECOMMENDATION_TYPE);
                    List<String> sourceLocationTags = recommendations.get(recommendedLocation).stream().map(LocationTag::getName).toList();
                    recommendation.setRecommendationReason(STR."Because you never experienced \{Utils.joinListOfStrings(sourceLocationTags, ", ")}");
                    trip.getRecommendations().add(recommendation);
                });
    }

    public void getRecommendationsBasedOnPreviousTrips(Trip trip, User user, Destination destination,
                                                       List<Location> pickedLocations, int limit) {
        Map<Location, List<Trip>> recommendations = new HashMap<>();
        if(user.getTrips().isEmpty()) {
            return;
        }

        List<Trip> previousTrips = user.getTrips().stream().filter(prevTrip -> prevTrip.getId() != trip.getId()).toList();
        previousTrips.forEach(prevTrip -> {
            Map<Location, List<LocationTag>> recommendedLocationFromTrip =
                    getRecommendationsFromDestinationBasedOnProvidedLocationCommonTags(destination, prevTrip.getTripObjectivesTags(),
                            pickedLocations, limit);
            recommendedLocationFromTrip.keySet()
                    .forEach(locationRecommended -> {
                        if(!recommendations.containsKey(locationRecommended)) {
                            recommendations.put(locationRecommended, new ArrayList<>());
                        }
                        recommendations.get(locationRecommended).add(prevTrip);
                    });

        });

        recommendations.keySet()
                .forEach(recommendedLocation -> {
                    Recommendation recommendation = new Recommendation();
                    recommendation.setTrip(trip);
                    recommendation.setLocation(recommendedLocation);
                    recommendation.setType(Constants.PREVIOUS_TRIPS_ASSOCIATION_RECOMMENDATION_TYPE);
                    List<String> sourceTripsNames = recommendations.get(recommendedLocation).stream().map(Trip::getName).toList();
                    recommendation.setRecommendationReason(STR."Based on your previous trips \{Utils.joinListOfStrings(sourceTripsNames, ", ")}");
                    trip.getRecommendations().add(recommendation);
                });

    }

    public void getRecommendationsBasedOnPickedLocationsAssociation(Trip trip, Destination destination,
                                                                    List<Location> pickedLocations, int limit) {
        Map<Location, List<Location>> recommendations = new HashMap<>();;

        pickedLocations
                .forEach(matchingLocation -> {
                    Map<Location, List<LocationTag>> recommendationBasedOnOneLocation =
                            getRecommendationsFromDestinationBasedOnProvidedLocationCommonTags(
                                    destination,
                                    matchingLocation.getLocationTags(),
                                    pickedLocations,
                                    limit
                            );

                    recommendationBasedOnOneLocation.keySet()
                            .forEach(locationRecommended -> {
                                if(recommendations.size() > limit) return;
                                if(!recommendations.containsKey(locationRecommended)) {
                                    recommendations.put(locationRecommended, new ArrayList<>());
                                }
                                recommendations.get(locationRecommended).add(matchingLocation);
                            });
                });

        recommendations.keySet()
                .forEach(recommendedLocation -> {
                    Recommendation recommendation = new Recommendation();
                    recommendation.setTrip(trip);
                    recommendation.setLocation(recommendedLocation);
                    recommendation.setType(Constants.LOCATION_ASSOCIATION_RECOMMENDATION_TYPE);
                    List<String> sourceLocationsNames = recommendations.get(recommendedLocation).stream().map(Location::getName).toList();
                    recommendation.setRecommendationReason(STR."Because you will visit \{Utils.joinListOfStrings(sourceLocationsNames, ", ")}");
                    trip.getRecommendations().add(recommendation);
                });
    }

    public Map<Location, List<LocationTag>> getRecommendationsFromDestinationBasedOnProvidedLocationCommonTags(
            Destination destination, List<LocationTag> providedLocationTags, List<Location> alreadyPlannedLocations,
            int limit) {
        Set<Integer> alreadyPlannedLocationsIds = alreadyPlannedLocations
                .stream()
                .map(Location::getId)
                .collect(Collectors.toSet());

        Map<Location, List<LocationTag>> matchingLocations = new HashMap<>();
        destination.getLocations().stream().sorted(Comparator.comparing(Location::getPopularity).reversed())
                .forEach(locationCandidate -> {
            if(matchingLocations.size() > limit) return;
            if(alreadyPlannedLocationsIds.contains(locationCandidate.getId())) {
                return;
            }

            List<LocationTag> locationTagsMatching = this.locationService
                    .getMatchingLocationTagsFromLocationWithTagsProvided(providedLocationTags, locationCandidate);
            if(!locationTagsMatching.isEmpty()) {
                matchingLocations.put(locationCandidate, locationTagsMatching);
            }
        });

        return matchingLocations;
    }

    public Map<Location, List<LocationTag>> getLocationsFromDestinationWhichMatchesTags(Destination destination,
                                                                                        List<LocationTagDTO> tripTags) {
        Map<Location, List<LocationTag>> matchingLocations = new HashMap<>();
        destination.getLocations()
                .forEach(location -> {
                    List<LocationTag> locationTagsMatching = this.locationService
                            .getMatchingLocationTagsFromLocation(tripTags, location);
                    if(!locationTagsMatching.isEmpty()) {
                        matchingLocations.put(location, locationTagsMatching);
                    }
                });

        return matchingLocations;
    }

    public void addLocationTagsToTrip(Trip trip, Location location) {
        location.getLocationTags()
                .forEach(locationTag -> {
                    if(!trip.getTripObjectivesTags().contains(locationTag)) {
                        trip.getTripObjectivesTags().add(locationTag);
                    }
                });
    }

    public void updateTripObjectivesTags(Trip trip) {
        Set<LocationTag> tripTags = trip.getPlannificationDays()
                .stream()
                .flatMap(tripPlannificationDay -> tripPlannificationDay.getLocations()
                        .stream()
                        .flatMap(tripDayObjective -> tripDayObjective.getLocation().getLocationTags().stream())
                        .collect(Collectors.toSet())
                        .stream())
                .collect(Collectors.toSet());
        tripTags.forEach(tripTag -> {
            if(!trip.getTripObjectivesTags().contains(tripTag)) {
                trip.getTripObjectivesTags().add(tripTag);
            }
        });

        this.tripRepository.save(trip);
    }

    public ResponseEntity<Trip> getTripById(int id) throws ResourceNotFoundException {
        Optional<Trip> tripOptional = this.tripRepository.findById(id);
        if(tripOptional.isEmpty()) {
            throw new ResourceNotFoundException(STR."Trip with id \{id} not found");
        }

        return new ResponseEntity<>(tripOptional.get(), HttpStatus.OK);
    }

    public void removeTripTagsAssociatedToLocation(Trip trip, Location location) {
        List<Location> tripLocations = trip.getPlannificationDays()
                .stream()
                .flatMap(tripPlannificationDay -> tripPlannificationDay.getLocations()
                        .stream()
                        .map(TripDayObjective::getLocation))
                .toList();

        location.getLocationTags()
                .forEach(locationTag -> {
                    List<Location> locationsWithTag = tripLocations.stream()
                            .filter(tripLocation -> tripLocation.getLocationTags().contains(locationTag))
                            .toList();
                    if(locationsWithTag.isEmpty()) {
                        System.out.println("removing " + locationTag.getName() + " because is associated only with the trip objective deleted");
                        trip.getTripObjectivesTags().remove(locationTag);
                    }
                });
    }

    public ResponseEntity<ResponseDTO> deleteTripById(int id, Principal principal) throws ResourceNotFoundException,
            UserNotFoundException, UnauthorizedOperationException {
        Optional<Trip> tripOptional = this.tripRepository.findById(id);
        if(tripOptional.isEmpty()) {
            throw new ResourceNotFoundException(STR."Trip with id \{id} not found");
        }

        Trip trip = tripOptional.get();

        Optional<User> userOptional = this.userRepository.findByEmail(principal.getName());
        if(userOptional.isEmpty()) {
            throw new UserNotFoundException("Couldn't delete trip because your user can't be found");
        }

        User user = userOptional.get();
        if(trip.getUser().getId() != user.getId()) {
            throw new UnauthorizedOperationException("You can't delete another user trip");
        }

        this.tripRepository.deleteById(id);
        return new ResponseEntity<>(new ResponseDTO(STR."Trip \{trip.getId()} deleted successfully"), HttpStatus.OK);
    }

    public ResponseEntity<Trip> updatePlannification(int id, UpdatePlannificationDTO updatePlannificationDTO,
                                                     Principal principal)
            throws ResourceNotFoundException, InsufficientPostDataException, UserNotFoundException,
            UnauthorizedOperationException, BadUpdateDetailsProvidedException {
        Optional<Trip> tripOptional = this.tripRepository.findById(id);
        if(tripOptional.isEmpty()) {
            throw new ResourceNotFoundException(STR."Trip with id \{id} couldn't be updated");
        }
        Trip trip = tripOptional.get();

        Optional<User> userOptional = this.userRepository.findByEmail(principal.getName());
        if(userOptional.isEmpty()) {
            throw new UserNotFoundException("Trip couldn't be updated, user not found");
        }
        User user = userOptional.get();
        if(trip.getUser().getId() != user.getId()) {
            throw new UnauthorizedOperationException("You can't update a trip plan which belongs to another user");
        }

        if(!checkUpdateTripPlannificationDTO(updatePlannificationDTO)) {
            throw new InsufficientPostDataException("Post request body must contain sourcePlanDayId, " +
                    "destinationPlanDayId, movedObjectiveId, afterObjectiveId.");
        }

        Optional<TripPlannificationDay> sourcePlanDayOptional = this.tripPlannificationDayRepository
                .findById(updatePlannificationDTO.getSourcePlanDayId());
        Optional<TripPlannificationDay> destinationPlanDayOptional = this.tripPlannificationDayRepository
                .findById(updatePlannificationDTO.getDestinationPlanDayId());
        Optional<TripDayObjective> movedTripObjectiveOptional = this.tripDayObjectiveRepository
                .findById(updatePlannificationDTO.getMovedObjectiveId());


        if(sourcePlanDayOptional.isEmpty() || destinationPlanDayOptional.isEmpty() || movedTripObjectiveOptional.isEmpty()) {
            throw new ResourceNotFoundException("One of the resources needed wasn't found");
        }

        TripPlannificationDay sourcePlanDay = sourcePlanDayOptional.get();
        TripPlannificationDay destinationPlanDay = destinationPlanDayOptional.get();
        TripDayObjective movedTripObjective = movedTripObjectiveOptional.get();

        if(sourcePlanDay.getId() == destinationPlanDay.getId()) {
            System.out.println("We need to swap into the same plannification day");
            if(!trip.getPlannificationDays().contains(sourcePlanDay)) {
                throw new BadUpdateDetailsProvidedException("The plannification day doesn't belong to the trip wanted to be updated");
            }

            if(!sourcePlanDay.getLocations().contains(movedTripObjective)) {
                throw new BadUpdateDetailsProvidedException("The tripObjective needed to be moved doesn't belong to " +
                        "the source day specified");
            }

            if(updatePlannificationDTO.getAfterObjectiveId() == -1) {
                // it means that we need to move the objective at the beginning
                if(movedTripObjective.getPriority() != 0) {
                    sourcePlanDay.getLocations()
                            .forEach(tripDayObjective -> {
                                if(tripDayObjective.getId() != movedTripObjective.getId()) {
                                    int oldPriority = tripDayObjective.getPriority();
                                    tripDayObjective.setPriority(oldPriority + 1);
                                    this.tripDayObjectiveRepository.save(tripDayObjective);
                                }
                            });
                    movedTripObjective.setPriority(0);
                    this.tripDayObjectiveRepository.save(movedTripObjective);
                }
            } else {
                Optional<TripDayObjective> afterObjectiveOptional = this.tripDayObjectiveRepository
                    .findById(updatePlannificationDTO.getAfterObjectiveId());
                if(afterObjectiveOptional.isEmpty()) {
                    throw new ResourceNotFoundException("After objective couldn't be found");
                }
                TripDayObjective afterObjective = afterObjectiveOptional.get();
                if(!sourcePlanDay.getLocations().contains(afterObjective)) {
                    throw new ResourceNotFoundException("After objective doesn't belong to the same plannification day");
                }

                int afterObjectivePriority = afterObjective.getPriority();
                int movedObjectivePriority = movedTripObjective.getPriority();
                if(afterObjectivePriority < movedObjectivePriority) {
                    sourcePlanDay.getLocations()
                            .forEach(tripDayObjective -> {
                                if(tripDayObjective.getPriority() > afterObjectivePriority &&
                                    tripDayObjective.getPriority() < movedObjectivePriority) {
                                    int priority = tripDayObjective.getPriority();
                                    tripDayObjective.setPriority(priority + 1);
                                    this.tripDayObjectiveRepository.save(tripDayObjective);
                                }
                            });
                    movedTripObjective.setPriority(afterObjectivePriority + 1);
                    this.tripDayObjectiveRepository.save(movedTripObjective);
                } else if(afterObjectivePriority > movedObjectivePriority) {
                    sourcePlanDay.getLocations()
                            .forEach(tripDayObjective -> {
                                int priority = tripDayObjective.getPriority();
                                if(priority > movedObjectivePriority && priority <= afterObjectivePriority) {
                                    tripDayObjective.setPriority(priority - 1);
                                    this.tripDayObjectiveRepository.save(tripDayObjective);
                                }
                            });
                    movedTripObjective.setPriority(afterObjectivePriority);
                    this.tripDayObjectiveRepository.save(movedTripObjective);
                }
            }

            computeObjectivesTimingsForTripDay(sourcePlanDay, true);
        } else {
            if(!sourcePlanDay.getLocations().contains(movedTripObjective)) {
                throw new BadUpdateDetailsProvidedException("The tripObjective needed to be moved doesn't belong to " +
                        "the source day specified");
            }

            if(updatePlannificationDTO.getAfterObjectiveId() == -1) {
                int movedObjectivePriority = movedTripObjective.getPriority();
                sourcePlanDay.getLocations()
                        .forEach(tripDayObjective -> {
                            int priority = tripDayObjective.getPriority();
                            if(tripDayObjective.getPriority() > movedObjectivePriority) {
                                tripDayObjective.setPriority(priority - 1);
                                this.tripDayObjectiveRepository.save(tripDayObjective);
                            }
                        });
                sourcePlanDay.getLocations().remove(movedTripObjective);
                destinationPlanDay.getLocations()
                        .forEach(tripDayObjective -> {
                            int priority = tripDayObjective.getPriority();
                            tripDayObjective.setPriority(priority + 1);
                            this.tripDayObjectiveRepository.save(tripDayObjective);
                        });
                movedTripObjective.setPriority(0);
                movedTripObjective.setTripDay(destinationPlanDay);
                destinationPlanDay.getLocations().add(movedTripObjective);
                this.tripDayObjectiveRepository.save(movedTripObjective);
                this.tripPlannificationDayRepository.save(sourcePlanDay);
            } else {
                Optional<TripDayObjective> afterObjectiveOptional = this.tripDayObjectiveRepository
                        .findById(updatePlannificationDTO.getAfterObjectiveId());
                if(afterObjectiveOptional.isEmpty()) {
                    throw new ResourceNotFoundException("After objective couldn't be found");
                }
                TripDayObjective afterObjective = afterObjectiveOptional.get();
                if(!destinationPlanDay.getLocations().contains(afterObjective)) {
                    throw new ResourceNotFoundException("After objective doesn't belong to the plannification day specified");
                }
                int movedObjectivePriority = movedTripObjective.getPriority();
                int afterObjectivePriority = afterObjective.getPriority();
                sourcePlanDay.getLocations()
                        .forEach(tripDayObjective -> {
                            int priority = tripDayObjective.getPriority();
                            if(tripDayObjective.getPriority() > movedObjectivePriority) {
                                tripDayObjective.setPriority(priority - 1);
                                this.tripDayObjectiveRepository.save(tripDayObjective);
                            }
                        });
                sourcePlanDay.getLocations().remove(movedTripObjective);

                destinationPlanDay.getLocations()
                        .forEach(tripDayObjective -> {
                            int priority = tripDayObjective.getPriority();
                            if(tripDayObjective.getPriority() > afterObjectivePriority) {
                                tripDayObjective.setPriority(priority + 1);
                                this.tripDayObjectiveRepository.save(tripDayObjective);
                            }
                        });
                movedTripObjective.setPriority(afterObjectivePriority + 1);
                movedTripObjective.setTripDay(destinationPlanDay);
                destinationPlanDay.getLocations().add(movedTripObjective);
                this.tripDayObjectiveRepository.save(movedTripObjective);
                this.tripPlannificationDayRepository.save(sourcePlanDay);
            }

            computeObjectivesTimingsForTripDay(sourcePlanDay, true);
            computeObjectivesTimingsForTripDay(destinationPlanDay, true);
        }

        return new ResponseEntity<>(updateTrip(trip), HttpStatus.OK);
    }

    public Trip updateTrip(Trip trip) {
        return this.tripRepository.save(trip);
    }

    public void deleteLocationFromTripRecommendationsIfPresent(Trip trip, Location location) {
        if(!checkIfLocationPresentInRecommendations(trip, location)) {
            return;
        }

        List<Recommendation> recommendationsToRemove = new ArrayList<>();
        trip.getRecommendations()
                .forEach(recommendation -> {
                    if(recommendation.getLocation().getId() == location.getId()) {
                        recommendationsToRemove.add(recommendation);
                    }
                }
        );
        recommendationsToRemove.forEach(recommendation -> {
                trip.getRecommendations().remove(recommendation);
                this.recommendationRepository.deleteById(recommendation.getId());
            }
        );
    }

    public boolean checkIfLocationPresentInRecommendations(Trip trip, Location location) {
        return !trip.getRecommendations()
                .stream().filter(recommendation -> recommendation.getLocation().getId() == location.getId())
                .toList().isEmpty();
    }

    public void validateGenerateTripDTO(GenerateTripDTO generateTripDTO)
            throws InsufficientPostDataException {
        if(generateTripDTO.getDestinationId() == null) {
            throw new InsufficientPostDataException("destinationId must be specified in order to generate a trip.");
        }

        if(generateTripDTO.getPeriodType() == null || generateTripDTO.getPeriodType().isEmpty()) {
            throw new InsufficientPostDataException("periodType must be specified, options available are: fixed(exact days) " +
                    "and indicative(number of days and optional month).");
        }

        if(generateTripDTO.getMaxNrOfObjectivesPerDay() == null) {
            generateTripDTO.setMaxNrOfObjectivesPerDay(Constants.MAXIMUM_OBJECTIVES_PER_DAY);
        }

    }

    public boolean checkUpdateTripPlannificationDTO(UpdatePlannificationDTO updatePlannificationDTO) {
        return updatePlannificationDTO.getSourcePlanDayId() != null && updatePlannificationDTO.getDestinationPlanDayId() != null &&
                updatePlannificationDTO.getMovedObjectiveId() != null && updatePlannificationDTO.getAfterObjectiveId() != null;
    }

    public List<Location> limitListOfLocation(List<Location> fullLocationsList, int maxNrOfLocations) {
        if(fullLocationsList == null) {
            return new ArrayList<>();
        }

        if(fullLocationsList.isEmpty()) {
            return new ArrayList<>();
        }

        if(fullLocationsList.size() <= maxNrOfLocations) {
            return fullLocationsList;
        }

        return fullLocationsList.subList(0, maxNrOfLocations);
    }

    public ResponseEntity<TripsByCriteriasDTO> getLatestTrips() {
        List<Trip> trips = this.tripRepository.findLatestTrips();
        List<Trip> latestTrips = trips.subList(0, Math.min(trips.size(), 5));

        TripsByCriteriasDTO tripsByCriteriasDTO = new TripsByCriteriasDTO();
        tripsByCriteriasDTO.setTrips(new ArrayList<>());

        TripsWithCriteriaDTO tripsWithCriteriaDTO = new TripsWithCriteriaDTO();
        tripsWithCriteriaDTO.setCriteria("latest");
        tripsWithCriteriaDTO.setTrips(new ArrayList<>());
        latestTrips.forEach(trip -> {
            TripMinDetailsDTO tripMinDetailsDTO = createTripMinDetailsDTOFromTripEntity(trip);
            tripsWithCriteriaDTO.getTrips().add(tripMinDetailsDTO);
        });

        tripsByCriteriasDTO.getTrips().add(tripsWithCriteriaDTO);

        return new ResponseEntity<>(tripsByCriteriasDTO, HttpStatus.OK);
    }

    public TripMinDetailsDTO createTripMinDetailsDTOFromTripEntity(Trip trip) {
        RatingDTO tripRating = reviewService.createRatingDTOForTrip(trip);
        TripMinDetailsDTO tripMinDetailsDTO = new TripMinDetailsDTO();
        tripMinDetailsDTO.setTripId(trip.getId());
        tripMinDetailsDTO.setUserId(trip.getUser().getId());
        tripMinDetailsDTO.setName(trip.getName());
        tripMinDetailsDTO.setAddressPath(trip.getDestination().getAddressPath());
        tripMinDetailsDTO.setRating(tripRating.getRating());
        Optional<LocationImage> destinationImageOptional = destinationService.getDestinationImage(trip.getDestination());
        destinationImageOptional.ifPresent(locationImage -> tripMinDetailsDTO.setImageSource(locationImage.getSource()));
        if(trip.getFirstDay() == null) {
            tripMinDetailsDTO.setPeriodType(Constants.INDICATIVE_PERIOD_TYPE);
            tripMinDetailsDTO.setNrOfDays(trip.getNumberOfDays());
            if(trip.getMonth() != null) {
                tripMinDetailsDTO.setMonth(trip.getMonth());
            }
        } else {
            tripMinDetailsDTO.setPeriodType(Constants.FIXED_PERIOD_TYPE);
            tripMinDetailsDTO.setFirstDay(trip.getFirstDay().toString());
            tripMinDetailsDTO.setLastDay(trip.getLastDay().toString());
            tripMinDetailsDTO.setNrOfDays(trip.getNumberOfDays());
            tripMinDetailsDTO.setMonth(trip.getMonth());
        }
        if(trip.getCreatedTimestamp() == null) {
            tripMinDetailsDTO.setTiming("");
        } else {
            tripMinDetailsDTO.setTiming(Utils.getDifferenceBetweenTimings(trip.getCreatedTimestamp(),
                    LocalDateTime.now()));
        }

        if (trip.getUser() != null) {
            if(trip.getUser().getFirstName() == null || trip.getUser().getLastName() == null) {
                tripMinDetailsDTO.setUsername("Unknown user");
            } else {
                tripMinDetailsDTO.setUsername(STR."\{trip.getUser().getFirstName()} \{trip.getUser().getLastName()}");
            }
        }

        List<TripDayMinDetailsDTO> tripDays = trip.getPlannificationDays()
                .stream()
                .map(tripPlannificationDay -> {
                    TripDayMinDetailsDTO tripDayMinDetailsDTO = new TripDayMinDetailsDTO();
                    tripDayMinDetailsDTO.setDayId(tripPlannificationDay.getId());
                    tripDayMinDetailsDTO.setDayName(tripPlannificationDay.getName());
                    return tripDayMinDetailsDTO;
                })
                .toList();

        tripMinDetailsDTO.setTripDays(tripDays);
        return tripMinDetailsDTO;
    }

    public ResponseEntity<TripsDTO> getUserTripsForDestination(int destinationId, Principal principal)
            throws UserNotFoundException {
        Optional<User> userOptional = this.userRepository.findByEmail(principal.getName());
        if(userOptional.isEmpty()) {
            throw new UserNotFoundException("Your user was not been found");
        }

        User user = userOptional.get();

        List<Trip> trips = user.getTrips()
                .stream()
                .filter(trip -> trip.getDestination().getId() == destinationId)
                .toList();

        List<TripMinDetailsDTO> tripsDTOs = trips
                .stream()
                .map(this::createTripMinDetailsDTOFromTripEntity)
                .toList();

        TripsDTO tripsDTO = new TripsDTO();
        tripsDTO.setTrips(tripsDTOs);

        return new ResponseEntity<>(tripsDTO, HttpStatus.OK);
    }

    public ResponseEntity<TripPlannificationDay> getTripDayPlannification(int id)
            throws ResourceNotFoundException {
        Optional<TripPlannificationDay> tripPlannificationDayOptional = this.tripPlannificationDayRepository.findById(id);
        if(tripPlannificationDayOptional.isEmpty()) {
            throw new ResourceNotFoundException(STR."Plannification day with id \{id} not found.");
        }

        return new ResponseEntity<>(tripPlannificationDayOptional.get(), HttpStatus.OK);
    }

    public ResponseEntity<TripRecommendationsDTO> getTripAttractionsRecommendations(int id)
            throws ResourceNotFoundException {
        Optional<Trip> tripOptional = this.tripRepository.findById(id);
        if(tripOptional.isEmpty()) {
            throw new ResourceNotFoundException(STR."Trip with id \{id} couldn't be found");
        }
        TripRecommendationsDTO tripRecommendationsDTO = new TripRecommendationsDTO();
        tripRecommendationsDTO.setRecommendationsByCriteria(new ArrayList<>());

        Map<String, List<Recommendation>> recommendationsByCriteria = new HashMap<>();
        tripOptional.get()
                .getRecommendations()
                .forEach(recommendation -> {
                    String recommendationType = recommendation.getType();
                    if(!recommendationsByCriteria.containsKey(recommendationType)) {
                        recommendationsByCriteria.put(recommendationType, new ArrayList<>());
                    }

                    recommendationsByCriteria.get(recommendationType).add(recommendation);
                });

        if(recommendationsByCriteria.containsKey(Constants.LOCATION_ASSOCIATION_RECOMMENDATION_TYPE)) {
            TripRecommendationDTO tripRecommendationDTO = new TripRecommendationDTO();
            tripRecommendationDTO.setRecommendations(recommendationsByCriteria.get(Constants.LOCATION_ASSOCIATION_RECOMMENDATION_TYPE));
            tripRecommendationDTO.setCriteria("Similar attractions with the ones already included in the itinerary.");
            tripRecommendationsDTO.getRecommendationsByCriteria().add(tripRecommendationDTO);
        }

        if(recommendationsByCriteria.containsKey(Constants.LOCATION_TAG_NEVER_EXPERIENCED_RECOMMENDATION_TYPE)) {
            TripRecommendationDTO tripRecommendationDTO = new TripRecommendationDTO();
            tripRecommendationDTO.setRecommendations(recommendationsByCriteria.get(Constants.LOCATION_TAG_NEVER_EXPERIENCED_RECOMMENDATION_TYPE));
            tripRecommendationDTO.setCriteria("Things that you never experienced.");
            tripRecommendationsDTO.getRecommendationsByCriteria().add(tripRecommendationDTO);
        }

        if(recommendationsByCriteria.containsKey(Constants.PREVIOUS_TRIPS_ASSOCIATION_RECOMMENDATION_TYPE)) {
            TripRecommendationDTO tripRecommendationDTO = new TripRecommendationDTO();
            tripRecommendationDTO.setRecommendations(recommendationsByCriteria.get(Constants.PREVIOUS_TRIPS_ASSOCIATION_RECOMMENDATION_TYPE));
            tripRecommendationDTO.setCriteria("Similar with your previous trips.");
            tripRecommendationsDTO.getRecommendationsByCriteria().add(tripRecommendationDTO);
        }

        if(recommendationsByCriteria.containsKey(Constants.MATCHING_CATEGORIES_RECOMMENDATION_TYPE)) {
            TripRecommendationDTO tripRecommendationDTO = new TripRecommendationDTO();
            tripRecommendationDTO.setRecommendations(recommendationsByCriteria.get(Constants.MATCHING_CATEGORIES_RECOMMENDATION_TYPE));
            tripRecommendationDTO.setCriteria("Things matching your preferences but which are less popular.");
            tripRecommendationsDTO.getRecommendationsByCriteria().add(tripRecommendationDTO);
        }

        return new ResponseEntity<>(tripRecommendationsDTO, HttpStatus.OK);
    }

    public ResponseEntity<PagedTripsDetailsDTO> getTrips(Integer userId, Integer destinationId,
                                                         Integer page, Integer size)
            throws BadQueryParametersException {
        boolean checkQueryParamsResult = checkQueryParameters(userId, destinationId);

        if(!checkQueryParamsResult) {
            throw new BadQueryParametersException("Maximum one query parameter must be " +
                    "specified between: destination_id or user_id");
        }

        Sort sorting = JpaSort.unsafe(Sort.Direction.DESC, "createdTimestamp");

        Pageable pageable = PageRequest.of(page, size, sorting);
        Page<Trip> tripPage;
        if(destinationId != null) {
            tripPage = this.tripRepository.findTripsByDestinationId(destinationId, pageable);
        } else if(userId != null) {
            tripPage = this.tripRepository.findTripsByUserId(userId, pageable);
        } else {
            tripPage = this.tripRepository.findAll(pageable);
        }

        PagedTripsDetailsDTO pagedTripsDetailsDTO = new PagedTripsDetailsDTO();
        List<TripMinDetailsDTO> trips = tripPage
                .get()
                .map(this::createTripMinDetailsDTOFromTripEntity)
                .toList();

        pagedTripsDetailsDTO.setTrips(trips);
        pagedTripsDetailsDTO.setNumberOfPages(tripPage.getTotalPages());
        pagedTripsDetailsDTO.setNumberOfElements(tripPage.getNumberOfElements());
        pagedTripsDetailsDTO.setPageSize(pageable.getPageSize());
        pagedTripsDetailsDTO.setPageNumber(pageable.getPageNumber());

        return new ResponseEntity<>(pagedTripsDetailsDTO, HttpStatus.OK);
    }

    public Optional<Trip> findById(int id) {
        return this.tripRepository.findById(id);
    }

    private boolean checkQueryParameters(Integer userId, Integer destinationId) {
        return userId == null || destinationId == null;
    }

    public ResponseEntity<TripPopularityDTO> getTripPopularity(int id) throws ResourceNotFoundException {
        Optional<Trip> tripOptional = findById(id);
        if(tripOptional.isEmpty()) {
            throw new ResourceNotFoundException(STR."Trip with id \{id} not found.");
        }
        Trip trip = tripOptional.get();

        DoubleSummaryStatistics statistics = trip.getReviews()
                .stream().mapToDouble(Review::getRating)
                .summaryStatistics();

        TripPopularityDTO tripPopularityDTO = new TripPopularityDTO();
        tripPopularityDTO.setPopularityScore(statistics.getAverage());
        tripPopularityDTO.setTripReviewsScore(statistics.getAverage());

        return new ResponseEntity<>(tripPopularityDTO, HttpStatus.OK);
    }

}
