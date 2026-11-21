package org.example.be.rest.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.util.ApiRes;
import org.example.be.rest.dto.res.group.GroupRes;
import org.example.be.rest.usecase.group.AcceptInvitationUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/invitations")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InvitationController {

    AcceptInvitationUseCase acceptInvitationUseCase;

    @PostMapping("/{code}/accept")
    public ApiRes<GroupRes> accept(@AuthenticationPrincipal Jwt jwt, @PathVariable String code) {
        return ApiRes.success(HttpStatus.OK.value(), acceptInvitationUseCase.execute(jwt.getSubject(), code));
    }

}
