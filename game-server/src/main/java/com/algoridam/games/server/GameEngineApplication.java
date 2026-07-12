package com.algoridam.games.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableCaching
@SpringBootApplication
@EntityScan(basePackages = "com.algoridam.games")
@EnableJpaRepositories(basePackages = "com.algoridam.games")
@ComponentScan(basePackages = "com.algoridam.games")
public class GameEngineApplication {
  public static void main(String[] args) {
    SpringApplication.run(GameEngineApplication.class, args);
  }
}
