package com.dotcms.rest.api.v1.asset.bulkupload;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.Junit5WeldBaseTest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.Config;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.jboss.weld.junit5.EnableWeld;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The two submission-time controls the endpoint has to supply itself (spec FR-003a, SC-012;
 * research R9).
 * <p>
 * <b>Why these need a test of their own, when they are three lines of code.</b> Neither control is
 * inherited. {@code TempFileAPI} enforces both on its own REST resource and not in the API this
 * endpoint calls, and no global filter supplies them either — the product's referer interceptor
 * guards a fixed list of paths that does not include {@code /api/}. So they hold only for as long
 * as these three lines stay where they are, and nothing else in the suite would notice if they
 * moved: every other test drives the helper, below the resource.
 * <p>
 * <b>What each test really pins is the ordering.</b> Each refusal is asserted from a request that
 * carries <b>no authenticated user</b>, which the {@code InitBuilder} immediately below rejects
 * with a {@link SecurityException}. So a refactor that reordered either control past the
 * {@code InitBuilder} — the exact regression this is written against — changes the exception these
 * tests see, and they fail. Asserting only "it was refused" would not: the request would still be
 * refused, just for the wrong reason and after the batch had been let through a check it should
 * have failed first.
 * <p>
 * {@link #anAuthorisedSubmissionIsNotRefusedByEitherControl()} is the negative control that makes
 * the other two mean something, and is the one that would catch a mock request that fails for a
 * reason unrelated to what is being tested.
 *
 * @author dotCMS
 */
@EnableWeld
public class BulkUploadSecurityIT extends Junit5WeldBaseTest {

    private static final String TEMP_RESOURCE_ENABLED = "TEMP_RESOURCE_ENABLED";

    /**
     * Injected rather than constructed, deliberately: {@code @ApplicationScoped} needs a no-args
     * constructor for Weld to proxy, and a version of this feature that lacked one aborted
     * container validation — dotCMS did not start and every URL answered 404, while tests that
     * constructed the bean with {@code new} stayed green. Taking it from the container is what
     * makes a test notice.
     */
    @Inject
    BulkUploadResource resource;

    private boolean stagingWasEnabled;

    @BeforeAll
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Puts the switch back however this run found it. A config property left flipped is not scoped
     * to the test that flipped it — it outlives the class and fails a neighbouring test that never
     * touched it, which is a failure nobody thinks to look for here.
     */
    @AfterEach
    public void restoreStagingSwitch() {
        Config.setProperty(TEMP_RESOURCE_ENABLED, stagingWasEnabled);
    }

    /**
     * Scenario: a submission arrives from another origin.
     * <p>
     * Expected result: refused as a bad request naming the origin, and refused <b>before</b>
     * authentication is even attempted — so no cross-origin caller reaches the point where a
     * stolen session would be spent.
     */
    @Test
    public void aCrossOriginSubmissionIsRefused() {

        rememberStagingSwitch();
        Config.setProperty(TEMP_RESOURCE_ENABLED, true);

        final BadRequestException refusal = assertThrows(BadRequestException.class,
                () -> submit(requestFrom("attacker.example.com")),
                "A cross-origin submission must be refused, and refused as a bad request rather "
                        + "than by the authentication below it");

        assertTrue(refusal.getMessage().toLowerCase().contains("referer")
                        || refusal.getMessage().toLowerCase().contains("origin"),
                "The refusal must name the origin as the cause, not something further down: "
                        + refusal.getMessage());
    }

    /**
     * Scenario: an operator has switched the staging layer off, and a submission arrives.
     * <p>
     * Expected result: refused, rather than writing staged content behind a switch they
     * deliberately turned. Refused before authentication, for the same ordering reason.
     */
    @Test
    public void aSubmissionIsRefusedWhileStagingIsDisabled() {

        rememberStagingSwitch();
        Config.setProperty(TEMP_RESOURCE_ENABLED, false);

        final BadRequestException refusal = assertThrows(BadRequestException.class,
                () -> submit(sameOriginRequest()),
                "With the staging layer disabled the submission must be refused, not accepted "
                        + "into a layer the operator turned off");

        assertTrue(refusal.getMessage().toLowerCase().contains("temp file"),
                "The refusal must name the disabled staging layer: " + refusal.getMessage());
    }

    /**
     * Scenario: a same-origin submission while staging is enabled — the case neither control is
     * meant to stop.
     * <p>
     * Expected result: it is <b>not</b> refused by either of them. It still fails, on the
     * authentication below, because this request carries no user — and that is the point: it
     * proves the two refusals above are caused by the controls under test and not by anything the
     * mock request happens to be missing. Without this, both would pass just as happily against an
     * endpoint that refused everything.
     */
    @Test
    public void anAuthorisedSubmissionIsNotRefusedByEitherControl() {

        rememberStagingSwitch();
        Config.setProperty(TEMP_RESOURCE_ENABLED, true);

        final Throwable outcome = assertThrows(Throwable.class,
                () -> submit(sameOriginRequest()));

        assertFalse(outcome instanceof BadRequestException,
                "A same-origin submission with staging enabled must get past both controls; it "
                        + "was refused by one of them instead: " + outcome.getMessage());
        assertTrue(outcome instanceof SecurityException,
                "It should reach the authentication below the two controls and be rejected there "
                        + "for having no user, but it failed with "
                        + outcome.getClass().getName() + ": " + outcome.getMessage());
    }

    /**
     * Submits an <b>empty</b> body on purpose. Both controls run before a single part is read, so
     * a body is not needed to reach them — and its absence sharpens the test: were either control
     * moved below {@link BulkUploadResource#readForm}, the missing {@code form} part would be
     * refused first and the message asserted above would not be the one that arrives.
     */
    private void submit(final HttpServletRequest request) throws Exception {
        final HttpServletResponse response = new MockHttpResponse();
        resource.bulkUpload(request, response, new FormDataMultiPart());
    }

    private void rememberStagingSwitch() {
        assertNotNull(resource, "The resource must come from the container, not from 'new'");
        stagingWasEnabled = Config.getBooleanProperty(TEMP_RESOURCE_ENABLED, true);
    }

    /** A request whose {@code Origin} is the host it is addressed to. */
    private HttpServletRequest sameOriginRequest() {
        return requestFrom("localhost");
    }

    /**
     * A request addressed to {@code localhost} but declaring {@code origin} as its origin. The
     * {@code Origin} header is what {@code SecurityUtils.validateReferer} reads first, so this is
     * the shape a browser actually sends on a cross-site request.
     */
    private HttpServletRequest requestFrom(final String origin) {
        return new MockSessionRequest(
                new MockHeaderRequest(
                        new MockHttpRequestIntegrationTest(
                                "localhost", "/api/v1/assets/_bulkupload").request(),
                        "Origin", origin).request());
    }
}
