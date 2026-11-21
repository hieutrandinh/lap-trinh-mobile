package org.example.be.rest.usecase.group.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.GroupEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.dto.req.group.UpdateGroupReq;
import org.example.be.rest.dto.res.group.GroupRes;
import org.example.be.rest.repository.GroupJpaRepository;
import org.example.be.rest.usecase.group.UpdateGroupUseCase;
import org.example.be.rest.usecase.group.support.GroupAccessService;
import org.example.be.rest.usecase.group.support.GroupResAssembler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UpdateGroupUseCaseImpl implements UpdateGroupUseCase {

    GroupJpaRepository groupJpaRepository;
    GroupAccessService groupAccessService;
    GroupResAssembler groupResAssembler;

    @Override
    @Transactional
    public GroupRes execute(String userId, String groupId, UpdateGroupReq req) {
        GroupAccessService.Access access = groupAccessService.requireAccess(groupId, userId);
        groupAccessService.requireOwnerOrAdmin(access.membership());
        groupAccessService.requireWritable(access.accessMode());

        if (req.getName() == null && !req.getDescription().isPresent()) {
            throw new BusinessLogicException(ErrorCode.VALIDATION_ERROR);
        }

        GroupEntity group = access.group();
        if (req.getName() != null) {
            group.setName(req.getName().trim());
        }
        if (req.getDescription().isPresent()) {
            group.setDescription(req.getDescription().get());
        }
        groupJpaRepository.save(group);

        return groupResAssembler.toRes(group, access.membership());
    }

}
