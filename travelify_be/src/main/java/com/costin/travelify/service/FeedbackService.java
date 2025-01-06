package com.costin.travelify.service;

import com.costin.travelify.dto.response_dto.*;
import com.costin.travelify.entities.Feedback;
import com.costin.travelify.entities.Review;
import com.costin.travelify.entities.User;
import com.costin.travelify.exceptions.InsufficientPostDataException;
import com.costin.travelify.repository.FeedbackRepository;
import com.costin.travelify.repository.ReviewRepository;
import com.costin.travelify.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FeedbackService {
    @Autowired
    private FeedbackRepository feedbackRepository;
    @Autowired
    private ReviewRepository reviewRepository;

    public ResponseEntity<Feedback> addFeedback(AddFeedbackDTO addFeedbackDTO)
            throws InsufficientPostDataException {
        Feedback feedback = new Feedback();
        if(addFeedbackDTO.getContent() == null) {
            throw new InsufficientPostDataException("The feedback must have a content");
        }
        if(addFeedbackDTO.getRating() == null) {
            throw new InsufficientPostDataException("The feedback must have a rating provided");
        }
        feedback.setContent(addFeedbackDTO.getContent());
        feedback.setRating(addFeedbackDTO.getRating());
        feedback.setCreatedTimestamp(LocalDateTime.now());
        return new ResponseEntity<>(this.feedbackRepository.save(feedback), HttpStatus.OK);
    }

    public ResponseEntity<MultipleFeedbacksDTO> getFeedback(Integer page, Integer size) {
        MultipleFeedbacksDTO multipleFeedbacksDTO = new MultipleFeedbacksDTO();
        Sort sorting = JpaSort.unsafe(Sort.Direction.DESC, "createdTimestamp");

        Pageable pageable = PageRequest.of(page, size, sorting);
        Page<Feedback> feedbackPage = this.feedbackRepository.findAll(pageable);

        List<FeedbackDetailsDTO> feedbackDTOs = feedbackPage.getContent()
                .stream().map(this::fromFeedbackEntityToDTO)
                .toList();

        multipleFeedbacksDTO.setFeedbacks(feedbackDTOs);
        multipleFeedbacksDTO.setNumberOfPages(feedbackPage.getTotalPages());
        multipleFeedbacksDTO.setNumberOfElements(feedbackPage.getNumberOfElements());
        multipleFeedbacksDTO.setPageSize(pageable.getPageSize());
        multipleFeedbacksDTO.setPageNumber(pageable.getPageNumber());

        return new ResponseEntity<>(multipleFeedbacksDTO, HttpStatus.OK);
    }

    public FeedbackDetailsDTO fromFeedbackEntityToDTO(Feedback feedback) {
        FeedbackDetailsDTO feedbackDetailsDTO = new FeedbackDetailsDTO();
        feedbackDetailsDTO.setId(feedback.getId());
        feedbackDetailsDTO.setContent(feedback.getContent());
        feedbackDetailsDTO.setRating(feedback.getRating());
        feedbackDetailsDTO.setCreatedDate(feedback.getCreatedTimestamp());

        feedbackDetailsDTO.setCreatedTimestamp(Utils.getDifferenceBetweenTimings(feedback.getCreatedTimestamp(),
                LocalDateTime.now()));

        return feedbackDetailsDTO;
    }

}
