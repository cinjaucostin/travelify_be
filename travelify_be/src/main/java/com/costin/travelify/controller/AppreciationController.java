package com.costin.travelify.controller;

import com.costin.travelify.dto.request_dto.AddAppreciationDTO;
import com.costin.travelify.dto.response_dto.MultipleAppreciationsDTO;
import com.costin.travelify.dto.response_dto.OneAppreciationDTO;
import com.costin.travelify.dto.response_dto.ResponseDTO;
import com.costin.travelify.dto.response_dto.UserActivityDTO;
import com.costin.travelify.exceptions.*;
import com.costin.travelify.service.AppreciationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/appreciations")
public class AppreciationController {
    @Autowired
    private AppreciationService appreciationService;

    @GetMapping
    public ResponseEntity<MultipleAppreciationsDTO> getAppreciations(@RequestParam(name = "destination_id", required = false) Integer destinationId,
                                                                     @RequestParam(name = "location_id", required = false) Integer locationId,
                                                                     @RequestParam(name = "trip_id", required = false) Integer tripId,
                                                                     @RequestParam(name = "user_id", required = false) Integer userId)
            throws BadQueryParametersException, ResourceNotFoundException {
        return this.appreciationService.getAppreciations(destinationId, locationId, tripId, userId);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OneAppreciationDTO> addAppreciation(@RequestBody AddAppreciationDTO addAppreciationDTO,
                                                              Principal principal)
            throws InsufficientPostDataException, ResourceNotFoundException, AlreadyExistingResourceException {
        return this.appreciationService.addAppreciation(addAppreciationDTO, principal);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResponseDTO> deleteAppreciation(@PathVariable int id, Principal principal)
            throws UserNotFoundException, ResourceNotFoundException, UnauthorizedOperationException {
        return this.appreciationService.deleteAppreciation(id, principal);
    }

}
