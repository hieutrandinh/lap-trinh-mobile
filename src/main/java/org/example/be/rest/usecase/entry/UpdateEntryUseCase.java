package org.example.be.rest.usecase.entry;

import org.example.be.rest.dto.req.entry.UpdateEntryReq;
import org.example.be.rest.dto.res.entry.EntryRes;

public interface UpdateEntryUseCase {

    EntryRes execute(String userId, String entryId, UpdateEntryReq req);

}
