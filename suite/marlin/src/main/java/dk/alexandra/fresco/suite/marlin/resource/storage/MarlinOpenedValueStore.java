package dk.alexandra.fresco.suite.marlin.resource.storage;

import dk.alexandra.fresco.framework.util.Pair;
import dk.alexandra.fresco.suite.marlin.datatypes.BigUInt;
import dk.alexandra.fresco.suite.marlin.datatypes.MarlinSInt;
import java.util.Collections;
import java.util.List;

/**
 * A class that stores all opened values along with their macs for subsequent mac checks.
 */
public interface MarlinOpenedValueStore<T extends BigUInt<T>> {

  /**
   * Store elements with macs that were just opened along with the corresponding open values.
   */
  void pushOpenedValues(List<MarlinSInt<T>> newSharesWithMacs, List<T> newOpenedValues);

  default void pushOpenedValue(MarlinSInt<T> newShareWithMac, T newOpenedValue) {
    pushOpenedValues(Collections.singletonList(newShareWithMac),
        Collections.singletonList(newOpenedValue));
  }

  /**
   * Retrieve all values that haven't been checked yet and clears the store.
   */
  Pair<List<MarlinSInt<T>>, List<T>> popValues();

  /**
   * Check if there are unchecked values.
   */
  boolean isEmpty();

  /**
   * Check number of unchecked values.
   */
  int size();

}
