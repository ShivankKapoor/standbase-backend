package com.shivankkapoor.standbase.service;

import com.shivankkapoor.standbase.repository.EntryRepository;
import org.springframework.stereotype.Service;

@Service
public class EntryService {
    private final EntryRepository entryRepository;

    public EntryService(EntryRepository entryRepository) {
        this.entryRepository = entryRepository;
    }


}
