package org.example.be.rest.repository;

import java.util.List;
import java.util.Optional;
import org.example.be.core.entity.GroupBalanceId;
import org.example.be.core.entity.GroupBalanceView;
import org.example.be.rest.dto.res.report.GroupBalanceRefRes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupBalanceViewRepository extends JpaRepository<GroupBalanceView, GroupBalanceId> {

    Optional<GroupBalanceView> findByGroupIdAndUserId(String groupId, String userId);

    @Query("""
            select new org.example.be.rest.dto.res.report.GroupBalanceRefRes(g.id, g.name, b.balance)
            from GroupBalanceView b
            join GroupMemberEntity m on m.groupId = b.groupId and m.userId = b.userId
            join GroupEntity g on g.id = b.groupId
            where b.userId = :userId and m.status = 'active' and m.hiddenAt is null
            order by g.name, g.id
            """)
    List<GroupBalanceRefRes> findVisibleActiveGroupBalances(@Param("userId") String userId);

}
