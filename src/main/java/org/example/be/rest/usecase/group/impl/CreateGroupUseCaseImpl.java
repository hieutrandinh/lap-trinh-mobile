package org.example.be.rest.usecase.group.impl;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.GroupEntity;
import org.example.be.core.entity.GroupMemberEntity;
import org.example.be.rest.dto.req.group.CreateGroupReq;
import org.example.be.rest.dto.res.group.GroupRes;
import org.example.be.rest.repository.GroupJpaRepository;
import org.example.be.rest.repository.GroupMemberJpaRepository;
import org.example.be.rest.usecase.group.CreateGroupUseCase;
import org.example.be.rest.usecase.group.support.GroupActivityLogService;
import org.example.be.rest.usecase.group.support.GroupResAssembler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CreateGroupUseCaseImpl implements CreateGroupUseCase {

    GroupJpaRepository groupJpaRepository;
    GroupMemberJpaRepository groupMemberJpaRepository;
    GroupActivityLogService groupActivityLogService;
    GroupResAssembler groupResAssembler;

    @Override
    @Transactional
    public GroupRes execute(String userId, CreateGroupReq req) {
        LocalDateTime now = LocalDateTime.now();

        GroupEntity group = new GroupEntity();
        group.setId(UUID.randomUUID().toString());
        group.setName(req.getName().trim());
        group.setDescription(req.getDescription());
        group.setStatus("active");
        group.setCreatedAt(now);
        group.setCreatorUserId(userId);
        groupJpaRepository.save(group);

        GroupMemberEntity membership = new GroupMemberEntity();
        membership.setId(UUID.randomUUID().toString());
        membership.setRole("owner");
        membership.setStatus("active");
        membership.setJoinedAt(now);
        membership.setGroupId(group.getId());
        membership.setUserId(userId);
        groupMemberJpaRepository.save(membership);

        groupActivityLogService.log(group.getId(), "group_created", userId);

        return groupResAssembler.toRes(group, membership);
    }

}
