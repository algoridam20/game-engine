package com.algoridam.games.player.controller;

import com.algoridam.games.common.dto.GeneralResponse;
import com.algoridam.games.player.dto.PasskeyDtos.AddPasskeyOptionsRequest;
import com.algoridam.games.player.dto.PasskeyDtos.LoginFinishRequest;
import com.algoridam.games.player.dto.PasskeyDtos.LoginOptionsRequest;
import com.algoridam.games.player.dto.PasskeyDtos.RegistrationFinishRequest;
import com.algoridam.games.player.dto.PasskeyDtos.SignupOptionsRequest;
import com.algoridam.games.player.service.PasskeyAuthService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class PasskeyAuthController {

  private final PasskeyAuthService passkeyAuthService;

  @PostMapping("/auth/passkeys/signup/options")
  public GeneralResponse<?, Object> signupOptions(
      @Valid @RequestBody SignupOptionsRequest request) {
    return GeneralResponse.ok(passkeyAuthService.signupOptions(request));
  }

  @PostMapping("/auth/passkeys/signup/finish")
  public GeneralResponse<?, Object> signupFinish(
      @Valid @RequestBody RegistrationFinishRequest request) {
    return GeneralResponse.ok(passkeyAuthService.signupFinish(request));
  }

  @PostMapping("/auth/passkeys/login/options")
  public GeneralResponse<?, Object> loginOptions(@Valid @RequestBody LoginOptionsRequest request) {
    return GeneralResponse.ok(passkeyAuthService.loginOptions(request));
  }

  @PostMapping("/auth/passkeys/login/finish")
  public GeneralResponse<?, Object> loginFinish(@Valid @RequestBody LoginFinishRequest request) {
    return GeneralResponse.ok(passkeyAuthService.loginFinish(request));
  }

  @GetMapping("/player/me")
  public GeneralResponse<?, Object> me() {
    return GeneralResponse.ok(passkeyAuthService.me());
  }

  @GetMapping("/player/passkeys")
  public GeneralResponse<?, Object> listPasskeys() {
    return GeneralResponse.ok(passkeyAuthService.listPasskeys());
  }

  @PostMapping("/player/passkeys/options")
  public GeneralResponse<?, Object> addPasskeyOptions(
      @Valid @RequestBody AddPasskeyOptionsRequest request) {
    return GeneralResponse.ok(passkeyAuthService.addPasskeyOptions(request));
  }

  @PostMapping("/player/passkeys/finish")
  public GeneralResponse<?, Object> addPasskeyFinish(
      @Valid @RequestBody RegistrationFinishRequest request) {
    return GeneralResponse.ok(passkeyAuthService.addPasskeyFinish(request));
  }

  @DeleteMapping("/player/passkeys/{passkeyId}")
  public GeneralResponse<?, Object> deletePasskey(@PathVariable UUID passkeyId) {
    passkeyAuthService.deletePasskey(passkeyId);
    return GeneralResponse.ok(null);
  }
}
