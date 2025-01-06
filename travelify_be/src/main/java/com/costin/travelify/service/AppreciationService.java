package com.costin.travelify.service;

import com.costin.travelify.dto.request_dto.AddAppreciationDTO;
import com.costin.travelify.dto.response_dto.*;
import com.costin.travelify.entities.*;
import com.costin.travelify.exceptions.*;
import com.costin.travelify.repository.*;
import com.costin.travelify.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class AppreciationService {
    @Autowired
    private AppreciationRepository appreciationRepository;
    @Autowired
    private DestinationRepository destinationRepository;
    @Autowired
    private LocationRepository locationRepository;
    @Autowired
    private DestinationService destinationService;
    @Autowired
    private TripService tripService;
    @Autowired
    private UserRepository userRepository;

    public ResponseEntity<OneAppreciationDTO> addAppreciation(AddAppreciationDTO addAppreciationDTO,
                                                              Principal principal)
            throws InsufficientPostDataException, ResourceNotFoundException, AlreadyExistingResourceException {

        if(addAppreciationDTO.getType() == null || addAppreciationDTO.getType().isEmpty()) {
            throw new InsufficientPostDataException("Appreciation type must be specified: destination or location or trip");
        }

        Appreciation savedAppreciation = null;
        String appreciationType = addAppreciationDTO.getType();

        switch(appreciationType) {
            case Constants.DESTINATION_TYPE -> {
                savedAppreciation = addAppreciationForDestination(addAppreciationDTO, principal);
            }

            case Constants.LOCATION_TYPE -> {
                savedAppreciation = addAppreciationForLocation(addAppreciationDTO, principal);
            }

            case Constants.TRIP_TYPE -> {
                savedAppreciation = addAppreciationForTrip(addAppreciationDTO, principal);
            }

            default -> {
            }
        }

        return new ResponseEntity<>(new OneAppreciationDTO(savedAppreciation), HttpStatus.OK);
    }


    public ResponseEntity<ResponseDTO> deleteAppreciation(int id, Principal principal)
            throws UserNotFoundException, ResourceNotFoundException, UnauthorizedOperationException {
        Optional<User> userOptional = this.userRepository.findByEmail(principal.getName());
        if(userOptional.isEmpty()) {
            throw new UserNotFoundException(STR."Appreciation \{id} couldn't be deleted, user not found.");
        }

        Optional<Appreciation> appreciationOptional = this.appreciationRepository.findById(id);
        if(appreciationOptional.isEmpty()) {
            throw new ResourceNotFoundException(STR."Appreciation with id \{id} not found");
        }

        User user = userOptional.get();
        if(!checkIfAppreciationsExistsForUser(user, id)) {
            throw new UnauthorizedOperationException(STR."Appreciation \{id} couldn't be deleted, it belongs to another user");
        }

        this.appreciationRepository.deleteById(id);
        return new ResponseEntity<>(new ResponseDTO(STR."Appreciation \{id} has been deleted"), HttpStatus.OK);
    }

    public List<Appreciation> getDestinationAppreciations(int destinationId) throws ResourceNotFoundException {
        Destination destination = this.destinationService.findDestinationById(destinationId);
        return this.appreciationRepository.findAppreciationsByDestinationId(destinationId);
    }

    public ResponseEntity<MultipleAppreciationsDTO> getAppreciations(Integer destinationId, Integer locationId,
                                                               Integer tripId, Integer userId)
            throws BadQueryParametersException, ResourceNotFoundException {
        List<Appreciation> appreciations;
        boolean checkQueryParamsResult = checkQueryParameters(destinationId, locationId, tripId);

        if(!checkQueryParamsResult) {
            throw new BadQueryParametersException("Maximum one query parameter must be " +
                    "specified between: destination_id or location_id or trip_id");
        }

        if(destinationId != null) {
            appreciations = this.getDestinationAppreciations(destinationId);
        } else if(locationId != null) {
            appreciations = this.getLocationAppreciations(locationId);
        } else if(tripId != null) {
            appreciations = this.getTripAppreciations(tripId);
        } else {
            appreciations = this.appreciationRepository.findAll();
        }

        if(userId != null) {
            appreciations = appreciations
                    .stream()
                    .filter(appreciation -> appreciation.getUser().getId() == userId)
                    .toList();
        }

        return new ResponseEntity<>(new MultipleAppreciationsDTO(appreciations), HttpStatus.OK);
    }

    public List<Appreciation> getLocationAppreciations(int locationId) throws ResourceNotFoundException {
        Optional<Location> locationOptional = this.locationRepository.findById(locationId);
        if(locationOptional.isEmpty()) {
            throw new ResourceNotFoundException(STR."Location with id \{locationId} not found");
        }

        return this.appreciationRepository.findAppreciationsByLocationId(locationId);
    }

    public List<Appreciation> getTripAppreciations(int tripId) throws ResourceNotFoundException {
        Optional<Trip> tripOptional = this.tripService.findById(tripId);
        if(tripOptional.isEmpty()) {
            throw new ResourceNotFoundException(STR."Trip with id \{tripId} not found");
        }

        return this.appreciationRepository.findAppreciationsByTripId(tripId);
    }

    public Appreciation addAppreciationForDestination(AddAppreciationDTO addAppreciationDTO, Principal principal)
            throws InsufficientPostDataException, ResourceNotFoundException, AlreadyExistingResourceException {
        if(addAppreciationDTO.getDestinationId() == null) {
            throw new InsufficientPostDataException("destinationId must be specified for a destination type appreciation");
        }

        int destinationId = addAppreciationDTO.getDestinationId();
        Destination destination = this.destinationService.findDestinationById(destinationId);
        Optional<User> userOptional = this.userRepository.findByEmail(principal.getName());

        if(userOptional.isEmpty()) {
            throw new ResourceNotFoundException("Appreciation couldn't be done, user not found");
        }

        User user = userOptional.get();

        if(checkIfDestinationAlreadyAppreciated(user, destinationId)) {
            throw new AlreadyExistingResourceException(STR."Destination \{destinationId} already liked by your user.");
        }

        Appreciation appreciation = new Appreciation();
        appreciation.setCreatedDate(LocalDateTime.now());
        appreciation.setDestination(destination);
        appreciation.setUser(user);

        return this.appreciationRepository.save(appreciation);
    }

    public Appreciation addAppreciationForLocation(AddAppreciationDTO addAppreciationDTO, Principal principal)
            throws InsufficientPostDataException, ResourceNotFoundException, AlreadyExistingResourceException {
        if(addAppreciationDTO.getLocationId() == null) {
            throw new InsufficientPostDataException("locationId must be specified for a location type appreciation");
        }

        int locationId = addAppreciationDTO.getLocationId();
        Optional<Location> locationOptional = this.locationRepository.findById(locationId);
        Optional<User> userOptional = this.userRepository.findByEmail(principal.getName());

        if(locationOptional.isEmpty()) {
            throw new ResourceNotFoundException(STR."Appreciation couldn't be done, locationId \{locationId} not found.");
        }

        if(userOptional.isEmpty()) {
            throw new ResourceNotFoundException("Appreciation couldn't be done, user not found");
        }

        User user = userOptional.get();

        if(checkIfLocationAlreadyAppreciated(user, locationId)) {
            throw new AlreadyExistingResourceException(STR."Location \{locationId} already liked by your user.");
        }

        Appreciation appreciation = new Appreciation();
        appreciation.setCreatedDate(LocalDateTime.now());
        appreciation.setLocation(locationOptional.get());
        appreciation.setUser(user);

        return this.appreciationRepository.save(appreciation);
    }

    public Appreciation addAppreciationForTrip(AddAppreciationDTO addAppreciationDTO, Principal principal)
            throws InsufficientPostDataException, ResourceNotFoundException, AlreadyExistingResourceException {
        if(addAppreciationDTO.getTripId() == null) {
            throw new InsufficientPostDataException("tripId must be specified for a trip type appreciation");
        }

        int tripId = addAppreciationDTO.getTripId();
        Optional<Trip> tripOptional = this.tripService.findById(tripId);
        Optional<User> userOptional = this.userRepository.findByEmail(principal.getName());

        if(tripOptional.isEmpty()) {
            throw new ResourceNotFoundException(STR."Appreciation couldn't be done, tripId \{tripId} not found.");
        }

        if(userOptional.isEmpty()) {
            throw new ResourceNotFoundException("Appreciation couldn't be done, user not found");
        }

        User user = userOptional.get();

        if(checkIfTripAlreadyAppreciated(user, tripId)) {
            throw new AlreadyExistingResourceException(STR."Trip \{tripId} already liked by your user.");
        }

        Appreciation appreciation = new Appreciation();
        appreciation.setCreatedDate(LocalDateTime.now());
        appreciation.setTrip(tripOptional.get());
        appreciation.setUser(user);

        return this.appreciationRepository.save(appreciation);
    }

    public boolean checkIfDestinationAlreadyAppreciated(User user, int destinationId) {
        return !user.getAppreciations().stream().filter(appreciation -> {
            if (appreciation.getDestination() != null) {
                return destinationId == appreciation.getDestination().getId();
            }
            return false;
        }).toList().isEmpty();
    }

    public boolean checkIfLocationAlreadyAppreciated(User user, int locationId) {
        return !user.getAppreciations().stream().filter(appreciation -> {
            if (appreciation.getLocation() != null) {
                return locationId == appreciation.getLocation().getId();
            }
            return false;
        }).toList().isEmpty();
    }

    public boolean checkIfTripAlreadyAppreciated(User user, int tripId) {
        return !user.getAppreciations().stream().filter(appreciation -> {
            if (appreciation.getTrip() != null) {
                return tripId == appreciation.getTrip().getId();
            }
            return false;
        }).toList().isEmpty();
    }

    public boolean checkIfAppreciationsExistsForUser(User user, int appreciationId) {
        List<Appreciation> appreciations = user.getAppreciations();
        if(appreciations == null) {
            return false;
        }

        if(appreciations.isEmpty()) {
            return false;
        }

        return !appreciations.stream().filter(appreciation -> appreciation.getId() == appreciationId)
                .toList().isEmpty();
    }

    public boolean checkQueryParameters(Integer destinationId, Integer locationId, Integer tripId) {
        return (destinationId == null || locationId == null) &&
                (destinationId == null || tripId == null) &&
                (locationId == null || tripId == null);
    }

    public ResponseEntity<UserActivityDTO> getUserActivity(Integer userId) {
        List<Appreciation> appreciations = this.appreciationRepository.findAppreciationsByUserId(userId);

        AppreciationByTypeDTO destinationsAppreciations = getAppreciationsForDestinations(appreciations);
        AppreciationByTypeDTO attractionsAppreciations = getAppreciationsForLocations(appreciations);
        AppreciationByTypeDTO tripsAppreciations = getAppreciationsForTrips(appreciations);

        UserActivityDTO activityDTO = new UserActivityDTO();
        activityDTO.setAppreciations(new ArrayList<>());
        activityDTO.getAppreciations().add(destinationsAppreciations);
        activityDTO.getAppreciations().add(attractionsAppreciations);
        activityDTO.getAppreciations().add(tripsAppreciations);

        return new ResponseEntity<>(activityDTO, HttpStatus.OK);
    }

    public AppreciationByTypeDTO getAppreciationsForDestinations(List<Appreciation> appreciations) {
        AppreciationByTypeDTO appreciationByTypeDTO = new AppreciationByTypeDTO();
        appreciationByTypeDTO.setType("destinations");
        appreciationByTypeDTO.setAppreciations(new ArrayList<>());
        List<Appreciation> appreciationsForDestinations = appreciations
                .stream()
                .filter(appreciation -> appreciation.getDestination() != null)
                .sorted(Comparator.comparing(Appreciation::getCreatedDate).reversed())
                .toList();

        appreciationsForDestinations
                .forEach(appreciation -> {
                    AppreciationDTO appreciationDTO = new AppreciationDTO();
                    appreciationDTO.setAppreciation(appreciation);
                    appreciationDTO.setType("destination");
                    appreciationDTO.setDestination(this.destinationService
                            .createDestinationMinDetailsDTOFromEntity(appreciation.getDestination(), appreciation.getDestination().getPopularity()));
                    appreciationByTypeDTO.getAppreciations().add(appreciationDTO);
                });

        return appreciationByTypeDTO;
    }

    public AppreciationByTypeDTO getAppreciationsForLocations(List<Appreciation> appreciations) {
        AppreciationByTypeDTO appreciationByTypeDTO = new AppreciationByTypeDTO();
        appreciationByTypeDTO.setType("attractions");
        appreciationByTypeDTO.setAppreciations(new ArrayList<>());
        List<Appreciation> appreciationsForDestinations = appreciations
                .stream()
                .filter(appreciation -> appreciation.getLocation() != null)
                .sorted(Comparator.comparing(Appreciation::getCreatedDate).reversed())
                .toList();

        appreciationsForDestinations
                .forEach(appreciation -> {
                    AppreciationDTO appreciationDTO = new AppreciationDTO();
                    appreciationDTO.setAppreciation(appreciation);
                    appreciationDTO.setType("location");
                    appreciationDTO.setLocation(appreciation.getLocation());
                    appreciationByTypeDTO.getAppreciations().add(appreciationDTO);
                });

        return appreciationByTypeDTO;
    }

    public AppreciationByTypeDTO getAppreciationsForTrips(List<Appreciation> appreciations) {
        AppreciationByTypeDTO appreciationByTypeDTO = new AppreciationByTypeDTO();
        appreciationByTypeDTO.setType("trips");
        appreciationByTypeDTO.setAppreciations(new ArrayList<>());
        List<Appreciation> appreciationsForDestinations = appreciations
                .stream()
                .filter(appreciation -> appreciation.getTrip() != null)
                .sorted(Comparator.comparing(Appreciation::getCreatedDate).reversed())
                .toList();

        appreciationsForDestinations
                .forEach(appreciation -> {
                    AppreciationDTO appreciationDTO = new AppreciationDTO();
                    appreciationDTO.setAppreciation(appreciation);
                    appreciationDTO.setType("trip");
                    appreciationDTO.setTrip(this.tripService.createTripMinDetailsDTOFromTripEntity(appreciation.getTrip()));
                    appreciationByTypeDTO.getAppreciations().add(appreciationDTO);
                });

        return appreciationByTypeDTO;
    }

}
