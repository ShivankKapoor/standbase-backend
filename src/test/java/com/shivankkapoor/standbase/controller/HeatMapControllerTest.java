package com.shivankkapoor.standbase.controller;

import com.shivankkapoor.standbase.config.SecurityConfig;
import com.shivankkapoor.standbase.dto.response.HeatMapResponseDTO;
import com.shivankkapoor.standbase.model.EntryLength;
import com.shivankkapoor.standbase.service.HeatMapService;
import com.shivankkapoor.standbase.service.IpService;
import com.shivankkapoor.standbase.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HeatMapController.class)
@Import(SecurityConfig.class)
class HeatMapControllerTest {

    @Autowired WebApplicationContext wac;
    MockMvc mockMvc;

    @MockitoBean HeatMapService heatMapService;
    @MockitoBean IpService ipService;
    @MockitoBean AuthService authService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String TOKEN = "test-session-token";
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 3);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();
        when(ipService.getClientIp(any())).thenReturn("1.2.3.4");
        when(authService.getSessionUserID(eq(TOKEN), eq("1.2.3.4"), any())).thenReturn(USER_ID);
    }

    private HeatMapResponseDTO buildResponse() {
        HeatMapResponseDTO dto = new HeatMapResponseDTO();
        dto.setAverage(150);
        dto.setEntries(List.of(new EntryLength(TODAY, 150)));
        return dto;
    }

    @Test
    void getHeatMap_authenticated_returnsHeatMapData() throws Exception {
        when(heatMapService.getHeatMap(USER_ID, TODAY)).thenReturn(buildResponse());

        mockMvc.perform(get("/heatmap?today=2026-09-03")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.average").value(150))
                .andExpect(jsonPath("$.entries[0].wordCount").value(150))
                .andExpect(jsonPath("$.entries[0].entryDate").value("2026-09-03"));
    }

    @Test
    void getHeatMap_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/heatmap?today=2026-09-03"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getHeatMap_missingTodayParam_returns400() throws Exception {
        mockMvc.perform(get("/heatmap")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getHeatMap_malformedTodayParam_returns400() throws Exception {
        mockMvc.perform(get("/heatmap?today=not-a-date")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getHeatMap_noEntries_returnsZeroAverageAndEmptyEntries() throws Exception {
        HeatMapResponseDTO empty = new HeatMapResponseDTO();
        empty.setAverage(0);
        empty.setEntries(List.of());
        when(heatMapService.getHeatMap(USER_ID, TODAY)).thenReturn(empty);

        mockMvc.perform(get("/heatmap?today=2026-09-03")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.average").value(0))
                .andExpect(jsonPath("$.entries").isEmpty());
    }

    @Test
    void getHeatMap_passesAuthenticatedUserIdToService() throws Exception {
        when(heatMapService.getHeatMap(USER_ID, TODAY)).thenReturn(buildResponse());

        mockMvc.perform(get("/heatmap?today=2026-09-03")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk());

        verify(heatMapService).getHeatMap(USER_ID, TODAY);
    }

    @Test
    void getHeatMap_differentUser_scopedToTheirOwnUserId() throws Exception {
        UUID otherUser = UUID.randomUUID();
        when(authService.getSessionUserID(eq("other-token"), eq("1.2.3.4"), any())).thenReturn(otherUser);
        HeatMapResponseDTO otherUsersData = new HeatMapResponseDTO();
        otherUsersData.setAverage(42);
        otherUsersData.setEntries(List.of());
        when(heatMapService.getHeatMap(otherUser, TODAY)).thenReturn(otherUsersData);

        mockMvc.perform(get("/heatmap?today=2026-09-03")
                        .header("Authorization", "Bearer other-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.average").value(42));

        verify(heatMapService).getHeatMap(otherUser, TODAY);
    }
}
