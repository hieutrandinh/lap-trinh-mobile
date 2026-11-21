package org.example.be.rest.usecase.group.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.GroupMemberEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.dto.res.group.GroupRes;
import org.example.be.rest.repository.GroupBalanceViewRepository;
import org.example.be.rest.repository.GroupMemberJpaRepository;
import org.example.be.rest.usecase.group.LeaveGroupUseCase;
import org.example.be.rest.usecase.group.support.GroupAccessService;
import org.example.be.rest.usecase.group.support.GroupActivityLogService;
import org.example.be.rest.usecase.group.support.GroupResAssembler;
import org.example.be.rest.usecase.group.support.PendingActionsChecker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The snapshot only captures the balance for now: bills/splits/activity (9, 12) aren't
 * modeled as part of it yet, so there's nothing more to snapshot from them.
 * PENDING_ACTIONS_EXIST only checks repayments/reversals - pending corrections (11.x)
 * aren't modeled yet.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LeaveGroupUseCaseImpl implements LeaveGroupUseCase {

    GroupMemberJpaRepository groupMemberJpaRepository;
    GroupBalanceViewRepository groupBalanceViewRepository;
    GroupAccessService groupAccessService;
    GroupActivityLogService groupActivityLogService;
    GroupResAssembler groupResAssembler;
    PendingActionsChecker pendingActionsChecker;

    @Override
    @Transactional
    public GroupRes execute(String userId, String groupId) {
        GroupAccessService.Access access = groupAccessService.requireAccess(groupId, userId);
        GroupMemberEntity membership = access.membership();

        if ("owner".equals(membership.getRole())) {
            throw new BusinessLogicException(ErrorCode.OWNER_MUST_TRANSFER);
        }
        groupAccessService.requireWritable(access.accessMode());

        BigDecimal balance = groupBalanceViewRepository.findByGroupIdAndUserId(groupId, userId)
                .map(v -> v.getBalance())
                .orElse(BigDecimal.ZERO);
        if (balance.compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessLogicException(ErrorCode.BALANCE_NOT_SETTLED, Map.of("balance", balance));
        }

        List<Map<String, String>> pending = pendingActionsChecker.findInvolvingUser(groupId, userId);
        if (!pending.isEmpty()) {
            throw new BusinessLogicException(ErrorCode.PENDING_ACTIONS_EXIST, Map.of("items", pending));
        }

        LocalDateTime now = LocalDateTime.now();
        String snapshot = "{\"balance\":\"" + balance.toPlainString() + "\",\"capturedAt\":\"" + now + "\","
                + "\"note\":\"bills/splits/activity will be included once fully modeled\"}";

        membership.setSnapshotData(snapshot);
        membership.setSnapshotCapturedAt(now);
        membership.setStatus("left");
        membership.setLeftAt(now);
        membership.setRetainedGroupName(access.group().getName());
        groupMemberJpaRepository.save(membership);

        groupActivityLogService.log(groupId, "member_left", userId);

        return groupResAssembler.toRes(access.group(), membership);
    }

}
