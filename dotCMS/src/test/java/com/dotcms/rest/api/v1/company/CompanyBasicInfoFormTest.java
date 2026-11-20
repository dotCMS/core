package com.dotcms.rest.api.v1.company;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.rest.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for the logo fields of {@link CompanyBasicInfoForm#checkValid()}.
 */
class CompanyBasicInfoFormTest {

    private static final String STOCK_LOGO =
            "/dA/164bf10d3ef4fdf363d39011e8cab4f4/asset/dotCMS-400x200.png";

    /** A login screen logo that is not a dotAsset path is rejected, naming the field. */
    @Test
    void loginScreenLogo_notADotAssetPath_isRejected() {
        final CompanyBasicInfoForm form = form("/dA/../html/logo.png", null);

        final BadRequestException error = assertThrows(BadRequestException.class, form::checkValid);
        final String body = String.valueOf(error.getResponse().getEntity());
        assertTrue(body.contains("loginScreenLogo"), body);
    }

    /** A navigation bar logo that is not a dotAsset path is rejected, naming the field. */
    @Test
    void navBarLogo_notADotAssetPath_isRejected() {
        final CompanyBasicInfoForm form = form(STOCK_LOGO, "/dA/%2e%2e/html/logo.png");

        final BadRequestException error = assertThrows(BadRequestException.class, form::checkValid);
        final String body = String.valueOf(error.getResponse().getEntity());
        assertTrue(body.contains("navBarLogo"), body);
    }

    /** dotAsset logos, including a file name with spaces and parentheses, are accepted. */
    @Test
    void dotAssetLogos_areAccepted() {
        final CompanyBasicInfoForm form = form(STOCK_LOGO,
                "/dA/e9f32fc04a4d7a0e7e6a025ee65fcf30/asset/my logo (1).png");

        assertDoesNotThrow(form::checkValid);
    }

    /** Logo file names with an apostrophe or a number sign are accepted. */
    @Test
    void logosWithApostropheOrNumberSignInName_areAccepted() {
        final CompanyBasicInfoForm form = form(
                "/dA/e9f32fc04a4d7a0e7e6a025ee65fcf30/asset/O'Neil logo.png",
                "/dA/e9f32fc04a4d7a0e7e6a025ee65fcf30/asset/logo#2.png");

        assertDoesNotThrow(form::checkValid);
    }

    /** Empty logos are accepted: they clear the stored value. */
    @Test
    void emptyLogos_areAccepted() {
        assertDoesNotThrow(form(null, null)::checkValid);
        assertDoesNotThrow(form("", "")::checkValid);
    }

    /** A bundled background, a dotAsset background and an empty value are accepted. */
    @ParameterizedTest
    @ValueSource(strings = {"", "/html/images/backgrounds/bg-3.jpg",
            "/dA/e9f32fc04a4d7a0e7e6a025ee65fcf30/asset/my bg.png"})
    void acceptedBackgrounds(final String background) {
        assertTrue(CompanyBasicInfoForm.isAcceptedBackgroundImage(background), background);
    }

    /** A dotAsset background with a dot segment, literal or percent-encoded, is not accepted. */
    @ParameterizedTest
    @ValueSource(strings = {"/dA/abc/../html/bg.png", "/dA/abc/%2e%2e/%2e%2e/html/bg.png",
            "/dA/abc/.%2E/html/bg.png", "/dAx/bg.png", "/images/bg.jpg"})
    void backgroundsNotAccepted(final String background) {
        assertFalse(CompanyBasicInfoForm.isAcceptedBackgroundImage(background), background);
    }

    /**
     * The background shown to clients: a dotAsset background in its canonical form, any other
     * stored background as it is, nothing for an empty value, the fresh-install value or a
     * dotAsset value that is not valid.
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '"', nullValues = "NULL", value = {
            "/dA/e9f32fc04a4d7a0e7e6a025ee65fcf30/asset/my bg.png | /dA/e9f32fc04a4d7a0e7e6a025ee65fcf30/asset/my%20bg.png",
            "/html/images/backgrounds/bg-3.jpg | /html/images/backgrounds/bg-3.jpg",
            "/application/themes/old/bg.jpg | /application/themes/old/bg.jpg",
            "/dA/abc/%2e%2e/%2e%2e/html/bg.png | NULL",
            "/html/%2E%2E/x.jpg | NULL",
            "/da/%2e%2e/%2e%2e/html/bg.png | NULL",
            "/html/images/../x.jpg | NULL",
            "localhost | NULL",
            "NULL | NULL"})
    void backgroundImageToShow(final String stored, final String expected) {
        assertEquals(expected, CompanyBasicInfoForm.backgroundImageToShow(stored));
    }

    /** Hex colors, color names and rgb/rgba/hsl/hsla values with numbers are written as they are. */
    @ParameterizedTest
    @ValueSource(strings = {"#1b3359", "#FFF", "#1b335980", "navy", "RebeccaPurple", "transparent",
            "rgb(1, 2, 3)", "rgba(1,2,3,0.5)", "hsl(10, 50%, 40%)", "HSLA(10,50%,40%,.5)"})
    void displayableColors(final String color) {
        assertTrue(CompanyBasicInfoForm.isDisplayableColor(color), color);
    }

    /** Any other value, including other functions or letters inside parentheses, is not written. */
    @ParameterizedTest
    @ValueSource(strings = {"", "#1b3359;}", "navy\"", "#fff>", "url(//x)", "a:b", "url(x)",
            "foo(1)", "rgb(a)", "navy blue", "#12", "rgb(1,2,3))"})
    void colorsNotDisplayable(final String color) {
        assertFalse(CompanyBasicInfoForm.isDisplayableColor(color), color);
    }

    private static CompanyBasicInfoForm form(final String loginScreenLogo, final String navBarLogo) {
        return new CompanyBasicInfoForm("http://localhost:8080", "admin@dotcms.com", null,
                "#FF0000", "#00FF00", null, null, loginScreenLogo, navBarLogo);
    }
}
