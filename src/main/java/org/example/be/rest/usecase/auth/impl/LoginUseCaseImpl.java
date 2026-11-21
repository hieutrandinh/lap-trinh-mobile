package org.example.be.rest.usecase.auth.impl;

import org.example.be.rest.usecase.auth.LoginUseCase;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.UserEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.core.security.JwtService;
import org.example.be.rest.dto.req.auth.LoginReq;
import org.example.be.rest.dto.res.auth.AuthRes;
import org.example.be.rest.dto.res.auth.OnlineSummaryRes;
import org.example.be.rest.repository.CategoryJpaRepository;
import org.example.be.rest.repository.EntryJpaRepository;
import org.example.be.rest.repository.SpendingBucketJpaRepository;
import org.example.be.rest.repository.UserJpaRepository;
import org.example.be.rest.usecase.auth.support.DeviceSessionService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LoginUseCaseImpl implements LoginUseCase {

    UserJpaRepository userJpaRepository;
    EntryJpaRepository entryJpaRepository;
    CategoryJpaRepository categoryJpaRepository;
    SpendingBucketJpaRepository spendingBucketJpaRepository;
    PasswordEncoder passwordEncoder;
    JwtService jwtService;
    DeviceSessionService deviceSessionService;

    @Override
    // Invalid credentials are raised before any account or device writes. The surrounding throttle commits the attempt.
    @Transactional(noRollbackFor = BusinessLogicException.class)
    public AuthRes execute(String deviceId, LoginReq req) {
        UserEntity user = userJpaRepository.findByEmailIgnoreCase(req.getEmail())
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BusinessLogicException(ErrorCode.INVALID_CREDENTIALS);
        }

        String refreshToken = deviceSessionService.issueRefreshToken(deviceId, req.getPlatform(), user.getId());
        String accessToken = jwtService.generateAccessToken(user.getId(), deviceId);
        AuthRes res = AuthRes.of(user, accessToken, jwtService.getAccessTokenExpiresIn(), refreshToken);
        res.setOnlineSummary(OnlineSummaryRes.builder()
                .entryCount(entryJpaRepository.countByUserId(user.getId()))
                .categoryCount(categoryJpaRepository.countByUserId(user.getId()))
                .bucketCount(spendingBucketJpaRepository.countByUserId(user.getId()))
                .build());
        return res;
    }

}
