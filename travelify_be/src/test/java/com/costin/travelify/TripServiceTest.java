package com.costin.travelify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import com.costin.travelify.dto.arcgis_dto.GeoLocationDTO;
import com.costin.travelify.dto.response_dto.TwoPointsRouteDTO;
import com.costin.travelify.entities.Destination;
import com.costin.travelify.entities.Location;
import com.costin.travelify.entities.TripDayObjective;
import com.costin.travelify.service.LocationService;
import com.costin.travelify.service.TripService;
import com.costin.travelify.service.apis.ArcgisService;
import com.mysql.cj.protocol.a.TracingPacketReader;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class TripServiceTest {
    @InjectMocks
    private TripService tripService;
    @Mock
    private LocationService locationService;
    @Mock
    private ArcgisService arcgisService;
    private Destination destination;
    private Location location1, location2, location3;

    @BeforeEach
    public void initBeforeEachTest() {
    }

    @Test
    public void testGetClosestLocationToCenter() {
        Destination destination = new Destination();
        destination.setLatitude(41.385597);
        destination.setLongitude(2.169576);

        Location location1 = new Location();
        location1.setLatitude(41.403423);
        location1.setLongitude(2.174611);

        Location location2 = new Location();
        location2.setLatitude(41.40944);
        location2.setLongitude(2.22239);

        Location closestLocation = this.tripService.getClosestLocationToCenter(destination, List.of(location1, location2));

        assertEquals(location1, closestLocation, "The closest location was not found properly");
    }

    @Test
    public void testGetOptimizedTour() {
        Destination destination = new Destination();
        destination.setLatitude(41.385597);
        destination.setLongitude(2.169576);

        Location location1 = new Location();
        location1.setLatitude(41.403423);
        location1.setLongitude(2.174611);

        Location location2 = new Location();
        location2.setLatitude(41.40944);
        location2.setLongitude(2.22239);

        Location location3 = new Location();
        location3.setLatitude(41.373955);
        location3.setLongitude(2.177461);

        List<Location> optimizedTour = this.tripService.getOptimizedTour(destination, List.of(location1, location2, location3));

        List<Location> optimalTour = List.of(location3, location1, location2);

        for(int i = 0; i < optimalTour.size(); i++) {
            assertEquals(optimalTour.get(i), optimizedTour.get(i));
        }
    }

    @Test
    public void testDivideObjectivesBetweenDays() {
        Map<Integer, List<Integer>> schedulerResults = this.tripService.divideObjectivesBetweenDays(7, 3);

        assertEquals(0, schedulerResults.get(0).getFirst());
        assertEquals(2, schedulerResults.get(0).getLast());

        assertEquals(2, schedulerResults.get(1).getFirst());
        assertEquals(4, schedulerResults.get(1).getLast());

        assertEquals(4, schedulerResults.get(2).getFirst());
        assertEquals(7, schedulerResults.get(2).getLast());
    }

    @Test
    public void testGetRoutingDetailsBetweenTwoPoints() {
        TripDayObjective firstObjective = new TripDayObjective();
        Location location1 = new Location();
        location1.setLatitude(41.403423);
        location1.setLongitude(2.174611);
        firstObjective.setLocation(location1);

        TripDayObjective secondObjective = new TripDayObjective();
        Location location2 = new Location();
        location2.setLatitude(41.40944);
        location2.setLongitude(2.22239);
        secondObjective.setLocation(location2);

        TwoPointsRouteDTO routeResult = this.tripService.getRoutingDetailsBetweenTwoPoints(firstObjective, secondObjective);

        ArgumentCaptor<List<GeoLocationDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(arcgisService).getRoutingBetweenLocations(captor.capture());

        List<GeoLocationDTO> capturedLocations = captor.getValue();
        assertEquals(2, capturedLocations.size());
        assertEquals(firstObjective.getLocation().getLatitude(), capturedLocations.get(0).getLatitude());
        assertEquals(firstObjective.getLocation().getLongitude(), capturedLocations.get(0).getLongitude());
        assertEquals(secondObjective.getLocation().getLatitude(), capturedLocations.get(1).getLatitude());
        assertEquals(secondObjective.getLocation().getLongitude(), capturedLocations.get(1).getLongitude());
    }

}
