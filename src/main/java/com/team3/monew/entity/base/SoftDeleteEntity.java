package com.team3.monew.entity.base;

import com.team3.monew.entity.enums.DeleteStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.time.Instant;

@MappedSuperclass
@Getter
public abstract class SoftDeleteEntity extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeleteStatus deleteStatus = DeleteStatus.ACTIVE;

    private Instant deletedAt;

    public boolean isDeleted() {
        return deleteStatus == DeleteStatus.DELETED;
    }
}
