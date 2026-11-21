package org.example.be.rest.dto.res.repayment;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import org.example.be.rest.dto.res.group.UserRefRes;

@Data
@Builder
public class SuggestedRepaymentRes {

    UserRefRes from;
    UserRefRes to;
    BigDecimal amount;

}
