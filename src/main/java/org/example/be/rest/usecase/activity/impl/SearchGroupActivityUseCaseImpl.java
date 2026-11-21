package org.example.be.rest.usecase.activity.impl;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.GroupActivityEntity;
import org.example.be.rest.dto.req.activity.ActivitySearchReq;
import org.example.be.rest.dto.res.activity.GroupActivityListRes;
import org.example.be.rest.dto.res.activity.GroupActivityRes;
import org.example.be.rest.dto.res.common.PaginationRes;
import org.example.be.rest.repository.GroupActivityJpaRepository;
import org.example.be.rest.usecase.activity.SearchGroupActivityUseCase;
import org.example.be.rest.usecase.activity.support.GroupActivityResAssembler;
import org.example.be.rest.usecase.group.support.GroupAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SearchGroupActivityUseCaseImpl implements SearchGroupActivityUseCase {

    GroupActivityJpaRepository groupActivityJpaRepository;
    GroupAccessService groupAccessService;
    GroupActivityResAssembler groupActivityResAssembler;

    @Override
    @Transactional(readOnly = true)
    public GroupActivityListRes execute(String userId, String groupId, ActivitySearchReq req) {
        var access = groupAccessService.requireAccess(groupId, userId);

        LocalDateTime from = req.getFrom() != null
                ? req.getFrom().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime() : null;
        LocalDateTime to = req.getTo() != null
                ? req.getTo().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime() : null;
        LocalDateTime snapshotCutoff = "left".equals(access.membership().getStatus())
                ? access.membership().getSnapshotCapturedAt() : null;

        List<GroupActivityEntity> filtered = groupActivityJpaRepository
                .findByGroupIdOrderByCreatedAtDesc(groupId).stream()
                .filter(a -> !StringUtils.hasText(req.getType()) || req.getType().equals(a.getActivityType()))
                .filter(a -> !StringUtils.hasText(req.getCategory())
                        || req.getCategory().equals(GroupActivityResAssembler.categoryOf(a.getActivityType())))
                .filter(a -> from == null || !a.getCreatedAt().isBefore(from))
                .filter(a -> to == null || a.getCreatedAt().isBefore(to))
                .filter(a -> snapshotCutoff == null || !a.getCreatedAt().isAfter(snapshotCutoff))
                .toList();

        int page = Math.max(req.getPage(), 1);
        int pageSize = req.getPageSize() < 1 || req.getPageSize() > 100 ? 20 : req.getPageSize();
        int from_ = (int) Math.min((page - 1L) * pageSize, filtered.size());
        int to_ = Math.min(from_ + pageSize, filtered.size());

        List<GroupActivityRes> data = filtered.subList(from_, to_).stream()
                .map(groupActivityResAssembler::toRes)
                .toList();

        int totalPages = pageSize == 0 ? 0 : (int) Math.ceil(filtered.size() / (double) pageSize);
        return GroupActivityListRes.builder()
                .data(data)
                .pagination(PaginationRes.builder()
                        .page(page).pageSize(pageSize).totalItems(filtered.size()).totalPages(totalPages)
                        .build())
                .build();
    }

}
