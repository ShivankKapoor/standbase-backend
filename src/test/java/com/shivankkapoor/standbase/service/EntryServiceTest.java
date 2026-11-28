package com.shivankkapoor.standbase.service;

import com.shivankkapoor.standbase.dto.request.CreateEntryRequestDTO;
import com.shivankkapoor.standbase.dto.response.EntryOverviewResponseDTO;
import com.shivankkapoor.standbase.model.DayType;
import com.shivankkapoor.standbase.model.Entry;
import com.shivankkapoor.standbase.repository.EntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntryServiceTest {

    @Mock EntryRepository entryRepository;
    @InjectMocks EntryService entryService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final LocalDate DATE = LocalDate.of(2026, 11, 3);

    private static CreateEntryRequestDTO request(DayType dayType, String content) {
        CreateEntryRequestDTO dto = new CreateEntryRequestDTO();
        dto.setDate(DATE);
        dto.setDayType(dayType);
        dto.setContent(content);
        return dto;
    }

    @Test
    void find_returnsEntry_orEmptyWhenTheLookupFails() {
        Entry entry = new Entry();
        when(entryRepository.findByUserIdAndEntryDate(USER_ID, DATE))
                .thenReturn(Optional.of(entry))
                .thenThrow(new RuntimeException("db down"));

        assertThat(entryService.findByUserIdAndEntryDate(USER_ID, DATE)).containsSame(entry);
        assertThat(entryService.findByUserIdAndEntryDate(USER_ID, DATE)).isEmpty();
    }

    @Test
    void delete_reportsWhetherARowWasRemoved() {
        when(entryRepository.deleteByUserIdAndEntryDate(USER_ID, DATE)).thenReturn(1, 0);

        assertThat(entryService.deleteEntry(USER_ID, DATE)).isTrue();
        assertThat(entryService.deleteEntry(USER_ID, DATE)).isFalse();
    }

    @Test
    void create_makesANewEntryForTheUser() {
        when(entryRepository.findByUserIdAndEntryDate(USER_ID, DATE)).thenReturn(Optional.empty());
        when(entryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Entry saved = entryService.createEntry(USER_ID, request(DayType.PTO, "notes"));

        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getEntryDate()).isEqualTo(DATE);
        assertThat(saved.getDayType()).isEqualTo("PTO");
        assertThat(saved.getContent()).isEqualTo("notes");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void create_updatesAnExistingEntry() {
        Entry existing = new Entry();
        existing.setDayType("PTO");
        when(entryRepository.findByUserIdAndEntryDate(USER_ID, DATE)).thenReturn(Optional.of(existing));
        when(entryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Entry saved = entryService.createEntry(USER_ID, request(null, "changed"));

        assertThat(saved).isSameAs(existing);
        assertThat(saved.getDayType()).isNull();
        assertThat(saved.getContent()).isEqualTo("changed");
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void getEntries_mapsDateAndDayType() {
        Entry plain = new Entry();
        plain.setEntryDate(DATE);
        Entry support = new Entry();
        support.setEntryDate(DATE.plusDays(1));
        support.setDayType("SUPPORT");
        when(entryRepository.findByUserIdAndEntryDateBetweenOrderByEntryDateDesc(USER_ID, DATE, DATE.plusDays(1)))
                .thenReturn(List.of(support, plain));

        assertThat(entryService.getEntries(USER_ID, DATE, DATE.plusDays(1)))
                .extracting(EntryOverviewResponseDTO::getDayType)
                .containsExactly(DayType.SUPPORT, null);
    }
}
