package com.shivankkapoor.standbase.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shivankkapoor.standbase.config.SecurityConfig;
import com.shivankkapoor.standbase.dto.request.CreateEntryRequestDTO;
import com.shivankkapoor.standbase.dto.response.EntryOverviewResponseDTO;
import com.shivankkapoor.standbase.model.DayType;
import com.shivankkapoor.standbase.model.Entry;
import com.shivankkapoor.standbase.service.EntryService;
import com.shivankkapoor.standbase.service.IpService;
import com.shivankkapoor.standbase.service.SessionService;
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

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EntryController.class)
@Import(SecurityConfig.class)
class EntryControllerTest {

    @Autowired WebApplicationContext wac;
    MockMvc mockMvc;

    @MockitoBean EntryService entryService;
    @MockitoBean IpService ipService;
    @MockitoBean SessionService sessionService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String TOKEN = "test-session-token";
    private static final LocalDate DATE = LocalDate.of(2026, 5, 26);

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();
        when(ipService.getClientIp(any())).thenReturn("1.2.3.4");
        when(sessionService.getSessionUserID(eq(TOKEN), eq("1.2.3.4"))).thenReturn(USER_ID);
    }

    private Entry buildEntry() {
        Entry entry = new Entry();
        entry.setId(UUID.randomUUID());
        entry.setUserId(USER_ID);
        entry.setEntryDate(DATE);
        entry.setDayType("SUPPORT");
        entry.setContent("Did some work.");
        entry.setCreatedAt(OffsetDateTime.now());
        entry.setUpdatedAt(OffsetDateTime.now());
        return entry;
    }

    @Test
    void getEntries_authenticated_returnsEntryList() throws Exception {
        EntryOverviewResponseDTO overview = new EntryOverviewResponseDTO();
        overview.setDate(DATE);
        overview.setDayType(DayType.SUPPORT);
        when(entryService.getEntries(eq(USER_ID), any(), any())).thenReturn(List.of(overview));

        mockMvc.perform(get("/entry?year=2026&month=5")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.entries[0].dayType").value("SUPPORT"));
    }

    @Test
    void getEntries_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/entry?year=2026&month=5"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getEntry_found_returnsEntry() throws Exception {
        when(entryService.findByUserIdAndEntryDate(USER_ID, DATE)).thenReturn(Optional.of(buildEntry()));

        mockMvc.perform(get("/entry/2026-05-26")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Did some work."))
                .andExpect(jsonPath("$.dayType").value("SUPPORT"));
    }

    @Test
    void getEntry_notFound_returns404() throws Exception {
        when(entryService.findByUserIdAndEntryDate(USER_ID, DATE)).thenReturn(Optional.empty());

        mockMvc.perform(get("/entry/2026-05-26")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteEntry_found_returns204() throws Exception {
        when(entryService.deleteEntry(USER_ID, DATE)).thenReturn(true);

        mockMvc.perform(delete("/entry/2026-05-26")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteEntry_notFound_returns404() throws Exception {
        when(entryService.deleteEntry(USER_ID, DATE)).thenReturn(false);

        mockMvc.perform(delete("/entry/2026-05-26")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isNotFound());
    }

    @Test
    void createEntry_valid_returns200() throws Exception {
        Entry saved = buildEntry();
        when(entryService.createEntry(eq(USER_ID), any())).thenReturn(saved);

        CreateEntryRequestDTO body = new CreateEntryRequestDTO();
        body.setDate(DATE);
        body.setDayType(DayType.SUPPORT);
        body.setContent("Did some work.");

        mockMvc.perform(post("/entry")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.content").value("Did some work."))
                .andExpect(jsonPath("$.dayType").value("SUPPORT"));
    }

    @Test
    void createEntry_missingDate_returns400() throws Exception {
        mockMvc.perform(post("/entry")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEntry_differentUser_cannotAccessOtherUsersEntry() throws Exception {
        // Entry belongs to USER_ID but a different user is authenticated — service returns empty
        UUID otherUser = UUID.randomUUID();
        when(sessionService.getSessionUserID(eq("other-token"), eq("1.2.3.4"))).thenReturn(otherUser);
        when(entryService.findByUserIdAndEntryDate(otherUser, DATE)).thenReturn(Optional.empty());

        mockMvc.perform(get("/entry/2026-05-26")
                        .header("Authorization", "Bearer other-token"))
                .andExpect(status().isNotFound());
    }
}
