package com.costin.travelify.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "recommendations")
@Data
public class Recommendation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String type;
    @Column(name = "recommendation_reason", length = 1024)
    private String recommendationReason;
    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location location;
    @ManyToOne
    @JoinColumn(name = "trip_id")
    @JsonIgnore
    private Trip trip;

    @JsonProperty("locationId")
    public Integer getLocationId() {
        return (location != null) ? location.getId() : null;
    }

    @JsonProperty("locationName")
    public String getLocationName() { return (location != null) ? location.getName() : ""; }

    @JsonProperty("tripId")
    public Integer getTripId() {
        return (trip != null) ? trip.getId() : null;
    }
}
