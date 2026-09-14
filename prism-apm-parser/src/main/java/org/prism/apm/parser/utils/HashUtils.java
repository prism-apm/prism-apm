package org.prism.apm.parser.utils;

import net.openhft.hashing.LongTupleHashFunction;

import java.util.UUID;

/**
 * Utility for hashing strings to 128-bit values (UUID) using XXH3.
 */
public class HashUtils {

    /**
     * Generate a 128-bit hash for the given string using XXH3 and return it as a
     * UUID.
     *
     * @param input the input string
     * @return the UUID representing the 128-bit hash
     */
    public static UUID hash128(String input) {
        if (input == null) {
            return null;
        }
        // XXH3 128-bit hash
        // The API returns a long array [lsb, msb] or similar.
        // version 0.x API: LongTupleHashFunction.xx128()
        long[] hashResult = LongTupleHashFunction.xx128().hashChars(input);

        // hashResult[0] and hashResult[1] are the two 64-bit parts.
        // We map them to UUID (msb, lsb).
        return new UUID(hashResult[1], hashResult[0]);
    }

}
