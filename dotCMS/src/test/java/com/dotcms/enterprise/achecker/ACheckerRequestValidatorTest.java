package com.dotcms.enterprise.achecker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotmarketing.business.DotValidationException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the validation applied to the Accessibility Checker request parameters
 * ({@code guidelines} and {@code lang}) before any lookup runs.
 */
class ACheckerRequestValidatorTest {

    @Test
    void singleGuidelineTokenIsReturnedUnchanged() {
        final List<String> tokens = ACheckerRequestValidator.guidelineTokens("WCAG2-AA");

        assertEquals(Collections.singletonList("WCAG2-AA"), tokens);
        assertThrows(UnsupportedOperationException.class, () -> tokens.add("x"));
    }

    @Test
    void tokensAreSplitOnCommaAndTrimmed() {
        assertEquals(Arrays.asList("WCAG2-AA", "508"),
                ACheckerRequestValidator.guidelineTokens(" WCAG2-AA , 508 "));
    }

    @Test
    void blankTokensAreSkipped() {
        assertEquals(Collections.singletonList("WCAG2-AA"),
                ACheckerRequestValidator.guidelineTokens("WCAG2-AA,,"));
        assertTrue(ACheckerRequestValidator.guidelineTokens(", ,").isEmpty());
        assertTrue(ACheckerRequestValidator.guidelineTokens(null).isEmpty());
    }

    @Test
    void tokenWithUnexpectedCharacterIsRejected() {
        for (final String character : Arrays.asList("'", " ", ";", "(", "%", "\n", "\t", "é")) {
            final String guidelines = "WCAG2" + character + "AA";
            final DotValidationException exception = assertThrows(DotValidationException.class,
                    () -> ACheckerRequestValidator.guidelineTokens(guidelines),
                    "Expected a rejection for a token containing [" + character + "]");
            assertEquals(ACheckerRequestValidator.UNSUPPORTED_GUIDELINES_MESSAGE, exception.getMessage());
        }
    }

    @Test
    void allowedCharactersAndCaseArePreserved() {
        assertEquals(Collections.singletonList("wcag2-aa"),
                ACheckerRequestValidator.guidelineTokens("wcag2-aa"));
        assertEquals(Collections.singletonList("WCAG1.0_x-y"),
                ACheckerRequestValidator.guidelineTokens("WCAG1.0_x-y"));
    }

    @Test
    void tokenAtMaximumLengthIsAccepted() {
        final String token = "a".repeat(100);

        final List<String> tokens = ACheckerRequestValidator.guidelineTokens(token);

        assertEquals(Collections.singletonList(token), tokens);
    }

    @Test
    void tokenOverMaximumLengthIsRejected() {
        final String token = "a".repeat(101);

        final DotValidationException exception = assertThrows(DotValidationException.class,
                () -> ACheckerRequestValidator.guidelineTokens(token));

        assertEquals(ACheckerRequestValidator.UNSUPPORTED_GUIDELINES_MESSAGE, exception.getMessage());
    }

    @Test
    void guidelineListAtMaximumSizeIsAccepted() {
        final String guidelines = String.join(",",
                Collections.nCopies(ACheckerRequestValidator.MAX_GUIDELINE_TOKENS, "WCAG2-AA"));

        final List<String> tokens = ACheckerRequestValidator.guidelineTokens(guidelines);

        assertEquals(20, tokens.size());
    }

    @Test
    void guidelineListOverMaximumSizeIsRejected() {
        final String guidelines = String.join(",",
                Collections.nCopies(ACheckerRequestValidator.MAX_GUIDELINE_TOKENS + 1, "WCAG2-AA"));

        final DotValidationException exception = assertThrows(DotValidationException.class,
                () -> ACheckerRequestValidator.guidelineTokens(guidelines));

        assertEquals("'guidelines' parameter accepts at most 20 values", exception.getMessage());
    }

    @Test
    void absentLangIsNull() {
        assertNull(ACheckerRequestValidator.lang(null));
        assertNull(ACheckerRequestValidator.lang(""));
        assertNull(ACheckerRequestValidator.lang("  "));
    }

    @Test
    void twoOrThreeLetterLangIsAccepted() {
        assertEquals("en", ACheckerRequestValidator.lang("en"));
        assertEquals("eng", ACheckerRequestValidator.lang("eng"));
        assertEquals("en", ACheckerRequestValidator.lang(" en "));
        assertEquals("en", ACheckerRequestValidator.lang("EN"));
        assertEquals("en", ACheckerRequestValidator.lang("en-US"));
        assertEquals("pt", ACheckerRequestValidator.lang("pt_BR"));
    }

    @Test
    void langOutsideLengthBoundsIsRejected() {
        assertLangRejected("e");
        assertLangRejected("engl");
    }

    @Test
    void langWithNonLetterIsRejected() {
        assertLangRejected("e1");
        assertLangRejected("e'");
        assertLangRejected("e-");
        assertLangRejected("e n");
    }

    private static void assertLangRejected(final String lang) {
        final DotValidationException exception = assertThrows(DotValidationException.class,
                () -> ACheckerRequestValidator.lang(lang),
                "Expected a rejection for lang [" + lang + "]");
        // Equality with the fixed message already shows the value is not echoed; a contains check
        // would misfire for one-letter values that naturally occur in the message text.
        assertEquals(ACheckerRequestValidator.UNSUPPORTED_LANG_MESSAGE, exception.getMessage());
    }
}
