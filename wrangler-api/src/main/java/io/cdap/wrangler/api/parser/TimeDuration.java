/*
 * Copyright © 2017-2019 Cask Data, Inc.
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

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * Represents a duration of time, supporting parsing from human-readable formats like ms, s, m, h, and us.
 */
public class TimeDuration implements Token {
  private final String rawValue;
  private final long milliseconds;

  public TimeDuration(String value) {
    this.rawValue = value;
    this.milliseconds = parse(value);
  }

  private long parse(String val) {
    if (val == null || val.isEmpty()) {
      throw new IllegalArgumentException("Time duration cannot be null or empty.");
    }

    // Trim any extra whitespace and convert the input to lowercase for consistency
    val = val.trim().toLowerCase();

    // Check if the value has a valid time unit (ms, s, m, h, us)
    if (val.endsWith("ms")) {
      return parseDuration(val, "ms", 1);
    } else if (val.endsWith("s")) {
      return parseDuration(val, "s", 1000);
    } else if (val.endsWith("m")) {
      return parseDuration(val, "m", 1000 * 60);
    } else if (val.endsWith("h")) {
      return parseDuration(val, "h", 1000 * 60 * 60);
    } 

    // Throw an exception if the unit is unrecognized
    throw new IllegalArgumentException("Invalid time duration: " + val);
  }

  /**
   * Helper method to parse the duration and handle the numeric part.
   * 
   * @param val The raw string input
   * @param unit The time unit (e.g., ms, s, m, h, us)
   * @param multiplier The multiplier for converting to milliseconds
   * @return The duration in milliseconds
   */
  private long parseDuration(String val, String unit, double multiplier) {
    try {
      String numericPart = val.replace(unit, "").trim();
      // Validate that the numeric part is valid
      if (numericPart.isEmpty() || !numericPart.matches("-?\\d+(\\.\\d+)?")) {
        throw new IllegalArgumentException("Invalid numeric value in duration: " + val);
      }

      double value = Double.parseDouble(numericPart);
      return (long) (value * multiplier);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid numeric value in duration: " + val, e);
    }
  }


  @Override
  public Object value() {
    return milliseconds;
  }

  public long getMilliseconds() {
    return milliseconds;
  }

  @Override
  public TokenType type() {
    return TokenType.TIME_DURATION;
  }

  @Override
  public JsonElement toJson() {
    return new JsonPrimitive(milliseconds);
  }

  @Override
  public String toString() {
    return rawValue;
  }
}
