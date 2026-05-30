package com.shivankkapoor.standbase.repository;

import com.shivankkapoor.standbase.model.AuthEvent;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface AuthEventRepository extends CrudRepository<AuthEvent, Long> {
    @Query("SELECT a FROM AuthEvent a WHERE a.ipAddress = :ip AND a.country IS NOT NULL ORDER BY a.createdAt DESC LIMIT 1")
    Optional<AuthEvent> findMostRecentWithLocationByIp(String ip);
}
