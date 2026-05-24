package com.shivankkapoor.standbase.repository;

import com.shivankkapoor.standbase.model.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends CrudRepository<User, UUID> {
    Optional<User> findById(UUID uuid);
    Optional<User> findByUsername(String username);
}
