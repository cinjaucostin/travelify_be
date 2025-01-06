package com.costin.travelify.controller;

import com.costin.travelify.dto.response_dto.DestinationsSearchResponseDTO;
import com.costin.travelify.dto.response_dto.ResponseDTO;
import com.costin.travelify.dto.response_dto.SearchResponseDTO;
import com.costin.travelify.service.DestinationService;
import com.costin.travelify.service.LocationService;
import com.costin.travelify.service.SearchService;
import com.costin.travelify.service.TripService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@CrossOrigin
public class SearchController {
    @Autowired
    private SearchService searchService;

    @GetMapping
    public ResponseEntity<SearchResponseDTO> search(@RequestParam(required=true, name = "query") String query,
                                                    @RequestParam(required=false, name = "type") String type,
                                                    @RequestParam(required = false, name = "order", defaultValue = "relevance") String order) {
        return this.searchService.search(query, type, order);
    }

    @GetMapping("/advanced")
    public ResponseEntity<SearchResponseDTO> advancedSearch(@RequestParam(name = "query") String query) {
        return this.searchService.advancedSearch(query);
    }

    @GetMapping("/advanced_dummy")
    public ResponseEntity<SearchResponseDTO> advancedSearchDummy(@RequestParam(name = "query") String query) throws InterruptedException {
        Thread.sleep(10000);
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

}
