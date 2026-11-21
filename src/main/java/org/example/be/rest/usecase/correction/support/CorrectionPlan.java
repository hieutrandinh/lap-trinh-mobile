package org.example.be.rest.usecase.correction.support;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * Fully-resolved correction: proposed.* fields already fall back to the expense's
 * current values where the request omitted them, and proposedSplits is the
 * complete target participant->amount map (empty for a cancellation).
 */
public record CorrectionPlan(
        String previousTitle, String proposedTitle,
        BigDecimal previousTotalAmount, BigDecimal proposedTotalAmount,
        LocalDateTime previousOccurredAt, LocalDateTime proposedOccurredAt,
        String previousCategory, String proposedCategory,
        String previousPayerUserId, String proposedPayerUserId,
        String previousSplitMethod, String proposedSplitMethod,
        Map<String, BigDecimal> previousSplits, Map<String, BigDecimal> proposedSplits,
        Set<String> affectedUserIds,
        boolean cancellation,
        boolean hasChanges) {
}
