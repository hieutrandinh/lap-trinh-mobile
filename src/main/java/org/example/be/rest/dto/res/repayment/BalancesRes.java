package org.example.be.rest.dto.res.repayment;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BalancesRes {

    String currency;
    List<MemberBalanceRes> members;
    List<SuggestedRepaymentRes> suggestedRepayments;
    BigDecimal pendingRepaymentsTotal;

}
