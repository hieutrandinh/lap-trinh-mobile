package org.example.be.rest.usecase.group.impl;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.GroupEntity;
import org.example.be.core.entity.GroupMemberEntity;
import org.example.be.core.entity.InvitationEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.dto.res.group.GroupRes;
import org.example.be.rest.repository.GroupJpaRepository;
import org.example.be.rest.repository.GroupMemberJpaRepository;
import org.example.be.rest.repository.InvitationJpaRepository;
import org.example.be.rest.usecase.group.AcceptInvitationUseCase;
import org.example.be.rest.usecase.group.support.GroupActivityLogService;
import org.example.be.rest.usecase.group.support.GroupResAssembler;
import org.example.be.rest.usecase.notification.support.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AcceptInvitationUseCaseImpl implements AcceptInvitationUseCase {

    InvitationJpaRepository invitationJpaRepository;
    GroupJpaRepository groupJpaRepository;
    GroupMemberJpaRepository groupMemberJpaRepository;
    NotificationService notificationService;
    GroupActivityLogService groupActivityLogService;
    GroupResAssembler groupResAssembler;

    @Override
    @Transactional
    public GroupRes execute(String userId, String code) {
        InvitationEntity invitation = invitationJpaRepository.findByCode(code)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.INVITATION_NOT_FOUND));

        if ("revoked".equals(invitation.getStatus())) {
            throw new BusinessLogicException(ErrorCode.INVITATION_REVOKED);
        }
        LocalDateTime now = LocalDateTime.now();
        if (invitation.getExpiresAt().isBefore(now)) {
            throw new BusinessLogicException(ErrorCode.INVITATION_EXPIRED);
        }
        if (invitation.getMaxUses() != null && invitation.getUseCount() >= invitation.getMaxUses()) {
            throw new BusinessLogicException(ErrorCode.INVITATION_USED_UP);
        }

        GroupEntity group = groupJpaRepository.findById(invitation.getGroupId())
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.GROUP_NOT_AVAILABLE));
        if ("closed".equals(group.getStatus())) {
            throw new BusinessLogicException(ErrorCode.GROUP_CLOSED);
        }

        GroupMemberEntity membership = groupMemberJpaRepository.findByGroupIdAndUserId(group.getId(), userId)
                .orElse(null);
        if (membership != null && "active".equals(membership.getStatus())) {
            throw new BusinessLogicException(ErrorCode.ALREADY_MEMBER);
        }

        if (membership != null) {
            membership.setRole("member");
            membership.setStatus("active");
            membership.setLeftAt(null);
            membership.setHiddenAt(null);
            membership.setRetainedGroupName(null);
            membership.setSnapshotCapturedAt(null);
            membership.setSnapshotData(null);
        } else {
            membership = new GroupMemberEntity();
            membership.setId(UUID.randomUUID().toString());
            membership.setRole("member");
            membership.setStatus("active");
            membership.setJoinedAt(now);
            membership.setGroupId(group.getId());
            membership.setUserId(userId);
        }
        groupMemberJpaRepository.save(membership);

        invitation.setUseCount(invitation.getUseCount() + 1);
        invitationJpaRepository.save(invitation);

        groupActivityLogService.log(group.getId(), "member_joined", "member", userId, "{}", userId);

        notificationService.notifyGroup(userId, "group_joined", "{\"groupId\":\"" + group.getId()
                + "\",\"invitedBy\":\"" + invitation.getCreatorUserId() + "\"}");

        return groupResAssembler.toRes(group, membership);
    }

}
