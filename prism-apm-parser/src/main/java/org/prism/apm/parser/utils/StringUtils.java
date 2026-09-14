package org.prism.apm.parser.utils;

/**
 * String utility class for common string operations.
 */
public class StringUtils {

    /**
     * Removes null characters ('\u0000') from the given string.
     *
     * @param value The string to sanitize.
     * @return The sanitized string with null characters removed, or the original value if null.
     */
    public static String removeNullCharacters(String value) {
        if (value != null && value.indexOf('\u0000') != -1) {
            return value.replace("\u0000", "");
        }
        return value;
    }
}
