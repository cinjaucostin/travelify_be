package com.costin.travelify.security;

import com.costin.travelify.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private final UserService userService;

    private final LogoutHandler logoutHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(this.userService);
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.setAllowedOrigins(List.of("http://localhost:3000"));
        corsConfiguration.setAllowedHeaders(Arrays.asList("Origin", "Access-Control-Allow-Origin", "Content-Type",
                "Accept", "Authorization", "Origin, Accept", "X-Requested-With",
                "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        corsConfiguration.setExposedHeaders(Arrays.asList("Origin", "Content-Type", "Accept", "Authorization",
                "Access-Control-Allow-Origin", "Access-Control-Allow-Origin", "Access-Control-Allow-Credentials"));
        corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        urlBasedCorsConfigurationSource.registerCorsConfiguration("/**", corsConfiguration);
        return new CorsFilter(urlBasedCorsConfigurationSource);
    }

    @Bean
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/authentication/login", "/api/authentication/register",
                                "/api/search", "/api/search/advanced")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/authentication/logout")
                        .authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/destinations/**", "/api/locations/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/destinations/**", "/api/locations/**")
                        .hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/destinations/**", "/api/locations/**")
                        .hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/feedback")
                        .hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/reviews", "/api/appreciations")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/reviews", "/api/appreciations")
                        .authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/reviews/**", "/api/appreciations/**")
                        .authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/reviews/**")
                        .authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/trips")
                        .authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/trips/filter")
                        .authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/trips/**")
                        .authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/trips/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/trips/**")
                        .authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/trips_objectives")
                        .authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/trips_objectives/**")
                        .authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users")
                        .permitAll()
                        .anyRequest()
                        .permitAll())
                .logout(logoutConfigurer ->
                        logoutConfigurer
                                .logoutSuccessHandler(
                                        (request, response, authentication) ->
                                                SecurityContextHolder.clearContext()))

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(this.jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
