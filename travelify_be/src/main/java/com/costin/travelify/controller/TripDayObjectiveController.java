package com.costin.travelify.controller;

import com.costin.travelify.dto.request_dto.AddDayObjectiveDTO;
import com.costin.travelify.dto.response_dto.OneTripDTO;
import com.costin.travelify.dto.response_dto.ResponseDTO;
import com.costin.travelify.entities.Trip;
import com.costin.travelify.exceptions.*;
import com.costin.travelify.service.TripDayObjectiveService;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/trips_objectives")
@CrossOrigin
public class TripDayObjectiveController {
    @Autowired
    private TripDayObjectiveService tripDayObjectiveService;

    @PostMapping
    public ResponseEntity<OneTripDTO> addDayObjectiveToTripPlan(@RequestBody AddDayObjectiveDTO addDayObjectiveDTO,
                                                                Principal principal)
            throws InsufficientPostDataException, ResourceNotFoundException, BadLocationException, UserNotFoundException,
            UnauthorizedOperationException {
        return this.tripDayObjectiveService.addDayObjectiveToTripPlan(addDayObjectiveDTO, principal);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<OneTripDTO> deleteDayObjectiveFromTripPlan(@PathVariable int id, Principal principal)
            throws ResourceNotFoundException, UserNotFoundException, UnauthorizedOperationException {
        return this.tripDayObjectiveService.deleteDayObjectiveFromTripPlan(id, principal);
    }


}
