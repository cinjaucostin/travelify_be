package com.costin.travelify.security;

import com.costin.travelify.dto.request_dto.LoginDTO;
import com.costin.travelify.dto.response_dto.LoginResponseDTO;
import com.costin.travelify.dto.request_dto.RegisterDTO;
import com.costin.travelify.entities.User;
import com.costin.travelify.exceptions.EmailAlreadyUsedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/authentication")
@CrossOrigin
public class AuthenticationController {
    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private LogoutHandler logoutHandler;

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody RegisterDTO registerDTO)
            throws EmailAlreadyUsedException {
        return this.authenticationService.performRegister(registerDTO);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginDTO loginRequest,
                                                  HttpServletRequest request,
                                                  HttpServletResponse response) {
        return this.authenticationService.performLogin(loginRequest, request, response);
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request,
                       HttpServletResponse response,
                       Authentication authentication) {
        logoutHandler.logout(request, response, authentication);
    }

}
