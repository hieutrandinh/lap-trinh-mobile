package org.example.be.rest.repository;

import java.util.Optional;
import org.example.be.core.entity.GroupBalanceId;
import org.example.be.core.entity.GroupBalanceView;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupBalanceViewRepository extends JpaRepository<GroupBalanceView, GroupBalanceId> {

    Optional<GroupBalanceView> findByGroupIdAndUserId(String groupId, String userId);

}
