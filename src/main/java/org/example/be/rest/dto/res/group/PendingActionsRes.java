package org.example.be.rest.dto.res.group;

import lombok.Builder;
import lombok.Data;

/**
 * Always zero until the repayment/reversal/correction modules (section 10-11) exist.
 */
@Data
@Builder
public class PendingActionsRes {

    int repayments;
    int reversals;
    int corrections;

}
