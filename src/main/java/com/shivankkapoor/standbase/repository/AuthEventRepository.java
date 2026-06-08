package com.shivankkapoor.standbase.repository;

import com.shivankkapoor.standbase.model.AuthEvent;
import org.springframework.data.repository.CrudRepository;

public interface AuthEventRepository extends CrudRepository<AuthEvent, Long> {
}
