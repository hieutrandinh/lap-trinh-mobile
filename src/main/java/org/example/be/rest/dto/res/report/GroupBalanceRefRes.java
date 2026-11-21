package org.example.be.rest.dto.res.report;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class GroupBalanceRefRes {

    String groupId;
    String name;
    BigDecimal balance;

}
