package com.shivankkapoor.standbase.service;


import com.shivankkapoor.standbase.dto.response.HeatMapResponseDTO;
import com.shivankkapoor.standbase.model.EntryLength;
import com.shivankkapoor.standbase.repository.EntryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class HeatMapService {
    private final EntryRepository entryRepository;

    public HeatMapService(EntryRepository entryRepository) {
        this.entryRepository = entryRepository;
    }

    public HeatMapResponseDTO getHeatMap(UUID userId, LocalDate entryDate){
        HeatMapResponseDTO data = new HeatMapResponseDTO();
        List<EntryLength> entries = getHeatMapDataFromDB(userId,entryDate);
        data.setEntries(entries);
        int noOfEntries = entries.size();
        int totalWords = 0;
        for (EntryLength entry : entries) {
            totalWords += entry.wordCount();
        }
        data.setAverage(noOfEntries == 0 ? 0 : totalWords / noOfEntries);
        return data;
    }

    private List<EntryLength> getHeatMapDataFromDB(UUID userId, LocalDate entryDate) {
        LocalDate from = entryDate.minusYears(1);
        return entryRepository.findWordCountsByUserIdAndEntryDateBetween(userId, from, entryDate);
    }


}
