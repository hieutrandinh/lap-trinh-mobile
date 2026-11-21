package org.example.be.rest.usecase.activity;

import org.example.be.rest.dto.req.activity.ActivitySearchReq;
import org.example.be.rest.dto.res.activity.GroupActivityListRes;

public interface SearchGroupActivityUseCase {

    GroupActivityListRes execute(String userId, String groupId, ActivitySearchReq req);

}
