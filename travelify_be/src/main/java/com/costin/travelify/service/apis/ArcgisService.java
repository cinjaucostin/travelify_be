package com.costin.travelify.service.apis;

import com.costin.travelify.dto.arcgis_dto.ArcgisRouteResponseDTO;
import com.costin.travelify.dto.arcgis_dto.GeoLocationDTO;
import com.costin.travelify.entities.Location;
import com.costin.travelify.utils.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Service
public class ArcgisService {
    @Value("${arcgis_api_key}")
    private String arcgisApiKey;

//    https://developers.arcgis.com/documentation/mapping-apis-and-services/routing/route-and-directions/
//    https://developers.arcgis.com/rest/network/api-reference/route-synchronous-service.htm#ESRI_SECTION2_1A9936A9B63043B7BBD2AF79830B2330

    public Optional<ArcgisRouteResponseDTO> getRoutingBetweenLocations(List<GeoLocationDTO> locations) {
        String getRouteUrl = String.format("https://route.arcgis.com/arcgis/rest/services/World/Route/NAServer/Route_World/solve?f=json&token=%s",
                arcgisApiKey);

        getRouteUrl += STR."&stops=\{this.createStopsParameterFromLocations(locations, ";")}";

        System.out.println(STR."Getting route: \{getRouteUrl}");;
        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<ArcgisRouteResponseDTO> response = restTemplate.getForEntity(getRouteUrl, ArcgisRouteResponseDTO.class);
            HttpStatusCode responseCode = response.getStatusCode();

            if (responseCode == HttpStatus.OK) {
                return Optional.ofNullable(response.getBody());
            }
        } catch (HttpClientErrorException e) {
            HttpStatusCode responseCode = e.getStatusCode();
            System.err.println(STR."Getting route \{getRouteUrl} didn't work, status code: \{responseCode}");
            return Optional.empty();
        }

        return Optional.empty();
    }

    public String createStopsParameterFromLocations(List<GeoLocationDTO> locations, String separ) {
        StringBuilder stopsParameterBuilder = new StringBuilder();
        int size = locations.size();
        for (int i = 0; i < size; i++) {
            GeoLocationDTO location = locations.get(i);
            stopsParameterBuilder.append(location.getLongitude())
                    .append(",")
                    .append(location.getLatitude());
            if (i < size - 1) {
                stopsParameterBuilder.append(separ);
            }
        }
        return stopsParameterBuilder.toString();
    }

}
