package com.dotcms.rest.api.v1.experiments;

import static com.dotcms.variant.VariantAPI.DEFAULT_VARIANT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.ExperimentDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.ExperimentVariant;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.model.User;
import com.liferay.util.Base64;
import java.util.Locale;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the page-change rule on {@code PATCH /api/v1/experiments/{id}}
 * (issue #37176).
 *
 * <p>The rule lives in {@link ExperimentsResource}'s private {@code patchExperiment}, so these tests
 * drive the resource directly rather than going through {@code ExperimentsAPI}: the API layer never
 * sees the incoming form and therefore cannot express "the page changed".
 *
 * <p><b>On status codes.</b> Calling the resource method directly bypasses the JAX-RS exception
 * mappers, so a refusal surfaces here as the thrown exception rather than as an HTTP status. The
 * mapping of {@link IllegalArgumentException} to <b>400</b> is owned by
 * {@code com.dotcms.rest.exception.mapper.badrequest.IllegalArgumentExceptionMapper}. Asserting the
 * status code itself requires a layer that runs the mapper.
 */
public class ExperimentsResourceIntegrationTest {

    private static ExperimentsResource resource;
    private static HttpServletResponse response;
    private static User adminUser;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        resource = new ExperimentsResource();
        adminUser = TestUserUtils.getAdminUser();
        response = new MockHttpResponse();
    }

    // ---------------------------------------------------------------------------------------------
    // The permitted change
    // ---------------------------------------------------------------------------------------------

    /**
     * Method to test: {@link ExperimentsResource#update(HttpServletRequest, HttpServletResponse, String, ExperimentForm)}
     * Given Scenario: A DRAFT Experiment whose only Variant is the control, PATCHed with the
     *                 identifier of a different Page.
     * ExpectedResult: The change is applied and the Experiment points at the new Page.
     */
    @Test
    public void patchPageId_onDraftWithOnlyControl_shouldChangeThePage() throws Exception {
        final Experiment experiment = draftWithOnlyControl();
        final HTMLPageAsset newPage = createPage();

        final Experiment updated = patchPageId(experiment, newPage.getIdentifier());

        assertEquals("The Experiment should point at the new Page",
                newPage.getIdentifier(), updated.pageId());
    }

    /**
     * Method to test: {@link ExperimentsResource#update(HttpServletRequest, HttpServletResponse, String, ExperimentForm)}
     * Given Scenario: The same permitted page change.
     * ExpectedResult: The control Variant's stored url is regenerated from the new Page. Without
     *                 this the "copy preview URL" action keeps handing back a link to the previous
     *                 Page — the half of the change that is easiest to miss.
     */
    @Test
    public void patchPageId_onDraftWithOnlyControl_shouldRegenerateControlVariantUrl()
            throws Exception {
        final Experiment experiment = draftWithOnlyControl();
        final HTMLPageAsset newPage = createPage();

        final Experiment updated = patchPageId(experiment, newPage.getIdentifier());

        final Optional<String> controlUrl = controlVariant(updated).url();
        assertTrue("The control Variant should carry a url", controlUrl.isPresent());
        assertEquals("The control Variant's url should address the new Page",
                newPage.getURI() + "?variantName=" + DEFAULT_VARIANT.name(), controlUrl.get());
    }

    /**
     * Method to test: {@link ExperimentsResource#update(HttpServletRequest, HttpServletResponse, String, ExperimentForm)}
     * Given Scenario: A DRAFT Experiment PATCHed with the very pageId it already holds.
     * ExpectedResult: Nothing changes and no error is raised.
     */
    @Test
    public void patchPageId_withUnchangedValue_shouldBeANoOp() throws Exception {
        final Experiment experiment = draftWithOnlyControl();
        final String originalPageId = experiment.pageId();

        final Experiment updated = patchPageId(experiment, originalPageId);

        assertEquals("An unchanged pageId should leave the Page alone",
                originalPageId, updated.pageId());
    }

    // ---------------------------------------------------------------------------------------------
    // The refusals — silently dropping the field is the bug this closes
    // ---------------------------------------------------------------------------------------------

    /**
     * Method to test: {@link ExperimentsResource#update(HttpServletRequest, HttpServletResponse, String, ExperimentForm)}
     * Given Scenario: A DRAFT Experiment carrying a Variant other than the control, PATCHed with a
     *                 different Page. That Variant holds a copy of the current Page's layout, so
     *                 repointing the Experiment would orphan it.
     * ExpectedResult: Refused, and the message names the Variants rule.
     */
    @Test
    public void patchPageId_onDraftWithNonControlVariant_shouldBeRefused() throws Exception {
        final Experiment experiment = new ExperimentDataGen()
                .addVariant("Test Green Button")
                .nextPersisted();
        final HTMLPageAsset newPage = createPage();

        final IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class,
                () -> patchPageId(experiment, newPage.getIdentifier()));

        assertTrue("The refusal should name the Variants rule, was: " + refusal.getMessage(),
                refusal.getMessage().toLowerCase(Locale.ROOT).contains("variant"));
    }

    /**
     * Method to test: {@link ExperimentsResource#update(HttpServletRequest, HttpServletResponse, String, ExperimentForm)}
     * Given Scenario: A RUNNING Experiment PATCHed with a different Page.
     * ExpectedResult: Refused, and the message names the status rule.
     */
    @Test
    public void patchPageId_onRunningExperiment_shouldBeRefused() throws Exception {
        final Experiment experiment = new ExperimentDataGen()
                .addVariant("Test Green Button")
                .nextPersistedAndStart();
        final HTMLPageAsset newPage = createPage();

        final IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class,
                () -> patchPageId(experiment, newPage.getIdentifier()));

        assertTrue("The refusal should name the status rule, was: " + refusal.getMessage(),
                refusal.getMessage().toUpperCase(Locale.ROOT).contains("DRAFT"));
    }

    /**
     * Method to test: {@link ExperimentsResource#update(HttpServletRequest, HttpServletResponse, String, ExperimentForm)}
     * Given Scenario: A refused page change.
     * ExpectedResult: The Experiment's stored Page is untouched — the refusal must not half-apply.
     */
    @Test
    public void patchPageId_whenRefused_shouldLeaveThePageUnchanged() throws Exception {
        final Experiment experiment = new ExperimentDataGen()
                .addVariant("Test Green Button")
                .nextPersisted();
        final String originalPageId = experiment.pageId();
        final HTMLPageAsset newPage = createPage();

        assertThrows(IllegalArgumentException.class,
                () -> patchPageId(experiment, newPage.getIdentifier()));

        final Experiment reloaded = APILocator.getExperimentsAPI()
                .find(experiment.id().orElseThrow(), adminUser)
                .orElseThrow();
        assertEquals("A refused change must leave the stored Page alone",
                originalPageId, reloaded.pageId());
    }

    /**
     * Method to test: {@link ExperimentsResource#update(HttpServletRequest, HttpServletResponse, String, ExperimentForm)}
     * Given Scenario: Experiments that could NOT accept a page change — one RUNNING, one DRAFT with
     *                 a real Variant — PATCHed with the pageId they already hold.
     * ExpectedResult: Accepted as a no-op. This is what keeps clients that echo the whole Experiment
     *                 back on every save working, and is why the equality check must run before the
     *                 eligibility check rather than after it.
     */
    @Test
    public void patchPageId_withUnchangedValue_onIneligibleExperiment_shouldBeANoOp()
            throws Exception {
        final Experiment running = new ExperimentDataGen()
                .addVariant("Test Green Button")
                .nextPersistedAndStart();
        final Experiment draftWithVariant = new ExperimentDataGen()
                .addVariant("Test Green Button")
                .nextPersisted();

        assertEquals("An unchanged pageId must not be refused on a RUNNING Experiment",
                running.pageId(), patchPageId(running, running.pageId()).pageId());
        assertEquals("An unchanged pageId must not be refused on a DRAFT carrying Variants",
                draftWithVariant.pageId(),
                patchPageId(draftWithVariant, draftWithVariant.pageId()).pageId());
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    /**
     * PATCHes nothing but the pageId, and returns the persisted result.
     */
    private Experiment patchPageId(final Experiment experiment, final String pageId)
            throws Exception {
        final ExperimentForm form = ExperimentForm.Builder.anExperimentForm()
                .withPageId(pageId)
                .build();

        return resource.update(getHttpRequest(), response, experiment.id().orElseThrow(), form)
                .getEntity();
    }

    /**
     * A DRAFT Experiment whose only Variant is the control.
     *
     * <p>{@link ExperimentDataGen} cannot express this state: its {@code persist()} adds a random
     * non-control Variant whenever none was requested, so {@code nextPersisted()} always yields
     * control + 1. Saving through the API directly yields the control alone, because
     * {@code ExperimentsAPI.save()} adds only the Original Variant.
     */
    private Experiment draftWithOnlyControl() throws Exception {
        final HTMLPageAsset page = createPage();

        return APILocator.getExperimentsAPI().save(Experiment.builder()
                .name("page-change-" + System.nanoTime())
                .description("Experiment under test for issue #37176")
                .pageId(page.getIdentifier())
                .createdBy(adminUser.getUserId())
                .lastModifiedBy(adminUser.getUserId())
                .build(), adminUser);
    }

    /**
     * The control is the DEFAULT Variant — the one holding no copied layout, because it is the Page.
     */
    private ExperimentVariant controlVariant(final Experiment experiment) {
        return experiment.trafficProportion().variants().stream()
                .filter(variant -> DEFAULT_VARIANT.name().equals(variant.id()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("The Experiment has no control Variant"));
    }

    private HTMLPageAsset createPage() {
        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(host, template).nextPersisted();
        return APILocator.getHTMLPageAssetAPI().fromContentlet(HTMLPageDataGen.publish(page));
    }

    private HttpServletRequest getHttpRequest() {
        final String userEmailAndPassword = adminUser.getEmailAddress() + ":admin";
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(
                                new MockHttpRequestIntegrationTest("localhost", "/").request())
                                .request())
                        .request());

        request.setHeader("Authorization",
                "Basic " + new String(Base64.encode(userEmailAndPassword.getBytes())));

        return request;
    }
}
