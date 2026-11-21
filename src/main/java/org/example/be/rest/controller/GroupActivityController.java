package org.example.be.rest.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.util.ApiRes;
import org.example.be.rest.dto.req.activity.ActivitySearchReq;
import org.example.be.rest.dto.res.activity.GroupActivityListRes;
import org.example.be.rest.usecase.activity.SearchGroupActivityUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/groups/{groupId}/activities")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupActivityController {

    SearchGroupActivityUseCase searchGroupActivityUseCase;

    @GetMapping
    public ApiRes<GroupActivityListRes> search(@AuthenticationPrincipal Jwt jwt, @PathVariable String groupId,
                                                @ModelAttribute ActivitySearchReq req) {
        return ApiRes.success(HttpStatus.OK.value(),
                searchGroupActivityUseCase.execute(jwt.getSubject(), groupId, req));
    }

}
