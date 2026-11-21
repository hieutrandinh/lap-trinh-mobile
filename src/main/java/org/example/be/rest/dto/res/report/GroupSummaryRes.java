package org.example.be.rest.dto.res.report;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GroupSummaryRes {

    BigDecimal youOwe;
    BigDecimal youAreOwed;
    List<GroupBalanceRefRes> groups;

}
