package com.shivankkapoor.standbase.service;

import com.shivankkapoor.standbase.dto.response.HeatMapResponseDTO;
import com.shivankkapoor.standbase.model.EntryLength;
import com.shivankkapoor.standbase.repository.EntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeatMapServiceTest {

    @Mock
    EntryRepository entryRepository;

    @InjectMocks
    HeatMapService heatMapService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 3);

    @Test
    void getHeatMap_noEntries_returnsZeroAverageAndEmptyList() {
        when(entryRepository.findWordCountsByUserIdAndEntryDateBetween(eq(USER_ID), any(), any()))
                .thenReturn(List.of());

        HeatMapResponseDTO result = heatMapService.getHeatMap(USER_ID, TODAY);

        assertThat(result.getAverage()).isZero();
        assertThat(result.getEntries()).isEmpty();
    }

    @Test
    void getHeatMap_singleEntry_averageEqualsThatEntrysWordCount() {
        List<EntryLength> entries = List.of(new EntryLength(TODAY, 250));
        when(entryRepository.findWordCountsByUserIdAndEntryDateBetween(eq(USER_ID), any(), any()))
                .thenReturn(entries);

        HeatMapResponseDTO result = heatMapService.getHeatMap(USER_ID, TODAY);

        assertThat(result.getAverage()).isEqualTo(250);
        assertThat(result.getEntries()).containsExactly(new EntryLength(TODAY, 250));
    }

    @Test
    void getHeatMap_multipleEntries_computesIntegerAverage() {
        List<EntryLength> entries = List.of(
                new EntryLength(TODAY.minusDays(2), 100),
                new EntryLength(TODAY.minusDays(1), 200),
                new EntryLength(TODAY, 210)
        );
        when(entryRepository.findWordCountsByUserIdAndEntryDateBetween(eq(USER_ID), any(), any()))
                .thenReturn(entries);

        HeatMapResponseDTO result = heatMapService.getHeatMap(USER_ID, TODAY);

        // total = 510, count = 3 -> 170.0 exactly, but division is integer so no rounding surprises here
        assertThat(result.getAverage()).isEqualTo(170);
        assertThat(result.getEntries()).hasSize(3);
    }

    @Test
    void getHeatMap_averageTruncatesTowardZero_doesNotRound() {
        List<EntryLength> entries = List.of(
                new EntryLength(TODAY.minusDays(1), 10),
                new EntryLength(TODAY, 1)
        );
        when(entryRepository.findWordCountsByUserIdAndEntryDateBetween(eq(USER_ID), any(), any()))
                .thenReturn(entries);

        HeatMapResponseDTO result = heatMapService.getHeatMap(USER_ID, TODAY);

        // total = 11, count = 2 -> 5.5, integer division truncates to 5, not rounds to 6
        assertThat(result.getAverage()).isEqualTo(5);
    }

    @Test
    void getHeatMap_queriesExactlyOneYearBack_fromToday() {
        when(entryRepository.findWordCountsByUserIdAndEntryDateBetween(eq(USER_ID), any(), any()))
                .thenReturn(List.of());

        heatMapService.getHeatMap(USER_ID, TODAY);

        ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> toCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(entryRepository).findWordCountsByUserIdAndEntryDateBetween(eq(USER_ID), fromCaptor.capture(), toCaptor.capture());

        assertThat(fromCaptor.getValue()).isEqualTo(TODAY.minusYears(1));
        assertThat(toCaptor.getValue()).isEqualTo(TODAY);
    }

    @Test
    void getHeatMap_passesCorrectUserId() {
        when(entryRepository.findWordCountsByUserIdAndEntryDateBetween(any(), any(), any()))
                .thenReturn(List.of());

        heatMapService.getHeatMap(USER_ID, TODAY);

        verify(entryRepository).findWordCountsByUserIdAndEntryDateBetween(eq(USER_ID), any(), any());
    }
}
