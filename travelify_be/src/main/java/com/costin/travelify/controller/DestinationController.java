package com.costin.travelify.controller;

import com.costin.travelify.dto.response_dto.*;
import com.costin.travelify.exceptions.ResourceNotFoundException;
import com.costin.travelify.service.DestinationService;
import com.costin.travelify.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/destinations")
@CrossOrigin
public class DestinationController {
    @Autowired
    private DestinationService destinationService;

    @GetMapping("/{id}")
    public ResponseEntity<OneDestinationDTO> getDestinationById(@PathVariable int id)
            throws ResourceNotFoundException, InterruptedException {
        return new ResponseEntity<>(new OneDestinationDTO(this.destinationService.getDestinationById(id)), HttpStatus.OK);
    }

    @GetMapping("/{id}/images")
    public ResponseEntity<ImagesDTO> getDestinationImages(@PathVariable int id)
            throws ResourceNotFoundException {
        return this.destinationService.getDestinationImages(id);
    }

    @GetMapping("/{id}/restaurants")
    public ResponseEntity<MultipleLocationsDTO> getDestinationRestaurants(@PathVariable int id)
            throws ResourceNotFoundException {
        return this.destinationService.getDestinationLocationWithType(id, Constants.RESTAURANT_TYPE);
    }

    @PostMapping("/{id}/addImages")
    public ResponseEntity<OneDestinationDTO> addImagesToDestination(@PathVariable int id)
            throws ResourceNotFoundException {
        return this.destinationService.addImagesToDestination(id);
    }

    @GetMapping("/{id}/hotels")
    public ResponseEntity<MultipleLocationsDTO> getDestinationHotels(@PathVariable int id)
            throws ResourceNotFoundException {
        return this.destinationService.getDestinationLocationWithType(id, Constants.HOTEL_TYPE);
    }

    @GetMapping("/{id}/locations")
    public ResponseEntity<MultipleLocationsDTO> getDestinationLocations(@PathVariable int id)
            throws ResourceNotFoundException {
        return this.destinationService.getDestinationLocations(id);
    }

    @GetMapping("/{id}/attractionsByExperiences")
    public ResponseEntity<AttractionsExperiencesDTO> getDestinationAttractions(@PathVariable int id)
            throws ResourceNotFoundException {
        return this.destinationService.getAttractionsByExperiences(id);
    }

    @GetMapping("/recommendations_popularity")
    public ResponseEntity<DestinationsRecommendationsDTO> getRecommendationsBasedOnPopularity() {
        return this.destinationService.getRecommendationsBasedOnPopularity();
    }

    @GetMapping("/recommendations_weather")
    public ResponseEntity<DestinationsRecommendationsDTO> getRecommendationsBasedOnWeather() {
        return this.destinationService.getRecommendationsBasedOnNearFutureWeather();
    }

    @GetMapping("/{id}/tags")
    public ResponseEntity<DestinationTagsDTO> getDestinationLocationsTags(@PathVariable int id) {
        return this.destinationService.getDestinationLocationsTags(id);
    }

    @GetMapping("/{id}/current_weather")
    public ResponseEntity<WeatherDTO> getDestinationCurrentWeather(@PathVariable int id) {
        return this.destinationService.getDestinationCurrentWeather(id);
    }

    @GetMapping("/{id}/popularity")
    public ResponseEntity<DestinationPopularityDTO> getDestinationRating(@PathVariable int id)
            throws ResourceNotFoundException {
        return this.destinationService.getDestinationPopularity(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResponseDTO> deleteDestinationById(@PathVariable int id) {
        return this.destinationService.deleteDestinationById(id);
    }

}
