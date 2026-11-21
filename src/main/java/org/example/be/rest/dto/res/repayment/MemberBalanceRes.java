package org.example.be.rest.dto.res.repayment;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import org.example.be.rest.dto.res.group.UserRefRes;

@Data
@Builder
public class MemberBalanceRes {

    UserRefRes user;
    BigDecimal balance;
    String position;

}
