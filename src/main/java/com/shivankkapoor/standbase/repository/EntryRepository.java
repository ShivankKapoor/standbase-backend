package com.shivankkapoor.standbase.repository;

import com.shivankkapoor.standbase.model.Entry;
import com.shivankkapoor.standbase.model.EntryLength;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
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

    @Query(value = "SELECT entry_date AS entryDate, word_count AS wordCount " +
            "FROM entries WHERE user_id = :userId AND entry_date BETWEEN :from AND :to " +
            "ORDER BY entry_date", nativeQuery = true)
    List<EntryLength> findWordCountsByUserIdAndEntryDateBetween(
            @Param("userId") UUID userId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
