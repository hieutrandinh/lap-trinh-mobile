package org.example.be.rest.repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.example.be.core.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, String> {

    long countByUserId(String userId);

    Optional<CategoryEntity> findByIdAndUserId(String id, String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CategoryEntity c where c.id = :id and c.userId = :userId")
    Optional<CategoryEntity> findForUpdate(@Param("id") String id, @Param("userId") String userId);

    Optional<CategoryEntity> findByUserIdAndNameIgnoreCase(String userId, String name);

    boolean existsByUserIdAndNameIgnoreCase(String userId, String name);

    Optional<CategoryEntity> findByUserIdAndIsSystemTrue(String userId);

    List<CategoryEntity> findByUserIdAndNameContainingIgnoreCaseOrderByNameAsc(String userId, String q);

    List<CategoryEntity> findByUserIdOrderByNameAsc(String userId);

}
