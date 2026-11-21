package org.example.be.rest.usecase.auth.impl;

import org.example.be.rest.usecase.auth.RegisterUseCase;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.CategoryEntity;
import org.example.be.core.entity.UserEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.core.security.JwtService;
import org.example.be.rest.dto.req.auth.RegisterReq;
import org.example.be.rest.dto.res.auth.AuthRes;
import org.example.be.rest.repository.CategoryJpaRepository;
import org.example.be.rest.repository.UserJpaRepository;
import org.example.be.rest.usecase.auth.support.DeviceSessionService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RegisterUseCaseImpl implements RegisterUseCase {

    private static final List<String> DEFAULT_CATEGORIES = List.of("Food", "Transport", "Shopping", "Bills", "Salary");

    UserJpaRepository userJpaRepository;
    CategoryJpaRepository categoryJpaRepository;
    PasswordEncoder passwordEncoder;
    JwtService jwtService;
    DeviceSessionService deviceSessionService;

    @Override
    @Transactional
    public AuthRes execute(String deviceId, RegisterReq req) {
        if (userJpaRepository.existsByEmailIgnoreCase(req.getEmail())) {
            throw new BusinessLogicException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }

        LocalDateTime now = LocalDateTime.now();
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setDisplayName(req.getDisplayName());
        user.setTimezone(resolveTimezone(req.getTimezone()));
        user.setNotifyBucketAlerts(true);
        user.setNotifyGroupAlerts(true);
        user.setCreatedAt(now);
        userJpaRepository.save(user);

        seedDefaultCategories(user.getId(), now);

        String refreshToken = deviceSessionService.issueRefreshToken(deviceId, req.getPlatform(), user.getId());
        String accessToken = jwtService.generateAccessToken(user.getId(), deviceId);
        return AuthRes.of(user, accessToken, jwtService.getAccessTokenExpiresIn(), refreshToken);
    }

    private String resolveTimezone(String timezone) {
        if (!StringUtils.hasText(timezone)) {
            return "UTC";
        }
        try {
            ZoneId.of(timezone);
        } catch (Exception e) {
            throw new BusinessLogicException(ErrorCode.VALIDATION_ERROR);
        }
        return timezone;
    }

    private void seedDefaultCategories(String userId, LocalDateTime now) {
        List<CategoryEntity> categories = new ArrayList<>();
        categories.add(newCategory("Uncategorized", true, userId, now));
        DEFAULT_CATEGORIES.forEach(name -> categories.add(newCategory(name, false, userId, now)));
        categoryJpaRepository.saveAll(categories);
    }

    private CategoryEntity newCategory(String name, boolean isSystem, String userId, LocalDateTime now) {
        CategoryEntity category = new CategoryEntity();
        category.setId(UUID.randomUUID().toString());
        category.setName(name);
        category.setSystem(isSystem);
        category.setVersion(1);
        category.setCreatedAt(now);
        category.setUpdatedAt(now);
        category.setUserId(userId);
        return category;
    }

}
