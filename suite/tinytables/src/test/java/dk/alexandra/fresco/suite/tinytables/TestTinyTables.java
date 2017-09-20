/*
 * Copyright (c) 2016 FRESCO (http://github.com/aicis/fresco).
 *
 * This file is part of the FRESCO project.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * FRESCO uses SCAPI - http://crypto.biu.ac.il/SCAPI, Crypto++, Miracl, NTL, and Bouncy Castle.
 * Please see these projects for any further licensing issues.
 *******************************************************************************/
package dk.alexandra.fresco.suite.tinytables;

import dk.alexandra.fresco.IntegrationTest;
import dk.alexandra.fresco.framework.ProtocolEvaluator;
import dk.alexandra.fresco.framework.TestThreadRunner;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThreadConfiguration;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThreadFactory;
import dk.alexandra.fresco.framework.builder.binary.ProtocolBuilderBinary;
import dk.alexandra.fresco.framework.configuration.NetworkConfiguration;
import dk.alexandra.fresco.framework.configuration.TestConfiguration;
import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.network.NetworkingStrategy;
import dk.alexandra.fresco.framework.network.ResourcePoolCreator;
import dk.alexandra.fresco.framework.sce.configuration.TestSCEConfiguration;
import dk.alexandra.fresco.framework.sce.evaluator.EvaluationStrategy;
import dk.alexandra.fresco.framework.sce.resources.ResourcePoolImpl;
import dk.alexandra.fresco.framework.util.DetermSecureRandom;
import dk.alexandra.fresco.lib.bool.BasicBooleanTests;
import dk.alexandra.fresco.lib.bool.ComparisonBooleanTests;
import dk.alexandra.fresco.lib.crypto.BristolCryptoTests;
import dk.alexandra.fresco.lib.math.bool.add.AddTests;
import dk.alexandra.fresco.suite.ProtocolSuite;
import dk.alexandra.fresco.suite.tinytables.online.TinyTablesProtocolSuite;
import dk.alexandra.fresco.suite.tinytables.prepro.TinyTablesPreproProtocolSuite;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class TestTinyTables {

  private void runTest(TestThreadFactory<ResourcePoolImpl, ProtocolBuilderBinary> f,
      EvaluationStrategy evalStrategy, boolean preprocessing, String name) throws Exception {
    int noPlayers = 2;
    // Since SCAPI currently does not work with ports > 9999 we use fixed
    // ports
    // here instead of relying on ephemeral ports which are often > 9999.
    List<Integer> ports = new ArrayList<>(noPlayers);
    for (int i = 1; i <= noPlayers; i++) {
      ports.add(9000 + i);
    }

    Map<Integer, NetworkConfiguration> netConf =
        TestConfiguration.getNetworkConfigurations(noPlayers, ports);
    Map<Integer, TestThreadConfiguration<ResourcePoolImpl, ProtocolBuilderBinary>> conf =
        new HashMap<>();

    for (int playerId : netConf.keySet()) {
      ProtocolEvaluator<ResourcePoolImpl, ProtocolBuilderBinary> evaluator;

      ProtocolSuite<ResourcePoolImpl, ProtocolBuilderBinary> suite;
      File tinyTablesFile = new File(getFilenameForTest(playerId, name));
      if (preprocessing) {
        suite = new TinyTablesPreproProtocolSuite(playerId, tinyTablesFile);
      } else {
        suite = new TinyTablesProtocolSuite(playerId, tinyTablesFile);
      }
      Network network = ResourcePoolCreator.getNetworkFromConfiguration(NetworkingStrategy.KRYONET,
          netConf.get(playerId));
      evaluator = EvaluationStrategy.fromEnum(evalStrategy);
      ResourcePoolImpl rp = new ResourcePoolImpl(playerId, noPlayers, network, new Random(),
          new DetermSecureRandom());
      TestThreadConfiguration<ResourcePoolImpl, ProtocolBuilderBinary> ttc =
          new TestThreadConfiguration<ResourcePoolImpl, ProtocolBuilderBinary>(
              netConf.get(playerId),
              new TestSCEConfiguration<ResourcePoolImpl, ProtocolBuilderBinary>(suite, evaluator,
                  netConf.get(playerId), false),
              rp);
      conf.put(playerId, ttc);
    }
    TestThreadRunner.run(f, conf);

  }

  /*
   * Helper methods
   */

  private String getFilenameForTest(int playerId, String name) {
    return "tinytables/TinyTables_" + name + "_" + playerId;
  }

  private static void deleteFileOrFolder(final Path path) throws IOException {
    Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
          throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(final Path file, final IOException e) {
        return handleException(e);
      }

      private FileVisitResult handleException(final IOException e) {
        e.printStackTrace(); // replace with more robust error handling
        return FileVisitResult.TERMINATE;
      }

      @Override
      public FileVisitResult postVisitDirectory(final Path dir, final IOException e)
          throws IOException {
        if (e != null) {
          return handleException(e);
        }
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  /*
   * Basic tests
   */

  // ensure that the tinytables folder is new for each test and is deleted upon exiting each test.
  @Before
  public void checkFolderExists() throws IOException {
    File f = new File("tinytables");
    if (f.exists()) {
      deleteFileOrFolder(f.toPath());
      f.mkdir();
    } else {
      f.mkdir();
    }
  }

  @After
  public void removeFolder() throws IOException {
    File f = new File("tinytables");
    if (f.exists()) {
      deleteFileOrFolder(f.toPath());
    }
  }

  @Test
  public void testInput() throws Exception {
    runTest(new BasicBooleanTests.TestInput<ResourcePoolImpl>(false), EvaluationStrategy.SEQUENTIAL,
        true, "testInput");
    runTest(new BasicBooleanTests.TestInput<ResourcePoolImpl>(true), EvaluationStrategy.SEQUENTIAL,
        false, "testInput");
  }

  @Test
  public void testXOR() throws Exception {
    runTest(new BasicBooleanTests.TestXOR<ResourcePoolImpl>(false), EvaluationStrategy.SEQUENTIAL,
        true, "testXOR");
    runTest(new BasicBooleanTests.TestXOR<ResourcePoolImpl>(true), EvaluationStrategy.SEQUENTIAL,
        false, "testXOR");
  }

  @Test
  public void testAND() throws Exception {
    runTest(new BasicBooleanTests.TestAND<ResourcePoolImpl>(false), EvaluationStrategy.SEQUENTIAL,
        true, "testAND");
    runTest(new BasicBooleanTests.TestAND<ResourcePoolImpl>(true), EvaluationStrategy.SEQUENTIAL,
        false, "testAND");
  }

  @Test
  public void testNOT() throws Exception {
    runTest(new BasicBooleanTests.TestNOT<ResourcePoolImpl>(false), EvaluationStrategy.SEQUENTIAL,
        true, "testNOT");
    runTest(new BasicBooleanTests.TestNOT<ResourcePoolImpl>(true), EvaluationStrategy.SEQUENTIAL,
        false, "testNOT");
  }

  @Test
  public void testBasicProtocols() throws Exception {
    runTest(new BasicBooleanTests.TestBasicProtocols<ResourcePoolImpl>(false),
        EvaluationStrategy.SEQUENTIAL, true, "testBasicProtocols");
    runTest(new BasicBooleanTests.TestBasicProtocols<ResourcePoolImpl>(true),
        EvaluationStrategy.SEQUENTIAL, false, "testBasicProtocols");
  }

  /* Bristol tests */

  @Category(IntegrationTest.class)
  @Test
  public void testMult() throws Exception {
    runTest(new BristolCryptoTests.Mult32x32Test<ResourcePoolImpl>(false),
        EvaluationStrategy.SEQUENTIAL, true, "testMult32x32");
    runTest(new BristolCryptoTests.Mult32x32Test<ResourcePoolImpl>(true),
        EvaluationStrategy.SEQUENTIAL, false, "testMult32x32");
  }

  @Category(IntegrationTest.class)
  @Test
  public void testAES() throws Exception {
    runTest(new BristolCryptoTests.AesTest<ResourcePoolImpl>(false), EvaluationStrategy.SEQUENTIAL,
        true, "testAES");
    runTest(new BristolCryptoTests.AesTest<ResourcePoolImpl>(true), EvaluationStrategy.SEQUENTIAL,
        false, "testAES");
  }

  @Category(IntegrationTest.class)
  @Test
  public void test_DES() throws Exception {
    runTest(new BristolCryptoTests.DesTest<ResourcePoolImpl>(false), EvaluationStrategy.SEQUENTIAL,
        true, "testDES");
    runTest(new BristolCryptoTests.DesTest<ResourcePoolImpl>(true), EvaluationStrategy.SEQUENTIAL,
        false, "testDES");
  }

  @Category(IntegrationTest.class)
  @Test
  public void test_SHA1() throws Exception {
    runTest(new BristolCryptoTests.Sha1Test<ResourcePoolImpl>(false), EvaluationStrategy.SEQUENTIAL,
        true, "testSHA1");
    runTest(new BristolCryptoTests.Sha1Test<ResourcePoolImpl>(true), EvaluationStrategy.SEQUENTIAL,
        false, "testSHA1");
  }

  @Category(IntegrationTest.class)
  @Test
  public void test_SHA256() throws Exception {
    runTest(new BristolCryptoTests.Sha256Test<ResourcePoolImpl>(false),
        EvaluationStrategy.SEQUENTIAL, true, "testSHA256");
    runTest(new BristolCryptoTests.Sha256Test<ResourcePoolImpl>(true),
        EvaluationStrategy.SEQUENTIAL, false, "testSHA256");
  }

  /* Advanced functionality */

  @Test
  public void test_Binary_Adder() throws Exception {
    runTest(new AddTests.TestFullAdder<ResourcePoolImpl>(false),
        EvaluationStrategy.SEQUENTIAL_BATCHED, true, "testAdder");
    runTest(new AddTests.TestFullAdder<ResourcePoolImpl>(true),
        EvaluationStrategy.SEQUENTIAL_BATCHED, false, "testAdder");
  }

  @Test
  public void test_comparison() throws Exception {
    runTest(new ComparisonBooleanTests.TestGreaterThan<ResourcePoolImpl>(false),
        EvaluationStrategy.SEQUENTIAL, true, "testGT");
    runTest(new ComparisonBooleanTests.TestGreaterThan<ResourcePoolImpl>(true),
        EvaluationStrategy.SEQUENTIAL, false, "testGT");
  }

  @Test
  public void test_equality() throws Exception {
    runTest(new ComparisonBooleanTests.TestEquality<ResourcePoolImpl>(false),
        EvaluationStrategy.SEQUENTIAL, true, "testEQ");
    runTest(new ComparisonBooleanTests.TestEquality<ResourcePoolImpl>(true),
        EvaluationStrategy.SEQUENTIAL, false, "testEQ");
  }
}
