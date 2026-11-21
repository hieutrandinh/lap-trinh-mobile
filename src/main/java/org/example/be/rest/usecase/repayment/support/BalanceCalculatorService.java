package org.example.be.rest.usecase.repayment.support;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.GroupMemberEntity;
import org.example.be.rest.dto.res.repayment.BalancesRes;
import org.example.be.rest.dto.res.repayment.MemberBalanceRes;
import org.example.be.rest.dto.res.repayment.SuggestedRepaymentRes;
import org.example.be.rest.repository.GroupBalanceViewRepository;
import org.example.be.rest.repository.GroupMemberJpaRepository;
import org.example.be.rest.repository.RepaymentJpaRepository;
import org.springframework.stereotype.Service;

/**
 * balance = paid (as payer) - shares + confirmed repayments sent - confirmed repayments
 * received; vGroupBalance (V1__init.sql section 4.1) already computes exactly this, so
 * every read here is just a query against that view.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BalanceCalculatorService {

    private record UserBalance(String userId, BigDecimal amount) {
    }

    GroupMemberJpaRepository groupMemberJpaRepository;
    GroupBalanceViewRepository groupBalanceViewRepository;
    RepaymentJpaRepository repaymentJpaRepository;
    RepaymentResAssembler repaymentResAssembler;

    public BigDecimal balanceOf(String groupId, String userId) {
        return groupBalanceViewRepository.findByGroupIdAndUserId(groupId, userId)
                .map(v -> v.getBalance())
                .orElse(BigDecimal.ZERO);
    }

    /** Remaining debt a sender may still record repayments against (10.2). */
    public BigDecimal remainingDebt(String groupId, String userId) {
        BigDecimal balance = balanceOf(groupId, userId);
        if (balance.compareTo(BigDecimal.ZERO) >= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal alreadyPending = repaymentJpaRepository.sumPendingByGroupIdAndFromUserId(groupId, userId);
        BigDecimal remaining = balance.negate().subtract(alreadyPending);
        return remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : BigDecimal.ZERO;
    }

    public BalancesRes computeGroupBalances(String groupId) {
        List<GroupMemberEntity> activeMembers = groupMemberJpaRepository.findByGroupIdAndStatus(groupId, "active");
        List<UserBalance> balances = activeMembers.stream()
                .map(m -> new UserBalance(m.getUserId(), balanceOf(groupId, m.getUserId())))
                .toList();

        List<MemberBalanceRes> members = balances.stream()
                .map(b -> MemberBalanceRes.builder()
                        .user(repaymentResAssembler.toRef(b.userId()))
                        .balance(b.amount())
                        .position(b.amount().signum() > 0 ? "receives" : b.amount().signum() < 0 ? "owes" : "settled")
                        .build())
                .toList();

        return BalancesRes.builder()
                .currency("USD")
                .members(members)
                .suggestedRepayments(greedySettle(balances))
                .pendingRepaymentsTotal(repaymentJpaRepository.sumPendingByGroupId(groupId))
                .build();
    }

    /** Greedily pairs the biggest debtor with the biggest creditor until everyone nets to zero. */
    private List<SuggestedRepaymentRes> greedySettle(List<UserBalance> balances) {
        List<UserBalance> debtors = new ArrayList<>();
        List<UserBalance> creditors = new ArrayList<>();
        for (UserBalance b : balances) {
            if (b.amount().signum() < 0) {
                debtors.add(new UserBalance(b.userId(), b.amount().negate()));
            } else if (b.amount().signum() > 0) {
                creditors.add(new UserBalance(b.userId(), b.amount()));
            }
        }
        debtors.sort((a, c) -> c.amount().compareTo(a.amount()));
        creditors.sort((a, c) -> c.amount().compareTo(a.amount()));

        List<SuggestedRepaymentRes> result = new ArrayList<>();
        int di = 0;
        int ci = 0;
        while (di < debtors.size() && ci < creditors.size()) {
            UserBalance debtor = debtors.get(di);
            UserBalance creditor = creditors.get(ci);
            BigDecimal amount = debtor.amount().min(creditor.amount());
            result.add(SuggestedRepaymentRes.builder()
                    .from(repaymentResAssembler.toRef(debtor.userId()))
                    .to(repaymentResAssembler.toRef(creditor.userId()))
                    .amount(amount)
                    .build());

            UserBalance updatedDebtor = new UserBalance(debtor.userId(), debtor.amount().subtract(amount));
            UserBalance updatedCreditor = new UserBalance(creditor.userId(), creditor.amount().subtract(amount));
            debtors.set(di, updatedDebtor);
            creditors.set(ci, updatedCreditor);
            if (updatedDebtor.amount().signum() == 0) {
                di++;
            }
            if (updatedCreditor.amount().signum() == 0) {
                ci++;
            }
        }
        return result;
    }

}
