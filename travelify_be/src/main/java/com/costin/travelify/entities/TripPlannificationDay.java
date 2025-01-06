package com.costin.travelify.entities;

import com.costin.travelify.utils.Utils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "trip_plannification_days")
@Getter
@Setter
public class TripPlannificationDay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;

    private LocalDate date;

    @OneToMany(mappedBy = "tripDay", cascade = CascadeType.ALL)
    @OrderBy("priority")
    private List<TripDayObjective> locations;

    @ManyToOne
    @JoinColumn(name = "trip_id")
    @JsonIgnore
    private Trip trip;

    @Column(name = "recommended_time_to_start")
    @JsonIgnore
    private Date recommendedTimeToStart;

    @Column(name = "recommended_time_to_end")
    @JsonIgnore
    private Date recommendedTimeToEnd;

    @JsonProperty("recommendedTimeToStart")
    public String getRecommendedTimeToStartString() { if(recommendedTimeToStart != null) return Utils.getHourFromDate(recommendedTimeToStart); else return ""; }

    @JsonProperty("recommendedTimeToEnd")
    public String getRecommendedTimeToEndString() { if(recommendedTimeToEnd != null) return Utils.getHourFromDate(recommendedTimeToEnd); else return ""; }

}
