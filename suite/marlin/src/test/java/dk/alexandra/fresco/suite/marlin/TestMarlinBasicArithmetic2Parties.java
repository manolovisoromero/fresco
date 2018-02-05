package dk.alexandra.fresco.suite.marlin;

import dk.alexandra.fresco.framework.sce.evaluator.EvaluationStrategy;
import dk.alexandra.fresco.lib.arithmetic.BasicArithmeticTests;
import org.junit.Ignore;
import org.junit.Test;

public class TestMarlinBasicArithmetic2Parties extends AbstractMarlinTest {

  @Ignore
  @Test
  public void testInput() {
    runTest(new BasicArithmeticTests.TestInput<>(), EvaluationStrategy.SEQUENTIAL_BATCHED, 2,
        false);
  }

}
