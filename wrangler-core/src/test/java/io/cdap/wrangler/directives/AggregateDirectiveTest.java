/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.cdap.wrangler.directives;

import io.cdap.wrangler.TestingRig;
import io.cdap.wrangler.api.Row;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for AggregateDirective.
 */
public class AggregateDirectiveTest {

  @Test
  public void testAggregateStats() throws Exception {
    // Input data
    List<Row> rows = new ArrayList<>();
    rows.add(new Row("data_transfer_size", 10485760).add("response_time", 5000)); // 10 MB, 5 ms
    rows.add(new Row("data_transfer_size", 5242880).add("response_time", 2000));  // 5 MB, 2 ms

    // Recipe
    String[] recipe = new String[] {
      "#pragma version 2.0;",
      "aggregate :data_transfer_size :response_time :total_size_mb :total_time_sec \"MB\" \"seconds\" \"total\""
    };

    // Execute
    List<Row> results = TestingRig.execute(recipe, rows);

    // Expected output
    Assert.assertEquals(1, results.size());
    Row result = results.get(0);
    Assert.assertEquals(15.0, result.getValue("total_size_mb")); // 15 MB
    Assert.assertEquals(7.0, (double) result.getValue("total_time_sec"), 0.001); // ✅ 7000ms = 7 seconds
  }

  @Test
  public void testAggregateStatsAverage() throws Exception {
    // Input data
    List<Row> rows = new ArrayList<>();
    rows.add(new Row("data_transfer_size", 10485760).add("response_time", 5000)); // 10 MB, 5 ms
    rows.add(new Row("data_transfer_size", 5242880).add("response_time", 2000));  // 5 MB, 2 ms

    // Recipe
    String[] recipeAverage = new String[] {
      "#pragma version 2.0;",
      "aggregate :data_transfer_size :response_time :avg_size_mb :avg_time_sec \"MB\" \"seconds\" \"average\""
    };

    // Execute
    List<Row> results = TestingRig.execute(recipeAverage, rows);

    // Expected output
    Assert.assertEquals(1, results.size());
    Row result = results.get(0);
    Assert.assertEquals(7.5, result.getValue("avg_size_mb")); // Average size: 7.5 MB
    Assert.assertEquals(3.5, (double) result.getValue("avg_time_sec"), 0.001); // ✅ average of 5ms & 2ms = 3.5ms = 3.5s

  }
}
