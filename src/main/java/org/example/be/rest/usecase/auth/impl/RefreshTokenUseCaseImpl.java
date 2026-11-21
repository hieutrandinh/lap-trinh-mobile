package org.example.be.rest.usecase.auth.impl;

import org.example.be.rest.usecase.auth.RefreshTokenUseCase;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.example.be.core.entity.DeviceEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.core.security.JwtService;
import org.example.be.core.security.RefreshTokenUtil;
import org.example.be.rest.dto.req.auth.RefreshTokenReq;
import org.example.be.rest.dto.res.auth.RefreshRes;
import org.example.be.rest.repository.DeviceJpaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RefreshTokenUseCaseImpl implements RefreshTokenUseCase {

    DeviceJpaRepository deviceJpaRepository;
    JwtService jwtService;

    @Value("${app.refresh-token.ttl-days:30}")
    @NonFinal
    long refreshTokenTtlDays;

    @Override
    @Transactional
    public RefreshRes execute(String deviceId, RefreshTokenReq req) {
        DeviceEntity device = deviceJpaRepository.findByIdForUpdate(deviceId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.INVALID_REFRESH_TOKEN));

        String hash = RefreshTokenUtil.hash(req.getRefreshToken());
        if (device.getRefreshTokenHash() == null
                || !device.getRefreshTokenHash().equals(hash)
                || device.getRefreshTokenExpiresAt() == null
                || device.getRefreshTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessLogicException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String newRefreshToken = RefreshTokenUtil.generate();
        device.setRefreshTokenHash(RefreshTokenUtil.hash(newRefreshToken));
        device.setRefreshTokenExpiresAt(LocalDateTime.now().plusDays(refreshTokenTtlDays));
        deviceJpaRepository.save(device);

        String accessToken = jwtService.generateAccessToken(device.getUserId(), deviceId);
        return RefreshRes.builder()
                .accessToken(accessToken)
                .accessTokenExpiresIn(jwtService.getAccessTokenExpiresIn())
                .refreshToken(newRefreshToken)
                .build();
    }

}
