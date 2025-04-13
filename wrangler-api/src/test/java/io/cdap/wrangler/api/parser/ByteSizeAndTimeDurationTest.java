/*
 * Copyright © 2017-2025 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

 package io.cdap.wrangler.api.parser;

 import org.junit.Assert;
 import org.junit.Test;
 
 /**
  * Unit tests for ByteSize and TimeDuration classes with edge cases.
  */
 public class ByteSizeAndTimeDurationTest {
 
  @Test
  public void testByteSizeParsing() {
    // Regular test cases
    ByteSize byteSize1 = new ByteSize("10kb");
    Assert.assertEquals(10240, byteSize1.getBytes());
 
    ByteSize byteSize2 = new ByteSize("1.5MB");
    Assert.assertEquals(1572864, byteSize2.getBytes());
 
    ByteSize byteSize3 = new ByteSize("2GB");
    Assert.assertEquals(2147483648L, byteSize3.getBytes());
 
    ByteSize byteSize4 = new ByteSize("1TB");
    Assert.assertEquals(1099511627776L, byteSize4.getBytes());
 
    // Edge cases
    ByteSize byteSize5 = new ByteSize("0B");
    Assert.assertEquals(0, byteSize5.getBytes()); // Zero byte size
 
    ByteSize byteSize6 = new ByteSize("100Kb");
    Assert.assertEquals(102400, byteSize6.getBytes()); // Uppercase 'K' case
 
    // Large value
    ByteSize byteSize7 = new ByteSize("1PB");
    Assert.assertEquals(1125899906842624L, byteSize7.getBytes()); // Very large size (Petabyte)
 
    // Invalid formats
    try {
     new ByteSize("10abc"); // Invalid size
     Assert.fail("Expected IllegalArgumentException for invalid byte size");
    } catch (IllegalArgumentException e) {
     // Expected exception
    }
 
    try {
     new ByteSize("1000ZZ"); // Invalid size unit
     Assert.fail("Expected IllegalArgumentException for invalid unit");
    } catch (IllegalArgumentException e) {
     // Expected exception
    }
  }
 
   @Test
   public void testTimeDurationParsing() {
     // Regular test cases
     TimeDuration timeDuration1 = new TimeDuration("5ms");
     Assert.assertEquals(5, timeDuration1.getMilliseconds());
 
     TimeDuration timeDuration2 = new TimeDuration("2.1s");
     Assert.assertEquals(2100, timeDuration2.getMilliseconds());
 
     TimeDuration timeDuration3 = new TimeDuration("3m");
     Assert.assertEquals(180000, timeDuration3.getMilliseconds());
 
     TimeDuration timeDuration4 = new TimeDuration("1h");
     Assert.assertEquals(3600000, timeDuration4.getMilliseconds());
 
     // Edge cases
     TimeDuration timeDuration5 = new TimeDuration("0ms");
     Assert.assertEquals(0, timeDuration5.getMilliseconds()); // Zero duration
 
     TimeDuration timeDuration6 = new TimeDuration("0S");
     Assert.assertEquals(0, timeDuration6.getMilliseconds()); // Zero duration with uppercase 'S'
 
     // Large value
     TimeDuration timeDuration7 = new TimeDuration("1000h");
     Assert.assertEquals(3600000000L, timeDuration7.getMilliseconds()); // Large time duration (1000 hours)
 
    
     // Invalid formats
     try {
       new TimeDuration("5xyz"); // Invalid duration
       Assert.fail("Expected IllegalArgumentException for invalid time duration");
     } catch (IllegalArgumentException e) {
       // Expected exception
     }
 
     try {
       new TimeDuration("1000xyz"); // Invalid unit for time
       Assert.fail("Expected IllegalArgumentException for invalid unit");
     } catch (IllegalArgumentException e) {
       // Expected exception
     }
   }
 }
