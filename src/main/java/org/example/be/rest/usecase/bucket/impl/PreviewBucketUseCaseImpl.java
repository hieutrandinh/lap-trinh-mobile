package org.example.be.rest.usecase.bucket.impl;

import org.example.be.rest.usecase.bucket.PreviewBucketUseCase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.core.util.MonthUtil;
import org.example.be.rest.dto.res.bucket.BucketPreviewRes;
import org.example.be.rest.repository.EntryDetailViewRepository;
import org.example.be.rest.usecase.bucket.support.BucketQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PreviewBucketUseCaseImpl implements PreviewBucketUseCase {

    EntryDetailViewRepository entryDetailViewRepository;
    BucketQueryService bucketQueryService;

    @Override
    @Transactional(readOnly = true)
    public BucketPreviewRes execute(String userId, List<String> categoryIds, BigDecimal monthlyTarget,
                                     String bucketId) {
        if (categoryIds == null || categoryIds.isEmpty() || categoryIds.size() > 50) {
            throw new BusinessLogicException(ErrorCode.VALIDATION_ERROR);
        }
        List<String> validated = bucketQueryService.validateCategoryIds(userId, categoryIds);

        YearMonth month = YearMonth.now(bucketQueryService.zoneOf(userId));
        LocalDateTime from = MonthUtil.startOf(month);
        LocalDateTime to = MonthUtil.startOfNext(month);

        BigDecimal spent = entryDetailViewRepository.sumPreviewSpent(userId, validated, bucketId, from, to);
        long entryCount = entryDetailViewRepository.countPreviewEntries(userId, validated, bucketId, from, to);
        BigDecimal remaining = monthlyTarget.subtract(spent);
        double progressPercent = monthlyTarget.signum() == 0 ? 0.0
                : spent.multiply(BigDecimal.valueOf(100))
                    .divide(monthlyTarget, 1, RoundingMode.HALF_UP)
                    .doubleValue();

        return BucketPreviewRes.builder()
                .month(month.toString())
                .currency("USD")
                .spent(spent)
                .remaining(remaining)
                .progressPercent(progressPercent)
                .entryCount(entryCount)
                .build();
    }

}
