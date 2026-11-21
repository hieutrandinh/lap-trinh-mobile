package org.example.be.rest.usecase.notification.support;

import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.NotificationEntity;
import org.example.be.rest.dto.res.notification.NotificationRes;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationResAssembler {

    ObjectMapper objectMapper;

    public NotificationRes toRes(NotificationEntity notification) {
        Map<String, Object> payload = parsePayload(notification.getPayload());
        String[] titleAndBody = titleAndBody(notification.getNotificationType(), payload);
        return NotificationRes.builder()
                .id(notification.getId())
                .type(notification.getNotificationType())
                .title(titleAndBody[0])
                .body(titleAndBody[1])
                .payload(payload)
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
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

    /** title/body are generated here from notificationType + payload (12.2) - never stored separately. */
    private String[] titleAndBody(String type, Map<String, Object> payload) {
        return switch (type) {
            case "bucket_80" -> new String[]{"Bucket at 80%", "A spending bucket has reached 80% of its target."};
            case "bucket_exceeded" -> new String[]{"Bucket exceeded", "A spending bucket has gone over its target."};
            case "expense_added" -> new String[]{"New shared expense", "A shared expense was added to the group."};
            case "correction_requested" ->
                    new String[]{"Correction requested", "Someone requested to correct a shared expense."};
            case "correction_completed" ->
                    new String[]{"Correction completed", "A correction to a shared expense was applied."};
            case "correction_rejected" ->
                    new String[]{"Correction rejected", "A correction request was rejected."};
            case "repayment_pending" -> new String[]{"New repayment", "Someone recorded a repayment to you."};
            case "repayment_confirmed" -> new String[]{"Repayment confirmed", "Your repayment was confirmed."};
            case "repayment_rejected" -> new String[]{"Repayment rejected", "Your repayment was rejected."};
            case "reversal_requested" ->
                    new String[]{"Reversal requested", "Someone requested to reverse a confirmed repayment."};
            case "group_joined" -> new String[]{"Joined group", "You joined a group."};
            case "ownership_transferred" -> new String[]{"Ownership transferred", "You are now the group owner."};
            case "group_closed" -> new String[]{"Group closed", "A group you're in was closed."};
            default -> new String[]{type, ""};
        };
    }

}
