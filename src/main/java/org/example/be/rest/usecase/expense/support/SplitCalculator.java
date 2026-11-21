package org.example.be.rest.usecase.expense.support;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.dto.req.expense.ParticipantReq;
import org.springframework.stereotype.Service;

@Service
public class SplitCalculator {

    public record SplitComputation(String userId, BigDecimal amount, BigDecimal roundingAdjustment) {
    }

    /**
     * equal: floor(total/n) cents each, remainder cents (r < n) go 1-by-1 to the
     * first r participants in request order (9.2 rule), recorded as roundingAdjustment.
     * exact: participants[].amount must sum to totalAmount exactly.
     */
    public List<SplitComputation> compute(BigDecimal totalAmount, String splitMethod,
                                           List<ParticipantReq> participants) {
        if ("equal".equals(splitMethod)) {
            return computeEqual(totalAmount, participants);
        }
        return computeExact(totalAmount, participants);
    }

    private List<SplitComputation> computeEqual(BigDecimal totalAmount, List<ParticipantReq> participants) {
        for (ParticipantReq p : participants) {
            if (p.getAmount() != null) {
                throw new BusinessLogicException(ErrorCode.SPLIT_AMOUNT_NOT_ALLOWED);
            }
        }
        int n = participants.size();
        long totalCents = totalAmount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
        long perCents = totalCents / n;
        long remainder = totalCents % n;

        List<SplitComputation> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            boolean extraCent = i < remainder;
            long cents = perCents + (extraCent ? 1 : 0);
            BigDecimal amount = BigDecimal.valueOf(cents, 2);
            BigDecimal adjustment = extraCent ? BigDecimal.valueOf(1, 2) : BigDecimal.ZERO.setScale(2);
            result.add(new SplitComputation(participants.get(i).getUserId(), amount, adjustment));
        }
        return result;
    }

    private List<SplitComputation> computeExact(BigDecimal totalAmount, List<ParticipantReq> participants) {
        BigDecimal sum = BigDecimal.ZERO;
        List<SplitComputation> result = new ArrayList<>(participants.size());
        for (ParticipantReq p : participants) {
            if (p.getAmount() == null) {
                throw new BusinessLogicException(ErrorCode.VALIDATION_ERROR);
            }
            sum = sum.add(p.getAmount());
            result.add(new SplitComputation(p.getUserId(), p.getAmount(), BigDecimal.ZERO.setScale(2)));
        }
        if (sum.compareTo(totalAmount) != 0) {
            throw new BusinessLogicException(ErrorCode.SPLIT_TOTAL_MISMATCH,
                    Map.of("sum", sum, "difference", totalAmount.subtract(sum)));
        }
        return result;
    }

}
