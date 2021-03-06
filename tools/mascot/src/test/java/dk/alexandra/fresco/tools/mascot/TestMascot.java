package dk.alexandra.fresco.tools.mascot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import dk.alexandra.fresco.framework.builder.numeric.Addable;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.tools.mascot.field.AuthenticatedElement;
import dk.alexandra.fresco.tools.mascot.field.InputMask;
import dk.alexandra.fresco.tools.mascot.field.MultiplicationTriple;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.Test;

public class TestMascot extends NetworkedTest {

  private final FieldElement macKeyShareOne = getFieldDefinition().createElement(11231);
  private final FieldElement macKeyShareTwo = getFieldDefinition().createElement(7719);

  private List<MultiplicationTriple> runTripleGen(MascotTestContext ctx, FieldElement macKeyShare,
      int numTriples) {
    Mascot mascot = new Mascot(ctx.getResourcePool(), ctx.getNetwork(), macKeyShare);
    return mascot.getTriples(numTriples);
  }

  private List<AuthenticatedElement> runRandomElementGeneration(MascotTestContext ctx,
      FieldElement macKeyShare, int numElements) {
    Mascot mascot = new Mascot(ctx.getResourcePool(), ctx.getNetwork(), macKeyShare);
    return mascot.getRandomElements(numElements);
  }

  private List<AuthenticatedElement> runRandomBitGeneration(MascotTestContext ctx,
      FieldElement macKeyShare, int numBits) {
    Mascot mascot = new Mascot(ctx.getResourcePool(), ctx.getNetwork(), macKeyShare);
    return mascot.getRandomBits(numBits);
  }

  private List<AuthenticatedElement> runInputter(MascotTestContext ctx, FieldElement macKeyShare,
      List<FieldElement> inputs) {
    Mascot mascot = new Mascot(ctx.getResourcePool(), ctx.getNetwork(), macKeyShare);
    return mascot.input(inputs);
  }

  private List<AuthenticatedElement> runNonInputter(MascotTestContext ctx, FieldElement macKeyShare,
      Integer inputterId, int numInputs) {
    Mascot mascot = new Mascot(ctx.getResourcePool(), ctx.getNetwork(), macKeyShare);
    return mascot.input(inputterId, numInputs);
  }

  private List<InputMask> runInputMask(MascotTestContext ctx, Integer inputterId, int numMasks,
      FieldElement macKeyShare) {
    Mascot mascot = new Mascot(ctx.getResourcePool(), ctx.getNetwork(), macKeyShare);
    return mascot.getInputMasks(inputterId, numMasks);
  }

  @Test
  public void testTriple() {
    // set up runtime environment and get contexts
    initContexts(2);

    // define per party task with params
    List<Callable<List<MultiplicationTriple>>> tasks = new ArrayList<>();
    tasks.add(() -> runTripleGen(contexts.get(1), macKeyShareOne, 1));
    tasks.add(() -> runTripleGen(contexts.get(2), macKeyShareTwo, 1));

    List<List<MultiplicationTriple>> results = testRuntime.runPerPartyTasks(tasks);
    assertEquals(results.get(0).size(), 1);
    assertEquals(results.get(1).size(), 1);
    List<MultiplicationTriple> combined = Addable.sumRows(results);
    for (MultiplicationTriple triple : combined) {
      CustomAsserts
          .assertTripleIsValid(getFieldDefinition(), triple, macKeyShareOne.add(macKeyShareTwo));
    }
  }

  @Test
  public void testRandomGen() {
    // set up runtime environment and get contexts
    initContexts(2);

    // define per party task with params
    List<Callable<List<AuthenticatedElement>>> tasks = new ArrayList<>();
    tasks.add(() -> runRandomElementGeneration(contexts.get(1), macKeyShareOne, 1));
    tasks.add(() -> runRandomElementGeneration(contexts.get(2), macKeyShareTwo, 1));

    List<List<AuthenticatedElement>> results = testRuntime.runPerPartyTasks(tasks);
    assertEquals(results.get(0).size(), 1);
    assertEquals(results.get(1).size(), 1);
    AuthenticatedElement recombined = Addable.sumRows(results).get(0);
    // sanity check
    BigInteger opened = getFieldDefinition().convertToUnsigned(recombined.getShare());
    assertNotEquals(BigInteger.ZERO, opened);
  }

  @Test
  public void testRandomBitGen() {
    // set up runtime environment and get contexts
    initContexts(2);

    // define per party task with params
    List<Callable<List<AuthenticatedElement>>> tasks = new ArrayList<>();
    tasks.add(() -> runRandomBitGeneration(contexts.get(1), macKeyShareOne, 1));
    tasks.add(() -> runRandomBitGeneration(contexts.get(2), macKeyShareTwo, 1));

    List<List<AuthenticatedElement>> results = testRuntime.runPerPartyTasks(tasks);
    assertEquals(results.get(0).size(), 1);
    assertEquals(results.get(1).size(), 1);

    AuthenticatedElement bit = results.get(0).get(0).add(results.get(1).get(0));
    FieldElement actualBit = bit.getShare();
    CustomAsserts.assertFieldElementIsBit(getFieldDefinition(), actualBit);
  }

  @Test
  public void testInputMask() {
    // set up runtime environment and get contexts
    initContexts(2);
    int numMasks = 16;

    // define per party task with params
    List<Callable<List<InputMask>>> tasks = new ArrayList<>();
    tasks.add(() -> runInputMask(contexts.get(1), 1, numMasks, macKeyShareOne));
    tasks.add(() -> runInputMask(contexts.get(2), 1, numMasks, macKeyShareTwo));

    List<List<InputMask>> results = testRuntime.runPerPartyTasks(tasks);
    List<InputMask> leftMasks = results.get(0);
    List<InputMask> rightMasks = results.get(1);
    assertEquals(results.get(0).size(), numMasks);
    assertEquals(results.get(1).size(), numMasks);

    FieldElement macKey = macKeyShareOne.add(macKeyShareTwo);
    for (int i = 0; i < leftMasks.size(); i++) {
      InputMask left = leftMasks.get(i);
      InputMask right = rightMasks.get(i);
      assertNull(right.getOpenValue());
      AuthenticatedElement recombined = left.getMaskShare().add(right.getMaskShare());
      AuthenticatedElement expected = new AuthenticatedElement(left.getOpenValue(),
          left.getOpenValue().multiply(macKey));
      CustomAsserts.assertEquals(getFieldDefinition(), expected, recombined);
    }

  }

  @Test
  public void testInput() {
    // set up runtime environment and get contexts
    initContexts(2);

    FieldElement input = getFieldDefinition().createElement(12345);

    // define per party task with params
    List<Callable<List<AuthenticatedElement>>> tasks = new ArrayList<>();
    tasks.add(() -> runInputter(contexts.get(1), macKeyShareOne, Collections.singletonList(input)));
    tasks.add(() -> runNonInputter(contexts.get(2), macKeyShareTwo, 1, 1));

    List<List<AuthenticatedElement>> results = testRuntime.runPerPartyTasks(tasks);
    assertEquals(results.get(0).size(), 1);
    assertEquals(results.get(1).size(), 1);
    List<AuthenticatedElement> combined =
        Addable.sumRows(results);
    FieldElement actualRecombinedValue = combined.get(0).getShare();
    FieldElement actualRecombinedMac = combined.get(0).getMac();
    CustomAsserts.assertEquals(getFieldDefinition(), input, actualRecombinedValue);
    FieldElement expectedMac = input.multiply(macKeyShareOne.add(macKeyShareTwo));
    CustomAsserts.assertEquals(getFieldDefinition(), expectedMac, actualRecombinedMac);
  }

  @Test
  public void testTripleDifferentModBiLength() {
    // set up runtime environment and get contexts
    initContexts(2, 8, new MascotSecurityParameters(8, 256, 3));

    FieldElement macKeyShareOne = getFieldDefinition().createElement(111);
    FieldElement macKeyShareTwo = getFieldDefinition().createElement(212);

    // define per party task with params
    List<Callable<List<MultiplicationTriple>>> tasks = new ArrayList<>();
    tasks.add(() -> runTripleGen(contexts.get(1), macKeyShareOne, 1));
    tasks.add(() -> runTripleGen(contexts.get(2), macKeyShareTwo, 1));

    List<List<MultiplicationTriple>> results = testRuntime.runPerPartyTasks(tasks);
    assertEquals(results.get(0).size(), 1);
    assertEquals(results.get(1).size(), 1);
    List<MultiplicationTriple> combined = Addable.sumRows(results);
    for (MultiplicationTriple triple : combined) {
      CustomAsserts
          .assertTripleIsValid(getFieldDefinition(), triple, macKeyShareOne.add(macKeyShareTwo));
    }
  }

}
