package com.shivankkapoor.standbase.service;

import com.shivankkapoor.standbase.dto.request.SaveCreditDayRequestDTO;
import com.shivankkapoor.standbase.dto.response.CreditDayResponseDTO;
import com.shivankkapoor.standbase.dto.response.CreditMonthResponseDTO;
import com.shivankkapoor.standbase.model.CreditDay;
import com.shivankkapoor.standbase.model.DayType;
import com.shivankkapoor.standbase.model.Entry;
import com.shivankkapoor.standbase.repository.CreditDayRepository;
import com.shivankkapoor.standbase.repository.EntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CreditService {

    private final CreditDayRepository creditDayRepository;
    private final EntryRepository entryRepository;

    public CreditService(CreditDayRepository creditDayRepository, EntryRepository entryRepository) {
        this.creditDayRepository = creditDayRepository;
        this.entryRepository = entryRepository;
    }

    /**
     * Totals are month-to-date, so a day's usage is the difference from the previous total.
     * The average covers the 1st up to the last logged total: weekdays count unless marked
     * "didn't work" or PTO (PTO still counts if credits were used); weekends count only if
     * a total was logged.
     */
    public CreditMonthResponseDTO getMonth(UUID userId, YearMonth month) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();

        List<CreditDayResponseDTO> days = new ArrayList<>();
        Map<LocalDate, CreditDayResponseDTO> byDate = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        LocalDate lastLogged = null;
        for (CreditDay day : creditDayRepository.findByUserIdAndEntryDateBetweenOrderByEntryDateAsc(userId, from, to)) {
            BigDecimal used = null;
            if (day.getMonthTotal() != null) {
                used = day.getMonthTotal().subtract(total);
                total = day.getMonthTotal();
                lastLogged = day.getEntryDate();
            }
            CreditDayResponseDTO dto = new CreditDayResponseDTO(day.getEntryDate(), day.getMonthTotal(), day.isDidNotWork(), used);
            days.add(dto);
            byDate.put(dto.getDate(), dto);
        }

        int workedDays = 0;
        if (lastLogged != null) {
            Set<LocalDate> pto = entryRepository.findByUserIdAndEntryDateBetweenOrderByEntryDateDesc(userId, from, lastLogged)
                    .stream()
                    .filter(e -> DayType.PTO.name().equals(e.getDayType()))
                    .map(Entry::getEntryDate)
                    .collect(Collectors.toSet());
            for (LocalDate date = from; !date.isAfter(lastLogged); date = date.plusDays(1)) {
                if (isWorked(date, byDate.get(date), pto.contains(date))) workedDays++;
            }
        }

        BigDecimal average = workedDays == 0 ? null
                : total.divide(BigDecimal.valueOf(workedDays), 2, RoundingMode.HALF_UP);
        return new CreditMonthResponseDTO(total, workedDays, average, days);
    }

    /** Saves a day's total or "didn't work" mark; clearing both removes the day. */
    @Transactional
    public void saveDay(UUID userId, LocalDate date, SaveCreditDayRequestDTO dto) {
        BigDecimal total = dto.getMonthTotal();
        if (total != null && dto.isDidNotWork()) {
            throw new InvalidCreditDayException("A day you didn't work can't have a credit total");
        }
        Optional<CreditDay> existing = creditDayRepository.findByUserIdAndEntryDate(userId, date);
        if (total == null && !dto.isDidNotWork()) {
            existing.ifPresent(creditDayRepository::delete);
            return;
        }
        if (total != null) checkOrder(userId, date, total);

        CreditDay day = existing.orElseGet(() -> new CreditDay(null, userId, date, null, false));
        day.setMonthTotal(total);
        day.setDidNotWork(dto.isDidNotWork());
        creditDayRepository.save(day);
    }

    /** A month-to-date total can never be lower than an earlier one or higher than a later one. */
    private void checkOrder(UUID userId, LocalDate date, BigDecimal total) {
        YearMonth month = YearMonth.from(date);
        for (CreditDay other : creditDayRepository.findByUserIdAndEntryDateBetweenOrderByEntryDateAsc(userId, month.atDay(1), month.atEndOfMonth())) {
            if (other.getMonthTotal() == null || other.getEntryDate().equals(date)) continue;
            boolean earlier = other.getEntryDate().isBefore(date);
            int comparison = total.compareTo(other.getMonthTotal());
            if (earlier ? comparison < 0 : comparison > 0) {
                throw new InvalidCreditDayException("Total can't be " + (earlier ? "lower" : "higher") + " than "
                        + other.getMonthTotal().stripTrailingZeros().toPlainString() + " logged on " + other.getEntryDate());
            }
        }
    }

    private static boolean isWorked(LocalDate date, CreditDayResponseDTO day, boolean pto) {
        if (day != null && day.isDidNotWork()) return false;
        if (pto) return day != null && day.getUsed() != null && day.getUsed().signum() > 0;
        boolean weekend = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
        return !weekend || (day != null && day.getMonthTotal() != null);
    }
}
