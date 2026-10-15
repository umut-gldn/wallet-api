package dev.umut.wallet_api.account;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class LuhnTest {

    @Test
    void calculatesKnownCheckDigit() {
        assertThat(Luhn.checkDigit("7992739871")).isEqualTo(3);
    }

    @Test
    void acceptsValidNumber() {
        assertThat(Luhn.isValid("79927398713")).isTrue();
    }

    @Test
    void rejectsSingleWrongDigit() {
        assertThat(Luhn.isValid("79927398714")).isFalse();
    }

    @Test
    void rejectsSwappedAdjacentDigits() {
        assertThat(Luhn.isValid("79297398713")).isFalse();
    }

    @Test
    void rejectsNonDigitsAndNull() {
        assertThat(Luhn.isValid("12A4")).isFalse();
        assertThat(Luhn.isValid(null)).isFalse();
    }
}
