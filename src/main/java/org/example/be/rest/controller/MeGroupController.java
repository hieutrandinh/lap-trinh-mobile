package org.example.be.rest.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.rest.usecase.group.RemoveGroupFromMyListUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me/groups")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MeGroupController {

    RemoveGroupFromMyListUseCase removeGroupFromMyListUseCase;

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal Jwt jwt, @PathVariable String groupId) {
        removeGroupFromMyListUseCase.execute(jwt.getSubject(), groupId);
        return ResponseEntity.noContent().build();
    }

}
