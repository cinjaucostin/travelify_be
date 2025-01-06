package com.costin.travelify.entities;


import com.costin.travelify.dto.tripadvisor_dto.LocationDetailsDTO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;
import java.util.List;

@Entity
@Table(name = "destinations")
@NoArgsConstructor
@Getter
@Setter
public class Destination {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private double latitude;

    private double longitude;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String description;

    private String name;

    @Column(name = "ancestor_name")
    private String ancestorName;

    private String country;

    @Column(name = "address_path")
    private String addressPath;

    private String timezone;

    @Column(name = "number_of_views")
    private int numberOfViews;

    @Column(name = "full_load")
    private Integer fullLoad;

    @ManyToOne
    @JoinColumn(name = "destination_type_id")
    private LocationType destinationType;

    @ManyToMany
    @JoinTable(
            name = "destination_location",
            joinColumns = @JoinColumn(name = "destination_id"),
            inverseJoinColumns = @JoinColumn(name = "location_id")
    )
    @JsonIgnore
    private List<Location> locations;

    @OneToMany(mappedBy = "destination")
    @JsonIgnore
    private List<Trip> trips;

    private double popularity;

    @OneToMany(mappedBy = "destination")
    @JsonIgnore
    private List<Review> reviews;

    @OneToMany(mappedBy = "destination", cascade = CascadeType.ALL)
    private List<LocationImage> destinationImages;

    private int temperature;

    private Date sunrise;
    private Date sunset;

    @Column(name = "tripadvisor_id", nullable = false)
    private long tripadvisorId;

    public Destination(double latitude, double longitude,
                       String description, String name,
                       String ancestorName, String country,
                       String addressPath, String timezone,
                       long tripadvisorId) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
        this.name = name;
        this.ancestorName = ancestorName;
        this.country = country;
        this.addressPath = addressPath;
        this.timezone = timezone;
        this.tripadvisorId = tripadvisorId;
    }

    public Destination(LocationDetailsDTO city) {
        this(
            city.getLatitude(),
            city.getLongitude(),
            city.getDescription(),
            city.getName(),
            city.getAddress_obj().getCity(),
            city.getAddress_obj().getCountry(),
            city.getAddress_obj().getAddress_string(),
            city.getTimezone(),
            city.getLocation_id()
        );
    }

}
