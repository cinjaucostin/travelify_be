package com.costin.travelify.entities;

import com.costin.travelify.dto.tripadvisor_dto.LocationDetailsDTO;
import com.costin.travelify.utils.Utils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "locations")
@NoArgsConstructor
@Getter
@Setter
public class Location {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private double latitude;

    private double longitude;

    private String name;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String description;

    @Column(name = "address_path")
    private String addressPath;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String features;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String amenities;

    private String styles;

    private String cuisine;

    private String timezone;

    @Column(name = "number_of_views")
    private int numberOfViews;

    @Lob
    @Column(name = "work_hours", columnDefinition = "LONGTEXT")
    private String workHours;

    @Column(name = "tripadvisor_ranking")
    private String tripadvisorRanking;

    @Column(name = "tripadvisor_number_reviews")
    private Integer tripadvisorNumberOfReviews;

    @Column(name = "tripadvisor_rating")
    private double tripadvisorRating;

    private String ranking;

    @ManyToMany(mappedBy = "locations")
    @JsonIgnore
    private List<Destination> destinations;

    @ManyToOne
    @JoinColumn(name = "location_type_id")
    private LocationType locationType;

    @ManyToMany
    @JoinTable(
            name = "location_location_tag",
            joinColumns = @JoinColumn(name = "location_id"),
            inverseJoinColumns = @JoinColumn(name = "location_tag_id")
    )
    private List<LocationTag> locationTags;

    @OneToMany(mappedBy = "location")
    private List<Review> reviews;

    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL)
    private List<LocationImage> locationImages;

    @Column(name = "tripadvisor_id", nullable = false)
    private long tripadvisorId;

    @JsonProperty("destinationId")
    public int getDestinationId() { return destinations.getFirst().getId(); }

    @JsonProperty("destinationLongitude")
    public double getDestinationLongitude() { return destinations.getFirst().getLongitude(); }

    @JsonProperty("destinationLatitude")
    public double getDestinationLatitude() { return destinations.getFirst().getLatitude(); }

    private double popularity;

    public Location(LocationDetailsDTO locationDetailsDTO) {
        this(
            locationDetailsDTO.getName(),
            locationDetailsDTO.getDescription(),
            locationDetailsDTO.getAddress_obj().getAddress_string(),
            locationDetailsDTO.getLatitude(),
            locationDetailsDTO.getLongitude(),
            locationDetailsDTO.getTimezone(),
            locationDetailsDTO.getRating(),
            locationDetailsDTO.getNum_reviews(),
            locationDetailsDTO.getLocation_id()
        );
    }

    public Location(String name, String description, String address, double latitude, double longitude, String timezone,
            double rating, int numberOfReviews, long tripadvisorId) {
        this.name = name;
        this.description = description;
        this.addressPath = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timezone = timezone;
        this.tripadvisorRating = rating;
        this.tripadvisorNumberOfReviews = numberOfReviews;
        this.tripadvisorId = tripadvisorId;
    }

}
