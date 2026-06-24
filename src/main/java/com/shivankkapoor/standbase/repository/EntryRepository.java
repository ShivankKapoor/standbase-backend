package com.shivankkapoor.standbase.repository;

import com.shivankkapoor.standbase.model.Entry;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EntryRepository extends CrudRepository<Entry, UUID> {
    Optional<Entry> findByUserIdAndEntryDate(UUID userId, LocalDate entryDate);
    List<Entry> findByUserIdAndEntryDateBetweenOrderByEntryDateDesc(UUID userId, LocalDate from, LocalDate to);
    int deleteByUserIdAndEntryDate(UUID userId, LocalDate entryDate);
}
