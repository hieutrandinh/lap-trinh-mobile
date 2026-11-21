package org.example.be.rest.usecase.repayment.impl;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.RepaymentEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.core.util.JpaPagination;
import org.example.be.rest.dto.req.repayment.RepaymentSearchReq;
import org.example.be.rest.dto.res.common.PaginationRes;
import org.example.be.rest.dto.res.repayment.RepaymentListRes;
import org.example.be.rest.repository.RepaymentJpaRepository;
import org.example.be.rest.usecase.group.support.GroupAccessService;
import org.example.be.rest.usecase.repayment.SearchRepaymentUseCase;
import org.example.be.rest.usecase.repayment.support.RepaymentResAssembler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SearchRepaymentUseCaseImpl implements SearchRepaymentUseCase {

    private static final Set<String> ALLOWED_STATUSES = Set.of("pending", "confirmed", "rejected", "cancelled",
            "reversed");
    private static final Set<String> ALLOWED_SORTS = Set.of("createdAt", "-createdAt", "-amount");

    RepaymentJpaRepository repaymentJpaRepository;
    GroupAccessService groupAccessService;
    RepaymentResAssembler repaymentResAssembler;

    @Override
    @Transactional(readOnly = true)
    public RepaymentListRes execute(String userId, String groupId, RepaymentSearchReq req) {
        groupAccessService.requireAccess(groupId, userId);

        String sort = StringUtils.hasText(req.getSort()) ? req.getSort() : "-createdAt";
        if (!ALLOWED_SORTS.contains(sort)) {
            throw new BusinessLogicException(ErrorCode.VALIDATION_ERROR);
        }

        List<String> statuses = null;
        if (StringUtils.hasText(req.getStatus())) {
            statuses = Arrays.stream(req.getStatus().split(",")).map(String::trim).toList();
            if (!ALLOWED_STATUSES.containsAll(statuses)) {
                throw new BusinessLogicException(ErrorCode.VALIDATION_ERROR);
            }
        }

        Specification<RepaymentEntity> spec = build(groupId, statuses, req.getInvolvingMe(), userId);
        int page = Math.max(req.getPage(), 1);
        int pageSize = req.getPageSize() < 1 || req.getPageSize() > 100 ? 20 : req.getPageSize();
        Page<RepaymentEntity> result = repaymentJpaRepository.findAll(spec,
                JpaPagination.request(page, pageSize, toSort(sort)));

        return RepaymentListRes.builder()
                .data(result.getContent().stream().map(repaymentResAssembler::toRes).toList())
                .pagination(PaginationRes.builder()
                        .page(page)
                        .pageSize(pageSize)
                        .totalItems(result.getTotalElements())
                        .totalPages(result.getTotalPages())
                        .build())
                .build();
    }

    private Specification<RepaymentEntity> build(String groupId, List<String> statuses, Boolean involvingMe,
                                                   String userId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("groupId"), groupId));
            if (statuses != null) {
                predicates.add(root.get("status").in(statuses));
            }
            if (Boolean.TRUE.equals(involvingMe)) {
                predicates.add(cb.or(cb.equal(root.get("fromUserId"), userId), cb.equal(root.get("toUserId"), userId)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Sort toSort(String sort) {
        boolean desc = sort.startsWith("-");
        String property = desc ? sort.substring(1) : sort;
        return Sort.by(desc ? Sort.Direction.DESC : Sort.Direction.ASC, property);
    }

}
