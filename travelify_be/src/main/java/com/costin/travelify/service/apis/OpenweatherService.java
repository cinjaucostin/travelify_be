package com.costin.travelify.service.apis;

import com.costin.travelify.dto.openweather_dto.WeatherCurrentDTO;
import com.costin.travelify.dto.openweather_dto.WeatherDetailsDTO;
import com.costin.travelify.dto.openweather_dto.WeatherForecastDTO;
import com.costin.travelify.entities.Destination;
import com.costin.travelify.repository.DestinationRepository;
import com.costin.travelify.service.DestinationService;
import com.costin.travelify.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.CompletableFuture;

@Service
public class OpenweatherService {
    private static final Logger log = LoggerFactory.getLogger(OpenweatherService.class);

    @Autowired
    private DestinationRepository destinationRepository;

    @Value("${openweather_api_key}")
    private String openweatherApiKey;

//    @Scheduled(cron = "0 03 14 * * *", zone = "Europe/Bucharest")
    @Scheduled(cron = "0 0 * * * *", zone = "Europe/Bucharest")
    public void getWeatherInfo() {
        fetchWeatherDetails();
    }

    @Async
    public CompletableFuture<Void> fetchWeatherDetails() {
        List<Destination> destinations = this.destinationRepository.findAll();

        destinations.forEach(destination -> {
            log.info(STR."Search weather forecast data for destination \{destination.getName()}");
            Optional<WeatherForecastDTO> weatherForecastOptional = searchFutureWeatherForCoordinates(destination.getLatitude(), destination.getLongitude(),
                    Utils.getTomorrowsLinuxTimestamp());

            weatherForecastOptional.ifPresent(weatherForecast -> {
                OptionalDouble highestTemperature = weatherForecast.getData()
                        .stream()
                        .mapToDouble(WeatherDetailsDTO::getFeels_like)
                        .max();
                destination.setSunrise(Utils.convertFromUnixTimestampToDateUsingTimezoneIdV2(weatherForecast.getData().getFirst().getSunrise(),
                        destination.getTimezone()));
                destination.setSunset(Utils.convertFromUnixTimestampToDateUsingTimezoneIdV2(weatherForecast.getData().getFirst().getSunset(),
                        destination.getTimezone()));
                highestTemperature.ifPresent(temperature ->
                        destination.setTemperature((int) Utils.convertFromKelvinToCelsiusDegrees(temperature)));
                this.destinationRepository.save(destination);
                log.info(STR."Weather data updated for destination \{destination.getName()}");
            });
        });

        return CompletableFuture.completedFuture(null);
    }

    public Optional<WeatherCurrentDTO> searchCurrentWeatherForCoordinates(double latitude, double longitude) {
        String getWeatherURL = String.format("https://api.openweathermap.org/data/3.0/onecall?lat=%f&lon=%f&exclude=minutely,hourly&appid=%s",
                latitude, longitude, openweatherApiKey);

        log.info(STR."Getting current weather info: \{getWeatherURL}");

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<WeatherCurrentDTO> response = restTemplate.getForEntity(getWeatherURL, WeatherCurrentDTO.class);
            HttpStatusCode responseCode = response.getStatusCode();
            if (responseCode == HttpStatus.OK) {
                return Optional.ofNullable(response.getBody());
            }
        } catch (HttpClientErrorException e) {
            HttpStatusCode responseCode = e.getStatusCode();
            log.error("Getting current weather for coordinates lat = " + latitude + " lon = " + longitude + " didn't work, status code: " + responseCode);
            return Optional.empty();
        }

        return Optional.empty();
    }

    /*
        The forecast is available for just 7 days beginning with the current date.
     */
    public Optional<WeatherForecastDTO> searchFutureWeatherForCoordinates(double latitude, double longitude, long dt) {
        String getWeatherURL = String.format("https://api.openweathermap.org/data/3.0/onecall/timemachine?lat=%f&lon=%f&dt=%d&appid=%s",
                latitude, longitude, dt, openweatherApiKey
        );

        log.info(STR."Getting forecast weather info: \{getWeatherURL}");

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<WeatherForecastDTO> response = restTemplate.getForEntity(getWeatherURL, WeatherForecastDTO.class);
            HttpStatusCode responseCode = response.getStatusCode();
            if (responseCode == HttpStatus.OK) {
                return Optional.ofNullable(response.getBody());
            }
        } catch (HttpClientErrorException e) {
            HttpStatusCode responseCode = e.getStatusCode();
            log.error(STR."Getting forecast weather for coordinates lat = \{latitude} lon = \{longitude} didn't work, status code: \{responseCode}");
            return Optional.empty();
        }

        return Optional.empty();
    }

}
