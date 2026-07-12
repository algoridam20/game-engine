package com.algoridam.games.player.config;

import com.fasterxml.jackson.databind.Module;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity;
import org.springframework.security.web.webauthn.jackson.WebauthnJackson2Module;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;
import org.springframework.security.web.webauthn.management.Webauthn4JRelyingPartyOperations;

@Configuration
public class PasskeyConfig {

  @Bean
  Module webauthnJackson2Module() {
    return new WebauthnJackson2Module();
  }

  @Bean
  WebAuthnRelyingPartyOperations webAuthnRelyingPartyOperations(
      AuthProperties properties,
      PublicKeyCredentialUserEntityRepository users,
      UserCredentialRepository credentials) {
    PublicKeyCredentialRpEntity rp =
        PublicKeyCredentialRpEntity.builder()
            .id(properties.passkey().rpId())
            .name(properties.passkey().rpName())
            .build();
    return new Webauthn4JRelyingPartyOperations(
        users, credentials, rp, Set.of(properties.passkey().origin()));
  }
}
