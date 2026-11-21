package org.example.be.rest.repository;

import org.example.be.core.entity.DeletionMarkerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeletionMarkerJpaRepository extends JpaRepository<DeletionMarkerEntity, String> {

    boolean existsByUserIdAndRecordTypeAndRecordId(String userId, String recordType, String recordId);

}
