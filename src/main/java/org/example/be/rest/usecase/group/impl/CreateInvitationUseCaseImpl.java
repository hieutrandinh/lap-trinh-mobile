package org.example.be.rest.usecase.group.impl;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.InvitationEntity;
import org.example.be.rest.dto.req.group.CreateInvitationReq;
import org.example.be.rest.dto.res.group.InvitationRes;
import org.example.be.rest.repository.InvitationJpaRepository;
import org.example.be.rest.usecase.group.CreateInvitationUseCase;
import org.example.be.rest.usecase.group.support.GroupAccessService;
import org.example.be.rest.usecase.group.support.InvitationCodeGenerator;
import org.example.be.rest.usecase.group.support.InvitationResAssembler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CreateInvitationUseCaseImpl implements CreateInvitationUseCase {

    private static final int DEFAULT_EXPIRES_IN_HOURS = 168;

    InvitationJpaRepository invitationJpaRepository;
    GroupAccessService groupAccessService;
    InvitationCodeGenerator invitationCodeGenerator;
    InvitationResAssembler invitationResAssembler;

    @Override
    @Transactional
    public InvitationRes execute(String userId, String groupId, CreateInvitationReq req) {
        GroupAccessService.Access access = groupAccessService.requireAccess(groupId, userId);
        groupAccessService.requireOwnerOrAdmin(access.membership());
        groupAccessService.requireWritable(access.accessMode());

        int expiresInHours = req.getExpiresInHours() != null ? req.getExpiresInHours() : DEFAULT_EXPIRES_IN_HOURS;
        LocalDateTime now = LocalDateTime.now();

        InvitationEntity invitation = new InvitationEntity();
        invitation.setId(UUID.randomUUID().toString());
        invitation.setCode(invitationCodeGenerator.generate());
        invitation.setMaxUses(req.getMaxUses());
        invitation.setUseCount(0);
        invitation.setExpiresAt(now.plusHours(expiresInHours));
        invitation.setStatus("active");
        invitation.setCreatedAt(now);
        invitation.setGroupId(groupId);
        invitation.setCreatorUserId(userId);
        invitationJpaRepository.save(invitation);

        return invitationResAssembler.toRes(invitation);
    }

}
