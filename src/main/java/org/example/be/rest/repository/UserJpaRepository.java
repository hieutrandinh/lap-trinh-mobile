package org.example.be.rest.repository;

import java.util.Optional;
import org.example.be.core.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserEntity, String> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<UserEntity> findByEmailIgnoreCase(String email);

}
