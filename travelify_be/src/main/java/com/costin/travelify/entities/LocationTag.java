package com.costin.travelify.entities;

import com.costin.travelify.repository.LocationTagRepository;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "locations_tags")
@NoArgsConstructor
@Getter
@Setter
@ToString
public class LocationTag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String tag;

    private String name;

    public LocationTag(String tag, String name) {
        this.tag = tag;
        this.name = name;
    }
}
