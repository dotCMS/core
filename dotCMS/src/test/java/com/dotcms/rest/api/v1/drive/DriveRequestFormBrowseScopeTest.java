package com.dotcms.rest.api.v1.drive;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

import java.util.List;
import javax.ws.rs.WebApplicationException;
import org.junit.Test;

/**
 * The invariant that keeps a browse scope and a folder path from contradicting each other.
 *
 * <p>The three browse scopes are things you can only be in at the top of a site: the whole site,
 * the site root itself, and System Host. A folder is addressed by its path. So there is no
 * combination of a folder path and a scope that means anything, and the request is refused rather
 * than resolved by precedence.</p>
 *
 * <p><b>Why refused and not resolved.</b> The precedent is {@code BulkUploadForm}, which refuses a
 * submission naming both a folder and a site with the same reasoning: a caller that says two
 * contradictory things has not expressed a preference, and picking one would silently put content
 * somewhere they did not choose. Ignoring the scope instead would be friendlier to a sloppy caller
 * and would hide the caller's bug, which is the trade this test pins.</p>
 *
 * <p><b>On Red.</b> This test names a field the form does not have yet, so it cannot compile until
 * the field is declared. The declaration is not the behavior under test: the behavior is the
 * refusal. The honest Red state for this test is therefore "the field exists, the validation does
 * not, and the assertion fails" — not a compile error. Confirm it in that state before writing the
 * validation.</p>
 *
 * <p>Needs no database.</p>
 */
public class DriveRequestFormBrowseScopeTest {

    private static final String SITE_ROOT = "//demo.dotcms.com/";
    private static final String A_FOLDER = "//demo.dotcms.com/application/";

    /**
     * Every builder here pins {@code language}. Its declared default asks the language API for the
     * default language, which reaches the database, so leaving it unset makes the build fail with
     * "No Company!" before any validation runs — a failure that looks like a refusal but is only
     * the bootstrap. Pinning it keeps the assertions about the browse scope.
     */
    private static DriveRequestForm.Builder formFor(final String assetPath) {
        return DriveRequestForm.builder().assetPath(assetPath).language(List.of("1"));
    }

    /**
     * Given a browse scope named together with a folder path, When the form is built, Then it is
     * refused, and the message names both halves of the contradiction so the caller can see which
     * two statements they made.
     */
    @Test
    public void testAnExplicitBrowseScopeWithAFolderPathIsRefused() {
        for (final BrowseScope scope : BrowseScope.values()) {
            // Deliberately not pinned to an exception type: where the refusal is raised is an
            // implementation choice, and a test that names the type would decide it here. What is
            // asserted is that the request does not succeed and that the reason is legible.
            final RuntimeException refused = assertThrows(
                    "a folder path with " + scope + " must be refused, not resolved",
                    RuntimeException.class,
                    () -> formFor(A_FOLDER).browseScope(scope).build());

            // The message assertions are what keep the broad type honest: an incidental
            // NullPointerException carries no message and fails here rather than passing as a
            // refusal that never happened.
            final String message = reasonGivenToTheCaller(refused);
            assertTrue("the refusal must name the scope, got: " + message,
                    message.contains(scope.name()));
            assertTrue("the refusal must name the path, got: " + message,
                    message.contains("/application/"));
        }
    }

    /**
     * The reason a refusal gives the caller, read from wherever the refusal carries it. A JAX-RS
     * exception puts it in the response the caller receives rather than in {@code getMessage()},
     * which returns only the status line; anything else is read the ordinary way. Written this way
     * so the assertion is about the caller being told what went wrong, not about which exception
     * the validation happens to raise.
     */
    private static String reasonGivenToTheCaller(final RuntimeException refused) {
        return refused instanceof WebApplicationException
                ? String.valueOf(((WebApplicationException) refused).getResponse().getEntity())
                : String.valueOf(refused.getMessage());
    }

    /**
     * Given each browse scope at the site root, When the form is built, Then it is accepted. This
     * is the other half of the invariant: the refusal must be about the contradiction, not about
     * the scope being present at all.
     */
    @Test
    public void testEveryBrowseScopeIsAcceptedAtTheSiteRoot() {
        for (final BrowseScope scope : BrowseScope.values()) {
            formFor(SITE_ROOT).browseScope(scope).build();
        }
    }

    /**
     * Given a folder path and no browse scope, When the form is built, Then it is accepted
     * unchanged. This is the shape the Asset Picker sends and the reason the field has no default:
     * a request that never mentions a scope must keep meaning exactly what it means today.
     */
    @Test
    public void testAFolderPathWithNoBrowseScopeIsUntouched() {
        formFor(A_FOLDER).build();
    }
}
