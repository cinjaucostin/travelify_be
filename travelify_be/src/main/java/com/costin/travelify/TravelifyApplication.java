package com.costin.travelify;

import com.costin.travelify.dto.arcgis_dto.ArcgisRouteResponseDTO;
import com.costin.travelify.dto.arcgis_dto.GeoLocationDTO;
import com.costin.travelify.entities.Roles;
import com.costin.travelify.entities.User;
import com.costin.travelify.repository.DestinationRepository;
import com.costin.travelify.repository.UserRepository;
import com.costin.travelify.service.apis.ArcgisService;
import com.costin.travelify.service.apis.FoursquareService;
import com.costin.travelify.service.apis.OpenweatherService;
import com.costin.travelify.service.apis.TripadvisorService;
import com.costin.travelify.utils.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@SpringBootApplication
@EnableScheduling
public class TravelifyApplication {
	@Value("${admin_username}")
	private String adminUsername;

	@Value("${admin_password}")
	private String adminPassword;

	@Bean
	CommandLineRunner commandLineRunner(
			TripadvisorService tripadvisorService,
			OpenweatherService openweatherService,
			DestinationRepository destinationRepository,
			ArcgisService arcgisService,
			FoursquareService foursquareService,
			UserRepository userRepository) {
		return args -> {
			if(userRepository.findByEmail(adminUsername).isEmpty()) {
				BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

				User adminUser = new User();
				adminUser.setEmail(adminUsername);
				adminUser.setPassword(bCryptPasswordEncoder.encode(adminPassword));
				adminUser.setRole(Roles.ADMIN);
				userRepository.save(adminUser);
			}
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(TravelifyApplication.class, args);
	}

}
