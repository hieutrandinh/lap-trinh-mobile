package org.example.be.rest.repository;

import org.example.be.core.entity.GroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupJpaRepository extends JpaRepository<GroupEntity, String> {
}
