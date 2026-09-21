package io.pipelinedemo.utils;

/**
 * Small collection of string helpers with well-defined edge-case behavior
 * (null/empty handling), the kind of thing a "Static Analysis" and
 * "Unit Tests" pipeline stage should actually have something to say about.
 */
public final class StringUtils {

    private StringUtils() {
        // utility class, no instances
    }

    /**
     * @return true if the string is null, empty, or made up entirely of
     *     whitespace
     */
    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Reverses a string. Returns null for null input rather than throwing,
     * mirroring how many battle-tested string utility libraries behave.
     */
    public static String reverse(String value) {
        if (value == null) {
            return null;
        }
        return new StringBuilder(value).reverse().toString();
    }

    /**
     * Case-insensitive palindrome check that ignores whitespace, so
     * "race car" and "RaceCar" both count. A null or blank input is not a
     * palindrome.
     */
    public static boolean isPalindrome(String value) {
        if (isBlank(value)) {
            return false;
        }
        String normalized = value.replaceAll("\\s+", "").toLowerCase();
        return normalized.equals(reverse(normalized));
    }

    /**
     * Truncates a string to {@code maxLength}, appending "..." when it was
     * actually shortened. Returns the original string unchanged if it
     * already fits.
     *
     * @throws IllegalArgumentException if maxLength is negative
     */
    public static String truncate(String value, int maxLength) {
        if (maxLength < 0) {
            throw new IllegalArgumentException("maxLength must not be negative");
        }
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    public static String capitalize(String value) {
        if (isBlank(value)) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
