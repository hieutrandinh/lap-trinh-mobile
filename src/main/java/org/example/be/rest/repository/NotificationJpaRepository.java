package org.example.be.rest.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.example.be.core.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, String> {

    List<NotificationEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    List<NotificationEntity> findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(String userId);

    long countByUserIdAndReadAtIsNull(String userId);

    Optional<NotificationEntity> findByIdAndUserId(String id, String userId);

    @Modifying
    @Query("update NotificationEntity n set n.readAt = :now where n.userId = :userId and n.readAt is null")
    int markAllRead(@Param("userId") String userId, @Param("now") LocalDateTime now);

}
