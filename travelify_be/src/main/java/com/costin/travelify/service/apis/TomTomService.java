package com.costin.travelify.service.apis;

import com.costin.travelify.dto.tomtom_dto.LocationsSearchTTDTO;
import com.costin.travelify.dto.tomtom_dto.PointOfInterestTTDTO;
import com.costin.travelify.dto.tripadvisor_dto.LocationSearchDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
public class TomTomService {
//    https://api.tomtom.com/search/2/poiSearch/barcelona.json?key=hqxNpzgRNUDH8HURnEzxgyYa4NFts0Bk&lat=41.3851&lon=2.1734&radius=15000&categorySet=7376
    private static final Logger log = LoggerFactory.getLogger(TomTomService.class);
    @Value("${tomtom_api_key}")
    private String tomtomApiKey;

    public Optional<LocationsSearchTTDTO> searchLocation(String query, double latitude, double longitude, int category) {
        String getLocationsURL = String.format("https://api.tomtom.com/search/2/poiSearch/%s.json?key=%s&lat=%f&lon=%f&radius=15000&categorySet=%d",
                query.toLowerCase(), tomtomApiKey, latitude, longitude, category);

        log.warn("Getting locations for query = {}, lat = {}, lon = {}, category = {} from TomTomApi ", query.toLowerCase(), category,
                latitude, longitude);
        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<LocationsSearchTTDTO> response = restTemplate.getForEntity(getLocationsURL, LocationsSearchTTDTO.class);
            HttpStatusCode responseCode = response.getStatusCode();
            if (responseCode == HttpStatus.OK) {
                return Optional.ofNullable(response.getBody());
            } else if(responseCode == HttpStatus.TOO_MANY_REQUESTS) {
                Thread.sleep(300);
            }
        } catch (HttpClientErrorException e) {
            HttpStatusCode responseCode = e.getStatusCode();
            log.warn("Getting locations for query = {}, category = {}, from TomTomApi didn't work, status code = {} ", query, category, responseCode);
            System.err.println();
            return Optional.empty();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return Optional.empty();
    }

    public boolean checkLocationsSearchTTDTO(LocationsSearchTTDTO locationsSearchTTDTO) {
        if(locationsSearchTTDTO.getResults() == null) {
            return false;
        }
        return !locationsSearchTTDTO.getResults().isEmpty();
    }

    public boolean checkPointsOfInterestTTDTO(PointOfInterestTTDTO pointOfInterestTTDTO) {
        if(pointOfInterestTTDTO == null) {
            return false;
        }
        if(pointOfInterestTTDTO.getName() == null) {
            return false;
        }
        return !pointOfInterestTTDTO.getName().isEmpty();
    }

//    7376 - turistic attractions
//    9357 - beach -> for city with sea access.

}
