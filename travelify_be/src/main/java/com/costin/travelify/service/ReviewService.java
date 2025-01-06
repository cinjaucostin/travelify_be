package com.costin.travelify.service;

import com.costin.travelify.dto.request_dto.AddReviewDTO;
import com.costin.travelify.dto.request_dto.UpdateReviewDTO;
import com.costin.travelify.dto.response_dto.*;
import com.costin.travelify.entities.*;
import com.costin.travelify.exceptions.*;
import com.costin.travelify.repository.*;
import com.costin.travelify.security.AuthenticationService;
import com.costin.travelify.utils.Constants;
import com.costin.travelify.utils.Utils;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ReviewService {
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private DestinationRepository destinationRepository;
    @Autowired
    private LocationRepository locationRepository;
    @Autowired
    private TripRepository tripRepository;
    @Autowired
    private UserRepository userRepository;

    public double calculateTravelifyRatingBasedOnReviewsList(List<Review> reviews) {
        double ratingSum = 0;
        if(reviews == null) {
            return 0;
        }
        if(reviews.isEmpty()) {
            return 0;
        }
        ratingSum = reviews.stream().mapToDouble(Review::getRating).sum();
        return ratingSum / reviews.size();
    }

    public double calculateLocationOverallRating(Location location) {
        double tripadvisorRating = location.getTripadvisorRating();
        double travelifyRating = calculateTravelifyRatingBasedOnReviewsList(location.getReviews());
        if(travelifyRating != 0 && tripadvisorRating != 0) {
            return 0.5 * travelifyRating + 0.5 * tripadvisorRating;
        }
        if(travelifyRating == 0) {
            return tripadvisorRating;
        }
        return travelifyRating;
    }

    public RatingDTO createRatingDTOForLocation(Location location) {
        RatingDTO ratingDTO = new RatingDTO();
        ratingDTO.setId(location.getId());
        ratingDTO.setType(Constants.LOCATION_TYPE);
        ratingDTO.setRating(calculateLocationOverallRating(location));
        ratingDTO.setLocationId(location.getId());
        if(location.getReviews() == null) {
            ratingDTO.setTravelifyReviews(new ArrayList<>());
        } else {
            ratingDTO.setTravelifyReviews(location.getReviews());
        }
        return ratingDTO;
    }

    public RatingDTO createRatingDTOForDestination(Destination destination) {
        RatingDTO ratingDTO = new RatingDTO();
        ratingDTO.setId(destination.getId());
        ratingDTO.setType(Constants.DESTINATION_TYPE);
        ratingDTO.setRating(calculateTravelifyRatingBasedOnReviewsList(destination.getReviews()));
        ratingDTO.setDestinationId(destination.getId());
        if(destination.getReviews() == null) {
            ratingDTO.setTravelifyReviews(new ArrayList<>());
        } else {
            ratingDTO.setTravelifyReviews(destination.getReviews());
        }
        return ratingDTO;
    }

    public RatingDTO createRatingDTOForTrip(Trip trip) {
        RatingDTO ratingDTO = new RatingDTO();
        ratingDTO.setId(trip.getId());
        ratingDTO.setType(Constants.TRIP_TYPE);
        ratingDTO.setRating(calculateTravelifyRatingBasedOnReviewsList(trip.getReviews()));
        ratingDTO.setTripId(trip.getId());
        if(trip.getReviews() == null) {
            ratingDTO.setTravelifyReviews(new ArrayList<>());
        } else {
            ratingDTO.setTravelifyReviews(trip.getReviews());
        }
        return ratingDTO;
    }

    public ResponseEntity<OneReviewDTO> addReview(AddReviewDTO addReviewDTO, Principal principal)
            throws ResourceNotFoundException, UserNotFoundException, InsufficientPostDataException {
        if(!checkReviewDTO(addReviewDTO)) {
            throw new InsufficientPostDataException("Fields like type, title, content, " +
                    "rating and the matching type id are required.");
        }

        Review savedReview = null;
        String reviewType = addReviewDTO.getType();
        switch (reviewType) {
            case Constants.DESTINATION_TYPE -> {
                savedReview = addReviewForDestination(addReviewDTO, principal);
            }

            case Constants.LOCATION_TYPE -> {
                savedReview = addReviewForLocation(addReviewDTO, principal);
            }

            case Constants.TRIP_TYPE -> {
                savedReview = addReviewForTrip(addReviewDTO, principal);
            }

            default -> throw new InsufficientPostDataException("Type not found, available option: " +
                    "'trip', 'destination' and 'location'");

        }
        return new ResponseEntity<>(new OneReviewDTO(savedReview), HttpStatus.OK);
    }

    public ResponseEntity<MultipleReviewsDTO> getReviews(Integer userId, Integer destinationId, Integer locationId, Integer tripId,
                                                         Integer page, Integer size)
            throws BadQueryParametersException, ResourceNotFoundException {
        boolean checkQueryParamsResult = checkQueryParameters(destinationId, locationId, tripId);

        if(!checkQueryParamsResult) {
            throw new BadQueryParametersException("Maximum one query parameter must be " +
                    "specified between: destination_id or location_id or trip_id");
        }

        Sort sorting = JpaSort.unsafe(Sort.Direction.DESC, "createdDate");

        Pageable pageable = PageRequest.of(page, size, sorting);
        Page<Review> reviewPage;
        if (userId != null) {
            reviewPage = getUserReviewsPaged(userId, pageable);
        } else if(destinationId != null) {
            reviewPage = getDestinationReviews(destinationId, pageable);
        } else if(locationId != null) {
            reviewPage = getLocationReviews(locationId, pageable);
        } else if(tripId != null) {
            reviewPage = getTripReviews(tripId, pageable);
        } else {
            reviewPage = reviewRepository.findAll(pageable);
        }

        MultipleReviewsDTO multipleReviewsDTO = new MultipleReviewsDTO();
        List<ReviewDetailsDTO> reviewsDTOs = reviewPage.getContent()
            .stream().map(this::fromReviewEntityToDTO)
            .toList();

        multipleReviewsDTO.setReviews(reviewsDTOs);
        multipleReviewsDTO.setNumberOfPages(reviewPage.getTotalPages());
        multipleReviewsDTO.setNumberOfElements(reviewPage.getNumberOfElements());
        multipleReviewsDTO.setPageSize(pageable.getPageSize());
        multipleReviewsDTO.setPageNumber(pageable.getPageNumber());

        return new ResponseEntity<>(multipleReviewsDTO, HttpStatus.OK);
    }

    public ReviewDetailsDTO fromReviewEntityToDTO(Review review) {
        ReviewDetailsDTO reviewDetailsDTO = new ReviewDetailsDTO();
        reviewDetailsDTO.setId(review.getId());
        reviewDetailsDTO.setTitle(review.getTitle());
        reviewDetailsDTO.setContent(review.getContent());
        reviewDetailsDTO.setRating(review.getRating());
        reviewDetailsDTO.setCreatedDate(review.getCreatedDate());

        User user = review.getUser();
        if(user != null) {
            reviewDetailsDTO.setUserId(user.getId());
        }

        if(user == null) {
            reviewDetailsDTO.setUserTimestamp("Member since unknown");
        } else if(user.getJoiningDate() == null) {
            reviewDetailsDTO.setUserTimestamp("Member since unknown");
        } else {
            reviewDetailsDTO.setUserTimestamp(STR."Member since \{Utils.getMonthNameAndYearForLocalDateTime(user.getJoiningDate())}");
        }

        if(user == null) {
            reviewDetailsDTO.setUserName("Unknown");
        } else if(user.getFirstName() == null || user.getLastName() == null) {
            reviewDetailsDTO.setUserName("Unknown");
        } else {
            reviewDetailsDTO.setUserName(STR."\{user.getFirstName()} \{user.getLastName()}");
        }

        reviewDetailsDTO.setCreatedTimestamp(Utils.getDifferenceBetweenTimings(review.getCreatedDate(), LocalDateTime.now()));

        if(review.getDestination() != null) {
            reviewDetailsDTO.setReviewedEntityType("destination");
            reviewDetailsDTO.setDestinationId(review.getDestination().getId());
            reviewDetailsDTO.setReviewedEntityName(review.getDestination().getName());
            reviewDetailsDTO.setReviewedEntityId(review.getDestination().getId());
        } else if(review.getLocation() != null) {
            reviewDetailsDTO.setReviewedEntityType("location");
            reviewDetailsDTO.setLocationId(review.getLocation().getId());
            reviewDetailsDTO.setReviewedEntityName(review.getLocation().getName());
            reviewDetailsDTO.setReviewedEntityId(review.getLocation().getId());
        } else if(review.getTrip() != null) {
            reviewDetailsDTO.setReviewedEntityType("trip");
            reviewDetailsDTO.setTripId(review.getTrip().getId());
            reviewDetailsDTO.setReviewedEntityName(review.getTrip().getName());
            reviewDetailsDTO.setReviewedEntityId(review.getTrip().getId());
        }

        return reviewDetailsDTO;
    }


    public Optional<Review> getReviewById(int id) {
        return this.reviewRepository.findById(id);
    }

    public List<Review> getUserReviews(int userId) {
        return this.reviewRepository.findReviewsByUserId(userId);
    }

    public Page<Review> getLocationReviews(int locationId, Pageable pageable) throws ResourceNotFoundException {
        Optional<Location> locationOptional = this.locationRepository.findById(locationId);
        if(locationOptional.isEmpty()) {
            throw new ResourceNotFoundException(String.format("Location with id %d not found", locationId));
        }

        return this.reviewRepository.findReviewsByLocationId(locationId, pageable);
    }

    public Page<Review> getUserReviewsPaged(int userId, Pageable pageable) throws ResourceNotFoundException {
        Optional<User> userOptional = this.userRepository.findById(userId);
        if(userOptional.isEmpty()) {
            throw new ResourceNotFoundException(String.format("User with id %d not found", userId));
        }

        return this.reviewRepository.findReviewsByUserIdPaged(userId, pageable);
    }

    public Page<Review> getDestinationReviews(int destinationId, Pageable pageable) throws ResourceNotFoundException {
        Optional<Destination> destinationOptional = this.destinationRepository.findById(destinationId);
        if(destinationOptional.isEmpty()) {
            throw new ResourceNotFoundException(String.format("Destination with id %d not found", destinationId));
        }

        return this.reviewRepository.findReviewsByDestinationId(destinationId, pageable);
    }

    public Page<Review> getTripReviews(int tripId, Pageable pageable) throws ResourceNotFoundException {
        Optional<Trip> tripOptional = this.tripRepository.findById(tripId);
        if(tripOptional.isEmpty()) {
            throw new ResourceNotFoundException(String.format("Trip with id %d not found", tripId));
        }

        return this.reviewRepository.findReviewsByTripId(tripId, pageable);
    }

    public Review addReviewForDestination(AddReviewDTO addReviewDTO, Principal principal)
            throws ResourceNotFoundException, UserNotFoundException, InsufficientPostDataException {
        if(addReviewDTO.getDestinationId() == null) {
            throw new InsufficientPostDataException("destinationId must be specified for a destination review.");
        }

        Optional<Destination> destinationOptional = this.destinationRepository.findById(addReviewDTO.getDestinationId());
        Optional<User> userOptional = this.userRepository.findByEmail(principal.getName());
        if(destinationOptional.isEmpty()) {
            throw new ResourceNotFoundException(STR."Resource \{Constants.DESTINATION_TYPE} with id \{addReviewDTO.getDestinationId()} not found");
        }
        if(userOptional.isEmpty()) {
            throw new UserNotFoundException(STR."User \{principal.getName()} not found.");
        }

        Review review = createReviewFromDTO(addReviewDTO);
        review.setDestination(destinationOptional.get());
        review.setUser(userOptional.get());

        return this.reviewRepository.save(review);
    }

    public Review addReviewForLocation(AddReviewDTO addReviewDTO, Principal principal)
            throws ResourceNotFoundException, UserNotFoundException, InsufficientPostDataException {
        if(addReviewDTO.getLocationId() == null) {
            throw new InsufficientPostDataException("locationId must be specified for a location review.");
        }

        Optional<Location> locationOptional = this.locationRepository.findById(addReviewDTO.getLocationId());
        Optional<User> userOptional = this.userRepository.findByEmail(principal.getName());
        if(locationOptional.isEmpty()) {
            throw new ResourceNotFoundException(STR."Resource \{Constants.LOCATION_TYPE} with id \{addReviewDTO.getDestinationId()} not found");
        }
        if(userOptional.isEmpty()) {
            throw new UserNotFoundException(STR."User \{principal.getName()} not found.");
        }

        Review review = createReviewFromDTO(addReviewDTO);
        review.setLocation(locationOptional.get());
        review.setUser(userOptional.get());

        return this.reviewRepository.save(review);
    }

    public Review addReviewForTrip(AddReviewDTO addReviewDTO, Principal principal)
            throws ResourceNotFoundException, UserNotFoundException, InsufficientPostDataException {
        if(addReviewDTO.getTripId() == null) {
            throw new InsufficientPostDataException("tripId must be specified for a trip review.");
        }

        Optional<Trip> tripOptional = this.tripRepository.findById(addReviewDTO.getTripId());
        Optional<User> userOptional = this.userRepository.findByEmail(principal.getName());
        if(tripOptional.isEmpty()) {
            throw new ResourceNotFoundException(STR."Resource \{Constants.TRIP_TYPE} with id \{addReviewDTO.getDestinationId()} not found");
        }
        if(userOptional.isEmpty()) {
            throw new UserNotFoundException(STR."User \{principal.getName()} not found.");
        }

        Review review = createReviewFromDTO(addReviewDTO);
        review.setTrip(tripOptional.get());
        review.setUser(userOptional.get());

        return this.reviewRepository.save(review);
    }

    public Review createReviewFromDTO(AddReviewDTO addReviewDTO) {
        Review review = new Review();
        review.setTitle(addReviewDTO.getTitle());
        review.setContent(addReviewDTO.getContent());
        review.setRating(addReviewDTO.getRating());
        review.setCreatedDate(LocalDateTime.now());
        return review;
    }

    public boolean checkQueryParameters(Integer destinationId, Integer locationId, Integer tripId) {
        return (destinationId == null || locationId == null) &&
                (destinationId == null || tripId == null) &&
                (locationId == null || tripId == null);
    }

    public ResponseEntity<ResponseDTO> deleteReview(int id, Authentication authentication)
            throws ResourceNotFoundException, UserNotFoundException {
        Optional<Review> reviewOptional = this.reviewRepository.findById(id);
        if (reviewOptional.isEmpty()) {
            throw new ResourceNotFoundException(STR."Review \{id} not found.");
        }

        if (this.authenticationService.checkIfAuthenticationRepresentsAdminUser(authentication)) {
            this.reviewRepository.deleteById(id);
            return new ResponseEntity<>(new ResponseDTO(STR."Review \{id} removed successfully"), HttpStatus.OK);
        }

        Review review = reviewOptional.get();
        Optional<User> userOptional = this.userRepository.findByEmail(authentication.getName());
        if(userOptional.isEmpty()) {
            throw new UserNotFoundException("Review couldn't be deleted because can't find your user");
        }
        User user = userOptional.get();
        if(review.getUser().getId() != user.getId()) {
            return new ResponseEntity<>(new ResponseDTO(STR."Review \{id} couldn't be deleted because it belongs to another user"),
                    HttpStatus.UNAUTHORIZED);
        }
        this.reviewRepository.deleteById(id);
        return new ResponseEntity<>(new ResponseDTO(STR."Review \{id} removed successfully"), HttpStatus.OK);
    }

    public boolean checkReviewDTO(AddReviewDTO addReviewDTO) {
        return addReviewDTO.getType() != null && addReviewDTO.getTitle() != null && addReviewDTO.getContent() != null
                && addReviewDTO.getRating() != null;
    }

    public ResponseEntity<OneReviewDTO> updateReview(int id, UpdateReviewDTO updateReviewDTO, Principal principal)
            throws ResourceNotFoundException, UserNotFoundException, UnauthorizedOperationException {
        Optional<Review> reviewOptional = this.reviewRepository.findById(id);
        if (reviewOptional.isEmpty()) {
            throw new ResourceNotFoundException(STR."Review \{id} not found.");
        }

        Review review = reviewOptional.get();
        Optional<User> userOptional = this.userRepository.findByEmail(principal.getName());
        if(userOptional.isEmpty()) {
            throw new UserNotFoundException("Review couldn't be updated because can't find your user");
        }
        User user = userOptional.get();
        if(review.getUser().getId() != user.getId()) {
            throw new UnauthorizedOperationException(STR."Review \{id} couldn't be updated because it belongs to another user");
        }

        updateReviewWithDTO(review, updateReviewDTO);
        Review updatedReview = this.reviewRepository.save(review);

        return new ResponseEntity<>(new OneReviewDTO(updatedReview), HttpStatus.OK);
    }

    public void updateReviewWithDTO(Review review, UpdateReviewDTO updateReviewDTO) {
        if(updateReviewDTO.getTitle() != null) {
            review.setTitle(updateReviewDTO.getTitle());
        }
        if(updateReviewDTO.getContent() != null) {
            review.setContent(updateReviewDTO.getContent());
        }
        if(updateReviewDTO.getRating() != null) {
            review.setRating(updateReviewDTO.getRating());
        }
    }

}
