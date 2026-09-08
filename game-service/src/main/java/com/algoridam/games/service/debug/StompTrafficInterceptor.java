package com.algoridam.games.service.debug;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;

@RequiredArgsConstructor
public class StompTrafficInterceptor implements ChannelInterceptor {

  private final StompTrafficLog stompTrafficLog;
  private final String direction;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    try {
      stompTrafficLog.record(direction, message);
    } catch (RuntimeException ignored) {
      // Debug capture must never break messaging.
    }
    return message;
  }
}
