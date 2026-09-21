package io.pipelinedemo.utils;

import java.util.regex.Pattern;

/**
 * Basic, intentionally-simple input validation helpers (email shape,
 * password strength). These are not meant to be exhaustive validators —
 * they exist to give the pipeline's unit-test and static-analysis stages
 * a couple more realistic edge cases to check.
 */
public final class Validator {

    // Pragmatic "looks like an email" check, not a full RFC 5322 parser.
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)*\\.[a-zA-Z]{2,}$");

    private Validator() {
        // utility class, no instances
    }

    public static boolean isValidEmail(String email) {
        if (StringUtils.isBlank(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * A password is considered "strong" when it is at least 8 characters
     * long and contains at least one digit and one letter.
     */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        return hasDigit && hasLetter;
    }

    /**
     * Validates a positive integer "age" is within a plausible human range.
     * Deliberately inclusive at both ends (0 and 150 are accepted) as an
     * edge case worth documenting and testing explicitly.
     */
    public static boolean isPlausibleAge(int age) {
        return age >= 0 && age <= 150;
    }
}
