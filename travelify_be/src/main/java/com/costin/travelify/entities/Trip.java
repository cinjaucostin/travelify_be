package com.costin.travelify.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "trips")
@NoArgsConstructor
@Getter
@Setter
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "first_day")
    private LocalDate firstDay;

    @Column(name = "last_day")
    private LocalDate lastDay;

    @Column(name = "number_of_days")
    private int numberOfDays;

    private String name;

    private String month;

    @Column(name = "created_timestamp")
    private LocalDateTime createdTimestamp;

    @ManyToMany
    @JoinTable(
            name = "trip_location_tag",
            joinColumns = @JoinColumn(name = "trip_id"),
            inverseJoinColumns = @JoinColumn(name = "location_tag_id")
    )
    private List<LocationTag> tripObjectivesTags;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @JsonIgnore
    private User user;

    @ManyToOne
    @JoinColumn(name = "destination_id", referencedColumnName = "id")
    @JsonIgnore
    private Destination destination;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL)
    private List<TripPlannificationDay> plannificationDays;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL)
    private List<Review> reviews;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Recommendation> recommendations;

    @OneToMany(mappedBy = "trip")
    private List<Appreciation> appreciations;

    @JsonProperty("destinationId")
    public double getDestinationId() { return destination.getId(); }

    @JsonProperty("userId")
    public int getUserId() { if(user != null ) return user.getId(); else return 1; }

}
