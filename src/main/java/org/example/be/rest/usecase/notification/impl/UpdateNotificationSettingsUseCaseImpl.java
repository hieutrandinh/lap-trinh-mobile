package org.example.be.rest.usecase.notification.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.UserEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.dto.req.notification.UpdateNotificationSettingsReq;
import org.example.be.rest.dto.res.auth.NotificationSettingsRes;
import org.example.be.rest.repository.UserJpaRepository;
import org.example.be.rest.usecase.notification.UpdateNotificationSettingsUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UpdateNotificationSettingsUseCaseImpl implements UpdateNotificationSettingsUseCase {

    UserJpaRepository userJpaRepository;

    @Override
    @Transactional
    public NotificationSettingsRes execute(String userId, UpdateNotificationSettingsReq req) {
        if (req.getBucketAlerts() == null && req.getGroupAlerts() == null) {
            throw new BusinessLogicException(ErrorCode.VALIDATION_ERROR);
        }
        UserEntity user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.USER_NOT_FOUND));

        if (req.getBucketAlerts() != null) {
            user.setNotifyBucketAlerts(req.getBucketAlerts());
        }
        if (req.getGroupAlerts() != null) {
            user.setNotifyGroupAlerts(req.getGroupAlerts());
        }
        userJpaRepository.save(user);

        return NotificationSettingsRes.builder()
                .bucketAlerts(user.isNotifyBucketAlerts())
                .groupAlerts(user.isNotifyGroupAlerts())
                .build();
    }

}
