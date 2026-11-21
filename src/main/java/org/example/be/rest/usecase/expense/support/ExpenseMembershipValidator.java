package org.example.be.rest.usecase.expense.support;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.dto.req.expense.ParticipantReq;
import org.example.be.rest.repository.GroupMemberJpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExpenseMembershipValidator {

    GroupMemberJpaRepository groupMemberJpaRepository;

    public void validate(String groupId, String payerUserId, List<ParticipantReq> participants) {
        if (participants == null || participants.isEmpty()) {
            throw new BusinessLogicException(ErrorCode.PARTICIPANTS_REQUIRED);
        }

        Set<String> seen = new HashSet<>();
        for (ParticipantReq p : participants) {
            if (!seen.add(p.getUserId())) {
                throw new BusinessLogicException(ErrorCode.DUPLICATE_PARTICIPANT);
            }
        }

        requireActiveMember(groupId, payerUserId, "payerUserId");
        for (ParticipantReq p : participants) {
            requireActiveMember(groupId, p.getUserId(), "participants.userId");
        }
    }

    private void requireActiveMember(String groupId, String userId, String field) {
        boolean active = groupMemberJpaRepository.findByGroupIdAndUserId(groupId, userId)
                .map(m -> "active".equals(m.getStatus()))
                .orElse(false);
        if (!active) {
            throw new BusinessLogicException(ErrorCode.NOT_ACTIVE_MEMBER, Map.of("field", field, "userId", userId));
        }
    }

}
