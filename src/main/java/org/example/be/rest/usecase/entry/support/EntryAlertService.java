package org.example.be.rest.usecase.entry.support;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.BucketAlertEntity;
import org.example.be.core.entity.SpendingBucketEntity;
import org.example.be.core.util.MonthUtil;
import org.example.be.rest.dto.res.entry.AlertRes;
import org.example.be.rest.repository.BucketAlertJpaRepository;
import org.example.be.rest.repository.EntryDetailViewRepository;
import org.example.be.rest.repository.SpendingBucketJpaRepository;
import org.example.be.rest.repository.UserJpaRepository;
import org.example.be.rest.usecase.notification.support.NotificationService;
import org.springframework.stereotype.Service;

/**
 * Implements the "send notification bucket_80/bucket_exceeded at most once per
 * threshold per month" rule from 4.2, backed by tblBucketAlert for dedup and
 * tblNotification for delivery (actual push/delivery infra is out of scope).
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EntryAlertService {

    private static final int[] THRESHOLDS = {80, 100};

    SpendingBucketJpaRepository spendingBucketJpaRepository;
    EntryDetailViewRepository entryDetailViewRepository;
    BucketAlertJpaRepository bucketAlertJpaRepository;
    NotificationService notificationService;
    UserJpaRepository userJpaRepository;

    public List<AlertRes> checkAndNotify(String userId, String bucketId) {
        if (bucketId == null) {
            return List.of();
        }
        SpendingBucketEntity bucket = spendingBucketJpaRepository.findById(bucketId).orElse(null);
        if (bucket == null || bucket.getMonthlyTarget().signum() == 0) {
            return List.of();
        }

        String timezone = userJpaRepository.findById(userId).map(u -> u.getTimezone()).orElse("UTC");
        YearMonth yearMonth = YearMonth.now(MonthUtil.safeZone(timezone));
        String month = yearMonth.toString();
        LocalDateTime from = MonthUtil.startOf(yearMonth);
        LocalDateTime to = MonthUtil.startOfNext(yearMonth);
        BigDecimal spent = entryDetailViewRepository.sumBucketSpent(userId, bucketId, from, to);
        double progressPercent = spent.multiply(BigDecimal.valueOf(100))
                .divide(bucket.getMonthlyTarget(), 4, java.math.RoundingMode.HALF_UP)
                .doubleValue();

        List<AlertRes> alerts = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int threshold : THRESHOLDS) {
            if (progressPercent < threshold) {
                continue;
            }
            if (bucketAlertJpaRepository.existsByBucketIdAndMonthAndThreshold(bucketId, month, threshold)) {
                continue;
            }
            BucketAlertEntity alert = new BucketAlertEntity();
            alert.setId(UUID.randomUUID().toString());
            alert.setBucketId(bucketId);
            alert.setMonth(month);
            alert.setThreshold(threshold);
            alert.setSentAt(now);
            bucketAlertJpaRepository.save(alert);

            String type = threshold >= 100 ? "bucket_exceeded" : "bucket_80";
            notificationService.notifyBucketAlert(userId, type,
                    "{\"bucketId\":\"" + bucketId + "\",\"month\":\"" + month + "\"}");

            alerts.add(AlertRes.builder().bucketId(bucketId).threshold(threshold).build());
        }
        return alerts;
    }

}
