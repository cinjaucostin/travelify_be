package com.costin.travelify.service;

import com.costin.travelify.dto.request_dto.AddDayObjectiveDTO;
import com.costin.travelify.dto.response_dto.OneTripDTO;
import com.costin.travelify.dto.response_dto.ResponseDTO;
import com.costin.travelify.dto.response_dto.TwoPointsRouteDTO;
import com.costin.travelify.entities.*;
import com.costin.travelify.exceptions.*;
import com.costin.travelify.repository.TripDayObjectiveRepository;
import com.costin.travelify.repository.TripPlannificationDayRepository;
import com.costin.travelify.repository.TripRepository;
import com.costin.travelify.repository.UserRepository;
import com.costin.travelify.utils.Constants;
import com.costin.travelify.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class TripDayObjectiveService {
    @Autowired
    private TripService tripService;
    @Autowired
    private LocationService locationService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TripRepository tripRepository;
    @Autowired
    private TripPlannificationDayRepository tripPlannificationDayRepository;
    @Autowired
    private TripDayObjectiveRepository tripDayObjectiveRepository;

    public ResponseEntity<OneTripDTO> addDayObjectiveToTripPlan(AddDayObjectiveDTO addDayObjectiveDTO,
                                                          Principal principal)
            throws InsufficientPostDataException, ResourceNotFoundException, BadLocationException,
            UserNotFoundException, UnauthorizedOperationException {
        checkAddDayObjectiveDTO(addDayObjectiveDTO);

        Optional<User> userOptional = this.userRepository.findByEmail(principal.getName());
        if(userOptional.isEmpty()) {
            throw new UserNotFoundException("Trip update couldn't be done, user not found");
        }
        User user = userOptional.get();

        Optional<Trip> tripOptional = this.tripRepository.findById(addDayObjectiveDTO.getTripId());
        if(tripOptional.isEmpty()) {
            throw new ResourceNotFoundException(STR."Trip with id \{addDayObjectiveDTO.getTripId()} couldn't be found");
        }
        Trip trip = tripOptional.get();

        if(trip.getUser().getId() != user.getId()) {
            throw new UnauthorizedOperationException("You can't update a trip which belongs to another user");
        }

        Location locationToAdd = this.locationService.findLocationById(addDayObjectiveDTO.getLocationIdToAdd());
        if(!this.locationService.checkIfLocationBelongsToDestination(trip.getDestination(), locationToAdd)) {
            throw new BadLocationException(STR."Location \{locationToAdd.getId()} does not belong to destination \{trip.getDestination().getId()} of the trip \{trip.getId()}");
        }

        Optional<TripPlannificationDay> tripPlannificationDayOptional = this.tripPlannificationDayRepository
                .findById(addDayObjectiveDTO.getTripPlannificationDayId());
        if(tripPlannificationDayOptional.isEmpty()) {
            throw new ResourceNotFoundException(STR."Trip plannification day with id \{addDayObjectiveDTO.getTripPlannificationDayId()}"
                    + " couldn't be found");
        }

        TripPlannificationDay tripPlannificationDay = tripPlannificationDayOptional.get();
        if(tripPlannificationDay.getTrip().getId() != trip.getId()) {
            throw new ResourceNotFoundException(STR."Plannification day with id \{tripPlannificationDay.getId()} doesn't belong to trip with id \{trip.getId()}");
        }

        TripDayObjective newDayObjective = new TripDayObjective();
        newDayObjective.setTripDay(tripPlannificationDay);
        newDayObjective.setType(locationToAdd.getLocationType().getName());
        newDayObjective.setPriority(tripPlannificationDay.getLocations().size());
        if(this.tripService.checkIfLocationPresentInRecommendations(trip, locationToAdd)) {
            newDayObjective.setReasonForChoosing("Your personal choice, it was also present in recommendations.");
        } else {
            newDayObjective.setReasonForChoosing("Your personal choice.");
        }
        newDayObjective.setLocation(locationToAdd);
        if(locationToAdd.getLocationType().getType().equalsIgnoreCase(Constants.ATTRACTION_TYPE)) {
            newDayObjective.setMinutesPlanned(Constants.ATTRACTION_MINUTES_ALLOCATED);
        } else if(locationToAdd.getLocationType().getType().equalsIgnoreCase(Constants.RESTAURANT_TYPE)) {
            newDayObjective.setMinutesPlanned(Constants.RESTAURANT_MINUTES_ALLOCATED);
        } else {
            newDayObjective.setMinutesPlanned(30);
        }

        List<TripDayObjective> objectives = tripPlannificationDay.getLocations()
                .stream()
                .sorted(Comparator.comparingInt(TripDayObjective::getPriority))
                .toList();
        int nrOfObjectives = objectives.size();

        if(nrOfObjectives == 0) {
            Date objectiveStartTime = Utils.addMinutesToDate(tripPlannificationDay.getRecommendedTimeToStart(),
                    Constants.TIMING_ERROR);
            Date objectiveEndTime = Utils.addMinutesToDate(objectiveStartTime, newDayObjective.getMinutesPlanned());
            newDayObjective.setStartTime(objectiveStartTime);
            newDayObjective.setEndTime(objectiveEndTime);
            newDayObjective.setNextObjectiveName("");
            newDayObjective.setTravelLengthToNextObjective(0);
            newDayObjective.setTravelTimeToNextObjective(0);
        } else {
            TripDayObjective lastObjective = objectives.get(nrOfObjectives - 1);
            lastObjective.setNextObjectiveName(newDayObjective.getLocation().getName());

            TwoPointsRouteDTO route = tripService.getRoutingDetailsBetweenTwoPoints(lastObjective, newDayObjective);
            lastObjective.setTravelTimeToNextObjective(route.getTime());
            lastObjective.setTravelLengthToNextObjective(route.getDistance());

            Date objectiveStartTime = Utils.addMinutesToDate(lastObjective.getEndTime(), Constants.TIMING_ERROR +
                    lastObjective.getTravelTimeToNextObjective());
            Date objectiveEndTime = Utils.addMinutesToDate(objectiveStartTime, newDayObjective.getMinutesPlanned());
            newDayObjective.setStartTime(objectiveStartTime);
            newDayObjective.setEndTime(objectiveEndTime);
            newDayObjective.setTravelTimeToNextObjective(0);
            newDayObjective.setTravelLengthToNextObjective(0);
            newDayObjective.setNextObjectiveName("");
        }

        this.tripService.addLocationTagsToTrip(trip, locationToAdd);
        this.tripService.deleteLocationFromTripRecommendationsIfPresent(trip, locationToAdd);

        TripDayObjective dayObjectiveSaved = this.tripDayObjectiveRepository.save(newDayObjective);
        tripPlannificationDay.getLocations().add(dayObjectiveSaved);

        return new ResponseEntity<>(new OneTripDTO(this.tripRepository.save(trip)), HttpStatus.OK);
    }

    public ResponseEntity<OneTripDTO> deleteDayObjectiveFromTripPlan(int id, Principal principal)
            throws ResourceNotFoundException, UserNotFoundException, UnauthorizedOperationException {
        Optional<TripDayObjective> tripDayObjectiveOptional = this.tripDayObjectiveRepository.findById(id);

        if(tripDayObjectiveOptional.isEmpty()) {
            throw new ResourceNotFoundException(STR."Trip day objective with id \{id} couldn't be found");
        }

        TripDayObjective tripDayObjectiveToDelete = tripDayObjectiveOptional.get();

        Optional<User> userOptional = this.userRepository.findByEmail(principal.getName());
        if(userOptional.isEmpty()) {
            throw new UserNotFoundException("Can't delete the trip day objective because your user couldn't be found");
        }
        User user = userOptional.get();

        TripPlannificationDay tripPlannificationDay = tripDayObjectiveToDelete.getTripDay();
        Trip trip = tripPlannificationDay.getTrip();

        if(trip.getUser().getId() != user.getId()) {
            throw new UnauthorizedOperationException("You can't modify the plan of a trip which belongs to another user");
        }

        if(tripDayObjectiveToDelete.getTripDay().getLocations().size() > 1) {
            List<TripDayObjective> tripDayObjectivesToUpdate = tripDayObjectiveToDelete
                    .getTripDay()
                    .getLocations()
                    .stream()
                    .filter(tripDayObjectiveFromSameDay -> tripDayObjectiveFromSameDay.getPriority() > tripDayObjectiveToDelete.getPriority())
                    .toList();

            tripDayObjectivesToUpdate
                    .forEach(tripDayObjective -> {
                        int oldPriority = tripDayObjective.getPriority();
                        tripDayObjective.setPriority(oldPriority - 1);
                        this.tripDayObjectiveRepository.save(tripDayObjective);
                    });
        }

        if(tripDayObjectiveToDelete.getPriority() == tripPlannificationDay.getLocations().size() - 1) {
            if(tripPlannificationDay.getLocations().size() == 1) {
                tripPlannificationDay.getLocations().remove(tripDayObjectiveToDelete);
            } else {
                tripPlannificationDay.getLocations().remove(tripDayObjectiveToDelete);
                TripDayObjective lastObjective = tripPlannificationDay.getLocations().getLast();
                lastObjective.setTravelLengthToNextObjective(0);
                lastObjective.setTravelTimeToNextObjective(0);
                lastObjective.setNextObjectiveName("");
            }
        } else {
            tripPlannificationDay.getLocations().remove(tripDayObjectiveToDelete);
            this.tripService.computeObjectivesTimingsForTripDay(tripPlannificationDay, false);
        }

        this.tripService.removeTripTagsAssociatedToLocation(trip, tripDayObjectiveToDelete.getLocation());
        this.tripDayObjectiveRepository.deleteById(tripDayObjectiveToDelete.getId());
        Trip tripUpdated = this.tripService.updateTrip(trip);
        return new ResponseEntity<>(new OneTripDTO(tripUpdated), HttpStatus.OK);
    }

    public void checkAddDayObjectiveDTO(AddDayObjectiveDTO addDayObjectiveDTO)
            throws InsufficientPostDataException {
        if(addDayObjectiveDTO.getTripId() == null) {
            throw new InsufficientPostDataException("Trip plan couldn't be updated because " +
                    "no tripId to be added was specified");
        }

        if(addDayObjectiveDTO.getLocationIdToAdd() == null) {
            throw new InsufficientPostDataException("Trip plan couldn't be updated because " +
                    "no locationId to be added was specified");
        }

        if(addDayObjectiveDTO.getTripPlannificationDayId() == null) {
            throw new InsufficientPostDataException("Trip plan couldn't be updated because no " +
                    "plannification day id was specified");
        }
    }

}
