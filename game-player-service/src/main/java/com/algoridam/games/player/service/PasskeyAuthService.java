package com.algoridam.games.player.service;

import com.algoridam.games.common.dto.ErrorCode;
import com.algoridam.games.common.exception.ServiceException;
import com.algoridam.games.common.id.UuidV7Generator;
import com.algoridam.games.player.dto.PasskeyDtos.AddPasskeyOptionsRequest;
import com.algoridam.games.player.dto.PasskeyDtos.AuthTokenResponse;
import com.algoridam.games.player.dto.PasskeyDtos.CreationOptionsResponse;
import com.algoridam.games.player.dto.PasskeyDtos.LoginFinishRequest;
import com.algoridam.games.player.dto.PasskeyDtos.LoginOptionsRequest;
import com.algoridam.games.player.dto.PasskeyDtos.PasskeyResponse;
import com.algoridam.games.player.dto.PasskeyDtos.PlayerResponse;
import com.algoridam.games.player.dto.PasskeyDtos.RegistrationFinishRequest;
import com.algoridam.games.player.dto.PasskeyDtos.RequestOptionsResponse;
import com.algoridam.games.player.dto.PasskeyDtos.SignupOptionsRequest;
import com.algoridam.games.player.entity.PlayerEntity;
import com.algoridam.games.player.repository.PlayerPasskeyRepository;
import com.algoridam.games.player.repository.PlayerRepository;
import com.algoridam.games.player.security.JwtService;
import com.algoridam.games.player.security.SpringSecurityCurrentPlayerProvider;
import com.algoridam.games.player.service.PasskeyCeremonyTokenService.LoginChallenge;
import com.algoridam.games.player.service.PasskeyCeremonyTokenService.RegistrationChallenge;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.ImmutablePublicKeyCredentialCreationOptionsRequest;
import org.springframework.security.web.webauthn.management.ImmutablePublicKeyCredentialRequestOptionsRequest;
import org.springframework.security.web.webauthn.management.ImmutableRelyingPartyRegistrationRequest;
import org.springframework.security.web.webauthn.management.RelyingPartyAuthenticationRequest;
import org.springframework.security.web.webauthn.management.RelyingPartyPublicKey;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasskeyAuthService {

  private final PlayerRepository players;
  private final PlayerPasskeyRepository passkeys;
  private final WebAuthnRelyingPartyOperations relyingParty;
  private final PasskeyCeremonyTokenService ceremonyTokens;
  private final PendingSignupContext pendingSignupContext;
  private final JwtService jwtService;
  private final SpringSecurityCurrentPlayerProvider currentPlayerProvider;

  @Transactional
  public CreationOptionsResponse signupOptions(SignupOptionsRequest request) {
    String handle = normalizeHandle(request.handle());
    if (players.existsByHandle(handle)) {
      throw new ServiceException(ErrorCode.DUPLICATE_REQUEST, "Duplicate handle: " + handle);
    }
    UUID playerId = UuidV7Generator.next();
    String displayName = displayNameOrDefault(request.displayName(), handle);
    String label = labelOrDefault(request.label());
    pendingSignupContext.set(playerId, handle, displayName);
    try {
      PublicKeyCredentialCreationOptions options = createRegistrationOptions(handle);
      String requestId =
          ceremonyTokens.issueRegistration(
              new RegistrationChallenge(
                  playerId,
                  label,
                  true,
                  options.getChallenge().toBase64UrlString(),
                  handle,
                  displayName));
      return new CreationOptionsResponse(requestId, options);
    } finally {
      pendingSignupContext.clear();
    }
  }

  @Transactional
  public AuthTokenResponse signupFinish(RegistrationFinishRequest request) {
    RegistrationChallenge challenge = ceremonyTokens.verifyRegistration(request.requestId());
    if (!challenge.signup()) {
      throw new ServiceException(ErrorCode.BAD_REQUEST);
    }
    PlayerEntity player;
    try {
      player =
          players.saveAndFlush(
              new PlayerEntity(challenge.playerId(), challenge.handle(), challenge.displayName()));
    } catch (DataIntegrityViolationException exception) {
      throw new ServiceException(exception, ErrorCode.DUPLICATE_REQUEST, "Duplicate signup", null);
    }
    PublicKeyCredentialCreationOptions options =
        registrationOptionsForChallenge(challenge.handle(), challenge.challenge());
    try {
      relyingParty.registerCredential(
          new ImmutableRelyingPartyRegistrationRequest(
              options, new RelyingPartyPublicKey(request.credential(), challenge.label())));
    } catch (DataIntegrityViolationException exception) {
      throw new ServiceException(exception, ErrorCode.DUPLICATE_REQUEST, "Duplicate signup", null);
    } catch (Exception exception) {
      throw new ServiceException(
          exception, ErrorCode.PASSKEY_VERIFICATION_FAILED, "Signup passkey failed", null);
    }
    return token(player, null);
  }

  @Transactional(readOnly = true)
  public RequestOptionsResponse loginOptions(LoginOptionsRequest request) {
    String handle =
        request.handle() == null || request.handle().isBlank()
            ? null
            : normalizeHandle(request.handle());
    Authentication authentication;
    if (handle == null) {
      authentication = UsernamePasswordAuthenticationToken.unauthenticated("passkey", null);
    } else {
      if (!players.existsByHandle(handle)) {
        throw new ServiceException(ErrorCode.DATA_NOT_FOUND, "Unknown handle: " + handle);
      }
      authentication = authenticated(handle);
    }
    PublicKeyCredentialRequestOptions options =
        relyingParty.createCredentialRequestOptions(
            new ImmutablePublicKeyCredentialRequestOptionsRequest(authentication));
    String requestId =
        ceremonyTokens.issueLogin(
            new LoginChallenge(
                options.getChallenge().toBase64UrlString(), handle, request.gameId()));
    return new RequestOptionsResponse(requestId, options);
  }

  @Transactional
  public AuthTokenResponse loginFinish(LoginFinishRequest request) {
    LoginChallenge challenge = ceremonyTokens.verifyLogin(request.requestId());
    PublicKeyCredentialRequestOptions options =
        requestOptionsForChallenge(challenge.handle(), challenge.challenge());
    PublicKeyCredentialUserEntity credentialUser;
    try {
      credentialUser =
          relyingParty.authenticate(
              new RelyingPartyAuthenticationRequest(options, request.credential()));
    } catch (Exception exception) {
      throw new ServiceException(
          exception, ErrorCode.PASSKEY_VERIFICATION_FAILED, "Login passkey failed", null);
    }
    PlayerEntity player =
        playerOrThrow(
            UUID.fromString(new String(credentialUser.getId().getBytes(), StandardCharsets.UTF_8)));
    return token(player, challenge.gameId());
  }

  @Transactional(readOnly = true)
  public PlayerResponse me() {
    PlayerEntity player = currentPlayerProvider.requirePlayer();
    return playerResponse(player);
  }

  @Transactional(readOnly = true)
  public List<PasskeyResponse> listPasskeys() {
    return passkeyResponses(currentPlayerProvider.requirePlayer());
  }

  private List<PasskeyResponse> passkeyResponses(PlayerEntity player) {
    return passkeys.findByPlayer(player).stream()
        .map(
            passkey ->
                new PasskeyResponse(
                    passkey.getId(),
                    passkey.getLabel(),
                    passkey.getCredentialId(),
                    passkey.getLastUsedAt() == null ? null : passkey.getLastUsedAt().toString()))
        .toList();
  }

  @Transactional(readOnly = true)
  public CreationOptionsResponse addPasskeyOptions(AddPasskeyOptionsRequest request) {
    PlayerEntity player = currentPlayerProvider.requirePlayer();
    String label = labelOrDefault(request.label());
    if (passkeys.existsByPlayerAndLabel(player, label)) {
      throw new ServiceException(ErrorCode.DUPLICATE_REQUEST, "Duplicate passkey label");
    }
    PublicKeyCredentialCreationOptions options = createRegistrationOptions(player.getHandle());
    String requestId =
        ceremonyTokens.issueRegistration(
            new RegistrationChallenge(
                player.getId(),
                label,
                false,
                options.getChallenge().toBase64UrlString(),
                null,
                null));
    return new CreationOptionsResponse(requestId, options);
  }

  @Transactional
  public List<PasskeyResponse> addPasskeyFinish(RegistrationFinishRequest request) {
    RegistrationChallenge challenge = ceremonyTokens.verifyRegistration(request.requestId());
    if (challenge.signup()) {
      throw new ServiceException(ErrorCode.BAD_REQUEST);
    }
    PlayerEntity player = currentPlayerProvider.requirePlayer();
    if (!player.getId().equals(challenge.playerId())) {
      throw new ServiceException(ErrorCode.FORBIDDEN);
    }
    if (passkeys.existsByPlayerAndLabel(player, challenge.label())) {
      throw new ServiceException(ErrorCode.DUPLICATE_REQUEST, "Duplicate passkey label");
    }
    PublicKeyCredentialCreationOptions options =
        registrationOptionsForChallenge(player.getHandle(), challenge.challenge());
    try {
      relyingParty.registerCredential(
          new ImmutableRelyingPartyRegistrationRequest(
              options, new RelyingPartyPublicKey(request.credential(), challenge.label())));
    } catch (DataIntegrityViolationException exception) {
      throw new ServiceException(exception, ErrorCode.DUPLICATE_REQUEST, "Duplicate passkey", null);
    } catch (Exception exception) {
      throw new ServiceException(
          exception, ErrorCode.PASSKEY_VERIFICATION_FAILED, "Add passkey failed", null);
    }
    return passkeyResponses(player);
  }

  @Transactional
  public void deletePasskey(UUID passkeyId) {
    PlayerEntity player = currentPlayerProvider.requirePlayer();
    if (passkeys.countByPlayer(player) <= 1) {
      throw new ServiceException(ErrorCode.CANNOT_DELETE_LAST_PASSKEY);
    }
    passkeys
        .findByIdAndPlayer(passkeyId, player)
        .ifPresentOrElse(
            passkeys::delete,
            () -> {
              throw new ServiceException(ErrorCode.DATA_NOT_FOUND);
            });
  }

  private PublicKeyCredentialCreationOptions createRegistrationOptions(String handle) {
    return relyingParty.createPublicKeyCredentialCreationOptions(
        new ImmutablePublicKeyCredentialCreationOptionsRequest(authenticated(handle)));
  }

  private PublicKeyCredentialCreationOptions registrationOptionsForChallenge(
      String handle, String challenge) {
    PublicKeyCredentialCreationOptions template = createRegistrationOptions(handle);
    return PublicKeyCredentialCreationOptions.builder()
        .rp(template.getRp())
        .user(template.getUser())
        .challenge(Bytes.fromBase64(challenge))
        .pubKeyCredParams(template.getPubKeyCredParams())
        .timeout(template.getTimeout())
        .excludeCredentials(template.getExcludeCredentials())
        .authenticatorSelection(template.getAuthenticatorSelection())
        .attestation(template.getAttestation())
        .extensions(template.getExtensions())
        .build();
  }

  private PublicKeyCredentialRequestOptions requestOptionsForChallenge(
      String handle, String challenge) {
    Authentication authentication =
        handle == null
            ? UsernamePasswordAuthenticationToken.unauthenticated("passkey", null)
            : authenticated(handle);
    PublicKeyCredentialRequestOptions template =
        relyingParty.createCredentialRequestOptions(
            new ImmutablePublicKeyCredentialRequestOptionsRequest(authentication));
    return PublicKeyCredentialRequestOptions.builder()
        .challenge(Bytes.fromBase64(challenge))
        .timeout(template.getTimeout())
        .rpId(template.getRpId())
        .allowCredentials(template.getAllowCredentials())
        .userVerification(template.getUserVerification())
        .extensions(template.getExtensions())
        .build();
  }

  private PlayerResponse playerResponse(PlayerEntity player) {
    return new PlayerResponse(
        player.getId(), player.getHandle(), player.getDisplayName(), passkeyResponses(player));
  }

  private AuthTokenResponse token(PlayerEntity player, UUID gameId) {
    return new AuthTokenResponse(
        jwtService.generate(player.getId(), player.getHandle(), player.getDisplayName(), gameId));
  }

  private PlayerEntity playerOrThrow(UUID playerId) {
    return players
        .findById(playerId)
        .orElseThrow(() -> new ServiceException(ErrorCode.DATA_NOT_FOUND));
  }

  private Authentication authenticated(String handle) {
    return UsernamePasswordAuthenticationToken.authenticated(
        handle, null, AuthorityUtils.NO_AUTHORITIES);
  }

  private String normalizeHandle(String handle) {
    return handle.trim().toLowerCase(Locale.ROOT);
  }

  private String labelOrDefault(String label) {
    return label == null || label.isBlank() ? "default" : label.trim();
  }

  private String displayNameOrDefault(String displayName, String handle) {
    return displayName == null || displayName.isBlank() ? handle : displayName.trim();
  }
}
