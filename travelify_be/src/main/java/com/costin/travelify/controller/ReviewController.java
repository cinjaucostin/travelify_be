package com.costin.travelify.controller;

import com.costin.travelify.dto.request_dto.AddReviewDTO;
import com.costin.travelify.dto.request_dto.UpdateReviewDTO;
import com.costin.travelify.dto.response_dto.MultipleReviewsDTO;
import com.costin.travelify.dto.response_dto.OneReviewDTO;
import com.costin.travelify.dto.response_dto.ResponseDTO;
import com.costin.travelify.entities.Review;
import com.costin.travelify.exceptions.*;
import com.costin.travelify.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    @Autowired
    private ReviewService reviewService;

    @GetMapping
    public ResponseEntity<MultipleReviewsDTO> getReviews(@RequestParam(name = "user_id", required = false) Integer userId,
                                                         @RequestParam(name = "destination_id", required = false) Integer destinationId,
                                                         @RequestParam(name = "location_id", required = false) Integer locationId,
                                                         @RequestParam(name = "trip_id", required = false) Integer tripId,
                                                         @RequestParam(defaultValue = "0") Integer page,
                                                         @RequestParam(defaultValue = "3") Integer size)
            throws BadQueryParametersException, ResourceNotFoundException {
        return this.reviewService.getReviews(userId, destinationId, locationId, tripId, page, size);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OneReviewDTO> getReviewById(@PathVariable int id) throws ResourceNotFoundException {
        Optional<Review> reviewOptional = this.reviewService.getReviewById(id);
        if(reviewOptional.isEmpty()) {
            throw new ResourceNotFoundException(STR."Review with id \{id} not found");
        }
        return new ResponseEntity<>(new OneReviewDTO(reviewOptional.get()), HttpStatus.OK);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OneReviewDTO> addReview(@RequestBody AddReviewDTO addReviewDTO,
                                                  Principal principal)
            throws UserNotFoundException, InsufficientPostDataException, ResourceNotFoundException {
        return this.reviewService.addReview(addReviewDTO, principal);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OneReviewDTO> updateReview(@PathVariable int id,
                                               @RequestBody UpdateReviewDTO updateReviewDTO,
                                               Principal principal)
            throws ResourceNotFoundException, UserNotFoundException, UnauthorizedOperationException {
        return this.reviewService.updateReview(id, updateReviewDTO, principal);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResponseDTO> deleteReview(@PathVariable int id, Authentication authentication)
            throws ResourceNotFoundException, UserNotFoundException {
        return this.reviewService.deleteReview(id, authentication);
    }


}
