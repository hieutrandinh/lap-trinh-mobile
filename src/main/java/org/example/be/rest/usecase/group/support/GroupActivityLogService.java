package org.example.be.rest.usecase.group.support;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.GroupActivityEntity;
import org.example.be.rest.repository.GroupActivityJpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupActivityLogService {

    GroupActivityJpaRepository groupActivityJpaRepository;

    public void log(String groupId, String activityType, String actorUserId) {
        log(groupId, activityType, null, null, "{}", actorUserId);
    }

    public void log(String groupId, String activityType, String targetType, String targetId, String payload,
                     String actorUserId) {
        GroupActivityEntity activity = new GroupActivityEntity();
        activity.setId(UUID.randomUUID().toString());
        activity.setGroupId(groupId);
        activity.setActivityType(activityType);
        activity.setTargetType(targetType);
        activity.setTargetId(targetId);
        activity.setPayload(payload == null ? "{}" : payload);
        activity.setCreatedAt(LocalDateTime.now());
        activity.setActorUserId(actorUserId);
        groupActivityJpaRepository.save(activity);
    }

}
