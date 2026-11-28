package com.shivankkapoor.standbase.controller;

import com.shivankkapoor.standbase.config.SecurityConfig;
import com.shivankkapoor.standbase.dto.response.CreditMonthResponseDTO;
import com.shivankkapoor.standbase.service.AuthService;
import com.shivankkapoor.standbase.service.CreditService;
import com.shivankkapoor.standbase.service.IpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CreditController.class)
@Import(SecurityConfig.class)
class CreditControllerTest {

    @Autowired WebApplicationContext wac;
    MockMvc mockMvc;

    @MockitoBean CreditService creditService;
    @MockitoBean IpService ipService;
    @MockitoBean AuthService authService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String TOKEN = "test-session-token";
    private static final LocalDate DATE = LocalDate.of(2026, 11, 3);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();
        when(ipService.getClientIp(any())).thenReturn("1.2.3.4");
        when(authService.getSessionUserID(eq(TOKEN), eq("1.2.3.4"), any())).thenReturn(USER_ID);
        when(creditService.getMonth(any(), any()))
                .thenReturn(new CreditMonthResponseDTO(BigDecimal.ZERO, 0, null, List.of()));
    }

    @Test
    void noToken_returns401() throws Exception {
        mockMvc.perform(get("/credits?year=2026&month=11")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/credits/" + DATE).contentType(MediaType.APPLICATION_JSON).content("{\"monthTotal\":1}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(creditService);
    }

    @Test
    void badToken_returns401() throws Exception {
        mockMvc.perform(get("/credits?year=2026&month=11").header("Authorization", "Bearer nope"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(creditService);
    }

    @Test
    void userIdComesFromSession_notFromRequest() throws Exception {
        UUID other = UUID.randomUUID();
        mockMvc.perform(get("/credits?year=2026&month=11&userId=" + other)
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk());
        mockMvc.perform(put("/credits/" + DATE + "?userId=" + other)
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monthTotal\":1,\"userId\":\"" + other + "\"}"))
                .andExpect(status().isOk());
        verify(creditService).saveDay(eq(USER_ID), eq(DATE), any());
        verify(creditService, times(2)).getMonth(USER_ID, YearMonth.from(DATE));
    }

    @Test
    void invalidTotals_return400() throws Exception {
        for (String total : new String[]{"-1", "1.234", "1e20", "\"abc\""}) {
            mockMvc.perform(put("/credits/" + DATE)
                            .header("Authorization", "Bearer " + TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"monthTotal\":" + total + "}"))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(creditService);
    }
}
