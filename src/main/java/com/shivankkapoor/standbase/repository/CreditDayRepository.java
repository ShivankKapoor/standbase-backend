package com.shivankkapoor.standbase.repository;

import com.shivankkapoor.standbase.model.CreditDay;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreditDayRepository extends CrudRepository<CreditDay, UUID> {
    Optional<CreditDay> findByUserIdAndEntryDate(UUID userId, LocalDate entryDate);
    List<CreditDay> findByUserIdAndEntryDateBetweenOrderByEntryDateAsc(UUID userId, LocalDate from, LocalDate to);
}
