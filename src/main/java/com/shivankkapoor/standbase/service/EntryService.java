package com.shivankkapoor.standbase.service;

import com.shivankkapoor.standbase.model.Entry;
import com.shivankkapoor.standbase.repository.EntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
public class EntryService {
    private final EntryRepository entryRepository;
    private static final Logger log = LoggerFactory.getLogger(EntryService.class);

    public EntryService(EntryRepository entryRepository) {
        this.entryRepository = entryRepository;
    }

    public Optional<Entry> findByUserIdAndEntryDate(UUID userId, LocalDate date) {
        try {
            return entryRepository.findByUserIdAndEntryDate(userId, date);
        } catch (Exception e) {
            log.error("Error fetching entry for userId={} date={}", userId, date, e);
            return Optional.empty();
        }
    }

}
