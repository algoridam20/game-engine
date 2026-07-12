package com.algoridam.games.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseEntity {
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  @Setter(AccessLevel.PROTECTED)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  @Setter(AccessLevel.PROTECTED)
  private LocalDateTime updatedAt;

  @Version
  @Column(name = "version", nullable = false)
  @Setter(AccessLevel.PROTECTED)
  private Long version;
}
