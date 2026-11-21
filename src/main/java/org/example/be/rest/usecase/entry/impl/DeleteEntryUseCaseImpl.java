package org.example.be.rest.usecase.entry.impl;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.DeletionMarkerEntity;
import org.example.be.core.entity.EntryEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.repository.DeletionMarkerJpaRepository;
import org.example.be.rest.repository.EntryJpaRepository;
import org.example.be.rest.repository.PersonalEntryJpaRepository;
import org.example.be.rest.usecase.entry.DeleteEntryUseCase;
import org.example.be.rest.usecase.entry.support.EntryQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeleteEntryUseCaseImpl implements DeleteEntryUseCase {

    private static final long DELETION_MARKER_TTL_DAYS = 30;

    EntryJpaRepository entryJpaRepository;
    PersonalEntryJpaRepository personalEntryJpaRepository;
    DeletionMarkerJpaRepository deletionMarkerJpaRepository;
    EntryQueryService entryQuery;

    @Override
    @Transactional
    public void execute(String userId, String entryId, int version) {
        EntryEntity entry = entryJpaRepository.findForUpdate(entryId, userId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.NOT_FOUND));
        if (entry.getVersion() != version) {
            throw new BusinessLogicException(ErrorCode.VERSION_CONFLICT, Map.of("current", entryQuery.toRes(entryId)));
        }
        if (entry.getExpenseSplitId() != null && entryQuery.toRes(entryId).getShared().isCanOpenGroup()) {
            throw new BusinessLogicException(ErrorCode.SHARED_ENTRY_GROUP_IN_VIEW);
        }

        if (entry.getExpenseSplitId() == null) {
            personalEntryJpaRepository.deleteById(entryId);
        }
        entryJpaRepository.delete(entry);

        LocalDateTime now = LocalDateTime.now();
        DeletionMarkerEntity marker = new DeletionMarkerEntity();
        marker.setId(UUID.randomUUID().toString());
        marker.setRecordType("entry");
        marker.setRecordId(entryId);
        marker.setDeletedAt(now);
        marker.setExpiresAt(now.plusDays(DELETION_MARKER_TTL_DAYS));
        marker.setUserId(userId);
        deletionMarkerJpaRepository.save(marker);
    }

}
