package com.costin.travelify.service;

import com.costin.travelify.dto.response_dto.RatingDTO;
import com.costin.travelify.dto.response_dto.SearchResponseDTO;
import com.costin.travelify.dto.response_dto.SearchResultDTO;
import com.costin.travelify.entities.*;
import com.costin.travelify.utils.Constants;
import com.costin.travelify.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class SearchService {
    private static final Logger log = LoggerFactory.getLogger(SearchService.class);
    @Autowired
    private DestinationService destinationService;
    @Autowired
    private LocationService locationService;
    @Autowired
    private TripService tripService;
    @Autowired
    private ReviewService reviewService;

    public ResponseEntity<SearchResponseDTO> search(String query, String type, String order) {
        SearchResponseDTO searchResponseDTO = new SearchResponseDTO();
        searchResponseDTO.setResults(new ArrayList<>());
        List<String> typesToFilter = Utils.tokenizeString(type, ",");

        if(typesToFilter.isEmpty()) {
            List<SearchResultDTO> destinationsSearchResults = this.fromDestinationsToSearchResults(this.destinationService
                    .searchDestinations(query), query);
            searchResponseDTO.getResults().addAll(destinationsSearchResults);
            List<SearchResultDTO> locationsSearchResults = this.fromLocationsToSearchResults(this.locationService
                    .searchLocation(query, ""), query);
            searchResponseDTO.getResults().addAll(locationsSearchResults);
            List<SearchResultDTO> tripsSearchResults = this.fromTripsToSearchResults(this.tripService
                    .searchTrips(query), query);
            searchResponseDTO.getResults().addAll(tripsSearchResults);
        }

        if(typesToFilter.contains("destinations")) {
            List<SearchResultDTO> destinationsSearchResults = this.fromDestinationsToSearchResults(this.destinationService
                    .searchDestinations(query), query);
            searchResponseDTO.getResults().addAll(destinationsSearchResults);
        }

        if(typesToFilter.contains("hotels")) {
            List<SearchResultDTO> hotelsSearchResults = this.fromLocationsToSearchResults(this.locationService
                    .searchLocation(query, "hotel"), query);
            searchResponseDTO.getResults().addAll(hotelsSearchResults);
        }

        if(typesToFilter.contains("restaurants")) {
            List<SearchResultDTO> restaurantsSearchResults = this.fromLocationsToSearchResults(this.locationService
                    .searchLocation(query, "restaurant"), query);
            searchResponseDTO.getResults().addAll(restaurantsSearchResults);
        }

        if(typesToFilter.contains("attractions")) {
            List<SearchResultDTO> attractionsSearchResult = this.fromLocationsToSearchResults(this.locationService
                    .searchLocation(query, "attraction"), query);
            searchResponseDTO.getResults().addAll(attractionsSearchResult);
        }

        if(typesToFilter.contains("trips")) {
            List<SearchResultDTO> tripsSearchResults = this.fromTripsToSearchResults(this.tripService
                    .searchTrips(query), query);
            searchResponseDTO.getResults().addAll(tripsSearchResults);
        }

        if(order.equalsIgnoreCase("relevance")) {
            sortAndAddResultTypeToSearchResultsForRelevanceOrder(searchResponseDTO);
        } else if(order.equalsIgnoreCase("rating")) {
            sortAndAddResultTypeToSearchResultsForRatingOrder(searchResponseDTO);
        }

        return new ResponseEntity<>(searchResponseDTO, HttpStatus.OK);
    }

    public void sortAndAddResultTypeToSearchResultsForRelevanceOrder(SearchResponseDTO searchResponseDTO) {
        searchResponseDTO.getResults().sort(Comparator.comparingDouble(SearchResultDTO::getMatchScore));
        Optional<SearchResultDTO> bestResultOptional = searchResponseDTO.getResults()
                .stream()
                .min(Comparator.comparingDouble(SearchResultDTO::getMatchScore));
        bestResultOptional.ifPresent(bestResult -> {
            double bestMatchScore = bestResult.getMatchScore();
            searchResponseDTO.getResults().forEach(result -> {
                if(result.getMatchScore() == bestMatchScore) {
                    result.setResultType("Top result");
                } else {
                    result.setResultType("Matches search");
                }
            });
        });
    }

    public void sortAndAddResultTypeToSearchResultsForRatingOrder(SearchResponseDTO searchResponseDTO) {
        searchResponseDTO.getResults().sort(Comparator.comparingDouble(SearchResultDTO::getRating).reversed());
        Optional<SearchResultDTO> bestResultOptional = searchResponseDTO.getResults()
                .stream()
                .max(Comparator.comparingDouble(SearchResultDTO::getRating));
        bestResultOptional.ifPresent(bestResult -> {
            double bestRating = bestResult.getRating();
            searchResponseDTO.getResults().forEach(result -> {
                if(result.getRating() == bestRating) {
                    result.setResultType("Top result");
                } else {
                    result.setResultType("Matches search");
                }
            });
        });
    }

    public ResponseEntity<SearchResponseDTO> advancedSearch(String query) {
        Destination savedDestination = this.destinationService.fetchDestinationFromTripadvisor(query);

        this.destinationService.populateDestinationWithLinkedLocations(savedDestination);

        return this.search(query, "", "");
    }

    public List<SearchResultDTO> fromDestinationsToSearchResults(List<Destination> destinations, String query) {
        return destinations.stream()
            .map(destination -> {
                SearchResultDTO searchResult = new SearchResultDTO();
                searchResult.setId(destination.getId());
                searchResult.setTripadvisorId(destination.getTripadvisorId());
                searchResult.setName(destination.getName());
                searchResult.setAddress(destination.getAddressPath());
                if(destination.getDestinationType() != null) {
                    searchResult.setType(destination.getDestinationType().getName());
                }
                Optional<LocationImage> destinationImageOptional = this.destinationService.getDestinationImage(destination);
                destinationImageOptional.ifPresent(locationImage -> searchResult.setImage(locationImage.getSource()));
                RatingDTO ratingDTO = this.reviewService.createRatingDTOForDestination(destination);
                searchResult.setRating(ratingDTO.getRating());
                searchResult.setReviews(ratingDTO.getTravelifyReviews().size());
                searchResult.setPopularity(destination.getPopularity());
                searchResult.setCategories("");
                List<LocationTag> mostCommonTagsForDestination = this.destinationService.getDestinationMostCommonTags(destination);
                searchResult.setCategories(Utils.joinListOfStrings(
                        mostCommonTagsForDestination.stream().map(LocationTag::getName).toList(),
                        ", ")
                );
                searchResult.setMatchScore(Utils.levenshteinDistance(query.toLowerCase(), destination.getName().toLowerCase()));

                return searchResult;
            }).toList();
    }

    public List<SearchResultDTO> fromLocationsToSearchResults(List<Location> locations, String query) {
        return locations.stream()
            .map(location -> {
                SearchResultDTO searchResult = new SearchResultDTO();
                searchResult.setId(location.getId());
                searchResult.setTripadvisorId(location.getTripadvisorId());
                searchResult.setName(location.getName());
                searchResult.setAddress(location.getAddressPath());
                searchResult.setType(location.getLocationType().getName());
                if(location.getLocationImages() != null) {
                    List<LocationImage> locationImages = location.getLocationImages();
                    if(!locationImages.isEmpty()) {
                        searchResult.setImage(locationImages.getFirst().getSource());
                    }
                }
                RatingDTO ratingDTO = this.reviewService.createRatingDTOForLocation(location);
                searchResult.setRating(ratingDTO.getRating());
                searchResult.setPopularity(location.getPopularity());
                if(location.getTripadvisorNumberOfReviews() != null) {
                    searchResult.setReviews(ratingDTO.getTravelifyReviews().size() + location.getTripadvisorNumberOfReviews());
                } else {
                    searchResult.setReviews(ratingDTO.getTravelifyReviews().size());
                }
                searchResult.setCategories(this.locationService.createStringFromLocationTags(location));
                searchResult.setMatchScore(Utils.levenshteinDistance(query.toLowerCase(), location.getName().toLowerCase()));

                return searchResult;
            }).toList();
    }

    public List<SearchResultDTO> fromTripsToSearchResults(List<Trip> trips, String query) {
        return trips.stream()
            .map(trip -> {
                SearchResultDTO searchResult = new SearchResultDTO();
                searchResult.setId(trip.getId());
                searchResult.setTripadvisorId(-1);
                searchResult.setName(trip.getName());
                searchResult.setAddress(trip.getDestination().getAddressPath());
                searchResult.setType(Constants.TRIP_CATEGORY);
                Optional<LocationImage> destinationImageOptional = this.destinationService.getDestinationImage(trip.getDestination());
                destinationImageOptional.ifPresent(locationImage -> searchResult.setImage(locationImage.getSource()));
                RatingDTO ratingDTO = this.reviewService.createRatingDTOForTrip(trip);
                searchResult.setRating(ratingDTO.getRating());
                searchResult.setReviews(ratingDTO.getTravelifyReviews().size());
                searchResult.setPopularity(ratingDTO.getRating());
                searchResult.setCategories(Utils.joinListOfStrings(
                        trip.getTripObjectivesTags().stream().map(LocationTag::getName).toList(),
                        ", ")
                );
                searchResult.setMatchScore(Utils.levenshteinDistance(query.toLowerCase(), trip.getName().toLowerCase()));

                return searchResult;
            }).toList();
    }

}
