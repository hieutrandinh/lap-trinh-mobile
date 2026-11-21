package org.example.be.rest.usecase.entry.impl;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.CategoryEntity;
import org.example.be.core.entity.EntryEntity;
import org.example.be.core.entity.PersonalEntryEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.core.util.JsonNullable;
import org.example.be.rest.dto.req.entry.UpdateEntryReq;
import org.example.be.rest.dto.res.entry.AlertRes;
import org.example.be.rest.dto.res.entry.EntryRes;
import org.example.be.rest.repository.EntryJpaRepository;
import org.example.be.rest.repository.PersonalEntryJpaRepository;
import org.example.be.rest.usecase.entry.UpdateEntryUseCase;
import org.example.be.rest.usecase.entry.support.EntryAlertService;
import org.example.be.rest.usecase.entry.support.EntryBucketAssignmentService;
import org.example.be.rest.usecase.entry.support.EntryQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UpdateEntryUseCaseImpl implements UpdateEntryUseCase {

    EntryJpaRepository entryJpaRepository;
    PersonalEntryJpaRepository personalEntryJpaRepository;
    EntryBucketAssignmentService entryBucketAssignmentService;
    EntryAlertService entryAlertService;
    EntryQueryService entryQueryService;

    @Override
    @Transactional
    public EntryRes execute(String userId, String entryId, UpdateEntryReq req) {
        EntryEntity entry = entryJpaRepository.findForUpdate(entryId, userId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.NOT_FOUND));
        if (entry.getVersion() != req.getVersion()) {
            throw new BusinessLogicException(ErrorCode.VERSION_CONFLICT, Map.of("current", entryQueryService.toRes(entryId)));
        }

        boolean shared = entry.getExpenseSplitId() != null;
        if (shared) {
            rejectOwnedOnlyFields(req);
        }

        LocalDateTime now = LocalDateTime.now();
        boolean categoryChanged = req.getCategoryId() != null && !req.getCategoryId().equals(entry.getCategoryId());
        String effectiveCategoryId = entry.getCategoryId();
        if (categoryChanged) {
            CategoryEntity category = entryBucketAssignmentService.requireCategory(userId, req.getCategoryId());
            effectiveCategoryId = category.getId();
            entry.setCategoryId(effectiveCategoryId);
        }

        PersonalEntryEntity personal = shared ? null
                : personalEntryJpaRepository.findById(entryId).orElseThrow(
                        () -> new IllegalStateException("Regular entry " + entryId + " missing personal row"));
        String effectiveType = shared ? "expense"
                : (req.getType() != null ? req.getType() : personal.getEntryType());

        if (!shared) {
            if (req.getName() != null) {
                personal.setName(req.getName().trim());
            }
            if (req.getAmount() != null) {
                personal.setAmount(req.getAmount());
            }
            if (req.getOccurredAt() != null) {
                personal.setOccurredAt(req.getOccurredAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime());
            }
            personal.setEntryType(effectiveType);
        }

        applyBucketRules(userId, entry, effectiveCategoryId, effectiveType, req.getBucketId(), categoryChanged);

        if (req.getNote().isPresent()) {
            entry.setNote(req.getNote().get());
        }

        entry.setVersion(entry.getVersion() + 1);
        entry.setUpdatedAt(now);
        entryJpaRepository.save(entry);
        if (!shared) {
            personalEntryJpaRepository.save(personal);
        }

        List<AlertRes> alerts = "expense".equals(effectiveType) && entry.getBucketId() != null
                ? entryAlertService.checkAndNotify(userId, entry.getBucketId())
                : List.of();

        EntryRes entryRes = entryQueryService.toRes(entryId);
        entryRes.setAlerts(alerts);
        return entryRes;
    }

    private void rejectOwnedOnlyFields(UpdateEntryReq req) {
        List<String> offending = new ArrayList<>();
        if (req.getName() != null) {
            offending.add("name");
        }
        if (req.getAmount() != null) {
            offending.add("amount");
        }
        if (req.getOccurredAt() != null) {
            offending.add("occurredAt");
        }
        if (req.getType() != null) {
            offending.add("type");
        }
        if (!offending.isEmpty()) {
            throw new BusinessLogicException(ErrorCode.SHARED_ENTRY_READ_ONLY, Map.of("fields", offending));
        }
    }

    private void applyBucketRules(String userId, EntryEntity entry, String categoryId, String effectiveType,
                                   JsonNullable<String> bucketIdField, boolean categoryChanged) {
        if ("income".equals(effectiveType)) {
            if (bucketIdField.isPresent() && bucketIdField.get() != null) {
                throw new BusinessLogicException(ErrorCode.BUCKET_NOT_ALLOWED_FOR_INCOME);
            }
            entry.setBucketId(null);
            return;
        }
        if (bucketIdField.isPresent()) {
            entry.setBucketId(entryBucketAssignmentService.resolveExpenseBucketId(userId, categoryId, bucketIdField));
        } else if (categoryChanged) {
            entry.setBucketId(entryBucketAssignmentService.resolveExpenseBucketId(userId, categoryId,
                    JsonNullable.undefined()));
        }
    }

}
