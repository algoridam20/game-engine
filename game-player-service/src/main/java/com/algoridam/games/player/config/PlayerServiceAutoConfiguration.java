package com.algoridam.games.player.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "com.algoridam.games.player")
@EnableConfigurationProperties(AuthProperties.class)
public class PlayerServiceAutoConfiguration {}
