package com.dotmarketing.quartz.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.quartz.ScheduledTask;
import com.liferay.portal.model.User;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

/**
 * Integration tests for the user {@link ContentImportThread} runs the import as.
 */
public class ContentImportThreadTest extends IntegrationTestBase {

    private static User systemUser;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.systemUser();
    }

    /**
     * Method to test: {@link ContentImportThread#execute(JobExecutionContext)}
     * Given: a job saved by a user who can write and publish content of the content type
     * Expected: the content is created live, and the user is its last editor
     */
    @Test
    public void test_execute_userCanWriteAndPublish_shouldImportAsThatUser() throws Exception {
        final ContentType contentType = newContentType();
        final User user = newUserWithPermissions(contentType,
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_PUBLISH);
        final String title = "imported-" + UUID.randomUUID();

        runImport(contentType, title, user.getUserId());

        final List<Contentlet> imported = findByTitle(contentType, title);
        assertEquals(1, imported.size());
        assertEquals(user.getUserId(), imported.get(0).getModUser());
        assertTrue(imported.get(0).isLive());
    }

    /**
     * Method to test: {@link ContentImportThread#execute(JobExecutionContext)}
     * Given: a job saved by a user who can write but not publish content of the content type
     * Expected: the content is created as a working version only
     */
    @Test
    public void test_execute_userCannotPublish_shouldImportWorkingVersion() throws Exception {
        final ContentType contentType = newContentType();
        final User user = newUserWithPermissions(contentType,
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE);
        final String title = "imported-" + UUID.randomUUID();

        runImport(contentType, title, user.getUserId());

        final List<Contentlet> imported = findByTitle(contentType, title);
        assertEquals(1, imported.size());
        assertEquals(user.getUserId(), imported.get(0).getModUser());
        assertFalse(imported.get(0).isLive());
    }

    /**
     * Method to test: {@link ContentImportThread#execute(JobExecutionContext)}
     * Given: a job saved by a user who can only read content of the content type
     * Expected: no content is created
     */
    @Test
    public void test_execute_userCannotWrite_shouldNotImport() throws Exception {
        final ContentType contentType = newContentType();
        final User user = newUserWithPermissions(contentType, PermissionAPI.PERMISSION_READ);
        final String title = "imported-" + UUID.randomUUID();

        runImport(contentType, title, user.getUserId());

        assertTrue(findByTitle(contentType, title).isEmpty());
    }

    /**
     * Method to test: {@link ContentImportThread#execute(JobExecutionContext)}
     * Given: a job with no user recorded, as saved before the user was recorded
     * Expected: no content is created and the file stays where it is, so it is imported once
     * the job is saved again
     */
    @Test
    public void test_execute_noUserRecorded_shouldNotImport() throws Exception {
        final ContentType contentType = newContentType();
        final String title = "imported-" + UUID.randomUUID();

        final File csv = runImport(contentType, title, null);

        assertTrue(findByTitle(contentType, title).isEmpty());
        assertTrue("The file should be left for the next run", csv.exists());
    }

    /**
     * Method to test: {@link ContentImportThread#execute(JobExecutionContext)}
     * Given: a job whose recorded user was deactivated after saving it
     * Expected: no content is created and the file stays where it is
     */
    @Test
    public void test_execute_userNotActive_shouldNotImport() throws Exception {
        final ContentType contentType = newContentType();
        final User user = newUserWithPermissions(contentType,
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_PUBLISH);
        user.setActive(false);
        APILocator.getUserAPI().save(user, systemUser, false);
        final String title = "imported-" + UUID.randomUUID();

        final File csv = runImport(contentType, title, user.getUserId());

        assertTrue(findByTitle(contentType, title).isEmpty());
        assertTrue("The file should be left for the next run", csv.exists());
    }

    /**
     * Method to test: {@link ContentImportThread#execute(JobExecutionContext)}
     * Given: a job whose recorded user no longer exists
     * Expected: no content is created and the file stays where it is
     */
    @Test
    public void test_execute_userNotFound_shouldNotImport() throws Exception {
        final ContentType contentType = newContentType();
        final String title = "imported-" + UUID.randomUUID();

        final File csv = runImport(contentType, title, "user-" + UUID.randomUUID());

        assertTrue(findByTitle(contentType, title).isEmpty());
        assertTrue("The file should be left for the next run", csv.exists());
    }

    private static ContentType newContentType() {
        return new ContentTypeDataGen()
                .host(new SiteDataGen().nextPersisted())
                .field(new FieldDataGen().type(TextField.class).name("title").velocityVarName("title").indexed(true).next())
                .nextPersisted();
    }

    private static User newUserWithPermissions(final ContentType contentType, final int permissions) throws Exception {
        final Role role = new RoleDataGen().nextPersisted();
        // On the content type, and on the content of its site, which new content inherits
        APILocator.getPermissionAPI().assignPermissions(List.of(
                        new Permission(contentType.getPermissionId(), role.getId(), permissions, true)),
                contentType, systemUser, false);
        final Host site = APILocator.getHostAPI().find(contentType.host(), systemUser, false);
        APILocator.getPermissionAPI().assignPermissions(List.of(
                        new Permission(site.getPermissionId(), role.getId(), PermissionAPI.PERMISSION_READ, true),
                        new Permission(Contentlet.class.getCanonicalName(), site.getPermissionId(),
                                role.getId(), permissions, true)),
                site, systemUser, false);
        return new UserDataGen().roles(role, APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();
    }

    /**
     * Runs the job on a one-line CSV with the given title.
     *
     * @return the CSV file the job was given
     */
    private static File runImport(final ContentType contentType, final String title, final String userId)
            throws Exception {
        final File folder = Files.createTempDirectory("content-import").toFile();
        final File csv = new File(folder, "import.csv");
        Files.writeString(csv.toPath(), "title\n" + title + "\n", StandardCharsets.UTF_8);

        final Map<String, Object> properties = new HashMap<>();
        properties.put("structure", contentType.name());
        properties.put("path", csv.getAbsolutePath());
        properties.put("logpath", new File(folder, "log").getAbsolutePath());
        if (userId != null) {
            properties.put(ScheduledTask.SCHEDULED_BY_USER_ID, userId);
        }
        final JobExecutionContext context = mock(JobExecutionContext.class);
        final JobDetail jobDetail = mock(JobDetail.class);
        when(jobDetail.getName()).thenReturn("content-import-test");
        when(context.getJobDetail()).thenReturn(jobDetail);
        when(context.getMergedJobDataMap()).thenReturn(new JobDataMap(properties));

        new ContentImportThread().execute(context);
        return csv;
    }

    private static List<Contentlet> findByTitle(final ContentType contentType, final String title) throws Exception {
        return APILocator.getContentletAPI().findByStructure(contentType.inode(), systemUser, false, 0, 0)
                .stream().filter(contentlet -> title.equals(contentlet.getTitle())).toList();
    }
}
