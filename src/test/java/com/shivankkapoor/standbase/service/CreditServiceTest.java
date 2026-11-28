package com.shivankkapoor.standbase.service;

import com.shivankkapoor.standbase.dto.request.SaveCreditDayRequestDTO;
import com.shivankkapoor.standbase.dto.response.CreditMonthResponseDTO;
import com.shivankkapoor.standbase.model.CreditDay;
import com.shivankkapoor.standbase.model.Entry;
import com.shivankkapoor.standbase.repository.CreditDayRepository;
import com.shivankkapoor.standbase.repository.EntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditServiceTest {

    @Mock CreditDayRepository creditDayRepository;
    @Mock EntryRepository entryRepository;
    @InjectMocks CreditService creditService;

    private static final UUID USER_ID = UUID.randomUUID();
    // November 2026 starts on a Sunday: the 2nd is a Monday, the 7th a Saturday.
    private static final YearMonth MONTH = YearMonth.of(2026, 11);

    private static LocalDate nov(int day) {
        return MONTH.atDay(day);
    }

    private static CreditDay total(int day, String total) {
        return new CreditDay(UUID.randomUUID(), USER_ID, nov(day), new BigDecimal(total), false);
    }

    private static CreditDay off(int day) {
        return new CreditDay(UUID.randomUUID(), USER_ID, nov(day), null, true);
    }

    private static Entry pto(int day) {
        Entry entry = new Entry();
        entry.setEntryDate(nov(day));
        entry.setDayType("PTO");
        return entry;
    }

    private void givenDays(CreditDay... days) {
        when(creditDayRepository.findByUserIdAndEntryDateBetweenOrderByEntryDateAsc(eq(USER_ID), any(), any()))
                .thenReturn(Arrays.asList(days));
    }

    private void givenEntries(Entry... entries) {
        when(entryRepository.findByUserIdAndEntryDateBetweenOrderByEntryDateDesc(eq(USER_ID), any(), any()))
                .thenReturn(List.of(entries));
    }

    private CreditMonthResponseDTO month() {
        return creditService.getMonth(USER_ID, MONTH);
    }

    private static SaveCreditDayRequestDTO request(String total, boolean didNotWork) {
        SaveCreditDayRequestDTO dto = new SaveCreditDayRequestDTO();
        dto.setMonthTotal(total == null ? null : new BigDecimal(total));
        dto.setDidNotWork(didNotWork);
        return dto;
    }

    @Test
    void emptyMonth_hasNoAverage() {
        givenDays();
        CreditMonthResponseDTO result = month();
        assertThat(result.getUsed()).isZero();
        assertThat(result.getWorkedDays()).isZero();
        assertThat(result.getAverage()).isNull();
    }

    @Test
    void dailyUsage_isDifferenceFromPreviousTotal() {
        givenDays(total(2, "40"), total(3, "90.5"));
        givenEntries();
        assertThat(month().getDays()).extracting(d -> d.getUsed().toPlainString())
                .containsExactly("40", "50.5");
    }

    @Test
    void workedWeekend_countsAndSkippedWeekendDoesNot() {
        givenDays(total(6, "200"), total(7, "230"), total(9, "270"));
        givenEntries();
        CreditMonthResponseDTO result = month();
        // Weekdays 2-6 and 9, plus Saturday the 7th. Sunday the 1st and 8th are left out.
        assertThat(result.getWorkedDays()).isEqualTo(7);
        assertThat(result.getAverage()).isEqualByComparingTo("38.57");
    }

    @Test
    void daysOffAndPto_areLeftOut() {
        givenDays(off(4), total(6, "150"));
        givenEntries(pto(5));
        CreditMonthResponseDTO result = month();
        assertThat(result.getWorkedDays()).isEqualTo(3);
        assertThat(result.getAverage()).isEqualByComparingTo("50");
    }

    @Test
    void ptoWithUsage_counts() {
        givenDays(total(2, "10"), total(3, "20"));
        givenEntries(pto(3));
        assertThat(month().getWorkedDays()).isEqualTo(2);
    }

    @Test
    void save_lowerThanEarlierTotal_isRejected() {
        givenDays(total(2, "100"));
        assertThatThrownBy(() -> creditService.saveDay(USER_ID, nov(3), request("90", false)))
                .isInstanceOf(InvalidCreditDayException.class)
                .hasMessageContaining("lower than 100");
        verify(creditDayRepository, never()).save(any());
    }

    @Test
    void save_higherThanLaterTotal_isRejected() {
        givenDays(total(4, "100"));
        assertThatThrownBy(() -> creditService.saveDay(USER_ID, nov(3), request("110", false)))
                .isInstanceOf(InvalidCreditDayException.class)
                .hasMessageContaining("higher than 100");
    }

    @Test
    void save_totalOnDayOff_isRejected() {
        assertThatThrownBy(() -> creditService.saveDay(USER_ID, nov(3), request("10", true)))
                .isInstanceOf(InvalidCreditDayException.class);
    }

    @Test
    void save_clearingBoth_deletesDay() {
        CreditDay existing = total(3, "10");
        when(creditDayRepository.findByUserIdAndEntryDate(USER_ID, nov(3))).thenReturn(Optional.of(existing));
        creditService.saveDay(USER_ID, nov(3), request(null, false));
        verify(creditDayRepository).delete(existing);
    }
}
