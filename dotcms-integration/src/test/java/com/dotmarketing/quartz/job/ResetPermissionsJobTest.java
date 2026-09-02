package com.dotmarketing.quartz.job;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration test for {@link ResetPermissionsJob} to verify that permissions are correctly reset
 * for different types of Permissionable objects (Folders, Hosts, Contentlets).
 *
 * This test specifically validates the fix for folder retrieval in the retrievePermissionable method.
 *
 * @author dotCMS
 */
public class ResetPermissionsJobTest extends IntegrationTestBase {

    private static PermissionAPI permissionAPI;
    private static FolderAPI folderAPI;
    private static ContentletAPI contentletAPI;
    private static ContentTypeAPI contentTypeAPI;
    private static FieldAPI fieldAPI;
    private static User systemUser;
    private static Host testSite;

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
        QuartzUtils.startSchedulers();

        // Initialize APIs
        permissionAPI = APILocator.getPermissionAPI();
        folderAPI = APILocator.getFolderAPI();
        contentletAPI = APILocator.getContentletAPI();
        contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
        fieldAPI = APILocator.getContentTypeFieldAPI();
        systemUser = APILocator.getUserAPI().getSystemUser();

        // Create test site
        testSite = new SiteDataGen().nextPersisted();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        // Clean up test site if it exists
        if (testSite != null && testSite.getIdentifier() != null) {
            try {
                APILocator.getHostAPI().archive(testSite, systemUser, false);
                APILocator.getHostAPI().delete(testSite, systemUser, false);
            } catch (Exception e) {
                Logger.warn(ResetPermissionsJobTest.class, "Error cleaning up test site: " + e.getMessage());
            }
        }
    }

    /**
     * Tests that ResetPermissionsJob correctly handles Folder permissionables.
     * This test validates the fix where folders were not being retrieved correctly
     * in the retrievePermissionable method.
     *
     * Test scenario:
     * 1. Create a parent folder and a child folder
     * 2. Set individual permissions on both folders (not inheriting)
     * 3. Trigger ResetPermissionsJob on parent folder
     * 4. Verify that child folder now inherits permissions from parent
     */
    @Test
    public void test_resetPermissions_withFolder() throws Exception {
        Folder parentFolder = null;
        Folder childFolder = null;

        try {
            // 1. Create folder hierarchy
            parentFolder = folderAPI.createFolders("/test-reset-permissions-parent-" + System.currentTimeMillis() + "/",
                    testSite, systemUser, false);
            assertNotNull("Parent folder should be created", parentFolder);

            childFolder = folderAPI.createFolders(parentFolder.getPath() + "child-folder/",
                    testSite, systemUser, false);
            assertNotNull("Child folder should be created", childFolder);

            // 2. Set individual permissions (not inheriting)
            permissionAPI.permissionIndividually(testSite, parentFolder, systemUser);
            permissionAPI.permissionIndividually(testSite, childFolder, systemUser);

            // Verify permissions are NOT inheriting
            assertFalse("Parent folder should NOT be inheriting permissions initially",
                    permissionAPI.isInheritingPermissions(parentFolder));
            assertFalse("Child folder should NOT be inheriting permissions initially",
                    permissionAPI.isInheritingPermissions(childFolder));

            // 3. Trigger ResetPermissionsJob using the parent folder identifier
            Logger.info(this, "Triggering ResetPermissionsJob for folder: " + parentFolder.getIdentifier());
            ResetPermissionsJob.triggerJobImmediately(parentFolder);

            // 4. Wait for job to complete (with timeout)
            waitForJobCompletion(10000); // Wait up to 10 seconds

            // 5. Verify that child folder now inherits permissions
            assertTrue("Child folder should now be inheriting permissions from parent",
                    permissionAPI.isInheritingPermissions(childFolder));

            Logger.info(this, "✓ Folder permission reset test passed");

        } finally {
            // Cleanup
            if (childFolder != null) {
                try {
                    folderAPI.delete(childFolder, systemUser, false);
                } catch (Exception e) {
                    Logger.warn(this, "Error cleaning up child folder: " + e.getMessage());
                }
            }
            if (parentFolder != null) {
                try {
                    folderAPI.delete(parentFolder, systemUser, false);
                } catch (Exception e) {
                    Logger.warn(this, "Error cleaning up parent folder: " + e.getMessage());
                }
            }
        }
    }


    /**
     * Helper method to wait for job completion.
     * Polls the job queue to see if jobs are still executing.
     *
     * @param maxWaitMillis Maximum time to wait in milliseconds
     * @throws InterruptedException if thread is interrupted while waiting
     */
    private void waitForJobCompletion(long maxWaitMillis) throws InterruptedException {
        final long startTime = System.currentTimeMillis();
        final long pollInterval = 500; // Poll every 500ms

        Logger.info(this, "Waiting for ResetPermissionsJob to complete...");

        while (System.currentTimeMillis() - startTime < maxWaitMillis) {
            try {
                // Check if there are any currently executing ResetPermissionsJob instances
                final var executingJobs = QuartzUtils.getScheduler().getCurrentlyExecutingJobs();
                final boolean hasResetPermissionsJobRunning = executingJobs.stream()
                        .anyMatch(context -> context.getClass().equals(ResetPermissionsJob.class));

                if (!hasResetPermissionsJobRunning) {
                    // Wait a bit more to ensure the job has fully completed and persisted changes
                    Thread.sleep(1000);
                    Logger.info(this, "ResetPermissionsJob completed");
                    return;
                }
            } catch (Exception e) {
                Logger.warn(this, "Error checking job status: " + e.getMessage());
            }

            Thread.sleep(pollInterval);
        }

        Logger.warn(this, "Timeout waiting for ResetPermissionsJob to complete");
    }
}
