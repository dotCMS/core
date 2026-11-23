package com.dotcms.company;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.liferay.portal.model.Company;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CompanyBrandingChanges}.
 */
class CompanyBrandingChangesTest {

    /** Every changed field is listed by its branding name, in a fixed order. */
    @Test
    void changedFields_areListedByName() {
        final Company before = company();
        final Company after = company();
        after.setPortalURL("https://example.com");
        after.setStreet("#000000");
        after.setCity("/dA/e9f32fc04a4d7a0e7e6a025ee65fcf30/asset/logo.png");
        after.setHomeURL("/html/images/backgrounds/bg-3.jpg");

        assertEquals(List.of("portalURL", "secondaryColor", "backgroundImage", "loginScreenLogo"),
                CompanyBrandingChanges.changedFields(before, after));
    }

    /** Nothing is listed when nothing changed, and an empty value equals a missing one. */
    @Test
    void unchangedFields_areNotListed() {
        final Company before = company();
        final Company after = company();
        before.setState(null);
        after.setState("");

        assertEquals(List.of(), CompanyBrandingChanges.changedFields(before, after));
    }

    /** Each of the nine fields is detected. */
    @Test
    void allFields_areDetected() {
        final Company after = new Company();
        after.setPortalURL("a");
        after.setEmailAddress("b");
        after.setMx("c");
        after.setSize("d");
        after.setType("e");
        after.setStreet("f");
        after.setHomeURL("g");
        after.setCity("h");
        after.setState("i");

        assertEquals(List.of("portalURL", "emailAddress", "mx", "backgroundColor", "primaryColor",
                        "secondaryColor", "backgroundImage", "loginScreenLogo", "navBarLogo"),
                CompanyBrandingChanges.changedFields(new Company(), after));
    }

    private static Company company() {
        final Company company = new Company();
        company.setPortalURL("http://localhost:8080");
        company.setEmailAddress("dotCMS Website <website@dotcms.com>");
        company.setMx("dotcms.com");
        company.setSize("#1b3359");
        company.setType("#4e65f1");
        company.setStreet("#233f9b");
        company.setHomeURL("/html/images/backgrounds/bg-11.jpg");
        company.setCity("/dA/164bf10d3ef4fdf363d39011e8cab4f4/asset/dotCMS-400x200.png");
        company.setState("");
        return company;
    }
}
