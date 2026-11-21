package org.example.be.rest.dto.res.entry;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateEntryResult {

    EntryRes entry;
    boolean alreadyExisted;

}
