package com.algoridam.games.service.config;

import com.algoridam.games.service.debug.StompTrafficInterceptor;
import com.algoridam.games.service.debug.StompTrafficLog;
import com.algoridam.games.service.security.StompAuthChannelInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.DefaultContentTypeResolver;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@RequiredArgsConstructor
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final ObjectMapper objectMapper;
  private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
  private final StompTrafficLog stompTrafficLog;

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    config.enableSimpleBroker("/topic");
    config.setApplicationDestinationPrefixes("/app/v1");
    config.setUserDestinationPrefix("/user");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    registry
        .addEndpoint("/ws")
        .setAllowedOriginPatterns("*")
        .withSockJS()
        .setSessionCookieNeeded(false);
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(
        new StompTrafficInterceptor(stompTrafficLog, "IN"), stompAuthChannelInterceptor);
  }

  @Override
  public void configureClientOutboundChannel(ChannelRegistration registration) {
    registration.interceptors(new StompTrafficInterceptor(stompTrafficLog, "OUT"));
  }

  @Override
  public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
    DefaultContentTypeResolver resolver = new DefaultContentTypeResolver();
    resolver.setDefaultMimeType(MimeTypeUtils.APPLICATION_JSON);
    MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
    converter.setObjectMapper(objectMapper);
    converter.setContentTypeResolver(resolver);
    messageConverters.add(converter);
    return false;
  }
}
