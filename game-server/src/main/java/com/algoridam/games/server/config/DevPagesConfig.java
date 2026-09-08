package com.algoridam.games.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class DevPagesConfig implements WebMvcConfigurer {

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addRedirectViewController("/dev", "/dev/");
    registry.addViewController("/dev/").setViewName("forward:/dev/index.html");
    registry.addRedirectViewController("/passkey-test.html", "/dev/passkeys.html");
    registry.addRedirectViewController("/stomp-inspector.html", "/dev/stomp.html");
  }
}
