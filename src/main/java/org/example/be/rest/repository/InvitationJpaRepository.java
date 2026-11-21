package org.example.be.rest.repository;

import java.util.List;
import java.util.Optional;
import org.example.be.core.entity.InvitationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationJpaRepository extends JpaRepository<InvitationEntity, String> {

    Optional<InvitationEntity> findByCode(String code);

    boolean existsByCode(String code);

    List<InvitationEntity> findByGroupIdOrderByCreatedAtDesc(String groupId);

    Optional<InvitationEntity> findByIdAndGroupId(String id, String groupId);

}
