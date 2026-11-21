package org.example.be.rest.usecase.entry;

import org.example.be.rest.dto.req.entry.EntrySearchReq;
import org.example.be.rest.dto.res.entry.EntryListRes;

public interface SearchEntryUseCase {

    EntryListRes execute(String userId, EntrySearchReq req);

}
