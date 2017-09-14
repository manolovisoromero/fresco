package dk.alexandra.fresco.suite.spdz.datatypes;

import dk.alexandra.fresco.suite.spdz.gates.SpdzNativeProtocol;

/**
 * Marks a protocol as being able to output to some party. This means that the protocol should be
 * added to the list of output protocols in the current batch. It also means that calling eval
 * starting from round 1 will trigger the actual evaluation of the output protocol.
 * 
 * @author Kasper Damgaard
 *
 */
public abstract class SpdzOutputProtocol<OutputT> extends SpdzNativeProtocol<OutputT> {

}
