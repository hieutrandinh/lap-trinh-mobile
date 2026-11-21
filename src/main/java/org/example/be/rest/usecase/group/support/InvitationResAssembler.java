package org.example.be.rest.usecase.group.support;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.InvitationEntity;
import org.example.be.rest.dto.res.group.InvitationRes;
import org.example.be.rest.dto.res.group.UserRefRes;
import org.example.be.rest.repository.UserJpaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InvitationResAssembler {

    UserJpaRepository userJpaRepository;

    @Value("${app.invitation.link-base-url}")
    @lombok.experimental.NonFinal
    String linkBaseUrl;

    /** "expired" is derived (expiresAt passed, never revoked) - tblInvitation.status only stores active/revoked. */
    public String displayStatus(InvitationEntity invitation) {
        if ("revoked".equals(invitation.getStatus())) {
            return "revoked";
        }
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            return "expired";
        }
        return "active";
    }

    public InvitationRes toRes(InvitationEntity invitation) {
        UserRefRes createdBy = userJpaRepository.findById(invitation.getCreatorUserId())
                .map(u -> UserRefRes.builder().userId(u.getId()).displayName(u.getDisplayName()).build())
                .orElse(null);
        return InvitationRes.builder()
                .id(invitation.getId())
                .code(invitation.getCode())
                .link(linkBaseUrl + "/" + invitation.getCode())
                .expiresAt(invitation.getExpiresAt())
                .maxUses(invitation.getMaxUses())
                .useCount(invitation.getUseCount())
                .status(displayStatus(invitation))
                .createdBy(createdBy)
                .createdAt(invitation.getCreatedAt())
                .build();
    }

}
