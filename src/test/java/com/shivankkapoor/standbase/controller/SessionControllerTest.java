package com.shivankkapoor.standbase.controller;

import com.shivankkapoor.standbase.config.SecurityConfig;
import com.shivankkapoor.standbase.service.AuthService;
import com.shivankkapoor.standbase.service.IpService;
import com.shivankkapoor.standbase.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SessionController.class)
@Import(SecurityConfig.class)
class SessionControllerTest {

    @Autowired WebApplicationContext wac;
    MockMvc mockMvc;

    @MockitoBean AuthService authService;
    @MockitoBean IpService ipService;
    @MockitoBean SessionService sessionService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String TOKEN = "test-session-token";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();
        when(ipService.getClientIp(any())).thenReturn("1.2.3.4");
        when(sessionService.getSessionUserID(eq(TOKEN), eq("1.2.3.4"))).thenReturn(USER_ID);
    }

    @Test
    void check_authenticated_returnsUsername() throws Exception {
        when(authService.getUsernameById(USER_ID)).thenReturn("shivank");

        mockMvc.perform(get("/session/check")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.userName").value("shivank"));
    }

    @Test
    void check_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/session/check"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void check_invalidToken_returns401() throws Exception {
        when(sessionService.getSessionUserID(eq("bad-token"), any())).thenReturn(null);

        mockMvc.perform(get("/session/check")
                        .header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_authenticated_returns200AndInvalidatesSession() throws Exception {
        mockMvc.perform(post("/session/logout")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        verify(authService).logoutByUserId(USER_ID);
    }
}
