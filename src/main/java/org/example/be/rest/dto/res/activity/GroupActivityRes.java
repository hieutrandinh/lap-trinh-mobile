package org.example.be.rest.dto.res.activity;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.example.be.rest.dto.res.group.UserRefRes;

@Data
@Builder
public class GroupActivityRes {

    String id;
    String type;
    UserRefRes actor;
    String targetType;
    String targetId;
    String summary;
    Map<String, Object> payload;
    LocalDateTime createdAt;

}
