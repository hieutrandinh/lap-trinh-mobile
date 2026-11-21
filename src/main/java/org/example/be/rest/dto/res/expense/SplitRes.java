package org.example.be.rest.dto.res.expense;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;
import org.example.be.rest.dto.res.group.UserRefRes;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SplitRes {

    UserRefRes user;
    BigDecimal amount;
    BigDecimal roundingAdjustment;

}
