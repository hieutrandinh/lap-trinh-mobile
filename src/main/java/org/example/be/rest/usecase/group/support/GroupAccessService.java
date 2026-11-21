package org.example.be.rest.usecase.group.support;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.GroupEntity;
import org.example.be.core.entity.GroupMemberEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.repository.GroupJpaRepository;
import org.example.be.rest.repository.GroupMemberJpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupAccessService {

    GroupJpaRepository groupJpaRepository;
    GroupMemberJpaRepository groupMemberJpaRepository;

    public record Access(GroupEntity group, GroupMemberEntity membership, String accessMode) {
    }

    /**
     * A user has access iff they have a tblGroupMember row for the group that hasn't
     * been hidden (0.7): never-a-member or removed-from-view both mean GROUP_NOT_AVAILABLE.
     */
    public Access requireAccess(String groupId, String userId) {
        GroupMemberEntity membership = groupMemberJpaRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.GROUP_NOT_AVAILABLE));
        if (membership.getHiddenAt() != null) {
            throw new BusinessLogicException(ErrorCode.GROUP_NOT_AVAILABLE);
        }
        GroupEntity group = groupJpaRepository.findById(groupId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.GROUP_NOT_AVAILABLE));
        return new Access(group, membership, accessModeOf(group, membership));
    }

    public String accessModeOf(GroupEntity group, GroupMemberEntity membership) {
        if ("closed".equals(group.getStatus())) {
            return "closed";
        }
        if ("left".equals(membership.getStatus())) {
            return "snapshot";
        }
        return "full";
    }

    public void requireWritable(String accessMode) {
        if (!"full".equals(accessMode)) {
            throw new BusinessLogicException(ErrorCode.GROUP_READ_ONLY);
        }
    }

    public void requireOwnerOrAdmin(GroupMemberEntity membership) {
        if (!"owner".equals(membership.getRole()) && !"admin".equals(membership.getRole())) {
            throw new BusinessLogicException(ErrorCode.FORBIDDEN);
        }
    }

}
