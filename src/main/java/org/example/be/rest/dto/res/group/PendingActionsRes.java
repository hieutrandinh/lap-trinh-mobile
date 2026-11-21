package org.example.be.rest.dto.res.group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;

/**
 * Always zero until the repayment/reversal/correction modules (section 10-11) exist.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingActionsRes {

    int repayments;
    int reversals;
    int corrections;

}
