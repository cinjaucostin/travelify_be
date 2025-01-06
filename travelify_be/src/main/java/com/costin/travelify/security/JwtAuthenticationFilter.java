package com.costin.travelify.security;

import com.costin.travelify.entities.Token;
import com.costin.travelify.repository.TokenRepository;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String AUTH_HEADER_PREFIX = "Bearer ";

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userService;

    @Autowired
    private TokenRepository tokenRepository;

    private boolean checkIfTokenIsExpired(String jwtToken) {
        Token token = this.tokenRepository.findByToken(jwtToken).orElse(null);

        if (token == null) {
            return false;
        }

        return !token.isExpired();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String jwtToken;
        String email;

        if (authHeader == null || !authHeader.startsWith(AUTH_HEADER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        jwtToken = authHeader.substring(AUTH_HEADER_PREFIX.length());

        try {
            boolean expired = jwtService.isTokenExpired(jwtToken);

            if (expired) {
                filterChain.doFilter(request, response);
                return;
            }
        } catch (ExpiredJwtException e) {
            Token token = this.tokenRepository.findByToken(jwtToken).orElse(null);

            if (token != null) {
                token.setExpired(true);
                this.tokenRepository.save(token);
            }

            filterChain.doFilter(request, response);
            return;
        }

        email = jwtService.extractUsername(jwtToken);

        // Daca nu este deja autentificat si exista in baza de date, il autentificam.
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userService.loadUserByUsername(email);

            if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()) {
                filterChain.doFilter(request, response);
                return;
            }

            boolean tokenIsNotExpired = checkIfTokenIsExpired(jwtToken);

            if (jwtService.isTokenValidForUser(jwtToken, userDetails) && tokenIsNotExpired) {
                UsernamePasswordAuthenticationToken authContextToken =
                        new UsernamePasswordAuthenticationToken(userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authContextToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authContextToken);
            }
        }

        filterChain.doFilter(request, response);
    }

}
