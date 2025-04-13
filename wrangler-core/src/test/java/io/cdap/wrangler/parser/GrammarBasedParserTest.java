/*
 *  Copyright © 2017-2019 Cask Data, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package io.cdap.wrangler.parser;

import io.cdap.wrangler.TestingRig;
import io.cdap.wrangler.api.CompileStatus;
import io.cdap.wrangler.api.Compiler;
import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.RecipeException;
import io.cdap.wrangler.api.RecipeParser;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.TimeDuration;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests {@link GrammarBasedParser}
 */
public class GrammarBasedParserTest {

  @Test
  public void testBasic() throws Exception {
    String[] recipe = new String[] {
        "#pragma version 2.0;",
        "rename :col1 :col2",
        "parse-as-csv :body ',' true;",
        "#pragma load-directives text-reverse, text-exchange;",
        "${macro} ${macro_2}",
        "${macro_${test}}"
    };

    RecipeParser parser = TestingRig.parse(recipe);
    List<Directive> directives = parser.parse();
    Assert.assertEquals(2, directives.size());
  }

  @Test
  public void testLoadableDirectives() throws Exception {
    String[] recipe = new String[] {
        "#pragma version 2.0;",
        "#pragma load-directives text-reverse, text-exchange;",
        "rename col1 col2",
        "parse-as-csv body , true",
        "text-reverse :body;",
        "test prop: { a='b', b=1.0, c=true};",
        "#pragma load-directives test-change,text-exchange, test1,test2,test3,test4;"
    };

    Compiler compiler = new RecipeCompiler();
    CompileStatus status = compiler.compile(new MigrateToV2(recipe).migrate());
    Assert.assertEquals(7, status.getSymbols().getLoadableDirectives().size());
  }

  @Test
  public void testCommentOnlyRecipe() throws Exception {
    String[] recipe = new String[] {
        "// test"
    };

    RecipeParser parser = TestingRig.parse(recipe);
    List<Directive> directives = parser.parse();
    Assert.assertEquals(0, directives.size());
  }

  @Test
  public void testValidByteSizeParsing() {
    ByteSize byteSize = new ByteSize("10kb");
    Assert.assertEquals(10240, byteSize.getBytes());

    byteSize = new ByteSize("1.5MB");
    Assert.assertEquals(1572864, byteSize.getBytes());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidByteSizeParsing() {
    new ByteSize("invalid");
  }

  @Test
  public void testValidTimeDurationParsing() {
    TimeDuration timeDuration = new TimeDuration("5ms");
    Assert.assertEquals(5, timeDuration.getMilliseconds());

    timeDuration = new TimeDuration("2.1s");
    Assert.assertEquals(2100, timeDuration.getMilliseconds());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidTimeDurationParsing() {
    new TimeDuration("invalid");
  }

  @Test
  public void testRecipeWithByteSizeAndTimeDuration() throws Exception {
    String[] recipe = new String[] {
        "#pragma version 2.0;",
        "set-column :size \"10kb\";",
        "set-column :duration \"2.1s\";"
    };

    RecipeParser parser = TestingRig.parse(recipe);
    List<Directive> directives = parser.parse();
    Assert.assertEquals(2, directives.size());
  }

  @Test
  public void testInvalidByteSizeInAggregate() throws Exception {
    String[] recipe = new String[] {
        "#pragma version 2.0;",
        "aggregate :size :time :out_size :out_time \"invalidSize\" \"seconds\" \"total\""
    };

    List<Row> rows = new ArrayList<>();
    rows.add(new Row("size", 1234).add("time", 1000));

    try {
      TestingRig.execute(recipe, rows);
      Assert.fail("Expected RecipeException to be thrown");
    } catch (RecipeException e) {
      // Exception caught, test passes
    } catch (Exception e) {
      // Catch other exceptions and fail the test
      Assert.fail("Expected RecipeException, but caught: " + e.getClass().getSimpleName());
    }
  }

  @Test
  public void testInvalidTimeDurationInAggregate() throws Exception {
    String[] recipe = new String[] {
        "#pragma version 2.0;",
        "aggregate :size :time :out_size :out_time \"MB\" \"invalidTime\" \"total\""
    };

    List<Row> rows = new ArrayList<>();
    rows.add(new Row("size", 1234).add("time", 1000));

    try {
      TestingRig.execute(recipe, rows);
      Assert.fail("Expected RecipeException to be thrown");
    } catch (RecipeException e) {
      // Debugging: Print the exception message
      System.out.println("Exception message: " + e.getMessage());
      // Exception caught, test passes
      Assert.assertTrue(e.getMessage().contains("Invalid time unit"));
      Assert.assertTrue(e.getMessage().contains("invalidTime")); // Ensure invalid value is mentioned
      Assert.assertTrue(e.getMessage().contains("Supported units are"));
    } catch (Exception e) {
      // Catch other exceptions and fail the test
      Assert.fail("Expected RecipeException, but caught: " + e.getClass().getSimpleName());
    }
  }

}
