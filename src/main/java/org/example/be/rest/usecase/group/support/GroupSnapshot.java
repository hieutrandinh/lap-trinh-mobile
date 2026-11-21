package org.example.be.rest.usecase.group.support;

import java.math.BigDecimal;
import java.util.List;
import org.example.be.core.entity.GroupMemberEntity;
import org.example.be.rest.dto.res.expense.GroupExpenseRes;
import org.example.be.rest.dto.res.group.GroupRes;
import org.example.be.rest.dto.res.group.PendingActionsRes;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class GroupSnapshot {

    // Snapshot storage is independent of the public API's money and date formats.
    private final JsonMapper mapper = JsonMapper.builder().build();

    public record Contents(GroupRes group, List<GroupExpenseRes> expenses) {
    }

    public String write(GroupRes group, List<GroupExpenseRes> expenses) {
        return mapper.writeValueAsString(new Contents(group, expenses));
    }

    public Contents read(GroupAccessService.Access access) {
        GroupMemberEntity membership = access.membership();
        if (membership.getSnapshotData() != null) {
            var tree = mapper.readTree(membership.getSnapshotData());
            if (tree.has("group")) {
                return mapper.treeToValue(tree, Contents.class);
            }
        }

        // Older snapshots only captured the settled balance and group name.
        GroupRes legacy = GroupRes.builder()
                .id(membership.getGroupId())
                .name(membership.getRetainedGroupName())
                .status("active")
                .accessMode("snapshot")
                .myRole(membership.getRole())
                .myBalance(BigDecimal.ZERO)
                .currency("USD")
                .createdAt(access.group().getCreatedAt())
                .snapshotCapturedAt(membership.getSnapshotCapturedAt())
                .members(List.of())
                .pendingActions(PendingActionsRes.builder().build())
                .expenseCategories(List.of())
                .build();
        return new Contents(legacy, List.of());
    }

    public boolean appliesTo(GroupAccessService.Access access) {
        return "left".equals(access.membership().getStatus());
    }
}
