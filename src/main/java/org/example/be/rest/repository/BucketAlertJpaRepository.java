package org.example.be.rest.repository;

import org.example.be.core.entity.BucketAlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BucketAlertJpaRepository extends JpaRepository<BucketAlertEntity, String> {

    boolean existsByBucketIdAndMonthAndThreshold(String bucketId, String month, int threshold);

}
