package org.example.be.rest.usecase.group.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.GroupMemberEntity;
import org.example.be.core.entity.UserEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.dto.req.group.ChangeRoleReq;
import org.example.be.rest.dto.res.group.GroupMemberRes;
import org.example.be.rest.repository.GroupMemberJpaRepository;
import org.example.be.rest.repository.UserJpaRepository;
import org.example.be.rest.usecase.group.ChangeMemberRoleUseCase;
import org.example.be.rest.usecase.group.support.GroupAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChangeMemberRoleUseCaseImpl implements ChangeMemberRoleUseCase {

    GroupMemberJpaRepository groupMemberJpaRepository;
    UserJpaRepository userJpaRepository;
    GroupAccessService groupAccessService;

    @Override
    @Transactional
    public GroupMemberRes execute(String userId, String groupId, String targetUserId, ChangeRoleReq req) {
        GroupAccessService.Access access = groupAccessService.requireAccess(groupId, userId);
        if (!"owner".equals(access.membership().getRole())) {
            throw new BusinessLogicException(ErrorCode.FORBIDDEN);
        }
        groupAccessService.requireWritable(access.accessMode());

        GroupMemberEntity target = groupMemberJpaRepository.findByGroupIdAndUserId(groupId, targetUserId)
                .filter(m -> "active".equals(m.getStatus()))
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.MEMBER_NOT_FOUND));
        if ("owner".equals(target.getRole())) {
            throw new BusinessLogicException(ErrorCode.CANNOT_CHANGE_OWNER_ROLE);
        }

        target.setRole(req.getRole());
        groupMemberJpaRepository.save(target);

        String displayName = userJpaRepository.findById(targetUserId).map(UserEntity::getDisplayName).orElse(null);
        return GroupMemberRes.builder()
                .userId(target.getUserId())
                .displayName(displayName)
                .role(target.getRole())
                .status(target.getStatus())
                .joinedAt(target.getJoinedAt())
                .leftAt(target.getLeftAt())
                .build();
    }

}
