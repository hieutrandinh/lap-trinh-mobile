package org.example.be.rest.usecase.entry;

import org.example.be.rest.dto.req.entry.CreateEntryReq;
import org.example.be.rest.dto.res.entry.CreateEntryResult;

public interface CreateEntryUseCase {

    CreateEntryResult execute(String userId, CreateEntryReq req);

}
