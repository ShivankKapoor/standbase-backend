package com.shivankkapoor.standbase.controller;

import com.shivankkapoor.standbase.dto.LoginRequestDTO;
import com.shivankkapoor.standbase.dto.LoginResponseDTO;
import com.shivankkapoor.standbase.service.AuthService;
import com.shivankkapoor.standbase.service.IpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final IpService ipService;

    public AuthController(AuthService authService, IpService ipService) {
        this.authService = authService;
        this.ipService = ipService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest, HttpServletRequest request) {
        String ip = ipService.getClientIp(request);
        String token = authService.login(loginRequest.getUsername(), loginRequest.getPassword(), ip);
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        LoginResponseDTO loginResponse = new LoginResponseDTO();
        loginResponse.setStatus("ok");
        loginResponse.setSessionToken(token);
        return ResponseEntity.ok(loginResponse);
    }

}
