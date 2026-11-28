package com.shivankkapoor.standbase.service;

import com.shivankkapoor.standbase.dto.request.SaveCreditDayRequestDTO;
import com.shivankkapoor.standbase.model.CreditDay;
import com.shivankkapoor.standbase.model.Entry;
import com.shivankkapoor.standbase.repository.CreditDayRepository;
import com.shivankkapoor.standbase.repository.EntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditServiceSaveTest {

    @Mock CreditDayRepository creditDayRepository;
    @Mock EntryRepository entryRepository;
    @InjectMocks CreditService creditService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final YearMonth MONTH = YearMonth.of(2026, 11);

    private static CreditDay day(int day, String total, boolean off) {
        return new CreditDay(UUID.randomUUID(), USER_ID, MONTH.atDay(day),
                total == null ? null : new BigDecimal(total), off);
    }

    private static SaveCreditDayRequestDTO request(String total, boolean off) {
        SaveCreditDayRequestDTO dto = new SaveCreditDayRequestDTO();
        dto.setMonthTotal(total == null ? null : new BigDecimal(total));
        dto.setDidNotWork(off);
        return dto;
    }

    private CreditDay saved() {
        ArgumentCaptor<CreditDay> captor = ArgumentCaptor.forClass(CreditDay.class);
        verify(creditDayRepository).save(captor.capture());
        return captor.getValue();
    }

    private void givenDays(CreditDay... days) {
        when(creditDayRepository.findByUserIdAndEntryDateBetweenOrderByEntryDateAsc(eq(USER_ID), any(), any()))
                .thenReturn(List.of(days));
    }

    @Test
    void newTotalBetweenItsNeighboursIsSaved() {
        // Day offs and the date's own old total don't take part in the ordering check.
        givenDays(day(2, "10", false), day(3, null, true), day(4, "99", false), day(5, "100", false));
        LocalDate date = MONTH.atDay(4);

        creditService.saveDay(USER_ID, date, request("50", false));

        assertThat(saved().getUserId()).isEqualTo(USER_ID);
        assertThat(saved().getEntryDate()).isEqualTo(date);
        assertThat(saved().getMonthTotal()).isEqualByComparingTo("50");
    }

    @Test
    void dayOffUpdatesTheExistingRow() {
        CreditDay existing = day(3, "10", false);
        when(creditDayRepository.findByUserIdAndEntryDate(USER_ID, MONTH.atDay(3))).thenReturn(Optional.of(existing));

        creditService.saveDay(USER_ID, MONTH.atDay(3), request(null, true));

        assertThat(saved()).isSameAs(existing);
        assertThat(existing.getMonthTotal()).isNull();
        assertThat(existing.isDidNotWork()).isTrue();
    }

    @Test
    void clearingADayWithNoRowDoesNothing() {
        creditService.saveDay(USER_ID, MONTH.atDay(3), request(null, false));

        verify(creditDayRepository, never()).save(any());
        verify(creditDayRepository, never()).delete(any());
    }

    @Test
    void ptoAndWeekendRowsWithoutUsageAreNotCounted() {
        // The 1st and 7th are weekend rows with no total; PTO on the 2nd has no row,
        // PTO on the 3rd has no total and PTO on the 4th used nothing.
        givenDays(day(1, null, false), day(3, null, false), day(4, "0", false), day(5, "30", false),
                day(7, null, false), day(9, "40", false));
        List<Entry> pto = List.of(2, 3, 4).stream().map(d -> {
            Entry entry = new Entry();
            entry.setEntryDate(MONTH.atDay(d));
            entry.setDayType("PTO");
            return entry;
        }).toList();
        when(entryRepository.findByUserIdAndEntryDateBetweenOrderByEntryDateDesc(eq(USER_ID), any(), any()))
                .thenReturn(pto);

        // Only the 5th, 6th and 9th count.
        assertThat(creditService.getMonth(USER_ID, MONTH).getWorkedDays()).isEqualTo(3);
    }
}
