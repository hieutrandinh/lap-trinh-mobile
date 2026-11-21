package org.example.be.rest.usecase.group.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.GroupMemberEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.dto.req.group.TransferOwnershipReq;
import org.example.be.rest.dto.res.group.GroupRes;
import org.example.be.rest.repository.GroupMemberJpaRepository;
import org.example.be.rest.usecase.group.TransferOwnershipUseCase;
import org.example.be.rest.usecase.group.support.GroupAccessService;
import org.example.be.rest.usecase.group.support.GroupActivityLogService;
import org.example.be.rest.usecase.group.support.GroupResAssembler;
import org.example.be.rest.usecase.notification.support.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TransferOwnershipUseCaseImpl implements TransferOwnershipUseCase {

    GroupMemberJpaRepository groupMemberJpaRepository;
    NotificationService notificationService;
    GroupAccessService groupAccessService;
    GroupActivityLogService groupActivityLogService;
    GroupResAssembler groupResAssembler;

    @Override
    @Transactional
    public GroupRes execute(String userId, String groupId, TransferOwnershipReq req) {
        GroupAccessService.Access access = groupAccessService.requireAccess(groupId, userId);
        if (!"owner".equals(access.membership().getRole())) {
            throw new BusinessLogicException(ErrorCode.FORBIDDEN);
        }
        groupAccessService.requireWritable(access.accessMode());

        if (req.getNewOwnerUserId().equals(userId)) {
            throw new BusinessLogicException(ErrorCode.VALIDATION_ERROR);
        }
        GroupMemberEntity newOwner = groupMemberJpaRepository.findByGroupIdAndUserId(groupId, req.getNewOwnerUserId())
                .filter(m -> "active".equals(m.getStatus()))
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.MEMBER_NOT_FOUND));

        GroupMemberEntity oldOwner = access.membership();
        oldOwner.setRole("admin");
        newOwner.setRole("owner");
        groupMemberJpaRepository.save(oldOwner);
        groupMemberJpaRepository.save(newOwner);

        groupActivityLogService.log(groupId, "ownership_transferred", "member", newOwner.getUserId(), "{}", userId);
        notificationService.notifyGroup(newOwner.getUserId(), "ownership_transferred",
                "{\"groupId\":\"" + groupId + "\"}");

        return groupResAssembler.toRes(access.group(), oldOwner);
    }

}
