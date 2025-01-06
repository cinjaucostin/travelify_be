package com.costin.travelify.security;

import com.costin.travelify.dto.request_dto.LoginDTO;
import com.costin.travelify.dto.response_dto.LoginResponseDTO;
import com.costin.travelify.dto.request_dto.RegisterDTO;
import com.costin.travelify.entities.Roles;
import com.costin.travelify.entities.Token;
import com.costin.travelify.entities.User;
import com.costin.travelify.exceptions.EmailAlreadyUsedException;
import com.costin.travelify.repository.TokenRepository;
import com.costin.travelify.service.UserService;
import com.costin.travelify.utils.Constants;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthenticationService {
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserService userService;
    @Autowired
    private TokenRepository tokenRepository;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public void revokeAndExpireUserTokens(int userId) {
        List<Token> tokens = this.tokenRepository.findAllValidTokensByUser(userId);
        tokens.forEach((token) -> {
            token.setExpired(true);
        });
        this.tokenRepository.saveAll(tokens);
    }

    public ResponseEntity<User> performRegister(RegisterDTO registerDTO)
            throws EmailAlreadyUsedException {
        Optional<User> userByEmailOptional = this.userService
                .findByEmail(registerDTO.getEmail());

        if(userByEmailOptional.isPresent()) {
            throw new EmailAlreadyUsedException("Email already used.");
        }

        User newUser = new User();
        newUser.setEmail(registerDTO.getEmail());
        newUser.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        newUser.setFirstName(registerDTO.getFirstName());
        newUser.setLastName(registerDTO.getLastName());
        newUser.setRole(Roles.USER);
        newUser.setJoiningDate(LocalDateTime.now());
        newUser.setProfileImage(Constants.PROFILE_IMAGE);

        this.userService.save(newUser);

        return new ResponseEntity<>(
                newUser,
                HttpStatus.OK
        );
    }

    public ResponseEntity<LoginResponseDTO> performLogin(LoginDTO loginDTO,
                                                         HttpServletRequest request,
                                                         HttpServletResponse response) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDTO.getEmail(),
                        loginDTO.getPassword()
                )
        );

        User user = this.userService.findByEmail(loginDTO.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found!"));

        revokeAndExpireUserTokens(user.getId());

        Map<String, Object> claims = new HashMap<>();
        claims.put("authorities", List.of(user.getRole().name()));
        claims.put("email", user.getEmail());

        String jwtToken = this.jwtService.generateToken(claims, user);
        Token token = new Token(jwtToken, false, user);

        this.tokenRepository.save(token);

        return new ResponseEntity<>(new LoginResponseDTO(user.getId(), jwtToken, user.getProfileImage()), HttpStatus.OK);
    }

    public boolean checkIfAuthenticationRepresentsAdminUser(Authentication authentication) {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        for(GrantedAuthority authority : authorities) {
            if(authority.getAuthority().equals("ADMIN")) {
                return true;
            }
        }
        return false;
    }

}
