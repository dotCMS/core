package com.dotcms.rest.api.v1.asset.bulkduplicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dotcms.Junit5WeldBaseTest;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.stream.IntStream;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import org.jboss.weld.junit5.EnableWeld;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@code POST /api/v1/assets/folders/_bulkduplicate}: the submission itself
 * (#37062, contract §1 to §3).
 * <p>
 * Drives {@link FolderBulkDuplicateHelper} injected through CDI, the convention
 * {@code FolderBulkDeleteResourceIT} follows, so a deployment Weld would refuse is noticed. The
 * exception mapper and the back-end-user requirement behind {@code 401} live on the resource, so
 * they are checked over the wire by the Postman collection, not here.
 */
@EnableWeld
public class FolderBulkDuplicateResourceIT extends Junit5WeldBaseTest {

    @Inject
    FolderBulkDuplicateHelper helper;

    @Inject
    JobQueueManagerAPI jobQueueManagerAPI;

    private static User admin;
    private static Host site;

    @BeforeAll
    public static void prepare() throws Exception {
        com.dotcms.util.IntegrationTestInitService.getInstance().init();
        admin = APILocator.systemUser();
        site = new SiteDataGen().nextPersisted();
    }

    @AfterEach
    public void restoreCeiling() {
        Config.setProperty(FolderBulkDuplicateHelper.MAX_PATHS_KEY, null);
    }

    private Folder folder() {
        return new FolderDataGen().site(site).nextPersisted();
    }

    private String pathOf(final Folder folder) {
        return String.format("//%s/%s/", site.getHostname(), folder.getName());
    }

    private FolderBulkDuplicateForm formOf(final List<String> paths) {
        return FolderBulkDuplicateForm.builder().assetPaths(paths).build();
    }

    /**
     * Method to test: {@link FolderBulkDuplicateHelper#submit(FolderBulkDuplicateForm, User)}
     * Given Scenario: A single folder
     * ExpectedResult: A handle with the job id, a ready-to-use status URL, and submitted == 1. A
     * single folder runs as a batch of one; there is no synchronous duplicate.
     */
    @Test
    public void test_submit_singlePath_returnsHandle_batchOfOne() throws Exception {
        final AbstractFolderBulkDuplicateSubmitResponse response =
                helper.submit(formOf(List.of(pathOf(folder()))), admin);

        assertNotNull(response.jobId());
        assertEquals("/api/v1/jobs/" + response.jobId() + "/status", response.statusUrl());
        assertEquals(1, response.submitted());
    }

    /**
     * Method to test: {@link FolderBulkDuplicateHelper#submit(FolderBulkDuplicateForm, User)}
     * Given Scenario: Three distinct folders
     * ExpectedResult: submitted equals the number of distinct folders, which is the total the
     * outcome later reports
     */
    @Test
    public void test_submit_severalPaths_submittedMatchesDistinctPaths() throws Exception {
        final AbstractFolderBulkDuplicateSubmitResponse response = helper.submit(
                formOf(List.of(pathOf(folder()), pathOf(folder()), pathOf(folder()))), admin);

        assertEquals(3, response.submitted());
    }

    /**
     * Method to test: {@link FolderBulkDuplicateHelper#submit(FolderBulkDuplicateForm, User)}
     * Given Scenario: The same folder three ways: as sent, without its trailing slash, and in a
     * different case. Folder resolution ignores case, so all three name one folder.
     * ExpectedResult: Collapsed to one, and the stored path is the first spelling exactly as sent,
     * since it is what the result key carries back to the author
     */
    @Test
    public void test_submit_samePathSpelledThreeWays_collapsedKeepingFirstAsSent()
            throws Exception {
        final String asSent = pathOf(folder());
        final String withoutSlash = asSent.substring(0, asSent.length() - 1);
        final String upperCase = asSent.toUpperCase();

        final AbstractFolderBulkDuplicateSubmitResponse response =
                helper.submit(formOf(List.of(asSent, withoutSlash, upperCase)), admin);

        assertEquals(1, response.submitted());
        assertEquals(List.of(asSent), FolderBulkDuplicateHelper.pathsOf(
                jobQueueManagerAPI.getJob(response.jobId()).parameters()));
    }

    /**
     * Method to test: {@link FolderBulkDuplicateHelper#submit(FolderBulkDuplicateForm, User)}
     * Given Scenario: An empty selection
     * ExpectedResult: Refused before any job exists, with errorCode EMPTY_SELECTION on assetPaths
     */
    @Test
    public void test_submit_emptySelection_refusedWithEmptySelectionCode() {
        final FolderBulkDuplicateRefusedException ex = assertThrows(
                FolderBulkDuplicateRefusedException.class,
                () -> helper.submit(FolderBulkDuplicateForm.builder().build(), admin));

        assertEquals("EMPTY_SELECTION", ex.errorCode());
        assertEquals("assetPaths", ex.fieldName());
        assertEquals(Response.Status.BAD_REQUEST, ex.status());
    }

    /**
     * Method to test: {@link FolderBulkDuplicateHelper#submit(FolderBulkDuplicateForm, User)}
     * Given Scenario: One more distinct path than the default ceiling of 50
     * ExpectedResult: Refused with errorCode OVER_MAX_PATHS, told apart from an empty selection
     * although both are 400s
     */
    @Test
    public void test_submit_overMaxPaths_refusedWithOverMaxPathsCode() {
        final List<String> tooMany = IntStream.rangeClosed(0,
                        FolderBulkDuplicateHelper.DEFAULT_MAX_PATHS)
                .mapToObj(i -> "//" + site.getHostname() + "/not-real-" + i + "/")
                .toList();

        final FolderBulkDuplicateRefusedException ex = assertThrows(
                FolderBulkDuplicateRefusedException.class,
                () -> helper.submit(formOf(tooMany), admin));

        assertEquals("OVER_MAX_PATHS", ex.errorCode());
        assertEquals("assetPaths", ex.fieldName());
        assertEquals(Response.Status.BAD_REQUEST, ex.status());
    }

    /**
     * Method to test: {@link FolderBulkDuplicateHelper#submit(FolderBulkDuplicateForm, User)}
     * Given Scenario: FOLDER_BULK_DUPLICATE_MAX_PATHS overridden to 2
     * ExpectedResult: Two folders are accepted and three are refused, so the ceiling enforced is
     * the configured one, the same value the app configuration advertises
     */
    @Test
    public void test_submit_honoursAnOverriddenCeiling() throws Exception {
        Config.setProperty(FolderBulkDuplicateHelper.MAX_PATHS_KEY, 2);

        assertEquals(2, helper.submit(
                formOf(List.of(pathOf(folder()), pathOf(folder()))), admin).submitted());

        final FolderBulkDuplicateRefusedException ex = assertThrows(
                FolderBulkDuplicateRefusedException.class,
                () -> helper.submit(formOf(
                        List.of(pathOf(folder()), pathOf(folder()), pathOf(folder()))), admin));
        assertEquals("OVER_MAX_PATHS", ex.errorCode());
    }

    /**
     * Method to test: {@link FolderBulkDuplicateHelper#submit(FolderBulkDuplicateForm, User)}
     * Given Scenario: The same folder submitted in two separate requests
     * ExpectedResult: Both are accepted as separate runs. Duplication carries no overlap guard,
     * unlike delete: a second duplicate is the author's choice.
     */
    @Test
    public void test_submit_sameFolderTwice_bothAccepted() throws Exception {
        final String path = pathOf(folder());

        final String first = helper.submit(formOf(List.of(path)), admin).jobId();
        final String second = helper.submit(formOf(List.of(path)), admin).jobId();

        assertNotNull(first);
        assertNotNull(second);
        assertNotEquals(first, second);
    }
}
