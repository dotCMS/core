package com.dotcms.rest.api.v1.company;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.rest.exception.BadRequestException;
import org.junit.jupiter.api.Test;

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

    private static CompanyBasicInfoForm form(final String loginScreenLogo, final String navBarLogo) {
        return new CompanyBasicInfoForm("http://localhost:8080", "admin@dotcms.com", null,
                "#FF0000", "#00FF00", null, null, loginScreenLogo, navBarLogo);
    }
}
