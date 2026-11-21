package org.example.be.rest.usecase.notification.impl;

import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.NotificationEntity;
import org.example.be.rest.dto.req.notification.NotificationSearchReq;
import org.example.be.rest.dto.res.common.PaginationRes;
import org.example.be.rest.dto.res.notification.NotificationListRes;
import org.example.be.rest.dto.res.notification.NotificationRes;
import org.example.be.rest.repository.NotificationJpaRepository;
import org.example.be.rest.usecase.notification.SearchNotificationUseCase;
import org.example.be.rest.usecase.notification.support.NotificationResAssembler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SearchNotificationUseCaseImpl implements SearchNotificationUseCase {

    NotificationJpaRepository notificationJpaRepository;
    NotificationResAssembler notificationResAssembler;

    @Override
    @Transactional(readOnly = true)
    public NotificationListRes execute(String userId, NotificationSearchReq req) {
        List<NotificationEntity> all = req.isUnreadOnly()
                ? notificationJpaRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId)
                : notificationJpaRepository.findByUserIdOrderByCreatedAtDesc(userId);

        int page = Math.max(req.getPage(), 1);
        int pageSize = req.getPageSize() < 1 || req.getPageSize() > 100 ? 20 : req.getPageSize();
        int from = Math.min((page - 1) * pageSize, all.size());
        int to = Math.min(from + pageSize, all.size());

        List<NotificationRes> data = all.subList(from, to).stream()
                .map(notificationResAssembler::toRes)
                .toList();

        int totalPages = pageSize == 0 ? 0 : (int) Math.ceil(all.size() / (double) pageSize);
        return NotificationListRes.builder()
                .data(data)
                .pagination(PaginationRes.builder()
                        .page(page).pageSize(pageSize).totalItems(all.size()).totalPages(totalPages)
                        .build())
                .unreadCount(notificationJpaRepository.countByUserIdAndReadAtIsNull(userId))
                .build();
    }

}
