package org.example.be.rest.dto.req.notification;

import lombok.Data;

@Data
public class UpdateNotificationSettingsReq {

    Boolean bucketAlerts;
    Boolean groupAlerts;

}
