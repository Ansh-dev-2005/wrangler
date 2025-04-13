/*
 * Copyright © 2017-2025 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package io.cdap.wrangler.directives;

import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.annotation.Plugin;
import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;
import java.util.Arrays;
import java.util.List;

/**
 * Directive for aggregating byte sizes and time durations.
 */
@Plugin(type = Directive.TYPE)
@Name("aggregate")
@Description("Aggregates byte sizes and time durations into totals or averages.")
public class AggregateDirective implements Directive {
  private String sizeColumn;
  private String timeColumn;
  private String targetSizeColumn;
  private String targetTimeColumn;
  private String sizeUnit;
  private String timeUnit;
  private String aggregationType;

  private long totalSize = 0;
  private long totalTime = 0;
  private int rowCount = 0;

  @Override
  public UsageDefinition define() {
    UsageDefinition.Builder builder = UsageDefinition.builder("aggregate");
    builder.define("size-column", TokenType.COLUMN_NAME);
    builder.define("time-column", TokenType.COLUMN_NAME);
    builder.define("target-size-column", TokenType.COLUMN_NAME);
    builder.define("target-time-column", TokenType.COLUMN_NAME);
    builder.define("size-unit", TokenType.TEXT); // e.g., 'MB', 'GB'
    builder.define("time-unit", TokenType.TEXT); // e.g., 'seconds', 'minutes'
    builder.define("aggregation-type", TokenType.TEXT); // e.g., 'total', 'average'
    return builder.build();
  }

  @Override
  public void initialize(Arguments arguments) throws DirectiveParseException {
    sizeColumn = ((ColumnName) arguments.value("size-column")).value();
    timeColumn = ((ColumnName) arguments.value("time-column")).value();
    targetSizeColumn = ((ColumnName) arguments.value("target-size-column")).value();
    targetTimeColumn = ((ColumnName) arguments.value("target-time-column")).value();
    sizeUnit = arguments.valueOrDefault("size-unit", "bytes").toString();
    timeUnit = arguments.valueOrDefault("time-unit", "nanoseconds").toString();
    aggregationType = arguments.valueOrDefault("aggregation-type", "total").toString();

    // Validate size unit
    if (!isValidSizeUnit(sizeUnit)) {
      throw new DirectiveParseException(String
        .format("Invalid size unit: '%s'. Supported units are: bytes, KB, MB, GB, TB.", sizeUnit));
    }

    // Validate time unit
    if (!isValidTimeUnit(timeUnit)) {
      throw new DirectiveParseException(String
        .format("Invalid time unit: '%s'. Supported units are: nanoseconds, milliseconds, seconds, minutes, hours.", 
        timeUnit));
    }
  }

  private boolean isValidSizeUnit(String unit) {
    return unit.equalsIgnoreCase("bytes") ||
           unit.equalsIgnoreCase("kb") ||
           unit.equalsIgnoreCase("mb") ||
           unit.equalsIgnoreCase("gb") ||
           unit.equalsIgnoreCase("tb");
  }

  private boolean isValidTimeUnit(String unit) {
    return unit.equalsIgnoreCase("nanoseconds") ||
           unit.equalsIgnoreCase("milliseconds") ||
           unit.equalsIgnoreCase("seconds") ||
           unit.equalsIgnoreCase("minutes") ||
           unit.equalsIgnoreCase("hours");
  }

  @Override
  public List<Row> execute(List<Row> rows, ExecutorContext context) throws DirectiveExecutionException {
    for (Row row : rows) {
      Object sizeValue = row.getValue(sizeColumn);
      Object timeValue = row.getValue(timeColumn);

      if (sizeValue != null && sizeValue instanceof Number) {
        totalSize += ((Number) sizeValue).longValue();
      } else {
        throw new DirectiveExecutionException(
            String.format("Column '%s' is missing or not a number.", sizeColumn));
      }

      if (timeValue != null && timeValue instanceof Number) {
        // Convert milliseconds to nanoseconds before summing
        totalTime += ((Number) timeValue).longValue(); // treat input as milliseconds
      } else {
        throw new DirectiveExecutionException(
            String.format("Column '%s' is missing or not a number.", timeColumn));
      }

      rowCount++;
    }
    return finalizeAggregation(rows, context);
  }

  @Override
  public void destroy() {
    // No-op
  }

  public List<Row> finalizeAggregation(List<Row> rows, ExecutorContext context) throws DirectiveExecutionException {
    Row aggregateRow = new Row();
    aggregateRow.add(targetSizeColumn, convertSize(totalSize));
    aggregateRow.add(targetTimeColumn, convertTime(totalTime));

    if (rowCount > 0 && "average".equalsIgnoreCase(aggregationType)) {
      int targetSizeIndex = aggregateRow.find(targetSizeColumn);
      aggregateRow.setValue(targetSizeIndex, convertSize(totalSize / rowCount));
      int targetTimeIndex = aggregateRow.find(targetTimeColumn);
      aggregateRow.setValue(targetTimeIndex, convertTime(totalTime / rowCount));
    }

    return Arrays.asList(aggregateRow);
  }

  private double convertSize(long sizeInBytes) {
    switch (sizeUnit.toLowerCase()) {
      case "mb":
        return sizeInBytes / (1024.0 * 1024.0);
      case "gb":
        return sizeInBytes / (1024.0 * 1024.0 * 1024.0);
      default:
        return sizeInBytes;
    }
  }

  private double convertTime(long timeInMillis) {
    switch (timeUnit.toLowerCase()) {
      case "seconds":
        return timeInMillis / 1000.0;
      case "minutes":
        return timeInMillis / (60.0 * 1000.0);
      default:
        return timeInMillis;
    }
  }

}
