package com.algoridam.games.player.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.security.web.webauthn.api.AuthenticatorAssertionResponse;
import org.springframework.security.web.webauthn.api.AuthenticatorAttestationResponse;
import org.springframework.security.web.webauthn.api.PublicKeyCredential;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions;

public final class PasskeyDtos {

  private PasskeyDtos() {}

  public record SignupOptionsRequest(
      @NotBlank
          @Size(min = 3, max = 64)
          @Pattern(
              regexp = "^[a-zA-Z0-9_\\-.]+$",
              message = "Handle contains unsupported characters")
          String handle,
      @Size(max = 128) String displayName,
      @Size(max = 64) String label) {}

  public record LoginOptionsRequest(@Size(max = 64) String handle, UUID gameId) {}

  public record AddPasskeyOptionsRequest(@Size(max = 64) String label) {}

  public record RegistrationFinishRequest(
      @NotBlank String requestId,
      @NotNull PublicKeyCredential<AuthenticatorAttestationResponse> credential) {}

  public record LoginFinishRequest(
      @NotBlank String requestId,
      @NotNull PublicKeyCredential<AuthenticatorAssertionResponse> credential) {}

  public record CreationOptionsResponse(
      String requestId, PublicKeyCredentialCreationOptions publicKey) {}

  public record RequestOptionsResponse(
      String requestId, PublicKeyCredentialRequestOptions publicKey) {}

  public record AuthTokenResponse(String token) {}

  public record PasskeyResponse(UUID id, String label, String credentialId, String lastUsedAt) {}

  public record PlayerResponse(
      UUID id, String handle, String displayName, List<PasskeyResponse> passkeys) {}
}
