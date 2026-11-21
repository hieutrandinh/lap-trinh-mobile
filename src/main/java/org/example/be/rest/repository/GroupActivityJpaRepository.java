package org.example.be.rest.repository;

import java.util.List;
import org.example.be.core.entity.GroupActivityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupActivityJpaRepository extends JpaRepository<GroupActivityEntity, String> {

    List<GroupActivityEntity> findByGroupIdOrderByCreatedAtDesc(String groupId);

}
