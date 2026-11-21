package org.example.be.rest.usecase.auth.support;

import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.example.be.core.entity.DeviceEntity;
import org.example.be.core.security.RefreshTokenUtil;
import org.example.be.rest.repository.DeviceJpaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeviceSessionService {

    DeviceJpaRepository deviceJpaRepository;

    @Value("${app.refresh-token.ttl-days:30}")
    @NonFinal
    long refreshTokenTtlDays;

    public DeviceSessionService(DeviceJpaRepository deviceJpaRepository) {
        this.deviceJpaRepository = deviceJpaRepository;
    }

    public String issueRefreshToken(String deviceId, String platform, String userId) {
        DeviceEntity device = deviceJpaRepository.findByIdForUpdate(deviceId).orElseGet(DeviceEntity::new);
        device.setId(deviceId);
        device.setPlatform(platform);
        if (!Objects.equals(device.getUserId(), userId)) {
            device.setLastSyncAt(null);
            device.setSyncCursor(null);
        }
        device.setUserId(userId);
        device.setSignedOutAt(null);

        String refreshToken = RefreshTokenUtil.generate();
        device.setRefreshTokenHash(RefreshTokenUtil.hash(refreshToken));
        device.setRefreshTokenExpiresAt(LocalDateTime.now().plusDays(refreshTokenTtlDays));
        deviceJpaRepository.save(device);
        return refreshToken;
    }

}
