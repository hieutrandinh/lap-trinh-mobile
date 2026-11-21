package org.example.be.rest.repository;

import java.util.List;
import java.util.Optional;
import org.example.be.core.entity.RepaymentReversalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepaymentReversalJpaRepository extends JpaRepository<RepaymentReversalEntity, String> {

    Optional<RepaymentReversalEntity> findByIdAndApproverUserId(String id, String approverUserId);

    Optional<RepaymentReversalEntity> findByRepaymentIdAndStatus(String repaymentId, String status);

    @Query("select rv from RepaymentReversalEntity rv where rv.status = 'pending' "
            + "and (rv.requesterUserId = :userId or rv.approverUserId = :userId) "
            + "and rv.repaymentId in :repaymentIds")
    List<RepaymentReversalEntity> findPendingInvolving(@Param("userId") String userId,
                                                         @Param("repaymentIds") List<String> repaymentIds);

    @Query("select count(rv) from RepaymentReversalEntity rv where rv.status = 'pending' "
            + "and rv.repaymentId in :repaymentIds")
    long countPendingByRepaymentIds(@Param("repaymentIds") List<String> repaymentIds);

    List<RepaymentReversalEntity> findByRepaymentIdInAndStatus(List<String> repaymentIds, String status);

}
