package com.shivankkapoor.standbase.controller;

import com.shivankkapoor.standbase.dto.response.CheckResponseDTO;
import com.shivankkapoor.standbase.dto.response.ResponseDTO;
import com.shivankkapoor.standbase.service.AuthService;
import com.shivankkapoor.standbase.service.IpService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/session")
public class SessionController {

    private final AuthService authService;
    private final IpService ipService;

    public SessionController(AuthService authService, IpService ipService) {
        this.authService = authService;
        this.ipService = ipService;
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
    public ResponseEntity<ResponseDTO> logout(Authentication authentication, HttpServletRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        authService.logoutByUserId(userId, ipService.getClientIp(request));
        ResponseDTO response = new ResponseDTO();
        response.setStatus("ok");
        return ResponseEntity.ok(response);
    }

}
