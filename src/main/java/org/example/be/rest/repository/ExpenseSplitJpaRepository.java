package org.example.be.rest.repository;

import java.util.List;
import java.util.Optional;
import org.example.be.core.entity.ExpenseSplitEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseSplitJpaRepository extends JpaRepository<ExpenseSplitEntity, String> {

    List<ExpenseSplitEntity> findByGroupExpenseId(String groupExpenseId);

    Optional<ExpenseSplitEntity> findByGroupExpenseIdAndUserId(String groupExpenseId, String userId);

    List<ExpenseSplitEntity> findByGroupExpenseIdIn(List<String> groupExpenseIds);

    List<ExpenseSplitEntity> findByUserId(String userId);

}
