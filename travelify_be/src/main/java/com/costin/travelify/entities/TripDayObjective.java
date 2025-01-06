package com.costin.travelify.entities;

import com.costin.travelify.utils.Utils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@Table(name = "trip_days_objectives")
@Getter
@Setter
public class TripDayObjective {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private int priority;
    // It can be a custom objective or turistic objective.
    private String type;
    @Column(name = "reason_for_choosing", length = 1024)
    private String reasonForChoosing;

    @Column(name = "minutes_planned")
    private Integer minutesPlanned;

    @Column(name = "start_time")
    @JsonIgnore
    private Date startTime;

    @Column(name = "end_time")
    @JsonIgnore
    private Date endTime;

    @Column(name = "travel_time_to_next_objective")
    private Integer travelTimeToNextObjective;

    @Column(name = "travel_length_to_next_objective")
    private Integer travelLengthToNextObjective;

    @Column(name = "next_objective_name")
    private String nextObjectiveName;

    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location location;

    @ManyToOne
    @JoinColumn(name = "trip_day_id")
    @JsonIgnore
    private TripPlannificationDay tripDay;

    @JsonProperty("locationId")
    public Integer getLocationId() {
        return (location != null) ? location.getId() : null;
    }

    @JsonProperty("tripDayId")
    public Integer getTripDayId() { return (tripDay != null) ? tripDay.getId() : null; }

    @JsonProperty("locationName")
    public String getLocationName() {
        return (location != null) ? location.getName() : null;
    }

    @JsonProperty("startTime")
    public String getStartTimeString() {if(startTime != null)  { return Utils.getHourFromDate(startTime); } else { return ""; } }

    @JsonProperty("endTime")
    public String getEndTimeString() {if(endTime != null)  { return Utils.getHourFromDate(endTime); } else { return ""; } }

}
