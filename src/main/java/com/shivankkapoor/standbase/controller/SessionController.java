package com.shivankkapoor.standbase.controller;

import com.shivankkapoor.standbase.dto.response.CheckResponseDTO;
import com.shivankkapoor.standbase.dto.response.ResponseDTO;
import com.shivankkapoor.standbase.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/session")
public class SessionController {

    private final AuthService authService;

    public SessionController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/check")
    public ResponseEntity<CheckResponseDTO> check(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        CheckResponseDTO resp = new CheckResponseDTO();
        resp.setUserName(authService.getUsernameById(userId));
        resp.setStatus("ok");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/logout")
    public ResponseEntity<ResponseDTO> logout(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        authService.logoutByUserId(userId);
        ResponseDTO response = new ResponseDTO();
        response.setStatus("ok");
        return ResponseEntity.ok(response);
    }

}
