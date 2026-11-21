package org.example.be.rest.usecase.entry.support;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.be.core.entity.CategoryEntity;
import org.example.be.core.entity.EntryDetailView;
import org.example.be.core.entity.SpendingBucketEntity;
import org.example.be.rest.dto.res.category.CategoryRefRes;
import org.example.be.rest.dto.res.entry.EntryRes;
import org.example.be.rest.dto.res.entry.SharedRefRes;
import org.example.be.rest.repository.CategoryJpaRepository;
import org.example.be.rest.repository.EntryDetailViewRepository;
import org.example.be.rest.repository.GroupExpenseJpaRepository;
import org.example.be.rest.repository.GroupJpaRepository;
import org.example.be.rest.repository.GroupMemberJpaRepository;
import org.example.be.rest.repository.SpendingBucketJpaRepository;
import org.example.be.rest.usecase.group.support.GroupAccessService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EntryQueryService {

    CategoryJpaRepository categoryJpaRepository;
    SpendingBucketJpaRepository spendingBucketJpaRepository;
    EntryDetailViewRepository entryDetailViewRepository;
    EntityManager entityManager;
    GroupJpaRepository groups;
    GroupMemberJpaRepository members;
    GroupExpenseJpaRepository expenses;
    GroupAccessService groupAccess;

    public EntryRes toRes(String entryId) {
        entityManager.flush();
        EntryDetailView view = entryDetailViewRepository.findById(entryId)
                .orElseThrow(() -> new IllegalStateException("Entry " + entryId + " not found right after write"));
        entityManager.refresh(view);
        return toRes(view);
    }

    public List<EntryRes> toRes(List<EntryDetailView> views) {
        List<String> categoryIds = views.stream().map(EntryDetailView::getCategoryId).distinct().toList();
        List<String> bucketIds = views.stream().map(EntryDetailView::getBucketId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        Map<String, CategoryEntity> categories = categoryJpaRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(CategoryEntity::getId, Function.identity()));
        Map<String, SpendingBucketEntity> buckets = bucketIds.isEmpty() ? Map.of()
                : spendingBucketJpaRepository.findAllById(bucketIds).stream()
                    .collect(Collectors.toMap(SpendingBucketEntity::getId, Function.identity()));

        return views.stream().map(v -> toRes(v, categories, buckets)).toList();
    }

    private EntryRes toRes(EntryDetailView view) {
        CategoryEntity category = categoryJpaRepository.findById(view.getCategoryId()).orElse(null);
        SpendingBucketEntity bucket = view.getBucketId() == null ? null
                : spendingBucketJpaRepository.findById(view.getBucketId()).orElse(null);
        return build(view, category, bucket);
    }

    private EntryRes toRes(EntryDetailView view, Map<String, CategoryEntity> categories,
                            Map<String, SpendingBucketEntity> buckets) {
        return build(view, categories.get(view.getCategoryId()),
                view.getBucketId() == null ? null : buckets.get(view.getBucketId()));
    }

    private EntryRes build(EntryDetailView view, CategoryEntity category, SpendingBucketEntity bucket) {
        CategoryRefRes categoryRef = category == null ? null
                : CategoryRefRes.builder().id(category.getId()).name(category.getName()).build();
        CategoryRefRes bucketRef = bucket == null ? null
                : CategoryRefRes.builder().id(bucket.getId()).name(bucket.getName()).build();
        SharedRefRes sharedRef = view.isShared() ? shared(view) : null;

        return EntryRes.builder()
                .id(view.getId())
                .type(view.getEntryType())
                .name(view.getName())
                .amount(view.getAmount())
                .currency("USD")
                .occurredAt(view.getOccurredAt())
                .category(categoryRef)
                .bucket(bucketRef)
                .note(view.getNote())
                .sharedFlag(view.isShared())
                .shared(sharedRef)
                .version(view.getVersion())
                .createdAt(view.getCreatedAt())
                .updatedAt(view.getUpdatedAt())
                .build();
    }

    private SharedRefRes shared(EntryDetailView view) {
        var group = groups.findById(view.getGroupId()).orElseThrow();
        var expense = expenses.findById(view.getGroupExpenseId()).orElseThrow();
        var membership = members.findByGroupIdAndUserId(group.getId(), view.getUserId()).orElse(null);
        boolean visible = membership != null && membership.getHiddenAt() == null;
        String name = membership != null && membership.getRetainedGroupName() != null
                ? membership.getRetainedGroupName() : group.getName();
        return SharedRefRes.builder().groupId(group.getId()).groupExpenseId(expense.getId()).groupName(name)
                .groupCategory(expense.getCategory()).totalAmount(expense.getTotalAmount()).canOpenGroup(visible)
                .accessMode(visible ? groupAccess.accessModeOf(group, membership) : "none").canDelete(!visible).build();
    }

}
