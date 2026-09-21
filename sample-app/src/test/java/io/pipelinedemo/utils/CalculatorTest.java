package io.pipelinedemo.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Calculator")
class CalculatorTest {

    @Test
    @DisplayName("adds two positive integers")
    void addsPositiveNumbers() {
        assertEquals(5, Calculator.add(2, 3));
    }

    @Test
    @DisplayName("adds negative numbers correctly")
    void addsNegativeNumbers() {
        assertEquals(-5, Calculator.add(-2, -3));
    }

    @Test
    @DisplayName("subtracts two integers")
    void subtractsNumbers() {
        assertEquals(1, Calculator.subtract(4, 3));
    }

    @Test
    @DisplayName("multiplies two integers")
    void multipliesNumbers() {
        assertEquals(12, Calculator.multiply(4, 3));
    }

    @Nested
    @DisplayName("division")
    class Division {

        @Test
        @DisplayName("divides two integers evenly")
        void dividesEvenly() {
            assertEquals(4, Calculator.divide(12, 3));
        }

        @Test
        @DisplayName("truncates toward zero on inexact division")
        void truncatesOnInexactDivision() {
            assertEquals(3, Calculator.divide(10, 3));
        }

        // Intentional edge case: division by zero must fail loudly and
        // predictably rather than silently returning a bogus value.
        @Test
        @DisplayName("throws ArithmeticException when dividing by zero")
        void throwsOnDivideByZero() {
            ArithmeticException ex = assertThrows(
                    ArithmeticException.class,
                    () -> Calculator.divide(10, 0));
            assertEquals("Division by zero is not allowed", ex.getMessage());
        }
    }

    @ParameterizedTest(name = "{0}% of {1} = {2}")
    @DisplayName("computes percentages")
    @CsvSource({
        "50, 200, 100.0",
        "10, 50, 5.0",
        "0, 999, 0.0"
    })
    void computesPercentage(double percent, double value, double expected) {
        assertEquals(expected, Calculator.percentage(value, percent), 0.0001);
    }
}
