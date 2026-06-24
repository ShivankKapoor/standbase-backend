package com.shivankkapoor.standbase.repository;

import com.shivankkapoor.standbase.model.Todo;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TodoRepository extends CrudRepository<Todo, UUID> {
    List<Todo> findByUserIdAndEntryDateOrderByPositionAscCreatedAtAsc(UUID userId, LocalDate entryDate);
    List<Todo> findByUserIdAndEntryDateBetween(UUID userId, LocalDate from, LocalDate to);
    void deleteByIdAndUserId(UUID id, UUID userId);
}
