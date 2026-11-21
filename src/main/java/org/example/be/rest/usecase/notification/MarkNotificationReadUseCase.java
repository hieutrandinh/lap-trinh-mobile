package org.example.be.rest.usecase.notification;

public interface MarkNotificationReadUseCase {

    void execute(String userId, String notificationId);

}
