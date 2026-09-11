package com.shivankkapoor.standbase.controller;

import com.shivankkapoor.standbase.dto.request.LoginRequestDTO;
import com.shivankkapoor.standbase.dto.request.TotpVerifyRequestDTO;
import com.shivankkapoor.standbase.dto.response.LoginResponseDTO;
import com.shivankkapoor.standbase.service.AuthService;
import com.shivankkapoor.standbase.service.IpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        String userAgent = request.getHeader("User-Agent");
        AuthService.LoginResult result = authService.login(loginRequest.getUsername(), loginRequest.getPassword(), ip, userAgent);
        if (result.rateLimited()) {
            return ResponseEntity.status(429).build();
        }
        if (!result.success()) {
            return ResponseEntity.status(401).build();
        }
        LoginResponseDTO response = new LoginResponseDTO();
        if (result.totpRequired()) {
            response.setStatus("totp_required");
            response.setPreAuthToken(result.preAuthToken());
        } else {
            response.setStatus("ok");
            response.setSessionToken(result.sessionToken());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/totp/verify")
    public ResponseEntity<LoginResponseDTO> verifyTotp(@Valid @RequestBody TotpVerifyRequestDTO totpRequest, HttpServletRequest request) {
        String ip = ipService.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        AuthService.VerifyTotpResult result = authService.verifyTotp(totpRequest.getPreAuthToken(), totpRequest.getTotpCode(), ip, userAgent);
        if (result.rateLimited()) {
            return ResponseEntity.status(429).build();
        }
        if (!result.success()) {
            return ResponseEntity.status(401).build();
        }
        LoginResponseDTO response = new LoginResponseDTO();
        response.setStatus("ok");
        response.setSessionToken(result.sessionToken());
        return ResponseEntity.ok(response);
    }
}
