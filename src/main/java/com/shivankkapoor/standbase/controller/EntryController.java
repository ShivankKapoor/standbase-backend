package com.shivankkapoor.standbase.controller;

import com.shivankkapoor.standbase.dto.request.CreateEntryRequestDTO;
import com.shivankkapoor.standbase.dto.response.CreateEntryResponseDTO;
import com.shivankkapoor.standbase.dto.response.EntryListResponseDTO;
import com.shivankkapoor.standbase.dto.response.EntryOverviewResponseDTO;
import com.shivankkapoor.standbase.model.DayType;
import com.shivankkapoor.standbase.model.Entry;
import com.shivankkapoor.standbase.service.EntryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/entry")
public class EntryController {

    private static final Logger log = LoggerFactory.getLogger(EntryController.class);
    private final EntryService entryService;

    public EntryController(EntryService entryService) {
        this.entryService = entryService;
    }

    @GetMapping("")
    public ResponseEntity<EntryListResponseDTO> getEntries(
            @RequestParam int year,
            @RequestParam int month,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        YearMonth ym = YearMonth.of(year, month);
        EntryListResponseDTO response = new EntryListResponseDTO();
        response.setStatus("ok");
        response.setEntries(entryService.getEntries(userId, ym.atDay(1), ym.atEndOfMonth()));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{date}")
    public ResponseEntity<Entry> getEntry(@PathVariable LocalDate date, Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        Optional<Entry> entry = entryService.findByUserIdAndEntryDate(userId, date);
        return entry.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{date}")
    public ResponseEntity<Void> deleteEntry(@PathVariable LocalDate date, Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        boolean deleted = entryService.deleteEntry(userId, date);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("")
    public ResponseEntity<CreateEntryResponseDTO> createEntry(@Valid @RequestBody CreateEntryRequestDTO createEntryRequestDTO, Authentication authentication){
        UUID userId = (UUID) authentication.getPrincipal();
        Entry entry = entryService.createEntry(userId, createEntryRequestDTO);
        CreateEntryResponseDTO response = new CreateEntryResponseDTO();
        response.setStatus("ok");
        response.setDate(entry.getEntryDate());
        response.setDayType(entry.getDayType() != null ? DayType.valueOf(entry.getDayType()) : null);
        response.setContent(entry.getContent());
        return ResponseEntity.ok(response);
    }

}
