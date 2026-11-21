package org.example.be.rest.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.util.ApiRes;
import org.example.be.rest.dto.req.entry.CreateEntryReq;
import org.example.be.rest.dto.req.entry.EntrySearchReq;
import org.example.be.rest.dto.req.entry.UpdateEntryReq;
import org.example.be.rest.dto.res.entry.EntryListRes;
import org.example.be.rest.dto.res.entry.EntryRes;
import org.example.be.rest.usecase.entry.CreateEntryUseCase;
import org.example.be.rest.usecase.entry.DeleteEntryUseCase;
import org.example.be.rest.usecase.entry.SearchEntryUseCase;
import org.example.be.rest.usecase.entry.UpdateEntryUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/entries")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EntryController {

    CreateEntryUseCase createEntryUseCase;
    SearchEntryUseCase searchEntryUseCase;
    UpdateEntryUseCase updateEntryUseCase;
    DeleteEntryUseCase deleteEntryUseCase;

    @GetMapping
    public ApiRes<EntryListRes> search(@AuthenticationPrincipal Jwt jwt, @ModelAttribute EntrySearchReq req) {
        return ApiRes.success(HttpStatus.OK.value(), searchEntryUseCase.execute(jwt.getSubject(), req));
    }

    @PostMapping
    public ApiRes<EntryRes> create(@AuthenticationPrincipal Jwt jwt, @RequestBody @Valid CreateEntryReq req) {
        var result = createEntryUseCase.execute(jwt.getSubject(), req);
        int status = result.isAlreadyExisted() ? HttpStatus.OK.value() : HttpStatus.CREATED.value();
        return ApiRes.success(status, result.getEntry());
    }

    @PatchMapping("/{entryId}")
    public ApiRes<EntryRes> update(@AuthenticationPrincipal Jwt jwt, @PathVariable String entryId,
                                    @RequestBody @Valid UpdateEntryReq req) {
        return ApiRes.success(HttpStatus.OK.value(), updateEntryUseCase.execute(jwt.getSubject(), entryId, req));
    }

    @DeleteMapping("/{entryId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable String entryId,
                                        @RequestParam int version) {
        deleteEntryUseCase.execute(jwt.getSubject(), entryId, version);
        return ResponseEntity.noContent().build();
    }

}
