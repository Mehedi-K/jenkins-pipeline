package io.pipelinedemo.utils;

/**
 * A tiny four-function calculator with checked-friendly behavior around
 * division by zero. Exists purely as a realistic, easy-to-test unit for the
 * Jenkins pipeline to build, test, and package.
 */
public final class Calculator {

    private Calculator() {
        // utility class, no instances
    }

    public static int add(int a, int b) {
        return a + b;
    }

    public static int subtract(int a, int b) {
        return a - b;
    }

    public static int multiply(int a, int b) {
        return a * b;
    }

    /**
     * Integer division. Division by zero is rejected explicitly rather than
     * letting the JVM throw {@link ArithmeticException} deep in a call
     * stack, so callers get a clear, documented failure mode.
     *
     * @throws ArithmeticException if {@code divisor} is zero
     */
    public static int divide(int dividend, int divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("Division by zero is not allowed");
        }
        return dividend / divisor;
    }

    public static double percentage(double value, double percent) {
        return (value * percent) / 100.0;
    }
}
