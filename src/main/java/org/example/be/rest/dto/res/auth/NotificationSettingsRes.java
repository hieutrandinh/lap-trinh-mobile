package org.example.be.rest.dto.res.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationSettingsRes {

    boolean bucketAlerts;
    boolean groupAlerts;

}
