package org.example.be.rest.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.util.ApiRes;
import org.example.be.rest.dto.req.group.ChangeRoleReq;
import org.example.be.rest.dto.res.group.GroupMemberRes;
import org.example.be.rest.usecase.group.ChangeMemberRoleUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/groups/{groupId}/members")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupMemberController {

    ChangeMemberRoleUseCase changeMemberRoleUseCase;

    @PatchMapping("/{userId}")
    public ApiRes<GroupMemberRes> changeRole(@AuthenticationPrincipal Jwt jwt, @PathVariable String groupId,
                                              @PathVariable String userId, @RequestBody @Valid ChangeRoleReq req) {
        return ApiRes.success(HttpStatus.OK.value(),
                changeMemberRoleUseCase.execute(jwt.getSubject(), groupId, userId, req));
    }

}
