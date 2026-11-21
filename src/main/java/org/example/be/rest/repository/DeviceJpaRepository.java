package org.example.be.rest.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.example.be.core.entity.DeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceJpaRepository extends JpaRepository<DeviceEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select device from DeviceEntity device where device.id = :id")
    Optional<DeviceEntity> findByIdForUpdate(@Param("id") String id);
}
