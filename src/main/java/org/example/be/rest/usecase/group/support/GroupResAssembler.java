package org.example.be.rest.usecase.group.support;

import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.GroupEntity;
import org.example.be.core.entity.GroupMemberEntity;
import org.example.be.rest.dto.res.group.GroupRes;
import org.example.be.rest.repository.GroupBalanceViewRepository;
import org.example.be.rest.repository.GroupMemberJpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupResAssembler {

    GroupMemberJpaRepository groupMemberJpaRepository;
    GroupBalanceViewRepository groupBalanceViewRepository;
    GroupAccessService groupAccessService;

    /** List/write-response shape (8.1, 8.2, 8.4, 8.7-8.11): no members/pendingActions/expenseCategories. */
    public GroupRes toRes(GroupEntity group, GroupMemberEntity membership) {
        String accessMode = groupAccessService.accessModeOf(group, membership);
        long memberCount = groupMemberJpaRepository.countByGroupIdAndStatus(group.getId(), "active");
        BigDecimal balance = groupBalanceViewRepository.findByGroupIdAndUserId(group.getId(), membership.getUserId())
                .map(v -> v.getBalance())
                .orElse(BigDecimal.ZERO);

        // 8.4: "former members / closed groups keep the frozen name" - only accessMode=full
        // reflects live edits. retainedGroupName is only set once leave (8.10) / close (8.11)
        // exist, so this falls back to the live name until then.
        String displayName = "full".equals(accessMode) ? group.getName()
                : membership.getRetainedGroupName() != null ? membership.getRetainedGroupName() : group.getName();

        return GroupRes.builder()
                .id(group.getId())
                .name(displayName)
                .description(group.getDescription())
                .status(group.getStatus())
                .accessMode(accessMode)
                .myRole(membership.getRole())
                .myBalance(balance)
                .currency("USD")
                .memberCount((int) memberCount)
                .snapshotCapturedAt(membership.getSnapshotCapturedAt())
                .closedAt(group.getClosedAt())
                .createdAt(group.getCreatedAt())
                .build();
    }

}
