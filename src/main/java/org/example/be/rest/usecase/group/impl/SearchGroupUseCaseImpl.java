package org.example.be.rest.usecase.group.impl;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.GroupEntity;
import org.example.be.core.entity.GroupMemberEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.dto.req.group.GroupSearchReq;
import org.example.be.rest.dto.res.common.PaginationRes;
import org.example.be.rest.dto.res.group.GroupListRes;
import org.example.be.rest.dto.res.group.GroupRes;
import org.example.be.rest.repository.GroupJpaRepository;
import org.example.be.rest.repository.GroupMemberJpaRepository;
import org.example.be.rest.usecase.group.SearchGroupUseCase;
import org.example.be.rest.usecase.group.support.GroupResAssembler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SearchGroupUseCaseImpl implements SearchGroupUseCase {

    private static final Set<String> ALLOWED_SORTS = Set.of("name", "-name", "createdAt", "-createdAt");
    private static final Set<String> ALLOWED_ACCESS_MODES = Set.of("full", "snapshot", "closed");

    GroupMemberJpaRepository groupMemberJpaRepository;
    GroupJpaRepository groupJpaRepository;
    GroupResAssembler groupResAssembler;

    @Override
    @Transactional(readOnly = true)
    public GroupListRes execute(String userId, GroupSearchReq req) {
        String sort = StringUtils.hasText(req.getSort()) ? req.getSort() : "-createdAt";
        if (!ALLOWED_SORTS.contains(sort)) {
            throw new BusinessLogicException(ErrorCode.VALIDATION_ERROR);
        }
        if (StringUtils.hasText(req.getAccessMode()) && !ALLOWED_ACCESS_MODES.contains(req.getAccessMode())) {
            throw new BusinessLogicException(ErrorCode.VALIDATION_ERROR);
        }

        List<GroupMemberEntity> memberships = groupMemberJpaRepository.findByUserIdAndHiddenAtIsNull(userId);
        Map<String, GroupEntity> groups = groupJpaRepository
                .findAllById(memberships.stream().map(GroupMemberEntity::getGroupId).toList()).stream()
                .collect(Collectors.toMap(GroupEntity::getId, Function.identity()));

        List<GroupRes> all = memberships.stream()
                .filter(m -> groups.containsKey(m.getGroupId()))
                .map(m -> groupResAssembler.toRes(groups.get(m.getGroupId()), m))
                .filter(g -> !StringUtils.hasText(req.getAccessMode()) || req.getAccessMode().equals(g.getAccessMode()))
                .filter(g -> !StringUtils.hasText(req.getQ())
                        || g.getName().toLowerCase().contains(req.getQ().trim().toLowerCase()))
                .sorted(comparator(sort))
                .toList();

        int page = Math.max(req.getPage(), 1);
        int pageSize = req.getPageSize() < 1 || req.getPageSize() > 100 ? 20 : req.getPageSize();
        int fromIndex = (int) Math.min((page - 1L) * pageSize, all.size());
        int toIndex = Math.min(fromIndex + pageSize, all.size());
        List<GroupRes> pageData = all.subList(fromIndex, toIndex);

        int totalPages = pageSize == 0 ? 0 : (int) Math.ceil(all.size() / (double) pageSize);
        return GroupListRes.builder()
                .data(pageData)
                .pagination(PaginationRes.builder()
                        .page(page)
                        .pageSize(pageSize)
                        .totalItems(all.size())
                        .totalPages(totalPages)
                        .build())
                .build();
    }

    private Comparator<GroupRes> comparator(String sort) {
        boolean desc = sort.startsWith("-");
        String field = desc ? sort.substring(1) : sort;
        Comparator<GroupRes> cmp = "name".equals(field)
                ? Comparator.comparing(GroupRes::getName, String.CASE_INSENSITIVE_ORDER)
                : Comparator.comparing(GroupRes::getCreatedAt);
        return desc ? cmp.reversed() : cmp;
    }

}
