package dk.alexandra.fresco.lib.common.math;

import dk.alexandra.fresco.framework.sce.evaluator.EvaluationStrategy;
import dk.alexandra.fresco.lib.bool.BasicBooleanTests;
import dk.alexandra.fresco.lib.common.collections.sort.CollectionsSortingTests;
import dk.alexandra.fresco.lib.common.collections.sort.CollectionsSortingTests.TestOddEvenMerge;
import dk.alexandra.fresco.lib.common.compare.CompareTests;
import dk.alexandra.fresco.lib.common.compare.ComparisonBooleanTests;
import dk.alexandra.fresco.lib.common.math.bool.add.AddTests;
import dk.alexandra.fresco.lib.common.math.bool.log.LogTests;
import dk.alexandra.fresco.lib.common.math.bool.mult.MultTests;
import dk.alexandra.fresco.suite.dummy.bool.AbstractDummyBooleanTest;
import org.junit.Test;

/**
 * Various tests of the dummy protocol suite.
 *
 * Currently, we simply test that AES works using the dummy protocol suite.
 */
public class TestBoolean extends AbstractDummyBooleanTest {

  // lib.math.bool
  @Test
  public void test_One_Bit_Half_Adder() {
    runTest(new AddTests.TestOnebitHalfAdder<>(), EvaluationStrategy.SEQUENTIAL_BATCHED);
  }

  @Test
  public void test_One_Bit_Full_Adder() {
    runTest(new AddTests.TestOnebitFullAdder<>(), EvaluationStrategy.SEQUENTIAL_BATCHED);
  }

  @Test
  public void test_Binary_Adder() {
    runTest(new AddTests.TestFullAdder<>(), EvaluationStrategy.SEQUENTIAL_BATCHED);
  }

  @Test
  public void test_Binary_BitIncrementAdder() {
    runTest(new AddTests.TestBitIncrement<>(), EvaluationStrategy.SEQUENTIAL_BATCHED);
  }

  @Test
  public void test_Binary_Mult() {
    runTest(new MultTests.TestBinaryMult<>(), EvaluationStrategy.SEQUENTIAL_BATCHED);
  }

  @Test
  public void test_basic_logic_all_in_one() {
    runTest(new BasicBooleanTests.TestBasicProtocols<>(true),
        EvaluationStrategy.SEQUENTIAL_BATCHED);
  }

  @Test
  public void test_Binary_Log_Nice() {
    runTest(new LogTests.TestLogNice<>(), EvaluationStrategy.SEQUENTIAL_BATCHED);
  }
}
