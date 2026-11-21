package org.example.be.rest.usecase.notification;

import org.example.be.rest.dto.req.notification.NotificationSearchReq;
import org.example.be.rest.dto.res.notification.NotificationListRes;

public interface SearchNotificationUseCase {

    NotificationListRes execute(String userId, NotificationSearchReq req);

}
