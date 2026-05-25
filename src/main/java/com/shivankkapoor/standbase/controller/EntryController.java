package com.shivankkapoor.standbase.controller;

import com.shivankkapoor.standbase.model.Entry;
import com.shivankkapoor.standbase.repository.EntryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/entry")
public class EntryController {

    private final EntryRepository entryRepository;

    public EntryController(EntryRepository entryRepository) {
        this.entryRepository = entryRepository;
    }

    @GetMapping("/{date}")
    public ResponseEntity<Entry> getEntry(@PathVariable LocalDate date, Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        Optional<Entry> entry = entryRepository.findByUserIdAndEntryDate(userId, date);
        return entry.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

}
