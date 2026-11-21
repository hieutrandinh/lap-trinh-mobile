package org.example.be.rest.repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.example.be.core.entity.EntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EntryJpaRepository extends JpaRepository<EntryEntity, String> {

    long countByUserId(String userId);

    long countByUserIdAndCategoryId(String userId, String categoryId);

    Optional<EntryEntity> findByIdAndUserId(String id, String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EntryEntity e where e.id = :id and e.userId = :userId")
    Optional<EntryEntity> findForUpdate(@Param("id") String id, @Param("userId") String userId);

    Optional<EntryEntity> findByExpenseSplitId(String expenseSplitId);

    @Modifying
    @Query("update EntryEntity e set e.bucketId = :bucketId, e.version = e.version + 1, e.updatedAt = :now "
            + "where e.id in :ids")
    int assignBucket(@Param("ids") List<String> ids, @Param("bucketId") String bucketId,
                      @Param("now") LocalDateTime now);

    @Modifying
    @Query("update EntryEntity e set e.bucketId = null, e.version = e.version + 1, e.updatedAt = :now "
            + "where e.userId = :userId and e.bucketId = :bucketId and e.categoryId in :categoryIds")
    int clearBucketForCategories(@Param("userId") String userId, @Param("bucketId") String bucketId,
                                  @Param("categoryIds") List<String> categoryIds, @Param("now") LocalDateTime now);

    @Modifying
    @Query("update EntryEntity e set e.bucketId = null, e.version = e.version + 1, e.updatedAt = :now "
            + "where e.userId = :userId and e.categoryId = :categoryId and e.bucketId is not null")
    int clearBucketForCategory(@Param("userId") String userId, @Param("categoryId") String categoryId,
                                @Param("now") LocalDateTime now);

    @Modifying
    @Query("update EntryEntity e set e.categoryId = :newCategoryId, e.version = e.version + 1, e.updatedAt = :now "
            + "where e.userId = :userId and e.categoryId = :oldCategoryId")
    int reassignCategory(@Param("userId") String userId, @Param("oldCategoryId") String oldCategoryId,
                          @Param("newCategoryId") String newCategoryId, @Param("now") LocalDateTime now);

    @Query("select distinct e.bucketId from EntryEntity e "
            + "where e.userId = :userId and e.categoryId = :categoryId and e.bucketId is not null")
    List<String> findDistinctBucketIdsByUserIdAndCategoryId(@Param("userId") String userId,
                                                              @Param("categoryId") String categoryId);

    @Modifying
    @Query("update EntryEntity e set e.bucketId = null, e.version = e.version + 1, e.updatedAt = :now "
            + "where e.userId = :userId and e.categoryId = :categoryId and e.bucketId = :bucketId")
    int clearBucketForCategoryAndBucket(@Param("userId") String userId, @Param("categoryId") String categoryId,
                                         @Param("bucketId") String bucketId, @Param("now") LocalDateTime now);

}
