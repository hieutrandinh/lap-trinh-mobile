package org.example.be.rest.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.util.ApiRes;
import org.example.be.rest.dto.req.group.CreateInvitationReq;
import org.example.be.rest.dto.res.group.InvitationListRes;
import org.example.be.rest.dto.res.group.InvitationRes;
import org.example.be.rest.usecase.group.CreateInvitationUseCase;
import org.example.be.rest.usecase.group.RevokeInvitationUseCase;
import org.example.be.rest.usecase.group.SearchInvitationUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/groups/{groupId}/invitations")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupInvitationController {

    CreateInvitationUseCase createInvitationUseCase;
    SearchInvitationUseCase searchInvitationUseCase;
    RevokeInvitationUseCase revokeInvitationUseCase;

    @PostMapping
    public ApiRes<InvitationRes> create(@AuthenticationPrincipal Jwt jwt, @PathVariable String groupId,
                                         @RequestBody(required = false) @Valid CreateInvitationReq req) {
        CreateInvitationReq body = req != null ? req : new CreateInvitationReq();
        return ApiRes.success(HttpStatus.CREATED.value(),
                createInvitationUseCase.execute(jwt.getSubject(), groupId, body));
    }

    @GetMapping
    public ApiRes<InvitationListRes> search(@AuthenticationPrincipal Jwt jwt, @PathVariable String groupId,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "20") int pageSize) {
        return ApiRes.success(HttpStatus.OK.value(),
                searchInvitationUseCase.execute(jwt.getSubject(), groupId, status, page, pageSize));
    }

    @DeleteMapping("/{invitationId}")
    public ResponseEntity<Void> revoke(@AuthenticationPrincipal Jwt jwt, @PathVariable String groupId,
                                        @PathVariable String invitationId) {
        revokeInvitationUseCase.execute(jwt.getSubject(), groupId, invitationId);
        return ResponseEntity.noContent().build();
    }

}
