package com.shivankkapoor.standbase.controller;

import com.shivankkapoor.standbase.dto.request.SaveCreditDayRequestDTO;
import com.shivankkapoor.standbase.dto.response.CreditMonthResponseDTO;
import com.shivankkapoor.standbase.service.CreditService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

@RestController
@RequestMapping("/credits")
public class CreditController {

    private final CreditService creditService;

    public CreditController(CreditService creditService) {
        this.creditService = creditService;
    }

    @GetMapping("")
    public ResponseEntity<CreditMonthResponseDTO> getMonth(
            @RequestParam int year,
            @RequestParam int month,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(creditService.getMonth(userId, YearMonth.of(year, month)));
    }

    /** Returns the updated month, since one total changes the usage of the days after it. */
    @PutMapping("/{date}")
    public ResponseEntity<CreditMonthResponseDTO> saveDay(
            @PathVariable LocalDate date,
            @Valid @RequestBody SaveCreditDayRequestDTO dto,
            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        creditService.saveDay(userId, date, dto);
        return ResponseEntity.ok(creditService.getMonth(userId, YearMonth.from(date)));
    }
}
