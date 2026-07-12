package com.algoridam.games.player.entity;

import com.algoridam.games.common.entity.BaseEntity;
import com.algoridam.games.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(
    name = "players",
    uniqueConstraints = @UniqueConstraint(name = "uk_players_handle", columnNames = "handle"))
@Check(name = "ck_players_handle_length", constraints = "char_length(handle) between 3 and 64")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerEntity extends BaseEntity {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "id", nullable = false, updatable = false, length = 36)
  private UUID id;

  @Column(name = "handle", nullable = false, length = 64)
  private String handle;

  @Column(name = "display_name", nullable = false, length = 128)
  private String displayName;

  public PlayerEntity(UUID id, String handle, String displayName) {
    this.id = id;
    this.handle = handle;
    this.displayName = displayName == null || displayName.isBlank() ? handle : displayName;
  }

  @PrePersist
  void prePersist() {
    if (id == null) {
      id = UuidV7Generator.next();
    }
  }
}
