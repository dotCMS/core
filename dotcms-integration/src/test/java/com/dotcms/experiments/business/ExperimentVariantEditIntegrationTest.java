package com.dotcms.experiments.business;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.ExperimentDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.ExperimentVariant;
import com.dotcms.experiments.model.RunningIds.RunningId;
import com.dotcms.experiments.model.Scheduling;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.dotcms.variant.VariantAPI.DEFAULT_VARIANT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Proves the premise the whole of dotCMS/core#37308 (editing a variant while its experiment is
 * live) rests on: that saving content on a variant of a RUNNING experiment leaves the experiment
 * itself untouched.
 *
 * <p>The frontend change in #37308 removes a guard that stopped editors reaching a live variant.
 * Everything about whether that is safe is a backend claim, and it is not checkable from the UI:
 * the running ids and the measurement window exist only on the server model. Hence this test.
 *
 * <p><b>What this test proves.</b> After the edit, the experiment's identity, status, variants,
 * traffic split, schedule, running ids and look-back window are all byte-identical to what they
 * were before it, and no new run has been started. It also reports which variant actually received
 * the write, which static analysis cannot settle: {@code resolveContentletByVariant} returns the
 * contentlet unchanged when the requested variant already matches its own, so the outcome depends
 * on where the edited contentlet lives.
 *
 * <p><b>What this test does not prove.</b> That already-collected measurements read back the same
 * afterwards. Results are fetched live from the analytics store, which an integration test reaches
 * only through a mock HTTP server asserting the exact query issued. The helpers that build those
 * expectations are private to {@code ExperimentAPIImpIntegrationTest}. What is proven here is the
 * precondition that matters: the running ids and window that the results query is derived from do
 * not move, so the query issued after an edit is the query issued before it.
 *
 * <p>Registered in {@code MainSuite1a} — an integration test that is not in a suite compiles, keeps
 * the build green, and never runs.
 */
public class ExperimentVariantEditIntegrationTest {

    private static final String TEXT_FIELD_VAR = "text";

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: saving a {@link Contentlet} on a non-default {@link Variant} of a RUNNING
     * {@link Experiment}.
     *
     * <p>Given: a started experiment with a variant beside its control.
     * <p>When: a contentlet is saved on the non-default variant, as editing that variant does.
     * <p>Should: leave the experiment's id, status, variants, traffic split, schedule, running ids
     * and look-back window exactly as they were, start no new run, and store the write on the
     * variant it was addressed to rather than on DEFAULT.
     */
    @Test
    public void editingAVariantLeavesARunningExperimentUntouched()
            throws DotDataException, DotSecurityException {

        final User systemUser = APILocator.systemUser();

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();
        final HTMLPageAsset page = new HTMLPageDataGen(host, template).nextPersisted();

        final Experiment startedExperiment = new ExperimentDataGen()
                .page(page)
                .addVariant("variant edited while the experiment runs")
                .nextPersistedAndStart();

        final String experimentId = startedExperiment.id().orElseThrow();

        try {
            final Experiment before = findOrFail(experimentId, systemUser);

            // Snapshot every field #37308 promises not to disturb, BEFORE the edit. Captured as
            // values rather than held as a reference: Experiment is immutable, but re-reading is
            // what makes the comparison meaningful.
            final String statusBefore = before.status().name();
            final List<String> variantIdsBefore = variantIdsOf(before);
            final List<Float> weightsBefore = weightsOf(before);
            final Optional<Scheduling> schedulingBefore = before.scheduling();
            final long lookBackWindowBefore = before.lookBackWindowExpireTime();
            final List<String> runningIdsBefore = runningIdsOf(before);

            final String variantName = nonDefaultVariantNameOf(before);
            assertNotEquals("The experiment must have a variant beside its control",
                    DEFAULT_VARIANT.name(), variantName);

            // The edit. A contentlet saved on the experiment's own variant is exactly what the
            // editor does once #37308 lets it reach one. The Variant is looked up rather than
            // constructed: the one the experiment created on start is the one under test.
            final Variant variant = APILocator.getVariantAPI().get(variantName)
                    .orElseThrow(() -> new AssertionError(
                            "Starting the experiment must have created variant " + variantName));

            final Contentlet edited = saveContentletOnVariant(variant, "edited while running");

            // FR-004 / SC-003 — which variant received the write. This is the assertion static
            // analysis could not make, and the reason this test exists beyond FR-025.
            assertEquals("The edit must be stored against the variant it was addressed to",
                    variantName, edited.getVariantId());

            final Experiment after = findOrFail(experimentId, systemUser);

            // FR-025 — the experiment is untouched by the edit.
            assertEquals("The experiment's identity must not change",
                    experimentId, after.id().orElseThrow());
            assertEquals("The edit must not change the experiment's status",
                    statusBefore, after.status().name());
            assertEquals("The edit must not add, remove or reorder variants",
                    variantIdsBefore, variantIdsOf(after));
            assertEquals("The edit must not re-weight the traffic split",
                    weightsBefore, weightsOf(after));
            assertEquals("The edit must not move the experiment's schedule",
                    schedulingBefore, after.scheduling());
            assertEquals("The edit must not change the look-back window",
                    lookBackWindowBefore, after.lookBackWindowExpireTime());

            // FR-026's precondition: the results query is derived from the running ids. If they
            // are unchanged, the query issued after the edit is the query issued before it, so
            // whatever the analytics store returned before it returns after.
            assertEquals("The edit must not rotate or add a running id — no new run was started",
                    runningIdsBefore, runningIdsOf(after));
            assertTrue("The experiment must still have a current run",
                    after.runningIds().getCurrent().isPresent());
        } finally {
            findQuietly(experimentId, systemUser).ifPresent(ExperimentDataGen::end);
        }
    }

    private static Experiment findOrFail(final String experimentId, final User user)
            throws DotDataException, DotSecurityException {
        return APILocator.getExperimentsAPI().find(experimentId, user)
                .orElseThrow(() -> new AssertionError("Experiment not found: " + experimentId));
    }

    private static Optional<Experiment> findQuietly(final String experimentId, final User user) {
        try {
            return APILocator.getExperimentsAPI().find(experimentId, user);
        } catch (final DotDataException | DotSecurityException e) {
            return Optional.empty();
        }
    }

    private static List<String> variantIdsOf(final Experiment experiment) {
        return experiment.trafficProportion().variants().stream()
                .map(ExperimentVariant::id)
                .sorted()
                .collect(Collectors.toList());
    }

    private static List<Float> weightsOf(final Experiment experiment) {
        return experiment.trafficProportion().variants().stream()
                .sorted((left, right) -> left.id().compareTo(right.id()))
                .map(ExperimentVariant::weight)
                .collect(Collectors.toList());
    }

    private static List<String> runningIdsOf(final Experiment experiment) {
        return experiment.runningIds().getAll().stream()
                .map(RunningId::id)
                .sorted()
                .collect(Collectors.toList());
    }

    private static String nonDefaultVariantNameOf(final Experiment experiment) {
        return experiment.trafficProportion().variants().stream()
                .map(ExperimentVariant::id)
                .filter(id -> !DEFAULT_VARIANT.name().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "The experiment was created with a variant beside its control"));
    }

    private static Contentlet saveContentletOnVariant(final Variant variant, final String value) {
        final Field textField = new FieldDataGen()
                .name(TEXT_FIELD_VAR)
                .velocityVarName(TEXT_FIELD_VAR)
                .type(TextField.class)
                .next();

        final ContentType contentType = new ContentTypeDataGen().field(textField).nextPersisted();

        final Contentlet onDefault = new ContentletDataGen(contentType)
                .setProperty(TEXT_FIELD_VAR, "before the edit")
                .nextPersisted();

        return ContentletDataGen.createNewVersion(
                onDefault,
                variant,
                Map.of(TEXT_FIELD_VAR, value));
    }
}
