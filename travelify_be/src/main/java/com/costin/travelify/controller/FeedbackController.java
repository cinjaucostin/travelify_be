package com.costin.travelify.controller;

import com.costin.travelify.dto.response_dto.AddFeedbackDTO;
import com.costin.travelify.dto.response_dto.MultipleFeedbacksDTO;
import com.costin.travelify.dto.response_dto.MultipleReviewsDTO;
import com.costin.travelify.entities.Feedback;
import com.costin.travelify.exceptions.BadQueryParametersException;
import com.costin.travelify.exceptions.InsufficientPostDataException;
import com.costin.travelify.exceptions.ResourceNotFoundException;
import com.costin.travelify.service.FeedbackService;
import com.costin.travelify.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @GetMapping
    public ResponseEntity<MultipleFeedbacksDTO> getFeedback(@RequestParam(defaultValue = "0") Integer page,
                                                            @RequestParam(defaultValue = "3") Integer size) {
        return this.feedbackService.getFeedback(page, size);
    }

    @PostMapping
    public ResponseEntity<Feedback> addFeedback(@RequestBody AddFeedbackDTO addFeedbackDTO)
            throws InsufficientPostDataException {
        return this.feedbackService.addFeedback(addFeedbackDTO);
    }


//    @GetMapping
//    @PreAuthorize("isAuthenticated()")
//    public ResponseEntity<List<Feedback>> getFeedback() {
//        return this.feedbackService.getFeedback();
//    }

}
