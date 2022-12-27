package com.dotcms.experiments.business;

import static com.dotcms.experiments.model.AbstractExperimentVariant.ORIGINAL_VARIANT;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.ExperimentDataGen;
import com.dotcms.datagen.MultiTreeDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.experiments.model.AbstractExperiment.Status;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.ExperimentVariant;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.BeforeClass;
import org.junit.Test;

public class ExperimentAPIImpIT {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link ExperimentsAPI#getRunningExperiments()}
     * When: You have tree Experiment:
     * - First one in DRAFT state, it has never been running.
     * - Second one in RUNNING state, it was already started and it was not stopped yet
     * - Finally one in STOP State, it was started, but it was stopped already
     */
    @Test
    public void getRunningExperiment() throws DotDataException, DotSecurityException {
        final Experiment draftExperiment = new ExperimentDataGen().nextPersisted();

        final Experiment runningExperiment = new ExperimentDataGen().nextPersisted();
        ExperimentDataGen.start(runningExperiment);

        final Experiment stoppedExperiment = new ExperimentDataGen().nextPersisted();
        ExperimentDataGen.start(stoppedExperiment);

        try {
            List<Experiment> experimentRunning = APILocator.getExperimentsAPI()
                    .getRunningExperiments();

            List<String> experiemtnsId = experimentRunning.stream()
                    .map(experiment -> experiment.getIdentifier()).collect(Collectors.toList());

            assertTrue(experiemtnsId.contains(runningExperiment.getIdentifier()));
            assertFalse(experiemtnsId.contains(draftExperiment.getIdentifier()));
            assertTrue(experiemtnsId.contains(stoppedExperiment.getIdentifier()));

            ExperimentDataGen.end(stoppedExperiment);

            experimentRunning = APILocator.getExperimentsAPI()
                    .getRunningExperiments();
            experiemtnsId = experimentRunning.stream()
                    .map(experiment -> experiment.getIdentifier()).collect(Collectors.toList());

            assertTrue(experiemtnsId.contains(runningExperiment.getIdentifier()));
            assertFalse(experiemtnsId.contains(draftExperiment.getIdentifier()));
            assertFalse(experiemtnsId.contains(stoppedExperiment.getIdentifier()));
        } finally {
            ExperimentDataGen.end(runningExperiment);

            final Optional<Experiment> experiment = APILocator.getExperimentsAPI()
                    .find(stoppedExperiment.getIdentifier(), APILocator.systemUser());

            if (experiment.isPresent() && experiment.get().status() == Status.RUNNING) {
                ExperimentDataGen.end(experiment.get());
            }
        }
    }


    /**
     * Method to test: {@link ExperimentsAPI#start(String, User)}
     * When: an {@link Experiment} is started
     * Should: publish all the contents in the variants created for the experiment.
     */
    @Test
    public void testStartExperiment_shouldPublishContent()
            throws DotDataException, DotSecurityException {

        final Experiment newExperiment = new ExperimentDataGen()
                .addVariant("Test Green Button")
                .nextPersisted();

        try {

            final String pageId = newExperiment.pageId();
            final Container container = new ContainerDataGen().nextPersisted();

            final List<Optional<Variant>> variants =
                    newExperiment.trafficProportion().variants().stream().map(
                                    variant ->
                                            Try.of(() -> APILocator.getVariantAPI().get(variant.id()))
                                                    .getOrNull())
                            .collect(Collectors.toList());

            final Variant variant1 = variants.get(0).orElseThrow();
            final Variant variant2 = variants.get(1).orElseThrow();

            final ContentType contentType = new ContentTypeDataGen().nextPersisted();
            final Contentlet content1Working = new ContentletDataGen(contentType).variant(variant1)
                    .nextPersisted();
            final Contentlet content2Working = new ContentletDataGen(contentType).variant(variant2)
                    .nextPersisted();
            final Contentlet content1Live = new ContentletDataGen(contentType).variant(variant1)
                    .nextPersisted();
            final Contentlet content2Live = new ContentletDataGen(contentType).variant(variant2)
                    .nextPersisted();

            final Contentlet pageAsContentlet = APILocator.getContentletAPI()
                    .findContentletByIdentifierAnyLanguage(pageId);
            final HTMLPageAsset page = APILocator.getHTMLPageAssetAPI()
                    .fromContentlet(pageAsContentlet);

            new MultiTreeDataGen().setPage(page).setContainer(container)
                    .setContentlet(content1Working).nextPersisted();

            new MultiTreeDataGen().setPage(page).setContainer(container)
                    .setContentlet(content2Working).nextPersisted();

            new MultiTreeDataGen().setPage(page).setContainer(container)
                    .setContentlet(content1Live).nextPersisted();

            new MultiTreeDataGen().setPage(page).setContainer(container)
                    .setContentlet(content2Live).nextPersisted();

            ExperimentDataGen.start(newExperiment);

            List<Contentlet> experimentContentlets = APILocator.getContentletAPI()
                    .getAllContentByVariants(APILocator.systemUser(),
                            false, newExperiment.trafficProportion().variants()
                                    .stream().map(ExperimentVariant::id).toArray(String[]::new));

            experimentContentlets.forEach((contentlet -> {
                assertTrue(Try.of(contentlet::isLive).getOrElse(false));
            }));

        } catch(Exception e) {
            Logger.error(this, e);
            throw e;
        }finally {
            APILocator.getExperimentsAPI().end(newExperiment.id().orElseThrow()
                    , APILocator.systemUser());
        }

    }

    /**
     * Method to test: {@link ExperimentsAPI#addVariant(String, String, User)} (String, User)}
     * When: The {@link com.dotcms.experiments.model.AbstractExperimentVariant#ORIGINAL_VARIANT} variant is created
     * Should: copy the page and the contentlet related to the page in the new variant
     */
    @Test
    public void testAddOriginalVariant_shouldCopyPageAndContentletToVariant()
            throws DotDataException, DotSecurityException {
        final HTMLPageAsset page = APILocator.getHTMLPageAssetAPI().fromContentlet(
                TestDataUtils.getPageContent(true, 1));
        ContentletDataGen.publish(page);

        final Container container = new ContainerDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();

        final Contentlet content1 = new ContentletDataGen(contentType)
                .nextPersisted();
        ContentletDataGen.publish(content1);

        final Contentlet content2 = new ContentletDataGen(contentType)
                .nextPersisted();
        ContentletDataGen.publish(content2);

        final Contentlet content3 = new ContentletDataGen(contentType)
                .nextPersisted();
        ContentletDataGen.publish(content3);

        new MultiTreeDataGen().setPage(page).setContainer(container)
                .setContentlet(content1).nextPersisted();

        new MultiTreeDataGen().setPage(page).setContainer(container)
                .setContentlet(content2).nextPersisted();

        new MultiTreeDataGen().setPage(page).setContainer(container)
                .setContentlet(content3).nextPersisted();

        final Experiment newExperiment = new ExperimentDataGen()
                .page(page)
                .addVariant("Test Gray Button")
                .nextPersisted();

        final ExperimentVariant originalVariant = newExperiment.
                trafficProportion().variants().first();

        List<Contentlet> experimentContentlets = APILocator.getContentletAPI()
                .getAllContentByVariants(APILocator.systemUser(),
                        false, originalVariant.id());

        // expecting the page + the 3 contentlets
        assertEquals(4, experimentContentlets.size());
    }

    /**
     * Method to test: {@link ExperimentsAPI#save(Experiment, User)}
     * When: The {@link HTMLPageAsset} when the Experiment is created on is NOT live
     * Should: throw DotStateException
     */
    @Test(expected = DotStateException.class)
    public void testSave_whenUnpublishedPage_shouldFail() {
        final HTMLPageAsset page = APILocator.getHTMLPageAssetAPI().fromContentlet(
                TestDataUtils.getPageContent(true, 1));
        new ExperimentDataGen()
                .page(page)
                .nextPersisted();
    }

    /*
     * Method to test: {@link ExperimentsAPI#start(String, User)}
     * When: an {@link Experiment} is started
     * Should: publish all the contents in the variants created for the experiment.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testAddMoreThanOneOriginalVariant_shouldFail() throws DotDataException, DotSecurityException {
        final Experiment newExperiment = new ExperimentDataGen()
                .addVariant("Test Green Button")
                .nextPersisted();

        APILocator.getExperimentsAPI().addVariant(newExperiment.id().orElse(""), ORIGINAL_VARIANT,
                APILocator.systemUser());
    }

    /**
     * Method to test: {@link ExperimentsAPI#deleteVariant(String, String, User)} (String, User)}
     * When: an {@link com.dotcms.experiments.model.AbstractExperimentVariant#ORIGINAL_VARIANT} is provided
     * Should: publish all the contents in the variants created for the experiment.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testADeleteOriginalVariant_shouldFail() throws DotDataException, DotSecurityException {
        final Experiment newExperiment = new ExperimentDataGen()
                .addVariant("Test Green Button")
                .nextPersisted();

        final ExperimentVariant originalVariant = newExperiment.trafficProportion()
                .variants().stream().filter((experimentVariant ->
                        experimentVariant.description().equals(ORIGINAL_VARIANT))).findFirst()
                .orElseThrow(()->new DotStateException("Unable to find Original Variant"));

        APILocator.getExperimentsAPI().deleteVariant(newExperiment.id().orElse(""), originalVariant.id(),
                APILocator.systemUser());
    }
}
