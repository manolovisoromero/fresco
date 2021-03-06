package dk.alexandra.fresco.framework.builder;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.DRes;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThread;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThreadFactory;
import dk.alexandra.fresco.framework.builder.numeric.ProtocolBuilderNumeric;
import dk.alexandra.fresco.framework.sce.resources.ResourcePool;
import dk.alexandra.fresco.framework.util.Pair;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.suite.dummy.arithmetic.AbstractDummyArithmeticTest;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class TestBuildStep extends AbstractDummyArithmeticTest {

  @Test
  public void test_while_no_iteration() {
    runTest(new TestWhileLoop<>(0, Collections.emptyList()), new TestParameters());
  }

  @Test
  public void test_while_single_iteration() {
    runTest(new TestWhileLoop<>(1, Collections.singletonList(0)), new TestParameters());
  }

  @Test
  public void test_while_multiple_iterations() {
    runTest(
        new TestWhileLoop<>(10, Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)), new TestParameters());
  }

  @Test
  public void test_pair_in_par() {
    runTest(new TestPairInPar<>(1, 2, new Pair<>(1, 2)), new TestParameters());
  }

  @Test
  public void test_early_out() {
    runTest(new TestEarlyOut<>(), new TestParameters());
  }

  private static final class IterationState implements DRes<IterationState> {

    private final int round;
    private final List<Integer> rounds;

    private IterationState(int round, List<Integer> rounds) {
      this.round = round;
      this.rounds = rounds;
    }

    @Override
    public IterationState out() {
      return this;
    }
  }

  private static final class State implements DRes<State> {

    private final int firstValue;
    private final int secondValue;

    private State(int firstValue, int seconValue) {
      this.firstValue = firstValue;
      this.secondValue = seconValue;
    }

    @Override
    public State out() {
      return this;
    }
  }

  /**
   * Tests that whileLoop method performs correct number of iterations.
   *
   * @param <ResourcePoolT>
   */
  private class TestWhileLoop<ResourcePoolT extends ResourcePool>
      extends TestThreadFactory<ResourcePoolT, ProtocolBuilderNumeric> {

    protected final int numIterations;
    protected final List<Integer> expected;

    public TestWhileLoop(int numIterations, List<Integer> expected) {
      this.numIterations = numIterations;
      this.expected = expected;
    }

    @Override
    public TestThread<ResourcePoolT, ProtocolBuilderNumeric> next() {
      return new TestThread<ResourcePoolT, ProtocolBuilderNumeric>() {
        @Override
        public void test() {
          // define functionality to be tested
          Application<List<Integer>, ProtocolBuilderNumeric> testApplication =
              root -> root.seq(seq -> {
                // initiate loop
                return new IterationState(0, new ArrayList<>());
              }).whileLoop(
                  // iterate
                  (state) -> state.round < numIterations,
                  (seq, state) -> {
                    List<Integer> roundsSoFar = state.rounds;
                    roundsSoFar.add(state.round);
                    return new IterationState(state.round + 1, roundsSoFar);
                  }).seq((seq, state) -> () -> state.rounds);
          List<Integer> actual = runApplication(testApplication);
          Assert.assertEquals(expected, actual);
        }
      };
    }
  }

  private class TestPairInPar<ResourcePoolT extends ResourcePool>
      extends TestThreadFactory<ResourcePoolT, ProtocolBuilderNumeric> {

    protected final int firstValue;
    protected final int secondValue;
    protected final Pair<Integer, Integer> expected;

    public TestPairInPar(int firstValue, int secondValue, Pair<Integer, Integer> expected) {
      this.firstValue = firstValue;
      this.secondValue = secondValue;
      this.expected = expected;
    }

    @Override
    public TestThread<ResourcePoolT, ProtocolBuilderNumeric> next() {
      return new TestThread<ResourcePoolT, ProtocolBuilderNumeric>() {
        @Override
        public void test() {
          Application<Pair<Integer, Integer>, ProtocolBuilderNumeric> testApplication =
              root ->
                  root.seq(seq -> new TestBuildStep.State(firstValue, secondValue))
                      .pairInPar(
                          (seq, state) -> () -> state.firstValue,
                          (seq, state) -> () -> state.secondValue);

          Pair<Integer, Integer> actual = runApplication(testApplication);
          Assert.assertEquals(expected, actual);
        }
      };
    }
  }

  private class TestEarlyOut<ResourcePoolT extends ResourcePool>
      extends TestThreadFactory<ResourcePoolT, ProtocolBuilderNumeric> {

    @Override
    public TestThread<ResourcePoolT, ProtocolBuilderNumeric> next() {
      return new TestThread<ResourcePoolT, ProtocolBuilderNumeric>() {
        @Override
        public void test() {
          Application<BigInteger, ProtocolBuilderNumeric> testApplication =
              root -> root.seq(seq -> {
                DRes<SInt> x = seq.seq(inner -> inner.numeric().input(1, 1));
                Assert.assertNull(x.out());
                return null;
              }).seq((seq, x) -> {
                return () -> BigInteger.ONE;
              });

          Assert.assertEquals(BigInteger.ONE, runApplication(testApplication));
        }
      };
    }
  }
}
