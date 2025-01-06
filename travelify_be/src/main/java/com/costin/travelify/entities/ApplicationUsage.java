package com.costin.travelify.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "application_usage")
public class ApplicationUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "tripadvisor_first_key_requests")
    private int tripadvisorFirstKeyRequests;

    @Column(name = "tripadvisor_second_key_requests")
    private int tripadvisorSecondKeyRequests;

    @Column(name = "ow_key_requests")
    private int openweatherKeyRequests;

    @Column(name = "foursquare_key_requests")
    private int foursquareKeyRequests;
}
