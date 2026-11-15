package dev.umut.wallet_api.auth;

import org.junit.jupiter.api.Test;
import java.util.Locale;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class EmailNormalizationTest {

    @Test
    void trimsAndLowercasesEmail() {
        assertThat(new RegisterRequest("  Umut@Test.COM ", "12345678").email()).isEqualTo("umut@test.com");
        assertThat(new LoginRequest(" UMUT@test.com", "x").email()).isEqualTo("umut@test.com");
    }

    @Test
    void doesNotDependOnMachineLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertThat(new LoginRequest("INFO@TEST.COM", "x").email()).isEqualTo("info@test.com");
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void keepsNullAsIsSoValidationCanReject() {
        assertThat(new LoginRequest(null, "x").email()).isNull();
    }
}
