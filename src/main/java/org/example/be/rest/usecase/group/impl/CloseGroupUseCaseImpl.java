package org.example.be.rest.usecase.group.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.GroupEntity;
import org.example.be.core.entity.GroupMemberEntity;
import org.example.be.core.entity.InvitationEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.dto.res.group.GroupRes;
import org.example.be.rest.repository.GroupBalanceViewRepository;
import org.example.be.rest.repository.GroupJpaRepository;
import org.example.be.rest.repository.GroupMemberJpaRepository;
import org.example.be.rest.repository.InvitationJpaRepository;
import org.example.be.rest.usecase.group.CloseGroupUseCase;
import org.example.be.rest.usecase.group.support.GroupAccessService;
import org.example.be.rest.usecase.group.support.GroupActivityLogService;
import org.example.be.rest.usecase.group.support.GroupResAssembler;
import org.example.be.rest.usecase.group.support.PendingActionsChecker;
import org.example.be.rest.usecase.notification.support.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PENDING_ACTIONS_EXIST only checks repayments/reversals - pending corrections (11.x) aren't modeled yet. */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CloseGroupUseCaseImpl implements CloseGroupUseCase {

    GroupJpaRepository groupJpaRepository;
    GroupMemberJpaRepository groupMemberJpaRepository;
    GroupBalanceViewRepository groupBalanceViewRepository;
    InvitationJpaRepository invitationJpaRepository;
    NotificationService notificationService;
    GroupAccessService groupAccessService;
    GroupActivityLogService groupActivityLogService;
    GroupResAssembler groupResAssembler;
    PendingActionsChecker pendingActionsChecker;

    @Override
    @Transactional
    public GroupRes execute(String userId, String groupId) {
        GroupAccessService.Access access = groupAccessService.requireAccess(groupId, userId);
        if (!"owner".equals(access.membership().getRole())) {
            throw new BusinessLogicException(ErrorCode.FORBIDDEN);
        }
        groupAccessService.requireWritable(access.accessMode());

        List<Map<String, String>> pending = pendingActionsChecker.findAnyInGroup(groupId);
        if (!pending.isEmpty()) {
            throw new BusinessLogicException(ErrorCode.PENDING_ACTIONS_EXIST, Map.of("items", pending));
        }

        List<GroupMemberEntity> activeMembers = groupMemberJpaRepository.findByGroupIdAndStatus(groupId, "active");
        List<Map<String, Object>> unsettled = new ArrayList<>();
        for (GroupMemberEntity member : activeMembers) {
            BigDecimal balance = groupBalanceViewRepository.findByGroupIdAndUserId(groupId, member.getUserId())
                    .map(v -> v.getBalance())
                    .orElse(BigDecimal.ZERO);
            if (balance.compareTo(BigDecimal.ZERO) != 0) {
                unsettled.add(Map.of("userId", member.getUserId(), "balance", balance));
            }
        }
        if (!unsettled.isEmpty()) {
            throw new BusinessLogicException(ErrorCode.GROUP_NOT_SETTLED, Map.of("members", unsettled));
        }

        GroupEntity group = access.group();
        LocalDateTime now = LocalDateTime.now();
        group.setStatus("closed");
        group.setClosedAt(now);
        groupJpaRepository.save(group);

        for (GroupMemberEntity member : groupMemberJpaRepository.findByGroupId(groupId)) {
            if (member.getRetainedGroupName() == null) {
                member.setRetainedGroupName(group.getName());
            }
            groupMemberJpaRepository.save(member);
        }

        for (InvitationEntity invitation : invitationJpaRepository.findByGroupIdOrderByCreatedAtDesc(groupId)) {
            if ("active".equals(invitation.getStatus())) {
                invitation.setStatus("revoked");
                invitationJpaRepository.save(invitation);
            }
        }

        for (GroupMemberEntity member : activeMembers) {
            if (member.getUserId().equals(userId)) {
                continue;
            }
            notificationService.notifyGroup(member.getUserId(), "group_closed",
                    "{\"groupId\":\"" + groupId + "\"}");
        }

        groupActivityLogService.log(groupId, "group_closed", userId);

        return groupResAssembler.toRes(group, access.membership());
    }

}
