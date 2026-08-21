package com.shivankkapoor.standbase.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class TotpReplayService {

    private final Cache<String, Boolean> usedCodes = Caffeine.newBuilder()
            .expireAfterWrite(90, TimeUnit.SECONDS)
            .build();

    /**
     * @param userId User id of the user trying to claim a code
     * @param code The code the user is trying to claim
     * @return true if the code was not already claimed (and is now claimed); false if it was already claimed
     */
    public boolean claim(UUID userId, String code) {
        String key = userId + ":" + code;
        return usedCodes.asMap().putIfAbsent(key, Boolean.TRUE) == null;
    }
}
