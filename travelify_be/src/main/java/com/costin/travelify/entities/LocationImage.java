package com.costin.travelify.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "location_images")
@Getter
@Setter
public class LocationImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String source;
    @Column(name = "tripadvisor_id")
    private long tripadvisorId;
    @ManyToOne
    @JoinColumn(name = "location_id")
    @JsonIgnore
    private Location location;
    @ManyToOne
    @JoinColumn(name = "destination_id")
    @JsonIgnore
    private Destination destination;

}
