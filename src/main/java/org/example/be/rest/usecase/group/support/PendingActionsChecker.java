package org.example.be.rest.usecase.group.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.RepaymentEntity;
import org.example.be.core.entity.RepaymentReversalEntity;
import org.example.be.rest.repository.RepaymentJpaRepository;
import org.example.be.rest.repository.RepaymentReversalJpaRepository;
import org.springframework.stereotype.Service;

/**
 * Backs PENDING_ACTIONS_EXIST for leave (8.10) and close (8.11). Only checks
 * repayments/reversals - pending corrections (11.x) aren't modeled yet.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PendingActionsChecker {

    RepaymentJpaRepository repaymentJpaRepository;
    RepaymentReversalJpaRepository repaymentReversalJpaRepository;

    public List<Map<String, String>> findInvolvingUser(String groupId, String userId) {
        List<Map<String, String>> result = new ArrayList<>();
        for (RepaymentEntity r : repaymentJpaRepository.findPendingInvolving(groupId, userId)) {
            result.add(Map.of("type", "repayment", "id", r.getId()));
        }
        List<String> groupRepaymentIds = repaymentJpaRepository.findByGroupId(groupId).stream()
                .map(RepaymentEntity::getId).toList();
        if (!groupRepaymentIds.isEmpty()) {
            for (RepaymentReversalEntity rv : repaymentReversalJpaRepository.findPendingInvolving(userId,
                    groupRepaymentIds)) {
                result.add(Map.of("type", "reversal", "id", rv.getId()));
            }
        }
        return result;
    }

    public List<Map<String, String>> findAnyInGroup(String groupId) {
        List<Map<String, String>> result = new ArrayList<>();
        for (RepaymentEntity r : repaymentJpaRepository.findByGroupIdAndStatus(groupId, "pending")) {
            result.add(Map.of("type", "repayment", "id", r.getId()));
        }
        List<String> groupRepaymentIds = repaymentJpaRepository.findByGroupId(groupId).stream()
                .map(RepaymentEntity::getId).toList();
        if (!groupRepaymentIds.isEmpty()) {
            for (RepaymentReversalEntity rv : repaymentReversalJpaRepository
                    .findByRepaymentIdInAndStatus(groupRepaymentIds, "pending")) {
                result.add(Map.of("type", "reversal", "id", rv.getId()));
            }
        }
        return result;
    }

}
