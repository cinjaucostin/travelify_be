package com.costin.travelify.service;

import com.costin.travelify.dto.openweather_dto.WeatherCurrentDTO;
import com.costin.travelify.dto.openweather_dto.WeatherDetailsDTO;
import com.costin.travelify.dto.openweather_dto.WeatherForecastDTO;
import com.costin.travelify.dto.response_dto.*;
import com.costin.travelify.dto.tripadvisor_dto.AncestorLocationDTO;
import com.costin.travelify.dto.tripadvisor_dto.ImagesSearchDTO;
import com.costin.travelify.dto.tripadvisor_dto.LocationDetailsDTO;
import com.costin.travelify.entities.*;
import com.costin.travelify.exceptions.ResourceNotFoundException;
import com.costin.travelify.repository.DestinationRepository;
import com.costin.travelify.service.apis.OpenweatherService;
import com.costin.travelify.service.apis.TripadvisorService;
import com.costin.travelify.utils.Constants;
import com.costin.travelify.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DestinationService {
    private static final Logger log = LoggerFactory.getLogger(DestinationService.class);
    @Autowired
    private DestinationRepository destinationRepository;
    @Autowired
    private TripadvisorService tripadvisorService;
    @Autowired
    private OpenweatherService openweatherService;
    @Autowired
    private LocationTypeService locationTypeService;
    @Autowired
    private LocationImageService locationImageService;
    @Autowired
    private LocationService locationService;
    @Autowired
    private ReviewService reviewService;

//    @Scheduled(cron = "0 34 15 * * *", zone = "Europe/Bucharest")
    @Scheduled(cron = "0 0 * * * *", zone = "Europe/Bucharest")
    @Transactional
    public void computePopularity() {
        computePopularityAsyncTask();
    }

    @Async
    public CompletableFuture<Void> computePopularityAsyncTask() {
        List<Destination> destinations = this.destinationRepository.findAll();

        Map<Destination, Double> destinationsPopularityBasedOnTrips = computeDestinationsPopularityBasedOnTrips(destinations);
        Map<Destination, Double> destinationsRatingsBasedOnReviews = computeDestinationsRatingBasedOnReviews(destinations);
        Map<Destination, Double> destinationsPopularityBasedOnViews = computeDestinationsPopularityBasedOnViews(destinations);
        Map<Destination, Double> destinationsPopularityBasedOnLocationsReviews = computeDestinationPopularityBasedOnLocationsReviews(destinations);
        Map<Destination, Double> destinationsPopularityBasedOnLocationsViews = computeDestinationPopularityBasedOnLocationsViews(destinations);

        Map<Destination, DestinationPopularityDTO> destinationsPopularity = computeDestinationsPopularity(
                destinations,
                destinationsPopularityBasedOnTrips,
                destinationsRatingsBasedOnReviews,
                destinationsPopularityBasedOnViews,
                destinationsPopularityBasedOnLocationsReviews,
                destinationsPopularityBasedOnLocationsViews
        );

        destinationsPopularity
                .forEach((destination, destinationPopularityDTO) ->
                        destination.setPopularity(destinationPopularityDTO.getPopularityScore()));
        this.destinationRepository.saveAll(destinations);

        destinations.forEach(destination -> {
            List<Location> destinationLocations = destination.getLocations();
            Map<Location, LocationPopularityDTO> locationsPopularity =
                    this.locationService.getLocationsPopularityForDestination(destinationLocations);
            locationsPopularity
                .forEach((location, locationPopularityDTO) -> {
                    location.setPopularity(locationPopularityDTO.getPopularityScore());
                });
            locationService.computeLocationsRankingsByCategory(destinationLocations, destination.getName());
            this.locationService.saveAllLocations(destinationLocations);
        });

        return CompletableFuture.completedFuture(null);
    }

    public List<Destination> getAllDestinations() {
        return this.destinationRepository.findAll();
    }

    public ResponseEntity<DestinationsRecommendationsDTO> getRecommendationsBasedOnNearFutureWeather() {
        List<Destination> destinations = this.destinationRepository.findAll();

        destinations.sort(Comparator.comparingInt(Destination::getTemperature).reversed());

        if(destinations.size() > Constants.MAXIMUM_DESTINATIONS_RECOMMENDED) {
            destinations = destinations.subList(0, Constants.MAXIMUM_DESTINATIONS_RECOMMENDED);
        }

        List<DestinationMinDetailsDTO> destinationsRecommendedDTOs = destinations
                .stream()
                .map(destination -> createDestinationMinDetailsDTOFromEntity(destination, 0))
                .toList();

        DestinationsRecommendationsDTO destinationsRecommendationsDTO = new DestinationsRecommendationsDTO();
        destinationsRecommendationsDTO.setDestinations(destinationsRecommendedDTOs);

        return new ResponseEntity<>(destinationsRecommendationsDTO, HttpStatus.OK);
    }

    public ResponseEntity<DestinationsRecommendationsDTO> getRecommendationsBasedOnPopularity() {
        List<Destination> destinations = this.destinationRepository.findAll();

        Map<Destination, Double> destinationsPopularityBasedOnTrips = computeDestinationsPopularityBasedOnTrips(destinations);
        Map<Destination, Double> destinationsRatingsBasedOnReviews = computeDestinationsRatingBasedOnReviews(destinations);
        Map<Destination, Double> destinationsPopularityBasedOnViews = computeDestinationsPopularityBasedOnViews(destinations);
        Map<Destination, Double> destinationsPopularityBasedOnLocationsReviews = computeDestinationPopularityBasedOnLocationsReviews(destinations);
        Map<Destination, Double> destinationsPopularityBasedOnLocationsViews = computeDestinationPopularityBasedOnLocationsViews(destinations);

        Map<Destination, DestinationPopularityDTO> destinationsPopularity = computeDestinationsPopularity(
                destinations,
                destinationsPopularityBasedOnTrips,
                destinationsRatingsBasedOnReviews,
                destinationsPopularityBasedOnViews,
                destinationsPopularityBasedOnLocationsReviews,
                destinationsPopularityBasedOnLocationsViews
        );

        List<Destination> destinationsSortedByPopularity = destinationsPopularity.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.comparingDouble(DestinationPopularityDTO::getPopularityScore).reversed()))
                .map(Map.Entry::getKey)
                .toList();

        List<Destination> recommendedDestinations = destinationsSortedByPopularity
                .subList(0, Math.min(10, destinationsSortedByPopularity.size()));

        List<DestinationMinDetailsDTO> destinationsDTOs = destinationsSortedByPopularity
                .stream()
                .map(destination -> createDestinationMinDetailsDTOFromEntity(destination, destinationsPopularity
                        .get(destination).getPopularityScore()))
                .toList();
        DestinationsRecommendationsDTO recommendationsDTO = new DestinationsRecommendationsDTO();
        recommendationsDTO.setDestinations(destinationsDTOs);

        return new ResponseEntity<>(recommendationsDTO, HttpStatus.OK);
    }

    public DestinationMinDetailsDTO createDestinationMinDetailsDTOFromEntity(Destination destination,
                                                                             double popularityScore) {
        DestinationMinDetailsDTO destinationDTO = new DestinationMinDetailsDTO();
        destinationDTO.setId(destination.getId());
        destinationDTO.setName(destination.getName());
        destinationDTO.setAddressPath(destination.getAddressPath());
        Optional<LocationImage> destinationImageOptional = getDestinationImage(destination);
        destinationImageOptional.ifPresent(destinationImage ->  {
            destinationDTO.setImageSource(destinationImage.getSource());
        });
        destinationDTO.setPopularity(popularityScore);
        destinationDTO.setTemperature(destination.getTemperature());
        return destinationDTO;
    }

    public Map<Destination, DestinationPopularityDTO> computeDestinationsPopularity(List<Destination> destinations,
                                                            Map<Destination, Double> destinationsPopularityBasedOnTrips,
                                                            Map<Destination, Double> destinationsRatingsBasedOnReviews,
                                                            Map<Destination, Double> destinationsPopularityBasedOnViews,
                                                            Map<Destination, Double> destinationsPopularityBasedOnLocationsReviews,
                                                            Map<Destination, Double> destinationsPopularityBasedOnLocationsViews) {
        Map<Destination, DestinationPopularityDTO> destinationPopularity = new HashMap<>();
        destinations.forEach(destination -> {
            DestinationPopularityDTO destinationPopularityDTO = new DestinationPopularityDTO();
            double popularity = destinationsPopularityBasedOnTrips.get(destination) * 0.2 +
                    destinationsRatingsBasedOnReviews.get(destination) * 0.2 +
                    destinationsPopularityBasedOnViews.get(destination) * 0.2 +
                    destinationsPopularityBasedOnLocationsReviews.get(destination) * 0.2 +
                    destinationsPopularityBasedOnLocationsViews.get(destination) * 0.2;
            destinationPopularityDTO.setPopularityScore(popularity);
            destinationPopularityDTO.setDestinationReviewsScore(destinationsRatingsBasedOnReviews.get(destination));
            destinationPopularityDTO.setDestinationViewsScore(destinationsPopularityBasedOnViews.get(destination));
            destinationPopularityDTO.setLocationsReviewsScore(destinationsPopularityBasedOnLocationsReviews.get(destination));
            destinationPopularityDTO.setLocationsViewsScore(destinationsPopularityBasedOnLocationsViews.get(destination));
            destinationPopularityDTO.setTripsScore(destinationsPopularityBasedOnTrips.get(destination));
            destinationPopularity.put(destination, destinationPopularityDTO);
        });

        return destinationPopularity;
    }

    public Map<Destination, Double> computeDestinationPopularityBasedOnLocationsReviews(List<Destination> destinations) {
        Map<Destination, Double> destinationsPopularityByLocationsRatings = new HashMap<>();
        destinations.forEach(destination -> {
            destinationsPopularityByLocationsRatings.put(destination,
                    this.locationService.getLocationsAverageRating(destination.getLocations()));
        });
        return destinationsPopularityByLocationsRatings;
    }

    public Map<Destination, Double> computeDestinationPopularityBasedOnLocationsViews(List<Destination> destinations) {
        Map<Destination, Integer> destinationsPopularityByLocationsViews = new HashMap<>();
        destinations.forEach(destination -> destinationsPopularityByLocationsViews.put(destination,
                this.locationService.getLocationsSumOfViews(destination.getLocations())));

        Optional<Integer> maxValueOptional = destinationsPopularityByLocationsViews.values().stream().max(Integer::compareTo);
        int maxPopularity = maxValueOptional.orElse(0);
        Optional<Integer> minValueOptional = destinationsPopularityByLocationsViews.values().stream().min(Integer::compareTo);
        int minPopularity= minValueOptional.orElse(0);

        Map<Destination, Double> destinationLocationsViewsPopularityNormalized = new HashMap<>();

        destinationsPopularityByLocationsViews.forEach((key, value) -> {
            if(value == 0) {
                destinationLocationsViewsPopularityNormalized.put(key, 0.0);
            } else {
                double normalizedPopularity = Utils.normalize(value, minPopularity,
                        maxPopularity, 0, 5);
                destinationLocationsViewsPopularityNormalized.put(key, normalizedPopularity);
            }
        });

        return destinationLocationsViewsPopularityNormalized;
    }

    public Map<Destination, Double> computeDestinationsRatingBasedOnReviews(List<Destination> destinations) {
        Map<Destination, Double> destinationsRatings = new HashMap<>();
        destinations.forEach(destination -> {
            if(destination.getReviews() == null) {
                destinationsRatings.put(destination, 0.0);
            } else if(destination.getReviews().isEmpty()) {
                destinationsRatings.put(destination, 0.0);
            } else {
                RatingDTO ratingDTO = this.reviewService.createRatingDTOForDestination(destination);
                destinationsRatings.put(destination, ratingDTO.getRating());
            }
        });

        return destinationsRatings;
    }

    public Map<Destination, Double> computeDestinationsPopularityBasedOnViews(List<Destination> destinations) {
        Map<Destination, Integer> destinationsViews = new HashMap<>();
        destinations.forEach(destination -> {
            destinationsViews.put(destination, destination.getNumberOfViews());
        });

        Optional<Integer> maxValueOptional = destinationsViews.values().stream().max(Integer::compareTo);
        int maxPopularity = maxValueOptional.orElse(0);
        Optional<Integer> minValueOptional = destinationsViews.values().stream().min(Integer::compareTo);
        int minPopularity= minValueOptional.orElse(0);

        Map<Destination, Double> destinationsViewsNormalized = new HashMap<>();

        destinationsViews.forEach((key, value) -> {
            if(value == 0) {
                destinationsViewsNormalized.put(key, 0.0);
            } else {
                double normalizedViews = Utils.normalize(value, minPopularity,
                        maxPopularity, 0, 5);
                destinationsViewsNormalized.put(key, normalizedViews);
            }
        });

        return destinationsViewsNormalized;
    }

    public Map<Destination, Double> computeDestinationsPopularityBasedOnTrips(List<Destination> destinations) {
        Map<Destination, Integer> destinationsTripsPopularity = new HashMap<>();
        destinations.forEach(destination -> {
            if(destination.getTrips() == null) {
                destinationsTripsPopularity.put(destination, 0);
            } else if(destination.getTrips().isEmpty()) {
                destinationsTripsPopularity.put(destination, 0);
            } else {
                destinationsTripsPopularity.put(destination, destination.getTrips().size());
            }
        });

        Optional<Integer> maxValueOptional = destinationsTripsPopularity.values().stream().max(Integer::compareTo);
        int maxPopularity = maxValueOptional.orElse(0);
        Optional<Integer> minValueOptional = destinationsTripsPopularity.values().stream().min(Integer::compareTo);
        int minPopularity= minValueOptional.orElse(0);

        Map<Destination, Double> destinationTripsPopularityNormalized = new HashMap<>();

        destinationsTripsPopularity.forEach((key, value) -> {
            if(value == 0) {
                destinationTripsPopularityNormalized.put(key, 0.0);
            } else {
                double normalizedPopularity = Utils.normalize(value, minPopularity,
                        maxPopularity, 0, 5);
                destinationTripsPopularityNormalized.put(key, normalizedPopularity);
            }
        });

        return destinationTripsPopularityNormalized;
    }


    public List<Destination> searchDestinations(String destination) {
        return this.destinationRepository.findByNameOrDescriptionOrAddressPathContaining(destination);
    }

    public Destination advancedSearchDestinations(String destination) {
        return this.fetchDestinationFromTripadvisor(destination);
    }

    public Destination fetchDestinationFromTripadvisor(String destination) {
        log.warn("No destination matching {} found in the database, searching in TripAdvisor", destination);
        Optional<LocationDetailsDTO> destinationOptional = tripadvisorService.searchCity(destination);

        if(destinationOptional.isEmpty()) {
            return null;
        }

        LocationDetailsDTO destinationDTO = destinationOptional.get();
        Optional<Destination> destinationExisting = this.destinationRepository.findByTripadvisorId(destinationDTO.getLocation_id());
        if(destinationExisting.isPresent()) {
            log.info(STR."Destination \{destination} already exists");
            return destinationExisting.get();
        }

        Destination newDestination = new Destination(destinationDTO);
        newDestination.setFullLoad(0);
        String ancestorName = validateAncestorForDestination(destinationDTO);
        newDestination.setAncestorName(ancestorName);

        Optional<LocationType> locationTypeOptional = this.locationTypeService.findByType("city");
        locationTypeOptional.ifPresentOrElse(newDestination::setDestinationType, () -> {
            LocationType cityLocationType = new LocationType("city", "City");
            LocationType locationTypeSaved = this.locationTypeService.saveLocationType(cityLocationType);
            newDestination.setDestinationType(locationTypeSaved);
        });

        addDestinationImages(newDestination);
        return this.saveDestination(newDestination);
    }

    public void populateDestinationWithLinkedLocations(Destination destination) {
        if(destination.getFullLoad() == 1) {
            return;
        }

        if(destination.getLocations() == null || destination.getLocations().isEmpty()) {
            addAttractionsToDestination(destination, 10);
            addRestaurantsToDestination(destination, 10);
            addHotelsToDestination(destination, 10);
            populateDestinationWithMoreLinkedAttractionsIfNeeded(destination);
            return;
        }

        populateDestinationWithLinkedAttractionsIfNeeded(destination);

        populateDestinationWithLinkedHotelsIfNeeded(destination);

        populateDestinationWithLinkedRestaurantsIfNeeded(destination);

        populateDestinationWithMoreLinkedAttractionsIfNeeded(destination);

        destination.setFullLoad(1);

        this.saveDestination(destination);
    }

    public void populateDestinationWithLinkedHotelsIfNeeded(Destination destination) {
        Optional<List<Location>> destinationHotelsOptional = getDestinationLocationsByCategory(destination, "hotel");
        if(destinationHotelsOptional.isEmpty()) {
            addHotelsToDestination(destination, 10);
        } else {
            List<Location> destinationHotels = destinationHotelsOptional.get();
            if(destinationHotels.isEmpty()) {
                addHotelsToDestination(destination, 10);
            } else if(destinationHotels.size() < 5) {
                addHotelsToDestination(destination, 10);
            }
        }
    }

    public void populateDestinationWithLinkedRestaurantsIfNeeded(Destination destination) {
        Optional<List<Location>> destinationRestaurantsOptional = getDestinationLocationsByCategory(destination, "restaurant");
        if(destinationRestaurantsOptional.isEmpty()) {
            addRestaurantsToDestination(destination, 10);
        } else {
            List<Location> destinationRestaurants = destinationRestaurantsOptional.get();
            if(destinationRestaurants.isEmpty()) {
                addRestaurantsToDestination(destination, 10);
            } else if(destinationRestaurants.size() < 5) {
                addRestaurantsToDestination(destination, 10);
            }
        }
    }

    public void populateDestinationWithLinkedAttractionsIfNeeded(Destination destination) {
        Optional<List<Location>> destinationAttractionsOptional = getDestinationLocationsByCategory(destination, "attraction");
        if(destinationAttractionsOptional.isEmpty()) {
            addAttractionsToDestination(destination, 10);
        } else {
            List<Location> destinationAttractions = destinationAttractionsOptional.get();
            if(destinationAttractions.isEmpty()) {
                addAttractionsToDestination(destination, 10);
            } else if(destinationAttractions.size() < 5) {
                addAttractionsToDestination(destination, 10);
            }
        }

    }

    public void populateDestinationWithMoreLinkedAttractionsIfNeeded(Destination destination) {
        addMoreAttractionWithCategoryToDestinationUsingTomTomAndTripadvisor(destination, Constants.TOMTOM_ATTRACTIONS_CATEGORY_ID, 10);

        addMoreAttractionWithCategoryToDestinationUsingTomTomAndTripadvisor(destination, Constants.TOMTOM_BEACHES_CATEGORY_ID, 3);
    }

    public Optional<List<Location>> getDestinationLocationsByCategory(Destination destination, String category) {
        if(destination.getLocations() == null) {
            return Optional.empty();
        }

        List<Location> locationsFiltered = destination.getLocations()
                .stream()
                .filter(location -> {
                    if(location.getLocationType() == null) {
                        return false;
                    }

                    if(location.getLocationType().getType() == null) {
                        return false;
                    }

                    return location.getLocationType().getType().equalsIgnoreCase(category);
                })
                .toList();

        if(locationsFiltered.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(locationsFiltered);
    }

    public void addRestaurantsToDestination(Destination destination, int limit) {
        List<Location> newRestaurants = this.locationService.fetchRestaurantsForDestinationFromTripadvisor(destination, limit);
        if(destination.getLocations() == null) {
            destination.setLocations(new ArrayList<>());
        }
        destination.getLocations().addAll(newRestaurants);
        this.saveDestination(destination);
    }

    public void addHotelsToDestination(Destination destination, int limit) {
        List<Location> newHotels = this.locationService.fetchHotelsForDestinationFromTripadvisor(destination, limit);
        if(destination.getLocations() == null) {
            destination.setLocations(new ArrayList<>());
        }
        destination.getLocations().addAll(newHotels);
        this.saveDestination(destination);
    }

    public void addAttractionsToDestination(Destination destination, int limit) {
        List<Location> newAttractions = this.locationService.fetchAttractionsForDestinationFromTripadvisor(destination, limit);
        if(destination.getLocations() == null) {
            destination.setLocations(new ArrayList<>());
        }
        destination.getLocations().addAll(newAttractions);
        this.saveDestination(destination);
    }

    public void addMoreAttractionWithCategoryToDestinationUsingTomTomAndTripadvisor(Destination destination, int attractionCategory, int limit) {
        List<Location> newAttractions = this.locationService
                .fetchAttractionsForDestinationFromTripadvisorUsingTomTom(destination, attractionCategory, limit);
        if(destination.getLocations() == null) {
            destination.setLocations(new ArrayList<>());
        }
        if(newAttractions == null) {
            return;
        }
        if(newAttractions.isEmpty()) {
            return;
        }
        destination.getLocations().addAll(newAttractions);
        this.saveDestination(destination);
    }

    public Destination saveDestination(Destination destination) {
        return this.destinationRepository.save(destination);
    }

    public ResponseEntity<DestinationTagsDTO> getDestinationLocationsTags(int id) {
        Optional<Destination> destinationOptional = this.destinationRepository.findById(id);

        if(destinationOptional.isPresent()) {
            Destination destination = destinationOptional.get();

            Set<LocationTag> locationTags = destination.getLocations()
                    .stream()
                    .flatMap(location -> location.getLocationTags().stream())
                    .collect(Collectors.toSet());

            return new ResponseEntity<>(new DestinationTagsDTO(locationTags.stream().toList()), HttpStatus.OK);
        }

        return new ResponseEntity<>(new DestinationTagsDTO(), HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity<AttractionsExperiencesDTO> getAttractionsByExperiences(int id)
            throws ResourceNotFoundException {
        Destination destination = findDestinationById(id);

        if(destination.getLocations() == null) {
            return new ResponseEntity<>(null, HttpStatus.OK);
        }

        if(destination.getLocations().isEmpty()) {
            return new ResponseEntity<>(null, HttpStatus.OK);
        }

        Map<LocationTag, Set<Location>> locationsExperiences = getLocationsSplitByExperiences(destination);

        List<LocationTag> mostCommonExperiences = locationsExperiences.entrySet()
                .stream()
                .sorted(Comparator.comparingInt(entry -> entry.getValue().size())) // Sort in descending order
                .map(Map.Entry::getKey)
                .toList()
                .reversed();

        if(mostCommonExperiences.size() > 4) {
            mostCommonExperiences = mostCommonExperiences.subList(0, 4);
        }

        AttractionsExperiencesDTO attractionsExperiences = new AttractionsExperiencesDTO();

        attractionsExperiences.setExperiences(new ArrayList<>());

        mostCommonExperiences.forEach(locationTag -> {
            ExperienceWithLocationsDTO experiencesLocationsDTO = new ExperienceWithLocationsDTO();
            experiencesLocationsDTO.setExperienceType(locationTag.getName());
            experiencesLocationsDTO.setLocations(locationsExperiences.get(locationTag).stream().toList());
            attractionsExperiences.getExperiences().add(experiencesLocationsDTO);
        });

        return new ResponseEntity<>(attractionsExperiences, HttpStatus.OK);
    }

    public Map<LocationTag, Set<Location>> getLocationsSplitByExperiences(Destination destination) {

        Map<LocationTag, Set<Location>> locationsByExperiences = new HashMap<>();
        destination.getLocations()
            .forEach(location -> {
                LocationType locationType = location.getLocationType();

                if(locationType== null) {
                    return;
                }

                if(locationType.getType() == null) {
                    return;
                }

                if(locationType.getType().equalsIgnoreCase(Constants.HOTEL_TYPE) ||
                        locationType.getType().equalsIgnoreCase(Constants.RESTAURANT_TYPE)) {
                    return;
                }

                if(location.getLocationTags() == null) {
                    return;
                }

                location.getLocationTags()
                    .forEach(locationTag -> {
                        if(locationTag.getTag().equalsIgnoreCase("attractions")) {
                            return;
                        }

                       if(!locationsByExperiences.containsKey(locationTag)) {
                           locationsByExperiences.put(locationTag, new HashSet<>());
                       }
                       locationsByExperiences.get(locationTag).add(location);
                    });
            });

        return locationsByExperiences;
    }

    public void addDestinationImages(Destination destination) {
        Optional<ImagesSearchDTO> imagesOptional = this.tripadvisorService.searchLocationImages(destination.getTripadvisorId());
        List<LocationImage> newImages = new ArrayList<>();

        imagesOptional.ifPresent(imagesDTO -> imagesDTO.getData().forEach(imageDTO -> {
            LocationImage locationImage = this.locationImageService.createLocationImageFromDTO(imageDTO);
            if(locationImage != null) {
                locationImage.setDestination(destination);
                newImages.add(locationImage);
            }
        }));

        destination.setDestinationImages(newImages);
    }

    public List<LocationTag> getDestinationMostCommonTags(Destination destination) {
        Map<LocationTag, Integer> tagsAppearances = new HashMap<>();
        if(destination.getLocations() == null) {
            return new ArrayList<>();
        }
        if(destination.getLocations().isEmpty()) {
            return new ArrayList<>();
        }
        destination.getLocations()
            .forEach(location -> {
               location.getLocationTags()
                       .forEach(locationTag -> {
                           if(locationTag.getTag().equalsIgnoreCase("hotel") ||
                                   locationTag.getTag().equalsIgnoreCase("restaurant")) {
                               return;
                           }
                           if(!tagsAppearances.containsKey(locationTag)) {
                               tagsAppearances.put(locationTag, 1);
                           }
                           int oldApp = tagsAppearances.get(locationTag);
                           tagsAppearances.replace(locationTag, oldApp + 1);
                       });
            });

        return tagsAppearances.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .toList();
    }

    public ResponseEntity<WeatherDTO> getDestinationCurrentWeather(int id) {
        Optional<Destination> destinationOptional = this.destinationRepository.findById(id);

        if(destinationOptional.isPresent()) {
            Destination destination = destinationOptional.get();
            Optional<WeatherCurrentDTO> currentWeatherOptional = this.openweatherService.searchCurrentWeatherForCoordinates(
                    destination.getLatitude(),
                    destination.getLongitude()
            );

            WeatherDTO weatherDTO = new WeatherDTO();

            currentWeatherOptional.ifPresent(currentWeather -> {
                Date sunriseDate = Utils.convertFromUnixTimestampToDateUsingTimezoneIdV2(currentWeather.getCurrent().getSunrise(),
                        destination.getTimezone());
                Date sunsetDate = Utils.convertFromUnixTimestampToDateUsingTimezoneIdV2(currentWeather.getCurrent().getSunset(),
                        destination.getTimezone());
                weatherDTO.setDestinationName(destination.getAddressPath());
                weatherDTO.setSunrise(Utils.getHourFromDate(sunriseDate));
                weatherDTO.setSunset(Utils.getHourFromDate(sunsetDate));
                weatherDTO.setWeatherDescription(currentWeather.getCurrent().getWeather().getFirst().getDescription());
                weatherDTO.setWeatherIcon(currentWeather.getCurrent().getWeather().getFirst().getIcon());
                weatherDTO.setTemperature((int) Utils.convertFromKelvinToCelsiusDegrees(currentWeather.getCurrent().getFeels_like()));
            });

            return new ResponseEntity<>(weatherDTO, HttpStatus.OK);
        }

        return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
    }

    public String validateAncestorForDestination(LocationDetailsDTO destinationDTO) {
        Set<String> invalidLevels = new HashSet<>(Arrays.asList("state", "country", "region", "nation"));
        AncestorLocationDTO firstLevel = destinationDTO.getAncestors().getFirst();

        if (!invalidLevels.contains(firstLevel.getLevel().toLowerCase())) {
            return firstLevel.getName();
        }

        return "";
    }

    public Optional<LocationImage> getDestinationImage(Destination destination) {
        if(destination.getDestinationImages() != null) {
            if(!destination.getDestinationImages().isEmpty()) {
                return Optional.of(destination.getDestinationImages().getFirst());
            }
        }

        if(destination.getDestinationImages() == null) {
            Optional<LocationImage> imageOptional = getImageFromDestinationLocations(destination);
            if(imageOptional.isPresent()) {
                return imageOptional;
            }
        }
        if(destination.getDestinationImages().isEmpty()) {
            Optional<LocationImage> imageOptional = getImageFromDestinationLocations(destination);
            if(imageOptional.isPresent()) {
                return imageOptional;
            }
        }

        return Optional.empty();
    }

    public Optional<LocationImage> getImageFromDestinationLocations(Destination destination) {
        if(destination != null && destination.getLocations() != null) {
            if(!destination.getLocations().isEmpty()) {
                for (Location location : destination.getLocations()) {
                    if(location.getLocationImages() != null) {
                        if(!location.getLocationImages().isEmpty()) {
                            return Optional.of(location.getLocationImages().getFirst());
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    public ResponseEntity<ResponseDTO> deleteDestinationById(int id) {
        Optional<Destination> destinationOptional = this.destinationRepository.findById(id);

        if(destinationOptional.isPresent()) {
            this.destinationRepository.deleteById(id);
            return new ResponseEntity<>(new ResponseDTO("Destination removed successfully"),
                    HttpStatus.OK);
        }

        return new ResponseEntity<>(new ResponseDTO(STR."Destination with id \{id} not found."),
                HttpStatus.BAD_REQUEST);
    }

    public Destination getDestinationById(int id)
            throws ResourceNotFoundException {
        Destination destination = findDestinationById(id);

        int oldViewsNumber = destination.getNumberOfViews();
        destination.setNumberOfViews(oldViewsNumber + 1);

        return this.destinationRepository.save(destination);
    }

    public ResponseEntity<DestinationPopularityDTO> getDestinationPopularity(int id)
            throws ResourceNotFoundException {
        Destination destination = findDestinationById(id);
        List<Destination> destinations = this.destinationRepository.findAll();

        Map<Destination, Double> destinationsPopularityBasedOnTrips = computeDestinationsPopularityBasedOnTrips(destinations);
        Map<Destination, Double> destinationsRatingsBasedOnReviews = computeDestinationsRatingBasedOnReviews(destinations);
        Map<Destination, Double> destinationsPopularityBasedOnViews = computeDestinationsPopularityBasedOnViews(destinations);
        Map<Destination, Double> destinationsPopularityBasedOnLocationsReviews = computeDestinationPopularityBasedOnLocationsReviews(destinations);
        Map<Destination, Double> destinationsPopularityBasedOnLocationsViews = computeDestinationPopularityBasedOnLocationsViews(destinations);

        Map<Destination, DestinationPopularityDTO> destinationsPopularity = computeDestinationsPopularity(
                destinations,
                destinationsPopularityBasedOnTrips,
                destinationsRatingsBasedOnReviews,
                destinationsPopularityBasedOnViews,
                destinationsPopularityBasedOnLocationsReviews,
                destinationsPopularityBasedOnLocationsViews
        );

        if(destinationsPopularity.containsKey(destination)) {
            return new ResponseEntity<>(destinationsPopularity.get(destination), HttpStatus.OK);
        }

        return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
    }

    public ResponseEntity<RatingDTO> getDestinationRating(int id) throws ResourceNotFoundException {
        Destination destination = findDestinationById(id);
        return new ResponseEntity<>(this.reviewService.createRatingDTOForDestination(destination),
                HttpStatus.OK);
    }

    public Destination findDestinationById(int id) throws ResourceNotFoundException {
        Optional<Destination> destinationOptional = this.destinationRepository.findById(id);

        if(destinationOptional.isEmpty()) {
            throw new ResourceNotFoundException(STR."Destination with id \{id} not found");
        }

        return destinationOptional.get();
    }

    public ResponseEntity<ImagesDTO> getDestinationImages(int id) throws ResourceNotFoundException {
        Destination destination = findDestinationById(id);
        ImagesDTO imagesDTO = new ImagesDTO();
        imagesDTO.setImages(new ArrayList<>());

        if(destination.getDestinationImages() != null) {
            imagesDTO.getImages().addAll(destination.getDestinationImages());
        }

        if(destination.getLocations() != null) {
            destination.getLocations()
                .forEach(location -> {
                    if(location.getLocationType().getType().equalsIgnoreCase(Constants.ATTRACTION_TYPE) &&
                            location.getLocationImages() != null) {
                        imagesDTO.getImages().addAll(location.getLocationImages());
                    }
                });
        }

        return new ResponseEntity<>(imagesDTO, HttpStatus.OK);
    }

    public ResponseEntity<MultipleLocationsDTO> getDestinationLocationWithType(int id, String type)
            throws ResourceNotFoundException {
        Destination destination = findDestinationById(id);
        MultipleLocationsDTO locationsDTO = new MultipleLocationsDTO();
        locationsDTO.setLocations(new ArrayList<>());

        if(destination.getLocations() == null) {
            return new ResponseEntity<>(locationsDTO, HttpStatus.OK);
        }

        List<Location> locations = destination.getLocations()
                .stream()
                .filter(location -> location.getLocationType().getType().equalsIgnoreCase(type))
                .toList();

        locationsDTO.setLocations(locations);
        return new ResponseEntity<>(locationsDTO, HttpStatus.OK);
    }

    public ResponseEntity<OneDestinationDTO> addImagesToDestination(int id) throws ResourceNotFoundException {
        Destination destination = findDestinationById(id);
        OneDestinationDTO oneDestinationDTO = new OneDestinationDTO();

        if(destination.getDestinationImages() == null) {
            addDestinationImages(destination);
            Destination savedDestination = this.destinationRepository.save(destination);
            oneDestinationDTO.setDestination(savedDestination);
            return new ResponseEntity<>(oneDestinationDTO, HttpStatus.OK);
        }

        if(destination.getDestinationImages().isEmpty()) {
            addDestinationImages(destination);
            Destination savedDestination = this.destinationRepository.save(destination);
            oneDestinationDTO.setDestination(savedDestination);
            return new ResponseEntity<>(oneDestinationDTO, HttpStatus.OK);
        }

        oneDestinationDTO.setDestination(destination);
        return new ResponseEntity<>(oneDestinationDTO, HttpStatus.OK);
    }

    public ResponseEntity<MultipleLocationsDTO> getDestinationLocations(int id)
            throws ResourceNotFoundException {
        Destination destination = findDestinationById(id);
        MultipleLocationsDTO multipleLocationsDTO = new MultipleLocationsDTO();
        multipleLocationsDTO.setLocations(destination.getLocations());
        return new ResponseEntity<>(multipleLocationsDTO, HttpStatus.OK);
    }

}
