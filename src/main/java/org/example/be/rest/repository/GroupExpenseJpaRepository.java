package org.example.be.rest.repository;

import java.util.List;
import java.util.Optional;
import org.example.be.core.entity.GroupExpenseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GroupExpenseJpaRepository extends JpaRepository<GroupExpenseEntity, String>,
        JpaSpecificationExecutor<GroupExpenseEntity> {

    Optional<GroupExpenseEntity> findByIdAndGroupId(String id, String groupId);

    List<GroupExpenseEntity> findByGroupId(String groupId);

}
