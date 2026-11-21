package org.example.be.rest.usecase.entry.impl;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.CategoryEntity;
import org.example.be.core.entity.EntryEntity;
import org.example.be.core.entity.PersonalEntryEntity;
import org.example.be.core.exception.BusinessLogicException;
import org.example.be.core.exception.ErrorCode;
import org.example.be.rest.dto.req.entry.CreateEntryReq;
import org.example.be.rest.dto.res.entry.CreateEntryResult;
import org.example.be.rest.repository.DeletionMarkerJpaRepository;
import org.example.be.rest.repository.EntryJpaRepository;
import org.example.be.rest.repository.PersonalEntryJpaRepository;
import org.example.be.rest.usecase.entry.CreateEntryUseCase;
import org.example.be.rest.usecase.PersonalRecordLocks;
import org.example.be.rest.usecase.entry.support.EntryAlertService;
import org.example.be.rest.usecase.entry.support.EntryBucketAssignmentService;
import org.example.be.rest.usecase.entry.support.EntryQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CreateEntryUseCaseImpl implements CreateEntryUseCase {

    EntryJpaRepository entryJpaRepository;
    PersonalEntryJpaRepository personalEntryJpaRepository;
    DeletionMarkerJpaRepository deletionMarkerJpaRepository;
    EntryBucketAssignmentService entryBucketAssignmentService;
    EntryAlertService entryAlertService;
    EntryQueryService entryQueryService;
    PersonalRecordLocks locks;

    @Override
    @Transactional
    public CreateEntryResult execute(String userId, CreateEntryReq req) {
        locks.lockCreation("entry", req.getId());
        var existingEntry = entryJpaRepository.findById(req.getId());
        if (existingEntry.isPresent()) {
            if (!userId.equals(existingEntry.get().getUserId())) throw new BusinessLogicException(ErrorCode.ID_ALREADY_EXISTS);
            return handleExisting(existingEntry.get(), req);
        }
        if (deletionMarkerJpaRepository.existsByUserIdAndRecordTypeAndRecordId(userId, "entry", req.getId())) {
            throw new BusinessLogicException(ErrorCode.RECORD_DELETED);
        }

        CategoryEntity category = entryBucketAssignmentService.requireCategory(userId, req.getCategoryId());
        LocalDateTime occurredAt = req.getOccurredAt() != null
                ? req.getOccurredAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime()
                : LocalDateTime.now();

        String bucketId;
        if ("income".equals(req.getType())) {
            if (req.getBucketId().isPresent() && req.getBucketId().get() != null) {
                throw new BusinessLogicException(ErrorCode.BUCKET_NOT_ALLOWED_FOR_INCOME);
            }
            bucketId = null;
        } else {
            bucketId = entryBucketAssignmentService.resolveExpenseBucketId(userId, category.getId(),
                    req.getBucketId());
        }

        LocalDateTime now = LocalDateTime.now();
        EntryEntity entry = new EntryEntity();
        entry.setId(req.getId());
        entry.setNote(req.getNote());
        entry.setVersion(1);
        entry.setCreatedAt(now);
        entry.setUpdatedAt(now);
        entry.setUserId(userId);
        entry.setCategoryId(category.getId());
        entry.setBucketId(bucketId);
        entryJpaRepository.save(entry);

        PersonalEntryEntity personal = new PersonalEntryEntity();
        personal.setEntryId(req.getId());
        personal.setEntryType(req.getType());
        personal.setName(req.getName().trim());
        personal.setAmount(req.getAmount());
        personal.setOccurredAt(occurredAt);
        personalEntryJpaRepository.save(personal);

        var alerts = "expense".equals(req.getType()) && bucketId != null
                ? entryAlertService.checkAndNotify(userId, bucketId)
                : java.util.List.<org.example.be.rest.dto.res.entry.AlertRes>of();

        var entryRes = entryQueryService.toRes(entry.getId());
        entryRes.setAlerts(alerts);
        return CreateEntryResult.builder().entry(entryRes).alreadyExisted(false).build();
    }

    private CreateEntryResult handleExisting(EntryEntity existing, CreateEntryReq req) {
        PersonalEntryEntity personal = personalEntryJpaRepository.findById(existing.getId()).orElse(null);
        boolean same = personal != null
                && Objects.equals(personal.getEntryType(), req.getType())
                && Objects.equals(personal.getName(), req.getName().trim())
                && personal.getAmount().compareTo(req.getAmount()) == 0
                && (req.getOccurredAt() == null || personal.getOccurredAt().equals(
                        req.getOccurredAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime()
                            .truncatedTo(java.time.temporal.ChronoUnit.MICROS)))
                && Objects.equals(existing.getCategoryId(), req.getCategoryId())
                && (!req.getBucketId().isPresent() || Objects.equals(existing.getBucketId(), req.getBucketId().get()))
                && Objects.equals(existing.getNote(), req.getNote());
        if (!same) {
            throw new BusinessLogicException(ErrorCode.ID_ALREADY_EXISTS);
        }
        return CreateEntryResult.builder().entry(entryQueryService.toRes(existing.getId())).alreadyExisted(true)
                .build();
    }

}
