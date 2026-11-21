package org.example.be.rest.usecase.expense.support;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.example.be.core.entity.CorrectionRequestEntity;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.ExpenseSplitEntity;
import org.example.be.core.entity.GroupExpenseEntity;
import org.example.be.core.entity.UserEntity;
import org.example.be.rest.dto.res.expense.GroupExpenseRes;
import org.example.be.rest.dto.res.expense.SplitRes;
import org.example.be.rest.dto.res.group.UserRefRes;
import org.example.be.rest.repository.CorrectionRequestJpaRepository;
import org.example.be.rest.repository.UserJpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExpenseResAssembler {

    UserJpaRepository userJpaRepository;
    CorrectionRequestJpaRepository correctionRequestJpaRepository;

    /** List/create-response shape (9.2, 9.3): no accessMode/corrections/myEntry/relatedRepayments. */
    public GroupExpenseRes toRes(GroupExpenseEntity expense, List<ExpenseSplitEntity> splits, String callerId) {
        Map<String, UserEntity> users = userJpaRepository.findAllById(collectUserIds(expense, splits)).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        List<SplitRes> splitRes = splits.stream()
                .map(s -> SplitRes.builder()
                        .user(toRef(users.get(s.getUserId()), s.getUserId()))
                        .amount(s.getAmount())
                        .roundingAdjustment(s.getRoundingAdjustment())
                        .build())
                .toList();

        BigDecimal myShare = splits.stream()
                .filter(s -> s.getUserId().equals(callerId))
                .map(ExpenseSplitEntity::getAmount)
                .findFirst()
                .orElse(null);

        return GroupExpenseRes.builder()
                .id(expense.getId())
                .groupId(expense.getGroupId())
                .title(expense.getTitle())
                .totalAmount(expense.getTotalAmount())
                .currency("USD")
                .occurredAt(expense.getOccurredAt())
                .category(expense.getCategory())
                .payer(toRef(users.get(expense.getPayerUserId()), expense.getPayerUserId()))
                .createdBy(toRef(users.get(expense.getCreatorUserId()), expense.getCreatorUserId()))
                .splitMethod(expense.getSplitMethod())
                .splits(splitRes)
                .note(expense.getNote())
                .status(expense.getStatus())
                .pendingCorrectionId(correctionRequestJpaRepository.findByGroupExpenseIdAndStatus(expense.getId(), "pending")
                        .map(CorrectionRequestEntity::getId).orElse(null))
                .myShare(myShare)
                .version(expense.getVersion())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
    }

    private List<String> collectUserIds(GroupExpenseEntity expense, List<ExpenseSplitEntity> splits) {
        List<String> ids = splits.stream().map(ExpenseSplitEntity::getUserId).collect(Collectors.toList());
        ids.add(expense.getPayerUserId());
        ids.add(expense.getCreatorUserId());
        return ids.stream().distinct().toList();
    }

    private UserRefRes toRef(UserEntity user, String fallbackId) {
        if (user == null) {
            return UserRefRes.builder().userId(fallbackId).displayName(null).build();
        }
        return UserRefRes.builder().userId(user.getId()).displayName(user.getDisplayName()).build();
    }

}
