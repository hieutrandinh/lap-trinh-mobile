package org.example.be.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tblpersonalentry")
@Getter
@Setter
@NoArgsConstructor
public class PersonalEntryEntity {

    @Id
    @Column(name = "tblentryid", length = 36)
    String entryId;

    @Column(name = "entrytype", nullable = false, length = 10)
    String entryType;

    @Column(name = "name", nullable = false, length = 100)
    String name;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    BigDecimal amount;

    @Column(name = "occurredat", nullable = false)
    LocalDateTime occurredAt;

}
