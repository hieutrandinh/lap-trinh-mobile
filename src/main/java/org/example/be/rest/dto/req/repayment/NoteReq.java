package org.example.be.rest.dto.req.repayment;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** Shared by reject-repayment (10.4) and reject-reversal (10.5): optional note, max 500. */
@Data
public class NoteReq {

    @Size(max = 500)
    String note;

}
