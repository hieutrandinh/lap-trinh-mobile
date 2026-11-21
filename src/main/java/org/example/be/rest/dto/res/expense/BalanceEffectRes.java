package org.example.be.rest.dto.res.expense;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import org.example.be.rest.dto.res.group.UserRefRes;

@Data
@Builder
public class BalanceEffectRes {

    UserRefRes user;
    BigDecimal change;

}
