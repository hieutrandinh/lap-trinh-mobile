package org.example.be.rest.repository;

import org.example.be.core.entity.PersonalEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalEntryJpaRepository extends JpaRepository<PersonalEntryEntity, String> {
}
