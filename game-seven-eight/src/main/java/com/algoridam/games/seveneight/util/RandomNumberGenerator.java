package com.algoridam.games.seveneight.util;

import java.util.concurrent.ThreadLocalRandom;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RandomNumberGenerator {
  public static int getRandomNumber(int min, int max) {
    return ThreadLocalRandom.current().nextInt(min, max);
  }
}
