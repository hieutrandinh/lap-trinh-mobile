package org.example.be.rest.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.example.be.core.entity.EntryDetailView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EntryDetailViewRepository extends JpaRepository<EntryDetailView, String>,
        JpaSpecificationExecutor<EntryDetailView> {

    @Query("select coalesce(sum(e.amount), 0) from EntryDetailView e "
            + "where e.userId = :userId and e.entryType = 'expense' "
            + "and e.occurredAt >= :from and e.occurredAt < :to "
            + "and ((e.bucketId is null and e.categoryId in :categoryIds) or e.bucketId = :bucketId)")
    BigDecimal sumPreviewSpent(@Param("userId") String userId, @Param("categoryIds") List<String> categoryIds,
                                @Param("bucketId") String bucketId, @Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to);

    @Query("select count(e) from EntryDetailView e "
            + "where e.userId = :userId and e.entryType = 'expense' "
            + "and e.occurredAt >= :from and e.occurredAt < :to "
            + "and ((e.bucketId is null and e.categoryId in :categoryIds) or e.bucketId = :bucketId)")
    long countPreviewEntries(@Param("userId") String userId, @Param("categoryIds") List<String> categoryIds,
                              @Param("bucketId") String bucketId, @Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to);

    @Query("select coalesce(sum(e.amount), 0) from EntryDetailView e "
            + "where e.userId = :userId and e.entryType = 'expense' and e.bucketId = :bucketId "
            + "and e.occurredAt >= :from and e.occurredAt < :to")
    BigDecimal sumBucketSpent(@Param("userId") String userId, @Param("bucketId") String bucketId,
                               @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select e.id from EntryDetailView e "
            + "where e.userId = :userId and e.entryType = 'expense' and e.bucketId is null "
            + "and e.categoryId in :categoryIds and e.occurredAt >= :from and e.occurredAt < :to")
    List<String> findUnbucketedExpenseIds(@Param("userId") String userId,
                                           @Param("categoryIds") List<String> categoryIds,
                                           @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select coalesce(sum(e.amount), 0) from EntryDetailView e "
            + "where e.userId = :userId and e.entryType = :entryType "
            + "and e.occurredAt >= :from and e.occurredAt < :to")
    BigDecimal sumByType(@Param("userId") String userId, @Param("entryType") String entryType,
                          @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select e.categoryId as categoryId, sum(e.amount) as amount from EntryDetailView e "
            + "where e.userId = :userId and e.entryType = 'expense' "
            + "and e.occurredAt >= :from and e.occurredAt < :to group by e.categoryId")
    List<CategoryAmountProjection> sumExpenseByCategory(@Param("userId") String userId,
                                                          @Param("from") LocalDateTime from,
                                                          @Param("to") LocalDateTime to);

}
