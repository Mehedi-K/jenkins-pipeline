package io.pipelinedemo.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("StringUtils")
class StringUtilsTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "  \n "})
    @DisplayName("isBlank is true for null, empty, and whitespace-only strings")
    void isBlankTrueCases(String value) {
        assertTrue(StringUtils.isBlank(value));
    }

    @Test
    @DisplayName("isBlank is false for non-blank strings")
    void isBlankFalseCase() {
        assertFalse(StringUtils.isBlank("hello"));
    }

    @Test
    @DisplayName("reverse reverses a simple string")
    void reversesString() {
        assertEquals("olleh", StringUtils.reverse("hello"));
    }

    // Intentional edge case: null in, null out — documented behavior,
    // not an accidental NullPointerException.
    @Test
    @DisplayName("reverse returns null for null input instead of throwing")
    void reverseHandlesNull() {
        assertNull(StringUtils.reverse(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"racecar", "RaceCar", "race car", "A"})
    @DisplayName("isPalindrome recognizes palindromes, case- and space-insensitively")
    void recognizesPalindromes(String value) {
        assertTrue(StringUtils.isPalindrome(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"hello", "jenkins"})
    @DisplayName("isPalindrome rejects non-palindromes")
    void rejectsNonPalindromes(String value) {
        assertFalse(StringUtils.isPalindrome(value));
    }

    @Test
    @DisplayName("isPalindrome is false for blank input")
    void isPalindromeFalseForBlank() {
        assertFalse(StringUtils.isPalindrome("   "));
    }

    @ParameterizedTest(name = "truncate(\"{0}\", {1}) = \"{2}\"")
    @CsvSource({
        "'continuous integration', 10, 'continuous...'",
        "'short', 10, 'short'",
        "'exact', 5, 'exact'"
    })
    void truncatesLongStrings(String input, int maxLength, String expected) {
        assertEquals(expected, StringUtils.truncate(input, maxLength));
    }

    @Test
    @DisplayName("capitalize upper-cases only the first letter")
    void capitalizesFirstLetter() {
        assertEquals("Jenkins", StringUtils.capitalize("jenkins"));
    }
}
