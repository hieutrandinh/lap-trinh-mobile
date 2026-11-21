package org.example.be.rest.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.example.be.core.entity.RepaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepaymentJpaRepository extends JpaRepository<RepaymentEntity, String>,
        JpaSpecificationExecutor<RepaymentEntity> {

    Optional<RepaymentEntity> findByIdAndGroupId(String id, String groupId);

    List<RepaymentEntity> findByGroupId(String groupId);

    List<RepaymentEntity> findByGroupIdAndStatus(String groupId, String status);

    @Query("select coalesce(sum(r.amount), 0) from RepaymentEntity r "
            + "where r.groupId = :groupId and r.status = 'pending'")
    BigDecimal sumPendingByGroupId(@Param("groupId") String groupId);

    @Query("select coalesce(sum(r.amount), 0) from RepaymentEntity r "
            + "where r.groupId = :groupId and r.fromUserId = :userId and r.status = 'pending'")
    BigDecimal sumPendingByGroupIdAndFromUserId(@Param("groupId") String groupId, @Param("userId") String userId);

    @Query("select r from RepaymentEntity r where r.groupId = :groupId and r.status = 'pending' "
            + "and (r.fromUserId = :userId or r.toUserId = :userId)")
    List<RepaymentEntity> findPendingInvolving(@Param("groupId") String groupId, @Param("userId") String userId);

    @Query("select count(r) from RepaymentEntity r where r.groupId = :groupId and r.status = 'pending'")
    long countPendingByGroupId(@Param("groupId") String groupId);

}
