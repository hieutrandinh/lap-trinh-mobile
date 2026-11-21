package org.example.be.rest.usecase.group.impl;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.GroupEntity;
import org.example.be.core.entity.GroupMemberEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.repository.GroupJpaRepository;
import org.example.be.rest.repository.GroupMemberJpaRepository;
import org.example.be.rest.usecase.group.RemoveGroupFromMyListUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RemoveGroupFromMyListUseCaseImpl implements RemoveGroupFromMyListUseCase {

    GroupJpaRepository groupJpaRepository;
    GroupMemberJpaRepository groupMemberJpaRepository;

    @Override
    @Transactional
    public void execute(String userId, String groupId) {
        GroupMemberEntity membership = groupMemberJpaRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.GROUP_NOT_AVAILABLE));
        if (membership.getHiddenAt() != null) {
            throw new BusinessLogicException(ErrorCode.GROUP_NOT_AVAILABLE);
        }
        GroupEntity group = groupJpaRepository.findById(groupId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.GROUP_NOT_AVAILABLE));

        boolean stillActiveInOpenGroup = "active".equals(membership.getStatus()) && !"closed".equals(group.getStatus());
        if (stillActiveInOpenGroup) {
            throw new BusinessLogicException(ErrorCode.MUST_LEAVE_FIRST);
        }

        membership.setHiddenAt(LocalDateTime.now());
        groupMemberJpaRepository.save(membership);
    }

}
