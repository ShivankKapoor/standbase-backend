package com.shivankkapoor.standbase.controller;

import com.shivankkapoor.standbase.dto.request.CreateEntryRequestDTO;
import com.shivankkapoor.standbase.dto.request.ResponseDTO;
import com.shivankkapoor.standbase.model.Entry;
import com.shivankkapoor.standbase.service.EntryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/entry")
public class EntryController {

    private final EntryService entryService;

    public EntryController(EntryService entryService) {
        this.entryService = entryService;
    }

    @GetMapping("/{date}")
    public ResponseEntity<Entry> getEntry(@PathVariable LocalDate date, Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        Optional<Entry> entry = entryService.findByUserIdAndEntryDate(userId, date);
        return entry.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("")
    public ResponseEntity<?> createEntry(@Valid @RequestBody CreateEntryRequestDTO createEntryRequestDTO){
        ResponseDTO response = new ResponseDTO();
        response.setStatus("ok");
        return ResponseEntity.ok(response);
    }

}
