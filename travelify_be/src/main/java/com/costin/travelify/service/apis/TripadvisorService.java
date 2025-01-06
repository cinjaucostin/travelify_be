package com.costin.travelify.service.apis;

import com.costin.travelify.dto.tripadvisor_dto.*;
import com.costin.travelify.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TripadvisorService {
    private static final Logger log = LoggerFactory.getLogger(TripadvisorService.class);

    @Value("${tripadvisor_api_key1}")
    private String tripadvisorApiKey1;

    private Optional<LocationSearchDTO> searchLocation(String query, String category) {
        String getLocationsURL = String.format("https://api.content.tripadvisor.com/api/v1/location/search?searchQuery=%s&category=%s&language=en&key=%s",
                query, category, tripadvisorApiKey1);

        log.warn("Getting locations for query = {}, category = {} from TripAdvisor ", query, category);
        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<LocationSearchDTO> response = restTemplate.getForEntity(getLocationsURL, LocationSearchDTO.class);
            HttpStatusCode responseCode = response.getStatusCode();
            if (responseCode == HttpStatus.OK) {
                return Optional.ofNullable(response.getBody());
            } else if (responseCode == HttpStatus.TOO_MANY_REQUESTS) {
                Thread.sleep(300);
            }
        } catch (HttpClientErrorException e) {
            HttpStatusCode responseCode = e.getStatusCode();
            log.warn("Getting locations for query = {}, category = {}, didn't work, status code = {} ", query, category, responseCode);
            System.err.println();
            return Optional.empty();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return Optional.empty();
    }

    public Optional<LocationDetailsDTO> searchLocationDetails(long tripadvisorId) {
        String getLocationDetailsURL = String.format("https://api.content.tripadvisor.com/api/v1/location/%d/details?language=en&currency=USD&key=%s",
                tripadvisorId, tripadvisorApiKey1);
        log.info("Getting locations details for location with tripadvisorId {}", tripadvisorId);
        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<LocationDetailsDTO> response = restTemplate.getForEntity(getLocationDetailsURL, LocationDetailsDTO.class);
            HttpStatusCode responseCode = response.getStatusCode();
            if (responseCode == HttpStatus.OK) {
                log.info("Getting locations details for location with tripadvisorId {} ended successfully", tripadvisorId);
                return Optional.ofNullable(response.getBody());
            } else if (responseCode == HttpStatus.TOO_MANY_REQUESTS) {
                Thread.sleep(300);
            }
        } catch (HttpClientErrorException e) {
            HttpStatusCode responseCode = e.getStatusCode();
            log.warn("Getting location details for tripadvisorId {} didn't work, status code = {}", tripadvisorId, responseCode);
            return Optional.empty();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    public Optional<ImagesSearchDTO> searchLocationImages(long tripadvisorId) {
        String getImagesForLocationURL = String.format("https://api.content.tripadvisor.com/api/v1/location/%d/photos?language=en&key=%s",
                tripadvisorId,
                tripadvisorApiKey1
        );

        log.info("Getting locations images for location with tripadvisorId {}", tripadvisorId);
        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<ImagesSearchDTO> response = restTemplate.getForEntity(getImagesForLocationURL, ImagesSearchDTO.class);
            HttpStatusCode responseCode = response.getStatusCode();
            if (responseCode == HttpStatus.OK) {
                log.info("Getting locations images for location with tripadvisorId {} ended successfully", tripadvisorId);
                return Optional.ofNullable(response.getBody());
            } else if (responseCode == HttpStatus.TOO_MANY_REQUESTS) {
                Thread.sleep(300);
            }
        } catch (HttpClientErrorException e) {
            HttpStatusCode responseCode = e.getStatusCode();
            log.warn("Getting location images for tripadvisorId {} didn't work, status code = {}", tripadvisorId, responseCode);
            return Optional.empty();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    public Optional<List<LocationSearchResultDTO>> searchLocationsByCategoryFromLocationWithoutFetchingDetails(
            String city, String locationCategory, int limit) {
        log.info("Searching for {} {} from {} without details", limit, locationCategory, city);
        Optional<LocationSearchDTO> searchResultOptional = searchLocation(city, locationCategory);

        if (searchResultOptional.isEmpty()) {
            log.info("Searching for {} {} from {} ended without any results", limit, locationCategory, city);
            return Optional.empty();
        }

        log.info("Searching for {} {} from {} ended successfully", limit, locationCategory, city);
        List<LocationSearchResultDTO> locationsFetched = searchResultOptional.get().getData();
        if (locationsFetched.size() > limit) {
            return Optional.of(locationsFetched.subList(0, limit));
        }

        return Optional.of(locationsFetched);
    }

    public Optional<List<LocationDetailsDTO>> searchLocationsByCategoryFromLocation(String city, String locationCategory, int limit) {
        log.info("Searching for maximum {} {} from {}", limit, locationCategory, city);
        Optional<LocationSearchDTO> searchResultOptional = searchLocation(city, locationCategory);
        List<LocationDetailsDTO> locationsDetails = new ArrayList<>();

        if (searchResultOptional.isEmpty()) {
            log.info("Searching for {} from {} ended without any results", locationCategory, city);
            return Optional.empty();
        }

        LocationSearchDTO searchResult = searchResultOptional.get();
        List<LocationSearchResultDTO> locations = searchResult.getData().subList(0, limit);
        log.info("Fetched details for {} {} from {}", locations.size(), locationCategory, city);

        locations.forEach(location -> {
            log.info("Fetching details for tripadvisorId {}", location.getLocation_id());
            Optional<LocationDetailsDTO> locationOptional = searchLocationDetails(location.getLocation_id());
            locationOptional.ifPresent(locationsDetails::add);
        });

        if (locationsDetails.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(locationsDetails);
    }

    public Optional<LocationDetailsDTO> searchCity(String city) {
        log.info("Searching in TripAdvisor for city {}", city);
        Optional<LocationSearchDTO> searchResultOptional = searchLocation(city, Constants.TRIPADVISOR_DESTINATION_CATEGORY);

        if (searchResultOptional.isEmpty()) {
            return Optional.empty();
        }

        LocationSearchDTO searchResult = searchResultOptional.get();
        List<LocationSearchResultDTO> locations = searchResult.getData();

        if (locations == null) {
            return Optional.empty();
        }
        if (locations.isEmpty()) {
            return Optional.empty();
        }

        if (locations.size() > Constants.MAX_SEARCH_FOR_CITY_LIMIT)
            locations = locations.subList(0, Constants.MAX_SEARCH_FOR_CITY_LIMIT);

        for (LocationSearchResultDTO location : locations) {
            long tripadvisorLocationId = location.getLocation_id();
            log.info("Searching in TripAdvisor for location with id {}", tripadvisorLocationId);
            Optional<LocationDetailsDTO> locationDetailsOptional = searchLocationDetails(tripadvisorLocationId);
            if (locationDetailsOptional.isPresent()) {
                {
                    List<CategoryDTO> subcategories = locationDetailsOptional.get().getSubcategory();
                    boolean citySubcategoryFound = subcategories.stream()
                            .anyMatch(subcategory -> subcategory.getName().equalsIgnoreCase("city") ||
                                    subcategory.getName().equalsIgnoreCase("municipality"));
                    if (citySubcategoryFound) {
                        log.info("City found in TripAdvisor for location id {}", tripadvisorLocationId);
                        return locationDetailsOptional;
                    }
                }
            }
        }

        return Optional.empty();
    }

    public Optional<LocationSearchResultDTO> searchSingleAttractionByNameWithoutDetails(String locationName) {
        log.info("Searching for single attraction in TripAdvisor for name {}", locationName);
        Optional<LocationSearchDTO> searchResultOptional = searchLocation(locationName, Constants.TRIPADVISOR_ATTRACTION_CATEGORY);

        if (searchResultOptional.isEmpty()) {
            return Optional.empty();
        }

        LocationSearchDTO searchResult = searchResultOptional.get();
        List<LocationSearchResultDTO> locations = searchResult.getData();

        if (locations == null) {
            return Optional.empty();
        }
        if (locations.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(locations.getFirst());
    }

}
