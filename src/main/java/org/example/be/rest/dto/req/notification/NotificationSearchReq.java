package org.example.be.rest.dto.req.notification;

import lombok.Data;

@Data
public class NotificationSearchReq {

    boolean unreadOnly = false;
    int page = 1;
    int pageSize = 20;

}
