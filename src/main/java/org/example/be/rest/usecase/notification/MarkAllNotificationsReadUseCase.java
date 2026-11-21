package org.example.be.rest.usecase.notification;

import org.example.be.rest.dto.res.notification.MarkAllReadRes;

public interface MarkAllNotificationsReadUseCase {

    MarkAllReadRes execute(String userId);

}
