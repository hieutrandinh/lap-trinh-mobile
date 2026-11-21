package org.example.be.rest.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.util.ApiRes;
import org.example.be.rest.dto.req.group.CreateGroupReq;
import org.example.be.rest.dto.req.group.GroupSearchReq;
import org.example.be.rest.dto.req.group.TransferOwnershipReq;
import org.example.be.rest.dto.req.group.UpdateGroupReq;
import org.example.be.rest.dto.res.group.GroupListRes;
import org.example.be.rest.dto.res.group.GroupRes;
import org.example.be.rest.usecase.group.CloseGroupUseCase;
import org.example.be.rest.usecase.group.CreateGroupUseCase;
import org.example.be.rest.usecase.group.DetailGroupUseCase;
import org.example.be.rest.usecase.group.LeaveGroupUseCase;
import org.example.be.rest.usecase.group.SearchGroupUseCase;
import org.example.be.rest.usecase.group.TransferOwnershipUseCase;
import org.example.be.rest.usecase.group.UpdateGroupUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupController {

    CreateGroupUseCase createGroupUseCase;
    SearchGroupUseCase searchGroupUseCase;
    DetailGroupUseCase detailGroupUseCase;
    UpdateGroupUseCase updateGroupUseCase;
    TransferOwnershipUseCase transferOwnershipUseCase;
    LeaveGroupUseCase leaveGroupUseCase;
    CloseGroupUseCase closeGroupUseCase;

    @GetMapping
    public ApiRes<GroupListRes> search(@AuthenticationPrincipal Jwt jwt, @ModelAttribute GroupSearchReq req) {
        return ApiRes.success(HttpStatus.OK.value(), searchGroupUseCase.execute(jwt.getSubject(), req));
    }

    @PostMapping
    public ApiRes<GroupRes> create(@AuthenticationPrincipal Jwt jwt, @RequestBody @Valid CreateGroupReq req) {
        return ApiRes.success(HttpStatus.CREATED.value(), createGroupUseCase.execute(jwt.getSubject(), req));
    }

    @GetMapping("/{groupId}")
    public ApiRes<GroupRes> detail(@AuthenticationPrincipal Jwt jwt, @PathVariable String groupId) {
        return ApiRes.success(HttpStatus.OK.value(), detailGroupUseCase.execute(jwt.getSubject(), groupId));
    }

    @PatchMapping("/{groupId}")
    public ApiRes<GroupRes> update(@AuthenticationPrincipal Jwt jwt, @PathVariable String groupId,
                                    @RequestBody @Valid UpdateGroupReq req) {
        return ApiRes.success(HttpStatus.OK.value(), updateGroupUseCase.execute(jwt.getSubject(), groupId, req));
    }

    @PostMapping("/{groupId}/transfer-ownership")
    public ApiRes<GroupRes> transferOwnership(@AuthenticationPrincipal Jwt jwt, @PathVariable String groupId,
                                               @RequestBody @Valid TransferOwnershipReq req) {
        return ApiRes.success(HttpStatus.OK.value(),
                transferOwnershipUseCase.execute(jwt.getSubject(), groupId, req));
    }

    @PostMapping("/{groupId}/leave")
    public ApiRes<GroupRes> leave(@AuthenticationPrincipal Jwt jwt, @PathVariable String groupId) {
        return ApiRes.success(HttpStatus.OK.value(), leaveGroupUseCase.execute(jwt.getSubject(), groupId));
    }

    @PostMapping("/{groupId}/close")
    public ApiRes<GroupRes> close(@AuthenticationPrincipal Jwt jwt, @PathVariable String groupId) {
        return ApiRes.success(HttpStatus.OK.value(), closeGroupUseCase.execute(jwt.getSubject(), groupId));
    }

}
