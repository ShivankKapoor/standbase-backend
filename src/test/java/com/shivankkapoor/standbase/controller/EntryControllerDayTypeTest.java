package com.shivankkapoor.standbase.controller;

import com.shivankkapoor.standbase.dto.request.CreateEntryRequestDTO;
import com.shivankkapoor.standbase.model.DayType;
import com.shivankkapoor.standbase.model.Entry;
import com.shivankkapoor.standbase.service.EntryService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EntryControllerDayTypeTest {

    @Test
    void dayTypeIsPassedThroughWhetherOrNotItIsSet() {
        EntryService service = mock(EntryService.class);
        EntryController controller = new EntryController(service);
        UUID userId = UUID.randomUUID();
        Authentication auth = new UsernamePasswordAuthenticationToken(userId, null);
        LocalDate date = LocalDate.of(2026, 11, 3);

        for (DayType type : new DayType[]{null, DayType.PTO}) {
            Entry entry = new Entry();
            entry.setEntryDate(date);
            entry.setDayType(type == null ? null : type.name());
            when(service.findByUserIdAndEntryDate(userId, date)).thenReturn(Optional.of(entry));
            when(service.createEntry(any(), any())).thenReturn(entry);

            assertThat(controller.getEntry(date, auth).getBody().getDayType()).isEqualTo(type);
            assertThat(controller.createEntry(new CreateEntryRequestDTO(), auth).getBody().getDayType()).isEqualTo(type);
        }
    }
}
