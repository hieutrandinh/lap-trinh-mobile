package org.example.be.rest.usecase.activity.support;

import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.GroupActivityEntity;
import org.example.be.rest.dto.res.activity.GroupActivityRes;
import org.example.be.rest.dto.res.group.UserRefRes;
import org.example.be.rest.repository.UserJpaRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupActivityResAssembler {

    UserJpaRepository userJpaRepository;
    ObjectMapper objectMapper;

    /** category (12.1) is derived from the activityType prefix, never stored separately. */
    public static String categoryOf(String activityType) {
        if (activityType.startsWith("expense_")) {
            return "expense";
        }
        if (activityType.startsWith("correction_")) {
            return "correction";
        }
        if (activityType.startsWith("repayment_") || activityType.startsWith("reversal_")) {
            return "repayment";
        }
        if (activityType.startsWith("member_")) {
            return "member";
        }
        return "other";
    }

    public GroupActivityRes toRes(GroupActivityEntity activity) {
        UserRefRes actor = activity.getActorUserId() == null ? null : toRef(activity.getActorUserId());
        return GroupActivityRes.builder()
                .id(activity.getId())
                .type(activity.getActivityType())
                .actor(actor)
                .targetType(activity.getTargetType())
                .targetId(activity.getTargetId())
                .summary(summarize(activity.getActivityType(), actor))
                .payload(parsePayload(activity.getPayload()))
                .createdAt(activity.getCreatedAt())
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parsePayload(String payload) {
        try {
            return objectMapper.readValue(payload, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String summarize(String activityType, UserRefRes actor) {
        String name = actor != null && actor.getDisplayName() != null ? actor.getDisplayName() : "Someone";
        return switch (activityType) {
            case "group_created" -> name + " created the group";
            case "member_joined" -> name + " joined the group";
            case "member_left" -> name + " left the group";
            case "ownership_transferred" -> name + " transferred ownership";
            case "group_closed" -> name + " closed the group";
            case "expense_created" -> name + " added a shared expense";
            case "correction_requested" -> name + " requested a correction";
            case "correction_completed" -> "A correction was completed";
            case "correction_rejected" -> name + " rejected a correction";
            case "repayment_confirmed" -> name + " confirmed a repayment";
            default -> activityType;
        };
    }

    private UserRefRes toRef(String userId) {
        return userJpaRepository.findById(userId)
                .map(u -> UserRefRes.builder().userId(u.getId()).displayName(u.getDisplayName()).build())
                .orElse(UserRefRes.builder().userId(userId).displayName(null).build());
    }

}
