package org.example.be.rest.usecase.notification;

import org.example.be.rest.dto.req.notification.UpdateNotificationSettingsReq;
import org.example.be.rest.dto.res.auth.NotificationSettingsRes;

public interface UpdateNotificationSettingsUseCase {

    NotificationSettingsRes execute(String userId, UpdateNotificationSettingsReq req);

}
