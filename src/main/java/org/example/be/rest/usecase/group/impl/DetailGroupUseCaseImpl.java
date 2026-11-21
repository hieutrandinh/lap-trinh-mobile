package org.example.be.rest.usecase.group.impl;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.GroupMemberEntity;
import org.example.be.core.entity.UserEntity;
import org.example.be.rest.dto.res.group.GroupMemberRes;
import org.example.be.rest.dto.res.group.GroupRes;
import org.example.be.rest.dto.res.group.PendingActionsRes;
import org.example.be.rest.repository.GroupMemberJpaRepository;
import org.example.be.rest.repository.UserJpaRepository;
import org.example.be.rest.usecase.group.DetailGroupUseCase;
import org.example.be.rest.usecase.group.support.GroupAccessService;
import org.example.be.rest.usecase.group.support.GroupResAssembler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * accessMode = snapshot should replay tblGroupMember.snapshotData (captured on leave,
 * 8.10) instead of live members - 8.10 isn't built yet so no snapshot ever exists and
 * this always falls back to the live member list. pendingActions/expenseCategories are
 * zero/empty until the repayment/reversal/correction and shared-expense modules exist.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DetailGroupUseCaseImpl implements DetailGroupUseCase {

    GroupMemberJpaRepository groupMemberJpaRepository;
    UserJpaRepository userJpaRepository;
    GroupAccessService groupAccessService;
    GroupResAssembler groupResAssembler;

    @Override
    @Transactional(readOnly = true)
    public GroupRes execute(String userId, String groupId) {
        GroupAccessService.Access access = groupAccessService.requireAccess(groupId, userId);

        List<GroupMemberEntity> activeMembers = groupMemberJpaRepository.findByGroupIdAndStatus(groupId, "active");
        Map<String, UserEntity> users = userJpaRepository
                .findAllById(activeMembers.stream().map(GroupMemberEntity::getUserId).toList()).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        List<GroupMemberRes> members = activeMembers.stream()
                .map(m -> GroupMemberRes.builder()
                        .userId(m.getUserId())
                        .displayName(users.containsKey(m.getUserId()) ? users.get(m.getUserId()).getDisplayName()
                                : null)
                        .role(m.getRole())
                        .status(m.getStatus())
                        .joinedAt(m.getJoinedAt())
                        .leftAt(m.getLeftAt())
                        .build())
                .toList();

        GroupRes res = groupResAssembler.toRes(access.group(), access.membership());
        res.setMembers(members);
        res.setPendingActions(PendingActionsRes.builder().repayments(0).reversals(0).corrections(0).build());
        res.setExpenseCategories(List.of());
        return res;
    }

}
