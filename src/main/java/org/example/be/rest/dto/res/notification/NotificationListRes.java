package org.example.be.rest.dto.res.notification;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.example.be.rest.dto.res.common.PaginationRes;

@Data
@Builder
public class NotificationListRes {

    List<NotificationRes> data;
    PaginationRes pagination;
    long unreadCount;

}
