package com.costin.travelify.controller;

import com.costin.travelify.dto.request_dto.AddDayObjectiveDTO;
import com.costin.travelify.dto.request_dto.GenerateTripDTO;
import com.costin.travelify.dto.request_dto.UpdatePlannificationDTO;
import com.costin.travelify.dto.response_dto.*;
import com.costin.travelify.entities.Trip;
import com.costin.travelify.entities.TripPlannificationDay;
import com.costin.travelify.exceptions.*;
import com.costin.travelify.service.TripService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.text.ParseException;

@RestController
@RequestMapping("/api/trips")
@CrossOrigin
public class TripController {
    @Autowired
    private TripService tripService;

    @GetMapping
    public ResponseEntity<PagedTripsDetailsDTO> getTrips(@RequestParam(name = "user_id", required = false) Integer userId,
                                                         @RequestParam(name = "destination_id", required = false) Integer destinationId,
                                                         @RequestParam(defaultValue = "0") Integer page,
                                                         @RequestParam(defaultValue = "3") Integer size)
            throws BadQueryParametersException, ResourceNotFoundException {
        return this.tripService.getTrips(userId, destinationId, page, size);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Trip> getTripById(@PathVariable int id) throws ResourceNotFoundException {
        return this.tripService.getTripById(id);
    }

    @GetMapping("/latest")
    public ResponseEntity<TripsByCriteriasDTO> getLatestTrips() {
        return this.tripService.getLatestTrips();
    }

    @GetMapping("/filter")
    public ResponseEntity<TripsDTO> getUserTripsForDestination(@RequestParam(name = "destination_id") int destinationId,
                                                               Principal principal) throws UserNotFoundException {
        return this.tripService.getUserTripsForDestination(destinationId, principal);
    }

    @GetMapping("/{id}/popularity")
    public ResponseEntity<TripPopularityDTO> getTripPopularity(@PathVariable int id)
            throws ResourceNotFoundException {
        return this.tripService.getTripPopularity(id);
    }

    @GetMapping("/{id}/recommendations")
    public ResponseEntity<TripRecommendationsDTO> getTripAttractionsRecommendations(@PathVariable int id)
            throws ResourceNotFoundException {
        return this.tripService.getTripAttractionsRecommendations(id);
    }

    @GetMapping("/plan_day/{id}")
    private ResponseEntity<TripPlannificationDay> getTripDayPlannification(@PathVariable int id)
            throws ResourceNotFoundException {
        return this.tripService.getTripDayPlannification(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResponseDTO> deleteTripById(@PathVariable int id, Principal principal)
            throws ResourceNotFoundException, UserNotFoundException, UnauthorizedOperationException {
        return this.tripService.deleteTripById(id, principal);
    }

    @PutMapping("/{id}/update_plannification")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Trip> updatePlannification(@PathVariable int id,
                                                     @RequestBody UpdatePlannificationDTO updatePlannificationDTO,
                                                     Principal principal)
            throws ResourceNotFoundException, InsufficientPostDataException, UserNotFoundException,
            UnauthorizedOperationException, BadUpdateDetailsProvidedException {
        return this.tripService.updatePlannification(id, updatePlannificationDTO, principal);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Trip> generateTrip(@RequestBody GenerateTripDTO generateTripDTO, Principal principal)
            throws InsufficientPostDataException, ResourceNotFoundException, UserNotFoundException, ParseException {
        return this.tripService.generateTrip(generateTripDTO, principal);
    }

    @PostMapping("/generate_placeholder")
    public ResponseEntity<ResponseDTO> generateTripDummy(@RequestBody GenerateTripDTO generateTripDTO)
            throws InterruptedException {
        Thread.sleep(10000);
        return new ResponseEntity<>(new ResponseDTO("Your trip was generated"), HttpStatus.BAD_REQUEST);
    }

}
