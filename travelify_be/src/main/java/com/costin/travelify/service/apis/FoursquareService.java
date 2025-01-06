package com.costin.travelify.service.apis;

import com.costin.travelify.dto.foursquare_dto.PlaceSearchResponseDTO;
import com.costin.travelify.dto.tripadvisor_dto.LocationSearchDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class FoursquareService {
    @Value("${foursquare_api_key}")
    private String foursquareApiKey;

    private final String FIELDS = "name,description,hours_popular,hours,price,features,categories,chains," +
            "popularity,location,website,tastes,social_media";

    /*
        I) Get Place Details
        - ai nevoie de fsqid pt locatia respectiva
        - trb sa specifici fields ca sa iti returneze ce field-uri iti trebuie, cele by default nu sunt asa interesante.
            fields = name,description,hours_popular,hours,price,features,categories,chains,popularity,location,website,tastes,social_media

        II) Place Search
        - categories:
            - 10000	Arts and Entertainment
            - 11000	Business and Professional Services
            - 12000	Community and Government
            - 13000	Dining and Drinking
            - 14000	Event
            - 16000	Landmarks and Outdoors
            - 18000	Sports and Recreation
            - 19000	Travel and Transportation
     */

    public Optional<PlaceSearchResponseDTO> searchPlaces(String query, double latitude, double longitude) {
        String ll = latitude + "," + longitude;
        query = query.replace(" ", "%20");
        String getPlacesURL = String.format("https://api.foursquare.com/v3/places/search?query=%s&ll=%s&radius=100", query, ll);

        System.out.println("Getting locations for query = " + query + " at " + latitude + ", " + longitude);

        HttpHeaders headers = new HttpHeaders();
        headers.set("accept", MediaType.APPLICATION_JSON_VALUE);
        headers.set(HttpHeaders.AUTHORIZATION, foursquareApiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<PlaceSearchResponseDTO> response = restTemplate.exchange(getPlacesURL, HttpMethod.GET,
                    entity, PlaceSearchResponseDTO.class);
            HttpStatusCode responseCode = response.getStatusCode();
            if (responseCode == HttpStatus.OK) {
                return Optional.ofNullable(response.getBody());
            }
        } catch (HttpClientErrorException e) {
            HttpStatusCode responseCode = e.getStatusCode();
            System.err.println("Getting locations for query = " + query + " ll = " + ll + " didn't work, status code: " + responseCode);
            return Optional.empty();
        }

        return Optional.empty();
    }

    public Optional<String> searchPlaceDetailsByFsqId(String fsq_id) {
        String getPlaceDetailsURL = String.format("https://api.foursquare.com/v3/places/%s?" +
                "fields=name,description,hours_popular,hours,price,features,categories,chains," +
                "popularity,location,website,tastes,social_media,price", fsq_id);

        System.out.println("Getting locations for place = " + fsq_id);
        System.out.println(getPlaceDetailsURL);

        HttpHeaders headers = new HttpHeaders();
        headers.set("accept", MediaType.APPLICATION_JSON_VALUE);
        headers.set(HttpHeaders.AUTHORIZATION, foursquareApiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<String> response = restTemplate.exchange(getPlaceDetailsURL, HttpMethod.GET,
                    entity, String.class);
            HttpStatusCode responseCode = response.getStatusCode();
            if (responseCode == HttpStatus.OK) {
                return Optional.ofNullable(response.getBody());
            }
        } catch (HttpClientErrorException e) {
            HttpStatusCode responseCode = e.getStatusCode();
            System.err.println("Getting locations for place = " + fsq_id + " didn't work, status code: " + responseCode);
            return Optional.empty();
        }

        return Optional.empty();
    }



}
