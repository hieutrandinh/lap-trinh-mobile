package org.example.be.rest.repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.example.be.core.entity.SpendingBucketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpendingBucketJpaRepository extends JpaRepository<SpendingBucketEntity, String> {

    long countByUserId(String userId);

    Optional<SpendingBucketEntity> findByIdAndUserId(String id, String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from SpendingBucketEntity b where b.id = :id and b.userId = :userId")
    Optional<SpendingBucketEntity> findForUpdate(@Param("id") String id, @Param("userId") String userId);

    List<SpendingBucketEntity> findByUserIdOrderByNameAsc(String userId);

}
