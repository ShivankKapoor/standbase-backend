package com.shivankkapoor.standbase.service;

import com.shivankkapoor.standbase.dto.request.CreateEntryRequestDTO;
import com.shivankkapoor.standbase.dto.response.EntryOverviewResponseDTO;
import com.shivankkapoor.standbase.model.DayType;
import com.shivankkapoor.standbase.model.Entry;
import com.shivankkapoor.standbase.repository.EntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
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

    @Transactional
    public boolean deleteEntry(UUID userId, LocalDate date) {
        if (entryRepository.findByUserIdAndEntryDate(userId, date).isEmpty()) {
            return false;
        }
        entryRepository.deleteByUserIdAndEntryDate(userId, date);
        return true;
    }

    public Entry createEntry(UUID userId, CreateEntryRequestDTO dto) {
        Entry entry = entryRepository.findByUserIdAndEntryDate(userId, dto.getDate())
                .orElseGet(() -> {
                    Entry e = new Entry();
                    e.setUserId(userId);
                    e.setEntryDate(dto.getDate());
                    e.setCreatedAt(OffsetDateTime.now());
                    return e;
                });
        entry.setDayType(dto.getDayType() != null ? dto.getDayType().name() : null);
        entry.setContent(dto.getContent());
        entry.setUpdatedAt(OffsetDateTime.now());
        return entryRepository.save(entry);
    }

    public List<EntryOverviewResponseDTO> getEntries(UUID userId, LocalDate from, LocalDate to) {
        return entryRepository.findByUserIdAndEntryDateBetweenOrderByEntryDateDesc(userId, from, to)
                .stream()
                .map(e -> {
                    EntryOverviewResponseDTO dto = new EntryOverviewResponseDTO();
                    dto.setDate(e.getEntryDate());
                    dto.setDayType(e.getDayType() != null ? DayType.valueOf(e.getDayType()) : null);
                    return dto;
                })
                .toList();
    }

}
