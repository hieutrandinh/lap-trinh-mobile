package org.example.be.rest.usecase.auth.impl;

import org.example.be.rest.usecase.auth.LogoutUseCase;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.security.RefreshTokenUtil;
import org.example.be.rest.dto.req.auth.LogoutReq;
import org.example.be.rest.repository.DeviceJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LogoutUseCaseImpl implements LogoutUseCase {

    DeviceJpaRepository deviceJpaRepository;

    @Override
    @Transactional
    public void execute(String userId, String deviceId, LogoutReq req) {
        String tokenHash = RefreshTokenUtil.hash(req.getRefreshToken());
        deviceJpaRepository.findByIdForUpdate(deviceId)
                .filter(device -> userId.equals(device.getUserId())
                        && tokenHash.equals(device.getRefreshTokenHash()))
                .ifPresent(device -> {
                    device.setRefreshTokenHash(null);
                    device.setRefreshTokenExpiresAt(null);
                    device.setSignedOutAt(LocalDateTime.now());
                });
    }

}
