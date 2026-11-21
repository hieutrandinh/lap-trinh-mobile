package org.example.be.rest.usecase.group.support;

import java.security.SecureRandom;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.rest.repository.InvitationJpaRepository;
import org.springframework.stereotype.Service;

/** Matches ckInvitationCode: ^[A-Z0-9]{8}$. */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InvitationCodeGenerator {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    InvitationJpaRepository invitationJpaRepository;

    public String generate() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(LENGTH);
            for (int i = 0; i < LENGTH; i++) {
                sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            }
            code = sb.toString();
        } while (invitationJpaRepository.existsByCode(code));
        return code;
    }

}
