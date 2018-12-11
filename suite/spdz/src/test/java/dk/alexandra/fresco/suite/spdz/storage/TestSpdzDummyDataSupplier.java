package dk.alexandra.fresco.suite.spdz.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import dk.alexandra.fresco.framework.builder.numeric.FieldDefinitionBigInteger;
import dk.alexandra.fresco.framework.builder.numeric.FieldElement;
import dk.alexandra.fresco.framework.builder.numeric.FieldElementBigInteger;
import dk.alexandra.fresco.framework.builder.numeric.ModulusBigInteger;
import dk.alexandra.fresco.framework.util.TransposeUtils;
import dk.alexandra.fresco.suite.spdz.datatypes.SpdzInputMask;
import dk.alexandra.fresco.suite.spdz.datatypes.SpdzSInt;
import dk.alexandra.fresco.suite.spdz.datatypes.SpdzTriple;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import org.junit.Test;

public class TestSpdzDummyDataSupplier {

  private final List<BigInteger> moduli = Arrays.asList(
      new BigInteger("251"),
      new BigInteger("340282366920938463463374607431768211283"),
      new BigInteger(
          "2582249878086908589655919172003011874329705792829223512830659356540647622016841"
              + "194629645353280137831435903171972747493557")
  );

  private List<SpdzDummyDataSupplier> setupSuppliers(int noOfParties,
      BigInteger modulus) {
    return setupSuppliers(noOfParties, modulus, 200);
  }

  private List<SpdzDummyDataSupplier> setupSuppliers(int noOfParties,
      BigInteger modulus, int expPipeLength) {
    List<SpdzDummyDataSupplier> suppliers = new ArrayList<>(noOfParties);
    Random random = new Random();
    for (int i = 0; i < noOfParties; i++) {
      BigInteger macKeyShare = new BigInteger(modulus.bitLength(), random)
          .mod(modulus);
      suppliers.add(
          new SpdzDummyDataSupplier(i + 1, noOfParties,
              new FieldDefinitionBigInteger(new ModulusBigInteger(modulus)),
              macKeyShare,
              expPipeLength));
    }
    return suppliers;
  }

  private FieldElement getMacKeyFromSuppliers(List<SpdzDummyDataSupplier> suppliers) {
    FieldElement macKey = new FieldElementBigInteger(0,
        new ModulusBigInteger("2582249878086908589655919172003011874329705"
            + "792829223512830659356540647622016841194629645353280137831435903171972747493557"));
    for (SpdzDummyDataSupplier supplier : suppliers) {
      macKey = macKey.add(supplier.getSecretSharedKey());
    }
    return macKey;
  }

  private void testGetNextTriple(int noOfParties, BigInteger modulus) {
    List<SpdzDummyDataSupplier> suppliers = setupSuppliers(noOfParties, modulus);
    FieldElement macKey = getMacKeyFromSuppliers(suppliers);
    List<SpdzTriple> triples = new ArrayList<>(noOfParties);
    for (SpdzDummyDataSupplier supplier : suppliers) {
      triples.add(supplier.getNextTriple());
    }
    SpdzTriple recombined = recombineTriples(triples);
    assertTripleValid(recombined, macKey);
  }

  private void testGetNextTriple(int noOfParties) {
    for (BigInteger modulus : moduli) {
      testGetNextTriple(noOfParties, modulus);
    }
  }

  private void testGetNextInputMask(int noOfParties, int towardParty,
      BigInteger modulus) {
    List<SpdzDummyDataSupplier> suppliers = setupSuppliers(noOfParties, modulus);
    FieldElement macKey = getMacKeyFromSuppliers(suppliers);
    List<SpdzInputMask> masks = new ArrayList<>(noOfParties);
    for (SpdzDummyDataSupplier supplier : suppliers) {
      masks.add(supplier.getNextInputMask(towardParty));
    }
    FieldElement realValue = null;
    List<SpdzSInt> shares = new ArrayList<>(noOfParties);
    for (int i = 0; i < noOfParties; i++) {
      SpdzInputMask spdzInputMask = masks.get(i);
      if (i + 1 != towardParty) {
        assertEquals(null, spdzInputMask.getRealValue());
      } else {
        realValue = spdzInputMask.getRealValue();
      }
      shares.add(spdzInputMask.getMask());
    }
    SpdzSInt recombined = recombine(shares);
    assertMacCorrect(recombined, macKey);
    assertEquals(realValue, recombined.getShare());
  }

  private void testGetNextInputMask(int noOfParties, int towardParty) {
    for (BigInteger modulus : moduli) {
      testGetNextInputMask(noOfParties, towardParty, modulus);
    }
  }

  private void testGetNextBit(int noOfParties, BigInteger modulus) {
    List<SpdzDummyDataSupplier> suppliers = setupSuppliers(noOfParties, modulus);
    FieldElement macKey = getMacKeyFromSuppliers(suppliers);
    List<SpdzSInt> bitShares = new ArrayList<>(noOfParties);
    for (SpdzDummyDataSupplier supplier : suppliers) {
      bitShares.add(supplier.getNextBit());
    }
    SpdzSInt recombined = recombine(bitShares);
    assertMacCorrect(recombined, macKey);
    FieldElement value = recombined.getShare();
    assertTrue("Value not a bit " + value,
        value.equals(BigInteger.ZERO) || value.convertToBigInteger().equals(BigInteger.ONE));
  }

  private void testGetNextBit(int noOfParties) {
    for (BigInteger modulus : moduli) {
      testGetNextBit(noOfParties, modulus);
    }
  }

  private void testGetNextRandomFieldElement(int noOfParties, BigInteger modulus) {
    List<SpdzDummyDataSupplier> suppliers = setupSuppliers(noOfParties, modulus);
    FieldElement macKey = getMacKeyFromSuppliers(suppliers);
    List<SpdzSInt> bitShares = new ArrayList<>(noOfParties);
    for (SpdzDummyDataSupplier supplier : suppliers) {
      bitShares.add(supplier.getNextRandomFieldElement());
    }
    SpdzSInt recombined = recombine(bitShares);
    assertMacCorrect(recombined, macKey);
    // sanity check not zero (with 251, that is actually not unlikely enough)
    if (!modulus.equals(new BigInteger("251"))) {
      FieldElement value = recombined.getShare();
      assertFalse("Random value was 0 ", value.convertToBigInteger().equals(BigInteger.ZERO));
    }
  }

  private void testGetNextRandomFieldElement(int noOfParties) {
    for (BigInteger modulus : moduli) {
      testGetNextRandomFieldElement(noOfParties, modulus);
    }
  }

  private void testGetNextExpPipe(int noOfParties, BigInteger modulus,
      int expPipeLength) {
    List<SpdzDummyDataSupplier> suppliers = setupSuppliers(noOfParties, modulus);
    FieldElement macKey = getMacKeyFromSuppliers(suppliers);
    List<SpdzSInt[]> expPipes = new ArrayList<>(noOfParties);
    for (SpdzDummyDataSupplier supplier : suppliers) {
      expPipes.add(supplier.getNextExpPipe());
    }
    for (SpdzSInt[] expPipe : expPipes) {
      assertEquals(expPipeLength + 1, expPipe.length);
    }
    List<List<SpdzSInt>> unwrapped = expPipes.stream()
        .map(pipe -> Arrays.stream(pipe).collect(Collectors.toList()))
        .collect(Collectors.toList());
    List<SpdzSInt> recombined = recombineExpPipe(unwrapped);
    assertExpPipeValid(recombined, macKey, modulus);
  }

  private void testGetNextExpPipe(int noOfParties) {
    for (BigInteger modulus : moduli) {
      testGetNextExpPipe(noOfParties, modulus, 200);
    }
  }

  @Test
  public void testGetNextTriple() {
    testGetNextTriple(2);
    testGetNextTriple(3);
    testGetNextTriple(5);
  }

  @Test
  public void testGetNextExpPipe() {
    testGetNextExpPipe(2);
    testGetNextExpPipe(3);
    testGetNextExpPipe(5);
  }

  @Test
  public void testGetNextInputMask() {
    List<Integer> partyCounts = Arrays.asList(2, 3, 5);
    for (int partyCount : partyCounts) {
      for (int i = 0; i < partyCount; i++) {
        testGetNextInputMask(partyCount, i + 1);
      }
    }
  }

  @Test
  public void testGetNextBit() {
    testGetNextBit(2);
    testGetNextBit(3);
    testGetNextBit(5);
  }

  @Test
  public void testGetNextRandomFieldElement() {
    testGetNextRandomFieldElement(2);
    testGetNextRandomFieldElement(3);
    testGetNextRandomFieldElement(5);
  }

  @Test
  public void testGetters() {
    SpdzDummyDataSupplier supplier = new SpdzDummyDataSupplier(1, 2,
        new FieldDefinitionBigInteger(new ModulusBigInteger(moduli.get(0))), BigInteger.ONE);
    assertEquals(moduli.get(0), supplier.getModulus());
    assertEquals(BigInteger.ONE, supplier.getSecretSharedKey().convertToBigInteger());
  }

  private SpdzSInt recombine(List<SpdzSInt> shares) {
    return shares.stream().reduce(SpdzSInt::add).get();
  }

  private List<SpdzSInt> recombineExpPipe(List<List<SpdzSInt>> expPipeShares) {
    return TransposeUtils.transpose(expPipeShares).stream()
        .map(this::recombine)
        .collect(Collectors.toList());
  }

  private SpdzTriple recombineTriples(List<SpdzTriple> triples) {
    List<SpdzSInt> left = new ArrayList<>(triples.size());
    List<SpdzSInt> right = new ArrayList<>(triples.size());
    List<SpdzSInt> product = new ArrayList<>(triples.size());
    for (SpdzTriple triple : triples) {
      left.add(triple.getA());
      right.add(triple.getB());
      product.add(triple.getC());
    }
    return new SpdzTriple(recombine(left), recombine(right), recombine(product));
  }

  private void assertMacCorrect(SpdzSInt recombined, FieldElement macKey) {
    FieldElement share = recombined.getShare().multiply(macKey);
    assertEquals(share, recombined.getMac());
  }

  private void assertTripleValid(SpdzTriple recombined, FieldElement macKey) {
    assertMacCorrect(recombined.getA(), macKey);
    assertMacCorrect(recombined.getB(), macKey);
    assertMacCorrect(recombined.getC(), macKey);

    FieldElement copy = recombined.getA().getShare().multiply(recombined.getB().getShare());
    // check that a * b = c
    assertEquals(recombined.getC().getShare(), copy
    );
  }

  private void assertExpPipeValid(List<SpdzSInt> recombined, FieldElement macKey,
      BigInteger modulus) {
    for (SpdzSInt element : recombined) {
      assertMacCorrect(element, macKey);
    }
    List<FieldElement> values = recombined.stream().map(SpdzSInt::getShare)
        .collect(Collectors.toList());
    FieldElement inverted = values.get(0);
    FieldElement first = values.get(1);
    BigInteger bigInteger = first.convertToBigInteger().modInverse(modulus);
    assertEquals(inverted.convertToBigInteger(), bigInteger);
    for (int i = 1; i < values.size(); i++) {
      BigInteger expected = first.convertToBigInteger()
          .modPow(BigInteger.valueOf(i), modulus);
      assertEquals(expected, values.get(i).convertToBigInteger());
    }
  }
}
