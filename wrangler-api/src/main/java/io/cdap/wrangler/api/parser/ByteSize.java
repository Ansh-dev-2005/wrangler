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
 * Represents a size in bytes, supporting parsing from human-readable formats like KB, MB, GB, TB, and PB.
 */
public class ByteSize implements Token {
  private final String rawValue;
  private final long bytes;

  public ByteSize(String value) {
    this.rawValue = value;
    this.bytes = parse(value);
  }

  private long parse(String val) {
    val = val.trim().toLowerCase();

    // Separate numeric and alphabetic characters for validation
    String numericPart = val.replaceAll("[a-zA-Z]", "").trim();
    String unitPart = val.replaceAll("[0-9.]+", "").trim();

    if (numericPart.isEmpty() || unitPart.isEmpty()) {
      throw new IllegalArgumentException("Invalid byte size: " + val);
    }

    double numericValue;
    try {
      numericValue = Double.parseDouble(numericPart);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid numeric value in byte size: " + numericPart);
    }

    switch (unitPart) {
      case "kb":
        return (long) (numericValue * 1024);
      case "mb":
        return (long) (numericValue * 1024 * 1024);
      case "gb":
        return (long) (numericValue * 1024 * 1024 * 1024);
      case "tb":
        return (long) (numericValue * 1024L * 1024L * 1024L * 1024L);
      case "pb":
        return (long) (numericValue * 1024L * 1024L * 1024L * 1024L * 1024L);
      case "b":
        return (long) numericValue;
      default:
        throw new IllegalArgumentException("Invalid byte size unit: " + unitPart);
    }
  }

  @Override
  public Object value() {
    return bytes;
  }

  public long getBytes() {
    return bytes;
  }

  @Override
  public TokenType type() {
    return TokenType.BYTE_SIZE;
  }

  @Override
  public JsonElement toJson() {
    return new JsonPrimitive(bytes);
  }

  @Override
  public String toString() {
    return rawValue;
  }
}
