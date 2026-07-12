package com.algoridam.games.common.id;

import java.security.SecureRandom;
import java.util.UUID;

public final class UuidV7Generator {

  private static final SecureRandom RANDOM = new SecureRandom();

  private UuidV7Generator() {}

  public static UUID next() {
    byte[] randomBytes = new byte[10];
    RANDOM.nextBytes(randomBytes);

    long timestamp = System.currentTimeMillis();
    long mostSignificantBits = (timestamp & 0x0000_00FF_FFFF_FFFFL) << 16;
    mostSignificantBits |= 0x7000L;
    mostSignificantBits |= Byte.toUnsignedLong(randomBytes[0]) << 8;
    mostSignificantBits |= Byte.toUnsignedLong(randomBytes[1]);

    long leastSignificantBits = 0x8000_0000_0000_0000L;
    for (int index = 2; index < randomBytes.length; index++) {
      leastSignificantBits |= Byte.toUnsignedLong(randomBytes[index]) << ((9 - index) * 8);
    }

    return new UUID(mostSignificantBits, leastSignificantBits);
  }
}
