package com.costin.travelify.controller;

import com.costin.travelify.dto.response_dto.UserActivityDTO;
import com.costin.travelify.service.AppreciationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private AppreciationService appreciationService;
    @GetMapping("/activity")
    public ResponseEntity<UserActivityDTO> getUserActivity(@RequestParam(name = "user_id") Integer userId) {
        return this.appreciationService.getUserActivity(userId);
    }

}
