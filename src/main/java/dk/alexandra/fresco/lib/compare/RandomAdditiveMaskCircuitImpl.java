/*******************************************************************************
 * Copyright (c) 2015 FRESCO (http://github.com/aicis/fresco).
 *
 * This file is part of the FRESCO project.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * FRESCO uses SCAPI - http://crypto.biu.ac.il/SCAPI, Crypto++, Miracl, NTL,
 * and Bouncy Castle. Please see these projects for any further licensing issues.
 *******************************************************************************/
package dk.alexandra.fresco.lib.compare;

import dk.alexandra.fresco.framework.ProtocolProducer;
import dk.alexandra.fresco.framework.value.OInt;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.lib.field.integer.BasicNumericFactory;
import dk.alexandra.fresco.lib.helper.AbstractSimpleProtocol;
import dk.alexandra.fresco.lib.helper.ParallelProtocolProducer;
import dk.alexandra.fresco.lib.helper.sequential.SequentialProtocolProducer;
import dk.alexandra.fresco.lib.math.integer.PreprocessedNumericBitFactory;
import dk.alexandra.fresco.lib.math.integer.linalg.InnerProductFactory;

/**
 * Load random value used as additive mask + bits
 * 
 * @author ttoft
 *
 */
public class RandomAdditiveMaskCircuitImpl extends AbstractSimpleProtocol implements
		RandomAdditiveMaskCircuit {

	private final int bitLength;
	private final int securityParameter;
	private final InnerProductFactory innerProdProvider;
	private final PreprocessedNumericBitFactory bitProvider;
	private final MiscOIntGenerators miscOIntGenerator;
	private final SInt[] rBits;
	private final SInt r;
	private BasicNumericFactory basicNumericFactory;

	/**
	 * Circuit taking no input and generating uniformly random r in Z_{2^{l+k}}
	 * along with the bits of r mod 2^l
	 * 
	 * @param bitLength
	 *            -- the desired number of least significant bits, l
	 * @param securityParameter
	 *            -- the desired security parameter, k, (leakage with
	 *            probability 2^{-k}
	 * @param bits
	 *            the first l bits of r
	 * @param r
	 * 
	 */
	public RandomAdditiveMaskCircuitImpl(int securityParameter, SInt[] bits, SInt r,
			BasicNumericFactory basicNumericFactory, PreprocessedNumericBitFactory bitProvider,
			MiscOIntGenerators miscOIntGenerator, InnerProductFactory innerProdProvider) {
		// Copy inputs, setup stuff
		this.bitLength = bits.length;
		this.securityParameter = securityParameter;
		this.rBits = bits;
		this.r = r;
		this.basicNumericFactory = basicNumericFactory;
		this.innerProdProvider = innerProdProvider;
		this.miscOIntGenerator = miscOIntGenerator;
		this.bitProvider = bitProvider;
	}

	@Override
	protected ProtocolProducer initializeGateProducer() {

		// loadRandBits
		// bits[i] = i'th bit; 0 <= i < bitLength
		SInt[] allbits = new SInt[bitLength + securityParameter];
		ParallelProtocolProducer randomBits = new ParallelProtocolProducer();
		int i;
		for (i = 0; i < bitLength; i++) {
			randomBits.append(bitProvider.createRandomSecretSharedBitProtocol(rBits[i]));
			allbits[i] = rBits[i];
		}
		for (i = bitLength; i < bitLength + securityParameter; i++) {
			allbits[i] = basicNumericFactory.getSInt();
			randomBits.append(bitProvider.createRandomSecretSharedBitProtocol(allbits[i]));
		}

		OInt[] twoPows = miscOIntGenerator.getTwoPowers(securityParameter + bitLength);
		return new SequentialProtocolProducer(randomBits, innerProdProvider.getInnerProductCircuit(
				allbits, twoPows, r));
	}

}
