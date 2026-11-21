package org.example.be.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tblbucketalert")
@Getter
@Setter
@NoArgsConstructor
public class BucketAlertEntity {

    @Id
    @Column(name = "id", length = 36)
    String id;

    @Column(name = "month", nullable = false, length = 7)
    String month;

    @Column(name = "threshold", nullable = false)
    int threshold;

    @Column(name = "sentat", nullable = false)
    LocalDateTime sentAt;

    @Column(name = "tblspendingbucketid", nullable = false, length = 36)
    String bucketId;

}
