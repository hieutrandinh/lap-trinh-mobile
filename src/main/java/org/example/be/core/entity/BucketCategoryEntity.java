package org.example.be.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tblbucketcategory")
@IdClass(BucketCategoryId.class)
@Getter
@Setter
@NoArgsConstructor
public class BucketCategoryEntity {

    @Id
    @Column(name = "tblspendingbucketid", length = 36)
    String bucketId;

    @Id
    @Column(name = "tblcategoryid", length = 36)
    String categoryId;

    public BucketCategoryEntity(String bucketId, String categoryId) {
        this.bucketId = bucketId;
        this.categoryId = categoryId;
    }

}
