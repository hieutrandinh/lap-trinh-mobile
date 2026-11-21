package org.example.be.rest.usecase.me.impl;

import org.example.be.rest.usecase.me.GetMeUseCase;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.UserEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.dto.res.auth.MeRes;
import org.example.be.rest.repository.UserJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetMeUseCaseImpl implements GetMeUseCase {

    UserJpaRepository userJpaRepository;

    @Override
    @Transactional(readOnly = true)
    public MeRes execute(String userId) {
        UserEntity user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.USER_NOT_FOUND));
        return MeRes.from(user);
    }

}
