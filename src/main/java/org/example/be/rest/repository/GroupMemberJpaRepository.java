package org.example.be.rest.repository;

import java.util.List;
import java.util.Optional;
import org.example.be.core.entity.GroupMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMemberJpaRepository extends JpaRepository<GroupMemberEntity, String> {

    Optional<GroupMemberEntity> findByGroupIdAndUserId(String groupId, String userId);

    List<GroupMemberEntity> findByGroupId(String groupId);

    long countByGroupIdAndStatus(String groupId, String status);

    List<GroupMemberEntity> findByGroupIdAndStatus(String groupId, String status);

    List<GroupMemberEntity> findByUserIdAndHiddenAtIsNull(String userId);

}
