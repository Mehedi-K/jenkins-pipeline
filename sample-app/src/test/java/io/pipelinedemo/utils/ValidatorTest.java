package io.pipelinedemo.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Validator")
class ValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"a@b.com", "first.last@example.co", "user+tag@sub.domain.io"})
    @DisplayName("accepts well-formed email addresses")
    void acceptsValidEmails(String email) {
        assertTrue(Validator.isValidEmail(email));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-an-email", "missing@domain", "@nodash.com", "spaces in@it.com"})
    @DisplayName("rejects malformed email addresses")
    void rejectsInvalidEmails(String email) {
        assertFalse(Validator.isValidEmail(email));
    }

    @Test
    @DisplayName("rejects null and blank emails")
    void rejectsBlankEmail() {
        assertFalse(Validator.isValidEmail(null));
        assertFalse(Validator.isValidEmail("  "));
    }

    @Test
    @DisplayName("accepts a password with letters, digits, and 8+ characters")
    void acceptsStrongPassword() {
        assertTrue(Validator.isStrongPassword("abcd1234"));
    }

    @Test
    @DisplayName("rejects a password shorter than 8 characters")
    void rejectsShortPassword() {
        assertFalse(Validator.isStrongPassword("ab1"));
    }

    @Test
    @DisplayName("rejects a password with no digits")
    void rejectsPasswordWithoutDigits() {
        assertFalse(Validator.isStrongPassword("abcdefgh"));
    }

    // Intentional edge cases: the boundaries of the accepted age range are
    // inclusive, and that needs to be pinned down by a test, not just the
    // implementation comment.
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 42, 149, 150})
    @DisplayName("accepts ages within the inclusive 0-150 range")
    void acceptsBoundaryAges(int age) {
        assertTrue(Validator.isPlausibleAge(age));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 151, 1000})
    @DisplayName("rejects ages outside the plausible range")
    void rejectsOutOfRangeAges(int age) {
        assertFalse(Validator.isPlausibleAge(age));
    }
}
