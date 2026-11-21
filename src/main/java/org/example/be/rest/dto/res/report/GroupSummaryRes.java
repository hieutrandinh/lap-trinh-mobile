package org.example.be.rest.dto.res.report;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * youOwe/youAreOwed/groups need vGroupBalance (Groups module, section 8-11, not built yet).
 * Always reports zero/empty until that module exists.
 */
@Data
@Builder
public class GroupSummaryRes {

    BigDecimal youOwe;
    BigDecimal youAreOwed;
    List<GroupBalanceRefRes> groups;

}
