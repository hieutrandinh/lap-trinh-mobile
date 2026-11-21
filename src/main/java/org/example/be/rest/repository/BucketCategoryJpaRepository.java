package org.example.be.rest.repository;

import java.util.List;
import org.example.be.core.entity.BucketCategoryEntity;
import org.example.be.core.entity.BucketCategoryId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BucketCategoryJpaRepository extends JpaRepository<BucketCategoryEntity, BucketCategoryId> {

    List<BucketCategoryEntity> findByBucketId(String bucketId);

    List<BucketCategoryEntity> findByCategoryId(String categoryId);

    void deleteByBucketId(String bucketId);

    void deleteByCategoryId(String categoryId);

    void deleteByBucketIdAndCategoryIdIn(String bucketId, List<String> categoryIds);

    boolean existsByBucketIdAndCategoryId(String bucketId, String categoryId);

}
