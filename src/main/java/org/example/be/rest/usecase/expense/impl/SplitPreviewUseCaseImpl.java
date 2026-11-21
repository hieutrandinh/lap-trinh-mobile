package org.example.be.rest.usecase.expense.impl;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.UserEntity;
import org.example.be.rest.dto.req.expense.SplitPreviewReq;
import org.example.be.rest.dto.res.expense.BalanceEffectRes;
import org.example.be.rest.dto.res.expense.SplitPreviewRes;
import org.example.be.rest.dto.res.expense.SplitRes;
import org.example.be.rest.dto.res.group.UserRefRes;
import org.example.be.rest.repository.UserJpaRepository;
import org.example.be.rest.usecase.expense.SplitPreviewUseCase;
import org.example.be.rest.usecase.expense.support.ExpenseMembershipValidator;
import org.example.be.rest.usecase.expense.support.SplitCalculator;
import org.example.be.rest.usecase.group.support.GroupAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SplitPreviewUseCaseImpl implements SplitPreviewUseCase {

    UserJpaRepository userJpaRepository;
    GroupAccessService groupAccessService;
    ExpenseMembershipValidator expenseMembershipValidator;
    SplitCalculator splitCalculator;

    @Override
    @Transactional(readOnly = true)
    public SplitPreviewRes execute(String userId, String groupId, SplitPreviewReq req) {
        var access = groupAccessService.requireAccess(groupId, userId);
        groupAccessService.requireWritable(access.accessMode());

        expenseMembershipValidator.validate(groupId, req.getPayerUserId(), req.getParticipants());
        var computations = splitCalculator.compute(req.getTotalAmount(), req.getSplitMethod(), req.getParticipants());

        Map<String, UserEntity> users = userJpaRepository.findAllById(
                computations.stream().map(SplitCalculator.SplitComputation::userId).distinct()
                        .collect(Collectors.toCollection(java.util.ArrayList::new))).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
        UserEntity payer = userJpaRepository.findById(req.getPayerUserId()).orElse(null);

        var splits = computations.stream()
                .map(c -> SplitRes.builder()
                        .user(toRef(users.get(c.userId()), c.userId()))
                        .amount(c.amount())
                        .roundingAdjustment(c.roundingAdjustment())
                        .build())
                .toList();
        BigDecimal roundingTotal = splits.stream().map(SplitRes::getRoundingAdjustment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> changeByUser = new LinkedHashMap<>();
        for (var c : computations) {
            changeByUser.merge(c.userId(), c.amount().negate(), BigDecimal::add);
        }
        changeByUser.merge(req.getPayerUserId(), req.getTotalAmount(), BigDecimal::add);

        var balanceEffects = changeByUser.entrySet().stream()
                .map(e -> BalanceEffectRes.builder()
                        .user(toRef(e.getKey().equals(req.getPayerUserId()) ? payer : users.get(e.getKey()),
                                e.getKey()))
                        .change(e.getValue())
                        .build())
                .toList();

        return SplitPreviewRes.builder()
                .totalAmount(req.getTotalAmount())
                .splits(splits)
                .roundingAdjustmentTotal(roundingTotal)
                .balanceEffects(balanceEffects)
                .build();
    }

    private UserRefRes toRef(UserEntity user, String fallbackId) {
        if (user == null) {
            return UserRefRes.builder().userId(fallbackId).displayName(null).build();
        }
        return UserRefRes.builder().userId(user.getId()).displayName(user.getDisplayName()).build();
    }

}
