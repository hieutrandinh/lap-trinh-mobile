package org.example.be.rest.usecase.group.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.InvitationEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.repository.InvitationJpaRepository;
import org.example.be.rest.usecase.group.RevokeInvitationUseCase;
import org.example.be.rest.usecase.group.support.GroupAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RevokeInvitationUseCaseImpl implements RevokeInvitationUseCase {

    InvitationJpaRepository invitationJpaRepository;
    GroupAccessService groupAccessService;

    @Override
    @Transactional
    public void execute(String userId, String groupId, String invitationId) {
        GroupAccessService.Access access = groupAccessService.requireAccess(groupId, userId);
        groupAccessService.requireOwnerOrAdmin(access.membership());
        groupAccessService.requireWritable(access.accessMode());

        InvitationEntity invitation = invitationJpaRepository.findByIdAndGroupId(invitationId, groupId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.NOT_FOUND));
        if ("revoked".equals(invitation.getStatus())) {
            throw new BusinessLogicException(ErrorCode.INVITATION_ALREADY_REVOKED);
        }
        invitation.setStatus("revoked");
        invitationJpaRepository.save(invitation);
    }

}
