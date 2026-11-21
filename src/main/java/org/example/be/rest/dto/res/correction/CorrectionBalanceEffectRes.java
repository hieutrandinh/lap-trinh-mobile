package org.example.be.rest.dto.res.correction;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import org.example.be.rest.dto.res.group.UserRefRes;

@Data
@Builder
public class CorrectionBalanceEffectRes {

    UserRefRes user;
    BigDecimal currentBalance;
    BigDecimal projectedBalance;

}
