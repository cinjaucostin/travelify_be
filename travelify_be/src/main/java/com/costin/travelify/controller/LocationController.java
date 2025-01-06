package com.costin.travelify.controller;

import com.costin.travelify.dto.response_dto.*;
import com.costin.travelify.entities.Location;
import com.costin.travelify.exceptions.ResourceNotFoundException;
import com.costin.travelify.service.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/locations")
@CrossOrigin
public class LocationController {

    @Autowired
    private LocationService locationService;

    @GetMapping
    public ResponseEntity<MultipleLocationsDTO> getLocations() {
        return this.locationService.getAllLocations();
    }

    @GetMapping("/{id}")
    public ResponseEntity<OneLocationDTO> getLocationById(@PathVariable int id)
            throws ResourceNotFoundException {
        return this.locationService.getLocationByIdResponseEntity(id);
    }

    @GetMapping("/{id}/facts")
    public ResponseEntity<LocationFactsDTO> getLocationsFacts(@PathVariable int id)
            throws ResourceNotFoundException {
        return this.locationService.getLocationFacts(id);
    }

    @GetMapping("/{id}/similar")
    public ResponseEntity<SimilarLocationsDTO> getSimilarLocations(@PathVariable int id)
            throws ResourceNotFoundException {
        return this.locationService.getSimilarLocations(id);
    }

    @GetMapping("/{id}/popularity")
    public ResponseEntity<LocationPopularityDTO> getLocationPopularity(@PathVariable int id)
            throws ResourceNotFoundException {
        return this.locationService.getLocationPopularity(id);
    }

    @PostMapping("/{id}/addImages")
    public ResponseEntity<OneLocationDTO> addImagesToLocation(@PathVariable int id)
        throws ResourceNotFoundException {
        return this.locationService.addImagesToLocation(id);
    }

    @GetMapping("/{id}/rating")
    public ResponseEntity<RatingDTO> getLocationRating(@PathVariable int id)
            throws ResourceNotFoundException {
        return this.locationService.getLocationRating(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResponseDTO> deleteLocationById(@PathVariable int id)
            throws ResourceNotFoundException {
        return this.locationService.deleteLocationById(id);
    }

}
