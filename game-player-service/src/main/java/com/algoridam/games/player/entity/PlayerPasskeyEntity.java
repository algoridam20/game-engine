package com.algoridam.games.player.entity;

import com.algoridam.games.common.entity.BaseEntity;
import com.algoridam.games.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.security.web.webauthn.api.AuthenticatorTransport;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.api.ImmutableCredentialRecord;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCose;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialType;

@Getter
@Entity
@Table(
    name = "player_passkeys",
    uniqueConstraints = {
      @UniqueConstraint(name = "uk_player_passkeys_credential_id", columnNames = "credential_id"),
      @UniqueConstraint(
          name = "uk_player_passkeys_label",
          columnNames = {"player_id", "label"})
    })
@Check(name = "ck_player_passkeys_signature_count", constraints = "signature_count >= 0")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerPasskeyEntity extends BaseEntity {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "id", nullable = false, updatable = false, length = 36)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "player_id",
      nullable = false,
      updatable = false,
      foreignKey = @ForeignKey(name = "fk_player_passkeys_player"))
  private PlayerEntity player;

  @Column(name = "credential_id", nullable = false, length = 512)
  private String credentialId;

  @Column(name = "credential_type", nullable = false, length = 32)
  private String credentialType;

  @Lob
  @Column(name = "public_key", nullable = false)
  private byte[] publicKey;

  @Column(name = "signature_count", nullable = false)
  private long signatureCount;

  @Column(name = "uv_initialized", nullable = false)
  private boolean uvInitialized;

  @Column(name = "backup_eligible", nullable = false)
  private boolean backupEligible;

  @Column(name = "backup_state", nullable = false)
  private boolean backupState;

  @Column(name = "transports", nullable = false, length = 256)
  private String transports;

  @Lob
  @Column(name = "attestation_object", nullable = false)
  private byte[] attestationObject;

  @Lob
  @Column(name = "attestation_client_data_json", nullable = false)
  private byte[] attestationClientDataJson;

  @Column(name = "label", nullable = false, length = 64)
  private String label;

  @Column(name = "last_used_at")
  private Instant lastUsedAt;

  @Column(name = "credential_created_at", nullable = false)
  private Instant credentialCreatedAt;

  public PlayerPasskeyEntity(PlayerEntity player) {
    this.player = player;
  }

  public void updateFrom(CredentialRecord credentialRecord) {
    this.credentialId = credentialRecord.getCredentialId().toBase64UrlString();
    this.credentialType = credentialRecord.getCredentialType().getValue();
    this.publicKey = credentialRecord.getPublicKey().getBytes();
    this.signatureCount = credentialRecord.getSignatureCount();
    this.uvInitialized = credentialRecord.isUvInitialized();
    this.backupEligible = credentialRecord.isBackupEligible();
    this.backupState = credentialRecord.isBackupState();
    this.transports =
        String.join(
            ",",
            credentialRecord.getTransports().stream()
                .map(AuthenticatorTransport::getValue)
                .sorted()
                .toList());
    this.attestationObject = credentialRecord.getAttestationObject().getBytes();
    this.attestationClientDataJson = credentialRecord.getAttestationClientDataJSON().getBytes();
    this.label = credentialRecord.getLabel();
    this.lastUsedAt = credentialRecord.getLastUsed();
    this.credentialCreatedAt =
        credentialRecord.getCreated() == null ? Instant.now() : credentialRecord.getCreated();
  }

  public CredentialRecord toCredentialRecord() {
    return ImmutableCredentialRecord.builder()
        .credentialType(PublicKeyCredentialType.valueOf(credentialType))
        .credentialId(Bytes.fromBase64(credentialId))
        .userEntityUserId(new Bytes(player.getId().toString().getBytes(StandardCharsets.UTF_8)))
        .publicKey(new ImmutablePublicKeyCose(publicKey))
        .signatureCount(signatureCount)
        .uvInitialized(uvInitialized)
        .transports(
            transports.isBlank()
                ? java.util.Set.of()
                : java.util.Arrays.stream(transports.split(","))
                    .map(AuthenticatorTransport::valueOf)
                    .collect(java.util.stream.Collectors.toSet()))
        .backupEligible(backupEligible)
        .backupState(backupState)
        .attestationObject(new Bytes(attestationObject))
        .attestationClientDataJSON(new Bytes(attestationClientDataJson))
        .created(credentialCreatedAt)
        .lastUsed(lastUsedAt)
        .label(label)
        .build();
  }

  @PrePersist
  void prePersist() {
    if (id == null) {
      id = UuidV7Generator.next();
    }
  }
}
