package com.shivankkapoor.standbase.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shivankkapoor.standbase.dto.request.LoginRequestDTO;
import com.shivankkapoor.standbase.dto.request.TotpVerifyRequestDTO;
import com.shivankkapoor.standbase.service.AuthService;
import com.shivankkapoor.standbase.service.IpService;
import com.shivankkapoor.standbase.service.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean AuthService authService;
    @MockitoBean IpService ipService;
    @MockitoBean SessionService sessionService;

    @Test
    void login_validCredentials_returns200WithSessionToken() throws Exception {
        when(ipService.getClientIp(any())).thenReturn("1.2.3.4");
        when(authService.login(eq("shivank"), eq("pass"), eq("1.2.3.4")))
                .thenReturn(AuthService.LoginResult.success("session-token"));

        LoginRequestDTO body = new LoginRequestDTO();
        body.setUsername("shivank");
        body.setPassword("pass");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.sessionToken").value("session-token"));
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        when(ipService.getClientIp(any())).thenReturn("1.2.3.4");
        when(authService.login(any(), any(), any())).thenReturn(AuthService.LoginResult.failure());

        LoginRequestDTO body = new LoginRequestDTO();
        body.setUsername("shivank");
        body.setPassword("wrong");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_totpEnabled_returns200WithPreAuthToken() throws Exception {
        when(ipService.getClientIp(any())).thenReturn("1.2.3.4");
        when(authService.login(any(), any(), any()))
                .thenReturn(AuthService.LoginResult.totpRequired("pre-auth-token"));

        LoginRequestDTO body = new LoginRequestDTO();
        body.setUsername("shivank");
        body.setPassword("pass");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("totp_required"))
                .andExpect(jsonPath("$.preAuthToken").value("pre-auth-token"));
    }

    @Test
    void login_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void totpVerify_validToken_returns200WithSessionToken() throws Exception {
        when(ipService.getClientIp(any())).thenReturn("1.2.3.4");
        when(authService.verifyTotp(eq("pre-auth-token"), eq("123456"), eq("1.2.3.4")))
                .thenReturn("session-token");

        TotpVerifyRequestDTO body = new TotpVerifyRequestDTO();
        body.setPreAuthToken("pre-auth-token");
        body.setTotpCode("123456");

        mockMvc.perform(post("/auth/totp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.sessionToken").value("session-token"));
    }

    @Test
    void totpVerify_invalidToken_returns401() throws Exception {
        when(ipService.getClientIp(any())).thenReturn("1.2.3.4");
        when(authService.verifyTotp(any(), any(), any())).thenReturn(null);

        TotpVerifyRequestDTO body = new TotpVerifyRequestDTO();
        body.setPreAuthToken("bad-token");
        body.setTotpCode("000000");

        mockMvc.perform(post("/auth/totp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }
}
