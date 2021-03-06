package dk.alexandra.fresco.suite.spdz.storage;

import dk.alexandra.fresco.framework.builder.numeric.field.FieldDefinition;
import dk.alexandra.fresco.framework.builder.numeric.field.FieldElement;
import dk.alexandra.fresco.framework.util.ArithmeticDummyDataSupplier;
import dk.alexandra.fresco.framework.util.ModularReductionAlgorithm;
import dk.alexandra.fresco.framework.util.MultiplicationTripleShares;
import dk.alexandra.fresco.framework.util.Pair;
import dk.alexandra.fresco.suite.spdz.datatypes.SpdzInputMask;
import dk.alexandra.fresco.suite.spdz.datatypes.SpdzSInt;
import dk.alexandra.fresco.suite.spdz.datatypes.SpdzTriple;
import java.math.BigInteger;
import java.util.List;

public class SpdzDummyDataSupplier implements SpdzDataSupplier {

  private final int myId;
  private final ArithmeticDummyDataSupplier supplier;
  private final FieldDefinition fieldDefinition;
  private final BigInteger secretSharedKey;
  private final int expPipeLength;
  private final ModularReductionAlgorithm reducer;

  public SpdzDummyDataSupplier(int myId, int noOfPlayers, FieldDefinition fieldDefinition,
      BigInteger secretSharedKey) {
    this(myId, noOfPlayers, fieldDefinition, secretSharedKey, 200);
  }

  public SpdzDummyDataSupplier(int myId, int noOfPlayers, FieldDefinition fieldDefinition,
      BigInteger secretSharedKey, int expPipeLength) {
    this.myId = myId;
    this.fieldDefinition = fieldDefinition;
    this.secretSharedKey = secretSharedKey;
    this.expPipeLength = expPipeLength;
    this.supplier =
        new ArithmeticDummyDataSupplier(myId, noOfPlayers, fieldDefinition.getModulus());
    this.reducer = ModularReductionAlgorithm.getReductionAlgorithm(fieldDefinition.getModulus());
  }

  @Override
  public SpdzTriple getNextTriple() {
    MultiplicationTripleShares rawTriple = supplier.getMultiplicationTripleShares();
    return new SpdzTriple(
        toSpdzSInt(rawTriple.getLeft()),
        toSpdzSInt(rawTriple.getRight()),
        toSpdzSInt(rawTriple.getProduct()));
  }

  @Override
  public SpdzSInt[] getNextExpPipe() {
    List<Pair<BigInteger, BigInteger>> rawExpPipe = supplier.getExpPipe(expPipeLength);
    return rawExpPipe.stream()
        .map(this::toSpdzSInt)
        .toArray(SpdzSInt[]::new);
  }

  @Override
  public SpdzInputMask getNextInputMask(int towardPlayerId) {
    Pair<BigInteger, BigInteger> raw = supplier.getRandomElementShare();
    if (myId == towardPlayerId) {
      return new SpdzInputMask(toSpdzSInt(raw), createElement(raw.getFirst()));
    } else {
      return new SpdzInputMask(toSpdzSInt(raw), null);
    }
  }

  @Override
  public SpdzSInt getNextBit() {
    return toSpdzSInt(supplier.getRandomBitShare());
  }

  @Override
  public FieldDefinition getFieldDefinition() {
    return fieldDefinition;
  }

  @Override
  public FieldElement getSecretSharedKey() {
    return createElement(secretSharedKey);
  }

  @Override
  public SpdzSInt getNextRandomFieldElement() {
    return toSpdzSInt(supplier.getRandomElementShare());
  }

  private SpdzSInt toSpdzSInt(Pair<BigInteger, BigInteger> raw) {
    return new SpdzSInt(
        createElement(raw.getSecond()),
        createElement(reducer.apply(raw.getFirst().multiply(secretSharedKey)))
    );
  }

  private FieldElement createElement(BigInteger value) {
    return fieldDefinition.createElement(value);
  }
}
