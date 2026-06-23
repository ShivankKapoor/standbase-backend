package com.shivankkapoor.standbase.repository;

import com.shivankkapoor.standbase.model.Session;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionRepository extends CrudRepository<Session, String> {
    Optional<Session> findByUserId(UUID userId);
    void deleteByUserId(UUID userId);
}
