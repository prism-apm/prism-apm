package org.prism.apm.parser.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link StringUtils}.
 */
class StringUtilsTest {

    @Test
    @DisplayName("Should return null when input is null")
    void removeNullCharacters_NullInput() {
        // Arrange
        String input = null;

        // Act
        String result = StringUtils.removeNullCharacters(input);

        // Assert
        assertNull(result, "Expected null output for null input");
    }

    @Test
    @DisplayName("Should return original string when no null characters are present")
    void removeNullCharacters_NoNullChars() {
        // Arrange
        String input = "normal string";

        // Act
        String result = StringUtils.removeNullCharacters(input);

        // Assert
        assertEquals(input, result, "Should return the string unchanged if no null bytes exist");
    }

    @Test
    @DisplayName("Should remove single null character from middle of string")
    void removeNullCharacters_SingleNullChar() {
        // Arrange
        String input = "hello\u0000world";
        String expected = "helloworld";

        // Act
        String result = StringUtils.removeNullCharacters(input);

        // Assert
        assertEquals(expected, result, "Should remove the single null byte");
    }

    @Test
    @DisplayName("Should remove multiple null characters")
    void removeNullCharacters_MultipleNullChars() {
        // Arrange
        String input = "h\u0000e\u0000l\u0000l\u0000o";
        String expected = "hello";

        // Act
        String result = StringUtils.removeNullCharacters(input);

        // Assert
        assertEquals(expected, result, "Should remove all occurrences of null bytes");
    }

    @Test
    @DisplayName("Should return empty string when input contains only null characters")
    void removeNullCharacters_OnlyNullChars() {
        // Arrange
        String input = "\u0000\u0000\u0000";
        String expected = "";

        // Act
        String result = StringUtils.removeNullCharacters(input);

        // Assert
        assertEquals(expected, result, "Should return empty string if input is all null bytes");
    }

    @Test
    @DisplayName("Should handle empty string correctly")
    void removeNullCharacters_EmptyString() {
        // Arrange
        String input = "";
        String expected = "";

        // Act
        String result = StringUtils.removeNullCharacters(input);

        // Assert
        assertEquals(expected, result, "Should return empty string for empty input");
    }

    @Test
    @DisplayName("Should not affect other control characters like newline")
    void removeNullCharacters_OtherControlChars() {
        // Arrange
        String input = "line1\nline2\tpart";
        
        // Act
        String result = StringUtils.removeNullCharacters(input);

        // Assert
        assertEquals(input, result, "Should only remove \u0000 and leave \n or \t untouched");
    }
}
