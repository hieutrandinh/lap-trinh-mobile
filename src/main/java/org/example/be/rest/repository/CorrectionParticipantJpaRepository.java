package org.example.be.rest.repository;

import java.util.List;
import java.util.Optional;
import org.example.be.core.entity.CorrectionParticipantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorrectionParticipantJpaRepository extends JpaRepository<CorrectionParticipantEntity, String> {

    List<CorrectionParticipantEntity> findByCorrectionRequestId(String correctionRequestId);

    Optional<CorrectionParticipantEntity> findByCorrectionRequestIdAndUserId(String correctionRequestId,
                                                                               String userId);

}
