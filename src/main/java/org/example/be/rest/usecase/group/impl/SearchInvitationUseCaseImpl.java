package org.example.be.rest.usecase.group.impl;

import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.dto.res.common.PaginationRes;
import org.example.be.rest.dto.res.group.InvitationListRes;
import org.example.be.rest.dto.res.group.InvitationRes;
import org.example.be.rest.repository.InvitationJpaRepository;
import org.example.be.rest.usecase.group.SearchInvitationUseCase;
import org.example.be.rest.usecase.group.support.GroupAccessService;
import org.example.be.rest.usecase.group.support.InvitationResAssembler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SearchInvitationUseCaseImpl implements SearchInvitationUseCase {

    InvitationJpaRepository invitationJpaRepository;
    GroupAccessService groupAccessService;
    InvitationResAssembler invitationResAssembler;

    @Override
    @Transactional(readOnly = true)
    public InvitationListRes execute(String userId, String groupId, String status, int pageParam, int pageSizeParam) {
        groupAccessService.requireAccess(groupId, userId);

        if (StringUtils.hasText(status) && !List.of("active", "revoked", "expired").contains(status)) {
            throw new BusinessLogicException(ErrorCode.VALIDATION_ERROR);
        }

        List<InvitationRes> all = invitationJpaRepository.findByGroupIdOrderByCreatedAtDesc(groupId).stream()
                .map(invitationResAssembler::toRes)
                .filter(i -> !StringUtils.hasText(status) || status.equals(i.getStatus()))
                .toList();

        int page = Math.max(pageParam, 1);
        int pageSize = pageSizeParam < 1 || pageSizeParam > 100 ? 20 : pageSizeParam;
        int fromIndex = Math.min((page - 1) * pageSize, all.size());
        int toIndex = Math.min(fromIndex + pageSize, all.size());

        int totalPages = pageSize == 0 ? 0 : (int) Math.ceil(all.size() / (double) pageSize);
        return InvitationListRes.builder()
                .data(all.subList(fromIndex, toIndex))
                .pagination(PaginationRes.builder()
                        .page(page)
                        .pageSize(pageSize)
                        .totalItems(all.size())
                        .totalPages(totalPages)
                        .build())
                .build();
    }

}
