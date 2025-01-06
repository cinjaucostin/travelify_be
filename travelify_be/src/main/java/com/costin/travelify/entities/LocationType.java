package com.costin.travelify.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "locations_types")
@NoArgsConstructor
@Getter
@Setter
public class LocationType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String type;
    private String name;

    public LocationType(String type, String name) {
        this.type = type;
        this.name = name;
    }
}
