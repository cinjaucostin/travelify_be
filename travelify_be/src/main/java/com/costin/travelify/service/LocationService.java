package com.costin.travelify.service;

import com.costin.travelify.dto.arcgis_dto.GeoLocationDTO;
import com.costin.travelify.dto.request_dto.LocationTagDTO;
import com.costin.travelify.dto.response_dto.*;
import com.costin.travelify.dto.tomtom_dto.LocationsSearchResultTTDTO;
import com.costin.travelify.dto.tomtom_dto.LocationsSearchTTDTO;
import com.costin.travelify.dto.tomtom_dto.PointOfInterestTTDTO;
import com.costin.travelify.dto.tripadvisor_dto.*;
import com.costin.travelify.entities.*;
import com.costin.travelify.exceptions.ResourceNotFoundException;
import com.costin.travelify.repository.LocationRepository;
import com.costin.travelify.service.apis.TomTomService;
import com.costin.travelify.service.apis.TripadvisorService;
import com.costin.travelify.utils.Constants;
import com.costin.travelify.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;


@Service
public class LocationService {
    private static final Logger log = LoggerFactory.getLogger(LocationService.class);
    @Autowired
    private LocationRepository locationRepository;
    @Autowired
    private LocationTagService locationTagService;
    @Autowired
    private LocationTypeService locationTypeService;
    @Autowired
    private LocationImageService locationImageService;
    @Autowired
    private TripadvisorService tripadvisorService;
    @Autowired
    private TomTomService tomTomService;
    @Autowired
    private ReviewService reviewService;

    public List<Location> searchLocation(String query, String locationType) {
        if (Objects.equals(locationType, "")) {
            return this.locationRepository.findByNameOrDescriptionOrAddressPathContaining(query.toLowerCase());
        }

        return this.locationRepository
                .findByNameOrDescriptionOrAddressPathContainingFilterByLocationType(query.toLowerCase(),
                        locationType.toLowerCase());
    }

    public List<Location> fetchRestaurantsForDestinationFromTripadvisor(Destination destination, int limit) {
        log.info("Fetching maximum {} restaurants for destination: {} tripadvisorId = {}", limit, destination.getName(), destination.getTripadvisorId());
        return this.fetchLocationsForDestinationFromTripadvisor(destination, Constants.TRIPADVISOR_RESTAURANT_CATEGORY, limit);
    }

    public List<Location> fetchHotelsForDestinationFromTripadvisor(Destination destination, int limit) {
        log.info("Fetching maximum {} hotels for destination: {} tripadvisorId = {}", limit, destination.getName(), destination.getTripadvisorId());
        return this.fetchLocationsForDestinationFromTripadvisor(destination, Constants.TRIPADVISOR_HOTEL_CATEGORY, limit);
    }

    public List<Location> fetchAttractionsForDestinationFromTripadvisorUsingTomTom(Destination destination, int attractionCategory, int limit) {
        log.info("Fetching maximum {} attractions, category {}, for destination: {} tripadvisorId = {}, using Tom Tom Api search",
                limit, attractionCategory, destination.getName(), destination.getTripadvisorId());
        return this.getAttractionsForDestinationFromTomTomApi(destination, attractionCategory, limit);
    }

    private List<Location> fetchLocationsForDestinationFromTripadvisor(Destination destination,
                                                                       String category, int limit) {
        List<Location> locations = new ArrayList<>();
        Optional<List<LocationSearchResultDTO>> locationsOptional =
                this.tripadvisorService.searchLocationsByCategoryFromLocationWithoutFetchingDetails(
                        destination.getAddressPath(),
                        category,
                        limit);

        if(locationsOptional.isPresent()) {
            locations = this.getLocationsForDestination(destination, locationsOptional.get());
        }

        log.info("Fetched {} {} for destination {} tripadvisorId = {}", locations.size(), category,
                destination.getName(), destination.getTripadvisorId());

        return locations;
    }

    public List<Location> fetchAttractionsForDestinationFromTripadvisor(Destination destination, int limit) {
        List<Location> locations = new ArrayList<>();
        Set<Long> locationsFetched = new HashSet<>();

        Optional<List<LocationSearchResultDTO>> attractionsOptional =
                this.tripadvisorService.searchLocationsByCategoryFromLocationWithoutFetchingDetails(
                destination.getAddressPath(),
                Constants.TRIPADVISOR_ATTRACTION_CATEGORY,
                limit);

        if(attractionsOptional.isPresent()) {
            locations = getLocationsForDestination(destination, attractionsOptional.get());
            locationsFetched = locations.stream().map(Location::getTripadvisorId).collect(Collectors.toSet());
        }

        if(destination.getAncestorName() != null) {
            if(!destination.getAncestorName().isEmpty()) {
                log.info("Searching for attractions for ancestor {} of destination {}", destination.getAncestorName(), destination.getName());
                List<Location> locationsByAncestor = this.fetchAttractionsForDestinationAncestor(destination, locationsFetched, limit);
                if(locationsByAncestor == null) return locations;
                if(locationsByAncestor.isEmpty()) return locations;
                locations.addAll(locationsByAncestor);
            }
        }

        return locations;
    }

    public List<Location> fetchAttractionsForDestinationAncestor(Destination destination, Set<Long> alreadyFetchedAttractions, int limit) {
        Optional<List<LocationSearchResultDTO>> attractionsByAncestorNameOptional = this.tripadvisorService
                .searchLocationsByCategoryFromLocationWithoutFetchingDetails(
                        destination.getAncestorName(),
                        Constants.TRIPADVISOR_ATTRACTION_CATEGORY,
                        limit
                );

        if (attractionsByAncestorNameOptional.isEmpty()) {
            return new ArrayList<>();
        }

        List<LocationSearchResultDTO> attractionsByAncestorName = attractionsByAncestorNameOptional.get();
        List<LocationSearchResultDTO> newAttractions = attractionsByAncestorName
                .stream()
                .filter(attraction -> !alreadyFetchedAttractions.contains(attraction.getLocation_id()))
                .toList();

        if(newAttractions.isEmpty()) {
            return new ArrayList<>();
        }

        log.info("New attractions found by searching by the ancestor {} of destination {}: {}",
                destination.getAncestorName(), destination.getName(),
                newAttractions.stream().map(LocationSearchResultDTO::getLocation_id).collect(Collectors.toList()));

        return getLocationsForDestination(destination, newAttractions);
    }

    public List<Location> getLocationsForDestination(Destination destination,
                                                     List<LocationSearchResultDTO> locationsSearchDTO) {
        Set<Location> locations = new HashSet<>();
        locationsSearchDTO.forEach(locationSearchDTO -> {
            long tripadvisorId = locationSearchDTO.getLocation_id();
            Optional<Location> alreadyExistingLocation = this.locationRepository.findByTripadvisorId(tripadvisorId);
            alreadyExistingLocation.ifPresentOrElse(location -> {
                if(checkIfLocationIsAlreadyAssignedToDestination(destination, location)) {
                    return;
                }

                if(!testDistanceBetweenDestinationAndAlreadyExistingLocation(destination, location)) {
                    return;
                }

                log.info(STR."Location \{location.getName()} already exists when trying to add to destination \{destination.getName()}");
                locations.add(location);
            }, () -> {
                Optional<LocationDetailsDTO> locationDetailsDTOOptional = this.tripadvisorService
                        .searchLocationDetails(tripadvisorId);
                locationDetailsDTOOptional.ifPresent(locationDetailsDTO -> {
                    if(!testDistanceBetweenDestinationAndNewLocation(destination, locationDetailsDTO)) {
                        return;
                    }
                    Location savedLocation = this.saveLocation(this.createLocationFromDTO(locationDetailsDTO));
                    locations.add(savedLocation);
                });
            });
        });

        if(locations.isEmpty()) return new ArrayList<>();

        return new ArrayList<>(locations);
    }

    public void computeLocationsRankingsByCategory(List<Location> locations, String destinationName) {
        Map<String, List<Location>> locationsByCategory = new HashMap<>();
        locations
            .forEach(location -> {
                String locationType = "";
                if(location.getLocationType() != null) {
                    if(location.getLocationType().getType() != null) {
                        locationType = location.getLocationType().getType().toLowerCase();
                    }
                }

                if(!locationType.isEmpty()) {
                    if(!locationsByCategory.containsKey(locationType)) {
                        locationsByCategory.put(locationType, new ArrayList<>());
                    }
                    locationsByCategory.get(locationType).add(location);
                }
            });
        Map<String, List<Location>> locationsRanking = new HashMap<>();
        locationsByCategory
                .forEach((locationType, locationsWithCategory) -> {
                    locationsWithCategory.sort(Comparator.comparingDouble(Location::getPopularity).reversed());
                    locationsRanking.put(locationType, locationsWithCategory);
                });

        locationsRanking
                .forEach((locationType, locationsWithCategory) -> {
                    locationsWithCategory
                            .forEach(location -> {
                                int rank = locationsWithCategory.indexOf(location) + 1;
                                location.setRanking(STR."#\{rank} \{locationType} in \{destinationName}");
                            });
                });
    }

    public double getLocationsAverageRating(List<Location> locations) {
        if(locations == null) {
            return 0.0;
        }

        if(locations.isEmpty()) {
            return 0.0;
        }

        double ratingsSum = locations
            .stream()
            .mapToDouble(location -> {
                RatingDTO ratingDTO = this.reviewService.createRatingDTOForLocation(location);
                if(ratingDTO != null) {
                    return ratingDTO.getRating();
                }
                return 0.0;
            })
            .sum();
        return ratingsSum / locations.size();
    }

    public int getLocationsSumOfViews(List<Location> locations) {
        if(locations == null) {
            return 0;
        }

        if(locations.isEmpty()) {
            return 0;
        }

        return locations
            .stream()
            .mapToInt(Location::getNumberOfViews)
            .sum();
    }

    public List<Location> getAttractionsForDestinationFromTomTomApi(Destination destination, int attractionCategory, int limit) {
        Set<Location> locations = new HashSet<>();
        Optional<LocationsSearchTTDTO> locationsFetchedFromTTOptional = this.tomTomService.searchLocation(
                destination.getName(),
                destination.getLatitude(),
                destination.getLongitude(),
                attractionCategory
        );

        if(locationsFetchedFromTTOptional.isPresent()) {
            LocationsSearchTTDTO locationsSearchTTDTO = locationsFetchedFromTTOptional.get();
            if(!this.tomTomService.checkLocationsSearchTTDTO(locationsSearchTTDTO)) {
                return null;
            }

            List<LocationsSearchResultTTDTO> ttResults = locationsSearchTTDTO.getResults();
            if(ttResults.size() > limit) {
                ttResults = ttResults.subList(0, limit);
            }

            ttResults.forEach(ttResult -> {
                    PointOfInterestTTDTO poiDTO = ttResult.getPoi();
                    if(!this.tomTomService.checkPointsOfInterestTTDTO(poiDTO)) {
                        return;
                    }
                    String locationName = poiDTO.getName();
                    if(!locationName.toLowerCase().contains(destination.getName().toLowerCase())) {
                        locationName += STR." \{destination.getName()}";
                    }

                    Optional<LocationSearchResultDTO> locationSearchResultDTOOptional = this.tripadvisorService
                            .searchSingleAttractionByNameWithoutDetails(locationName);

                    if(locationSearchResultDTOOptional.isEmpty()) {
                        return;
                    }

                    Optional<Location> alreadyExistingLocationOptional = this.locationRepository
                            .findByTripadvisorId(locationSearchResultDTOOptional.get().getLocation_id());
                    if(alreadyExistingLocationOptional.isPresent()) {
                        Location existingLocation = alreadyExistingLocationOptional.get();
                        if(!checkIfLocationBelongsToDestination(destination, existingLocation) && !locations.contains(existingLocation)) {
                            if(testDistanceBetweenDestinationAndAlreadyExistingLocation(destination, existingLocation)) {
                                locations.add(existingLocation);
                            }
                        }
                    } else {
                        Optional<LocationDetailsDTO> attractionDetailsOptional = this.tripadvisorService
                                .searchLocationDetails(locationSearchResultDTOOptional.get().getLocation_id());
                        attractionDetailsOptional.ifPresent(attractionDetails -> {
                            if(!testDistanceBetweenDestinationAndNewLocation(destination, attractionDetails)) {
                                return;
                            }
                            Location savedLocation = this.saveLocation(this.createLocationFromDTO(attractionDetails));
                            locations.add(savedLocation);
                        });
                     }
                 }
            );
        }

        log.info("Fetched {} new locations for destination {} using Tom Tom Api Search, category {} and Tripadvisor Details",
                locations.size(), destination.getName(), attractionCategory);

        return locations.stream().toList();
    }

    public List<LocationTag> getMatchingLocationTagsFromLocation(List<LocationTagDTO> tripTags, Location location) {
        Set<Integer> tripTagsIds = tripTags.stream().map(LocationTagDTO::getId).collect(Collectors.toSet());

        return location.getLocationTags()
                .stream()
                .filter(locationTag -> tripTagsIds.contains(locationTag.getId()))
                .collect(Collectors.toList());
    }

    public List<LocationTag> getMatchingLocationTagsFromLocationWithTagsProvided(List<LocationTag> locationTags, Location location) {
        Set<Integer> tripTagsIds = locationTags.stream().map(LocationTag::getId).collect(Collectors.toSet());

        return location.getLocationTags()
                .stream()
                .filter(locationTag -> tripTagsIds.contains(locationTag.getId()))
                .collect(Collectors.toList());
    }

    public Location createLocationFromDTO(LocationDetailsDTO locationDetailsDTO) {
        Location location = new Location(locationDetailsDTO);
        location.setLocationTags(new ArrayList<>());
        location.setLocationImages(new ArrayList<>());

        addLocationType(location, locationDetailsDTO.getCategory());
        addLocationTagsFromGroups(location, locationDetailsDTO.getGroups());
        addLocationTagsFromSubcategories(location, locationDetailsDTO.getSubcategory());
        addLocationImages(location);

        if(locationDetailsDTO.getRanking_data() != null) {
            if(locationDetailsDTO.getRanking_data().getRanking_string() != null) {
                location.setTripadvisorRanking(locationDetailsDTO.getRanking_data().getRanking_string());
            }
        }

        location.setFeatures(formatLocationFeaturesToString(locationDetailsDTO));
        location.setStyles(formatLocationStylesToString(locationDetailsDTO));
        location.setCuisine(formatRestaurantCuisinesToString(locationDetailsDTO));
        location.setAmenities(formatLocationAmenitiesToString(locationDetailsDTO));
        location.setWorkHours(formatWorkHoursToString(locationDetailsDTO));

        return location;
    }

    public boolean checkIfLocationIsAlreadyAssignedToDestination(Destination destination, Location existingLocation) {
        if(destination.getLocations() == null) {
            return false;
        }

        if(destination.getLocations().isEmpty()) {
            return false;
        }

        return !destination.getLocations()
                .stream()
                .filter(location -> location.getId() == existingLocation.getId())
                .toList()
                .isEmpty();
    }

    public Set<Long> getLocationNeighborsSetFromLocationDetails(LocationDetailsDTO locationDetailsDTO) {
        if(locationDetailsDTO.getNeighborhood_info() == null) {
            return null;
        }

        return locationDetailsDTO
                .getNeighborhood_info()
                .stream()
                .map(LocationSearchResultDTO::getLocation_id)
                .collect(Collectors.toSet());
    }

    public void addLocationImages(Location location) {
        Optional<ImagesSearchDTO> imagesOptional = this.tripadvisorService.searchLocationImages(location.getTripadvisorId());
        List<LocationImage> newImages = new ArrayList<>();

        imagesOptional.ifPresent(imagesDTO -> imagesDTO.getData().forEach(imageDTO -> {
            LocationImage locationImage = this.locationImageService.createLocationImageFromDTO(imageDTO);
            if(locationImage != null) {
                locationImage.setLocation(location);
                newImages.add(locationImage);
            }
        }));

        location.setLocationImages(newImages);
    }

    public void addLocationType(Location location, CategoryDTO categoryDTO) {
        LocationType locationType = this.locationTypeService.saveLocationTypeOrGetExisting(categoryDTO);
        location.setLocationType(locationType);
    }

    public void addLocationTagsFromSubcategories(Location location, List<CategoryDTO> subcategoryDTOS) {
        if(subcategoryDTOS == null) {
            return;
        }

        if(subcategoryDTOS.isEmpty()) {
            return;
        }

        subcategoryDTOS.forEach(categoryDTO -> {
            LocationTag locationTag = this.locationTagService.saveLocationTagOrGetExistingFromCategory(categoryDTO);
            location.getLocationTags().add(locationTag);
        });
    }

    public boolean checkIfLocationIsHotelOrRestaurant(Location location) {
        return location.getLocationType().getType().equalsIgnoreCase("hotel") ||
                location.getLocationType().getType().equalsIgnoreCase("restaurant");
    }

    public void addLocationTagsFromGroups(Location location, List<GroupDTO> groupDTOS) {
        if(groupDTOS == null) {
            return;
        }

        groupDTOS.forEach(groupDTO -> {
            if(groupDTO.getName().equalsIgnoreCase("other") ||
                groupDTO.getName().equalsIgnoreCase("others")) return;

            List<LocationTag> locationTags = this.locationTagService.saveLocationsTagsOrGetExistingFromGroup(groupDTO);
            location.getLocationTags().addAll(locationTags);
        });
    }

    public String formatLocationFeaturesToString(LocationDetailsDTO locationDetailsDTO) {
        if(locationDetailsDTO.getFeatures() == null) {
            return "";
        }
        if(locationDetailsDTO.getFeatures().isEmpty()) {
            return "";
        }
        return Utils.joinListOfStrings(locationDetailsDTO.getFeatures(), ";");
    }

    public String formatLocationAmenitiesToString(LocationDetailsDTO locationDetailsDTO) {
        if(locationDetailsDTO.getAmenities() == null) {
            return "";
        }

        if(locationDetailsDTO.getAmenities().isEmpty()) {
            return "";
        }

        return Utils.joinListOfStrings(locationDetailsDTO.getAmenities(), ";");
    }

    public String formatLocationStylesToString(LocationDetailsDTO locationDetailsDTO) {
        if(locationDetailsDTO.getStyles() == null) {
            return "";
        }

        if(locationDetailsDTO.getStyles().isEmpty()) {
            return "";
        }

        return Utils.joinListOfStrings(locationDetailsDTO.getStyles(), ";");
    }

    public String formatRestaurantCuisinesToString(LocationDetailsDTO locationDetailsDTO) {
        if(locationDetailsDTO.getCuisine() == null) {
            return "";
        }

        if(locationDetailsDTO.getCuisine().isEmpty()) {
            return "";
        }

        List<String> cuisinesList = locationDetailsDTO.getCuisine()
                .stream()
                .map(CategoryDTO::getLocalized_name)
                .collect(Collectors.toList());

        return Utils.joinListOfStrings(cuisinesList, ";");
    }

    public String formatWorkHoursToString(LocationDetailsDTO locationDetailsDTO) {
        WorkHoursDTO workHoursDTO = locationDetailsDTO.getHours();
        if(workHoursDTO == null) {
            return "";
        }

        if(workHoursDTO.getWeekday_text() == null) {
            return "";
        }

        return Utils.joinListOfStrings(workHoursDTO.getWeekday_text(), ";");
    }

    public boolean testDistanceBetweenDestinationAndAlreadyExistingLocation(Destination destination,
                                                                            Location location) {
        GeoLocationDTO existingGeoLocation = new GeoLocationDTO();
        existingGeoLocation.setLatitude(location.getLatitude());
        existingGeoLocation.setLongitude(location.getLongitude());
        if(!testDistanceBetweenDestinationAndLocation(destination, existingGeoLocation)) {
            log.warn("Location {} with tripadvisorId {} too far from destination {}", location.getName(),
                    location.getTripadvisorId(), destination.getName());
            return false;
        }
        log.info("Location {} with tripadvisorId {} is close to destination {}", location.getName(),
                location.getTripadvisorId(), destination.getName());
        return true;
    }

    public boolean testDistanceBetweenDestinationAndNewLocation(Destination destination,
                                                                LocationDetailsDTO locationDetailsDTO) {
        GeoLocationDTO existingGeoLocation = new GeoLocationDTO();
        existingGeoLocation.setLatitude(locationDetailsDTO.getLatitude());
        existingGeoLocation.setLongitude(locationDetailsDTO.getLongitude());
        if(!testDistanceBetweenDestinationAndLocation(destination, existingGeoLocation)) {
            log.warn("Location {} with tripadvisorId {} too far from destination {}", locationDetailsDTO.getName(),
                    locationDetailsDTO.getLocation_id(), destination.getName());
            return false;
        }
        log.info("Location {} with tripadvisorId {} is close to destination {}", locationDetailsDTO.getName(),
                locationDetailsDTO.getLocation_id(), destination.getName());
        return true;
    }

    public boolean testDistanceBetweenDestinationAndLocation(Destination destination,
                                                             GeoLocationDTO geoLocationDTO) {
        double distance = Utils.haversineDistance(
                destination.getLatitude(),
                destination.getLongitude(),
                geoLocationDTO.getLatitude(),
                geoLocationDTO.getLongitude()
        );

        return !(distance > Constants.DISTANCE_LIMIT_KM);
    }


    public Location saveLocation(Location location) {
        return this.locationRepository.save(location);
    }

    public ResponseEntity<ResponseDTO> deleteLocationById(int id) throws ResourceNotFoundException {

        Optional<Location> locationOptional = this.locationRepository.findById(id);

        if(locationOptional.isEmpty()) {
            throw new ResourceNotFoundException(STR."Location with id \{id} not found.");
        }

        Location location = locationOptional.get();
        List<Destination> destinations = location.getDestinations();
        destinations.forEach(destination -> destination.getLocations().remove(location));

        this.locationRepository.deleteById(id);

        return new ResponseEntity<>(new ResponseDTO(STR."Location \{id} removed successfully"),
                HttpStatus.OK);

    }

    public ResponseEntity<OneLocationDTO> getLocationByIdResponseEntity(int id)
            throws ResourceNotFoundException {
        Location location = findLocationById(id);

        int oldNumberOfViews = location.getNumberOfViews();
        location.setNumberOfViews(oldNumberOfViews + 1);
        this.locationRepository.save(location);

        return new ResponseEntity<>(new OneLocationDTO(location), HttpStatus.OK);
    }

    public boolean checkIfLocationBelongsToDestination(Destination destination, Location location) {
        return !destination.getLocations()
                .stream()
                .filter(locationCurr -> locationCurr.getId() == location.getId())
                .toList().isEmpty();
    }

    public Location findLocationById(int id) throws ResourceNotFoundException {
        Optional<Location> locationOptional = this.locationRepository.findById(id);
        if(locationOptional.isEmpty()) {
            throw new ResourceNotFoundException(STR."Location with id \{id} not found.");
        }

        return locationOptional.get();
    }

    public ResponseEntity<MultipleLocationsDTO> getAllLocations() {
        return new ResponseEntity<>(new MultipleLocationsDTO(this.locationRepository.findAll()), HttpStatus.OK);
    }

    public Optional<Location> findByTripadvisorId(long tripadvisorId) {
        return this.locationRepository.findByTripadvisorId(tripadvisorId);
    }

    public ResponseEntity<RatingDTO> getLocationRating(int id) throws ResourceNotFoundException {
        Optional<Location> locationOptional = this.locationRepository.findById(id);
        if(locationOptional.isEmpty()) {
            throw new ResourceNotFoundException(STR."Location with id \{id} not found");
        }
        RatingDTO locationRating = this.reviewService.createRatingDTOForLocation(locationOptional.get());
        return new ResponseEntity<>(locationRating, HttpStatus.OK);
    }

    public String createStringFromLocationTags(Location location) {
        return Utils.joinListOfStrings(location.getLocationTags().stream().map(LocationTag::getName).toList(), ", ");
    }

    public Map<Location, LocationPopularityDTO> getLocationsPopularityForDestination(List<Location> destinationLocations) {
        Map<Location, Double> locationsPopularityBasedOnViews = getLocationsPopularityBasedOnViews(destinationLocations);
        Map<Location, Double> locationsPopularityBasedOnReviews = getLocationsPopularityBasedOnReviews(destinationLocations);

        return computeLocationPopularityBasedOnViewsAndReviews(destinationLocations, locationsPopularityBasedOnViews,
                locationsPopularityBasedOnReviews);
    }

    public Map<Location, LocationPopularityDTO> computeLocationPopularityBasedOnViewsAndReviews(List<Location> locations,
            Map<Location, Double> locationsPopularityBasedOnViews,
            Map<Location, Double> locationsPopularityBasedOnReviews) {
        Map<Location, LocationPopularityDTO> locationsPopularity = new HashMap<>();
        locations.forEach(location -> {
            double popularity = locationsPopularityBasedOnViews.get(location) * 0.5
                    + locationsPopularityBasedOnReviews.get(location) * 0.5;
            LocationPopularityDTO locationPopularityDTO = new LocationPopularityDTO();
            locationPopularityDTO.setPopularityScore(popularity);
            locationPopularityDTO.setLocationsReviewsScore(locationsPopularityBasedOnReviews.get(location));
            locationPopularityDTO.setLocationsViewsScore(locationsPopularityBasedOnViews.get(location));
            locationsPopularity.put(location, locationPopularityDTO);
        });

        return locationsPopularity;
    }

    public Map<Location, Double> getLocationsPopularityBasedOnViews(List<Location> locations) {
        Map<Location, Integer> locationsPopularityByLocationsViews = new HashMap<>();
        locations.forEach(location -> locationsPopularityByLocationsViews.put(location, location.getNumberOfViews()));
        Optional<Integer> maxValueOptional = locationsPopularityByLocationsViews.values().stream().max(Integer::compareTo);
        int maxPopularity = maxValueOptional.orElse(0);
        Optional<Integer> minValueOptional = locationsPopularityByLocationsViews.values().stream().min(Integer::compareTo);
        int minPopularity= minValueOptional.orElse(0);
        Map<Location, Double> locationsViewsPopularityNormalized = new HashMap<>();
        locationsPopularityByLocationsViews.forEach((key, value) -> {
            if(value == 0) {
                locationsViewsPopularityNormalized.put(key, 0.0);
            } else {
                double normalizedPopularity = Utils.normalize(value, minPopularity,
                        maxPopularity, 0, 5);
                locationsViewsPopularityNormalized.put(key, normalizedPopularity);
            }
        });
        return locationsViewsPopularityNormalized;
    }

    public Map<Location, Double> getLocationsPopularityBasedOnReviews(List<Location> locations) {
        Map<Location, Double> locationsRatings = new HashMap<>();
        locations.forEach(location -> {
            RatingDTO ratingDTO = this.reviewService.createRatingDTOForLocation(location);
            locationsRatings.put(location, ratingDTO.getRating());
        });
        return locationsRatings;
    }

    public Location getClosestLocationFromList(Location referenceLocation, List<Location> locations) {
        if(referenceLocation == null) return null;
        if(locations == null) return null;
        if(locations.isEmpty()) return null;

        Map<Location, Double> distanceToReference = new HashMap<>();
        locations.forEach(location -> distanceToReference.put(location, getDistanceBetweenLocations(referenceLocation, location)));

        return locations.stream()
                .sorted(Comparator.comparingDouble(distanceToReference::get))
                .toList()
                .getFirst();
    }

    public List<Location> getClosestLocationsFromList(Location referenceLocation, List<Location> locations, int limit) {
        if(referenceLocation == null) return null;
        if(locations == null) return null;
        if(locations.isEmpty()) return null;

        Map<Location, Double> distanceToReference = new HashMap<>();

        List<Location> locationsToConsider = locations
            .stream()
            .filter(location -> referenceLocation.getId() != location.getId()).toList();

        locationsToConsider
            .forEach(location -> distanceToReference.put(location, getDistanceBetweenLocations(referenceLocation, location)));

        List<Location> sortedLocations = locationsToConsider.stream()
            .filter(location -> referenceLocation.getId() != location.getId())
            .sorted(Comparator.comparingDouble(distanceToReference::get))
            .toList();

        return sortedLocations.subList(0, Math.min(sortedLocations.size(), limit));
    }

    public double getDistanceBetweenLocations(Location firstLocation, Location secondLocation) {
        return Utils.haversineDistance(firstLocation.getLatitude(), firstLocation.getLongitude(),
                secondLocation.getLatitude(), secondLocation.getLongitude());
    }

    public ResponseEntity<OneLocationDTO> addImagesToLocation(int id) throws ResourceNotFoundException {
        Location location = findLocationById(id);
        OneLocationDTO locationDTO = new OneLocationDTO();

        if(location.getLocationImages() == null) {
            addLocationImages(location);
            Location savedLocation = this.locationRepository.save(location);
            locationDTO.setLocation(savedLocation);
            return new ResponseEntity<>(locationDTO, HttpStatus.OK);
        }

        if(location.getLocationImages().isEmpty()) {
            addLocationImages(location);
            Location savedLocation = this.locationRepository.save(location);
            locationDTO.setLocation(savedLocation);
            return new ResponseEntity<>(locationDTO, HttpStatus.OK);
        }

        return new ResponseEntity<>(locationDTO, HttpStatus.OK);
    }

    public ResponseEntity<LocationFactsDTO> getLocationFacts(int id) throws ResourceNotFoundException {
        Location location = findLocationById(id);
        LocationFactsDTO locationFactsDTO = new LocationFactsDTO();
        locationFactsDTO.setFacts(new ArrayList<>());

        if(location.getLocationType() != null) {
            if(location.getLocationType().getType().equalsIgnoreCase(Constants.RESTAURANT_TYPE)) {
                locationFactsDTO.getFacts().add(STR."This page was visited for \{location.getNumberOfViews()} times.");
                List<String> cuisines = Utils.tokenizeString(location.getCuisine(), ";");
                if(!cuisines.isEmpty()) {
                    if(cuisines.size() == 1) {
                        locationFactsDTO.getFacts().add("It's a restaurant with one type of cuisine.");
                    } else {
                        locationFactsDTO.getFacts().add(STR."It's a restaurant with \{cuisines.size()} types of cuisines.");
                    }
                }
                List<String> features = Utils.tokenizeString(location.getFeatures(), ";");
                if(!features.isEmpty()) {
                    if(features.size() == 1) {
                        locationFactsDTO.getFacts().add(STR."\{location.getName()} it's a restaurant with one feature.");
                    } else {
                        locationFactsDTO.getFacts().add(location.getName() + STR." it's a restaurant with \{features.size()} features.");
                    }
                }
            } else if(location.getLocationType().getType().equalsIgnoreCase(Constants.HOTEL_TYPE)) {
                locationFactsDTO.getFacts().add(STR."This page was visited for \{location.getNumberOfViews()} times.");
                List<String> styles = Utils.tokenizeString(location.getStyles(), ";");
                if(!styles.isEmpty()) {
                    if(styles.size() == 1) {
                        locationFactsDTO.getFacts().add(STR."\{location.getName()} benefits of one style.");
                    } else {
                        locationFactsDTO.getFacts().add(location.getName() + STR." benefites of \{styles.size()} styles.");
                    }
                }
                List<String> amenities = Utils.tokenizeString(location.getAmenities(), ";");
                if(!amenities.isEmpty()) {
                    if(amenities.size() == 1) {
                        locationFactsDTO.getFacts().add(STR."\{location.getName()} benefits of one amenity.");
                    } else {
                        locationFactsDTO.getFacts().add(location.getName() + STR." benefites of \{amenities.size()} amenities.");
                    }
                }
            } else if(location.getLocationType().getType().equalsIgnoreCase(Constants.ATTRACTION_TYPE)) {
                locationFactsDTO.getFacts().add(STR."This page was visited for \{location.getNumberOfViews()} times.");
                List<LocationTag> tags = location.getLocationTags();
                if(tags != null) {
                    if (!tags.isEmpty()) {
                        if (tags.size() == 1) {
                            locationFactsDTO.getFacts().add(STR."\{location.getName()} it's an attraction described by one keyword.");
                        } else {
                            locationFactsDTO.getFacts().add(location.getName() + STR." it's an attraction described by \{tags.size()} keywords.");
                        }
                    }
                }
            }
        }

        return new ResponseEntity<>(locationFactsDTO, HttpStatus.OK);
    }

    public ResponseEntity<SimilarLocationsDTO> getSimilarLocations(int id) throws ResourceNotFoundException {
        Location location = findLocationById(id);
        Set<LocationTag> referenceLocationTags = new HashSet<>(location.getLocationTags());

        Destination destination = location.getDestinations().getFirst();
        List<Location> locationsWithSameCategory = destination.getLocations()
            .stream()
            .filter(loc -> {
                if(loc.getLocationType() == null) {
                    return false;
                }
                if(loc.getId() == location.getId()) {
                    return false;
                }
                return loc.getLocationType().getType().equalsIgnoreCase(location.getLocationType().getType());
            })
            .toList();

        List<Location> similarLocations = locationsWithSameCategory
            .stream()
            .filter(simLocation -> !simLocation.getLocationTags()
                    .stream()
                    .filter(referenceLocationTags::contains)
                    .toList().isEmpty())
            .toList();

        similarLocations = similarLocations.subList(0, Math.min(similarLocations.size(), 5));

        List<Location> nearbyLocations = getClosestLocationsFromList(location,
            destination.getLocations().stream().filter(fLocation -> {
                if(fLocation.getLocationType() == null) {
                    return false;
                }
                return !fLocation.getLocationType().getType().equalsIgnoreCase(Constants.HOTEL_TYPE);
            }).toList(),
            5);

        SimilarLocationsDTO similarLocationsDTO = new SimilarLocationsDTO();
        similarLocationsDTO.setLocations(new ArrayList<>());
        LocationsByCriteriaDTO simLocDTO = new LocationsByCriteriaDTO();
        simLocDTO.setCriteria("Similar locations");
        simLocDTO.setLocations(similarLocations);
        similarLocationsDTO.getLocations().add(simLocDTO);
        LocationsByCriteriaDTO nearbyLocDTO = new LocationsByCriteriaDTO();
        nearbyLocDTO.setCriteria("Nearby locations");
        nearbyLocDTO.setLocations(nearbyLocations);
        similarLocationsDTO.getLocations().add(nearbyLocDTO);

        return new ResponseEntity<>(similarLocationsDTO, HttpStatus.OK);
    }

    public ResponseEntity<LocationPopularityDTO> getLocationPopularity(int id) throws ResourceNotFoundException {
        Location location = findLocationById(id);
        Destination destination = location.getDestinations().getFirst();
        Map<Location, LocationPopularityDTO> locationsPopularity =
                getLocationsPopularityForDestination(destination.getLocations());

        LocationPopularityDTO locationPopularityDTO = new LocationPopularityDTO();
        locationPopularityDTO.setLocationsViewsScore(0);
        locationPopularityDTO.setLocationsReviewsScore(0);
        locationPopularityDTO.setPopularityScore(0);

        if(!locationsPopularity.containsKey(location)) {
            return new ResponseEntity<>(locationPopularityDTO, HttpStatus.OK);
        }
        locationsPopularity.get(location).setRanking(location.getRanking());
        return new ResponseEntity<>(locationsPopularity.get(location), HttpStatus.OK);
    }

    public List<Location> saveAllLocations(List<Location> locations) {
        return this.locationRepository.saveAll(locations);
    }

}
