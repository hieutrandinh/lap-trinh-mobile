package org.example.be.rest.repository;

import java.util.List;
import java.util.Optional;
import org.example.be.core.entity.CorrectionRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CorrectionRequestJpaRepository extends JpaRepository<CorrectionRequestEntity, String>,
        JpaSpecificationExecutor<CorrectionRequestEntity> {

    Optional<CorrectionRequestEntity> findByGroupExpenseIdAndStatus(String groupExpenseId, String status);

    Optional<CorrectionRequestEntity> findByIdAndGroupExpenseId(String id, String groupExpenseId);

    List<CorrectionRequestEntity> findByGroupExpenseId(String groupExpenseId);

}
