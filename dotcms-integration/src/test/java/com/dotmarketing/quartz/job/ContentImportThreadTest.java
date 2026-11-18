package com.dotmarketing.quartz.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
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
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.quartz.ScheduledTask;
import com.liferay.portal.model.User;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
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

    /**
     * Method to test: {@link ContentImportThread#execute(JobExecutionContext)}
     * Given: a job saved by a user who can write and publish, importing a line with a tag
     * Expected: the content is created with the tag related to it
     */
    @Test
    public void test_execute_withTags_shouldRelateTheTags() throws Exception {
        final ContentType contentType = newContentType();
        final User user = newUserWithPermissions(contentType,
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_PUBLISH);
        final String title = "imported-" + UUID.randomUUID();
        final String tag = "tag-" + UUID.randomUUID();

        runImport(contentType, "title,tags\n" + title + "," + tag + "\n", null, user.getUserId());

        final List<Contentlet> imported = findByTitle(contentType, title);
        assertEquals(1, imported.size());
        assertEquals(List.of(tag), tagNames(imported.get(0)));
    }

    /**
     * Method to test: {@link ContentImportThread#execute(JobExecutionContext)}
     * Given: a job saved by a user who can only read content of the content type, importing a line with a tag
     * Expected: neither the content nor the tag is created
     */
    @Test
    public void test_execute_withTags_userCannotWrite_shouldNotCreateTheTag() throws Exception {
        final ContentType contentType = newContentType();
        final User user = newUserWithPermissions(contentType, PermissionAPI.PERMISSION_READ);
        final String title = "imported-" + UUID.randomUUID();
        final String tag = "tag-" + UUID.randomUUID();

        runImport(contentType, "title,tags\n" + title + "," + tag + "\n", null, user.getUserId());

        assertTrue(findByTitle(contentType, title).isEmpty());
        assertTrue(APILocator.getTagAPI().getTagsByName(tag).isEmpty());
    }

    /**
     * Method to test: {@link ContentImportThread#execute(JobExecutionContext)}
     * Given: a job updating content by a key field, run twice with different tags
     * Expected: the content is updated once and keeps only the tags of the last run
     */
    @Test
    public void test_execute_updateByKeyField_shouldReplaceTheTags() throws Exception {
        final ContentType contentType = newContentType();
        final User user = newUserWithPermissions(contentType,
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_PUBLISH);
        final String title = "imported-" + UUID.randomUUID();
        final Host site = APILocator.getHostAPI().find(contentType.host(), systemUser, false);
        new ContentletDataGen(contentType).host(site).setProperty("title", title).nextPersisted();
        final String keyFieldId = fieldId(contentType, "title");

        runImport(contentType, "title,tags\n" + title + ",first\n", keyFieldId, user.getUserId());
        runImport(contentType, "title,tags\n" + title + ",second\n", keyFieldId, user.getUserId());

        final List<Contentlet> imported = findByTitle(contentType, title);
        assertEquals(1, imported.size());
        assertEquals(user.getUserId(), imported.get(0).getModUser());
        assertEquals(List.of("second"), tagNames(imported.get(0)));
    }

    /**
     * Method to test: {@link ContentImportThread#execute(JobExecutionContext)}
     * Given: a job updating content by a key field with a line that fails validation, then saved by another
     * user and run again with a valid line
     * Expected: the failed line leaves the content unlocked, so the other user's run updates it
     */
    @Test
    public void test_execute_updateByKeyField_lineWithErrors_shouldReleaseTheLock() throws Exception {
        final ContentType contentType = newContentTypeWithRequiredSummary();
        final int permissions = PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE
                | PermissionAPI.PERMISSION_PUBLISH;
        final User firstUser = newUserWithPermissions(contentType, permissions);
        final User secondUser = newUserWithPermissions(contentType, permissions);
        final String title = "imported-" + UUID.randomUUID();
        newContentWithSummary(contentType, title);
        final String keyFieldId = fieldId(contentType, "title");

        runImport(contentType, "title,summary\n" + title + ",\n", keyFieldId, firstUser.getUserId());
        final List<Contentlet> afterFailedLine = findByTitle(contentType, title);
        assertEquals(1, afterFailedLine.size());
        assertFalse(APILocator.getVersionableAPI().isLocked(afterFailedLine.get(0)));

        runImport(contentType, "title,summary\n" + title + ",updated\n", keyFieldId, secondUser.getUserId());

        final List<Contentlet> imported = findByTitle(contentType, title);
        assertEquals(1, imported.size());
        assertEquals(secondUser.getUserId(), imported.get(0).getModUser());
        assertEquals("updated", imported.get(0).getStringProperty("summary"));
    }

    /**
     * Method to test: {@link ContentImportThread#execute(JobExecutionContext)}
     * Given: a key field that matches two contents and a line that fails validation
     * Expected: neither content is left locked
     */
    @Test
    public void test_execute_updateByKeyField_severalMatches_lineWithErrors_shouldReleaseEveryLock() throws Exception {
        final ContentType contentType = newContentTypeWithRequiredSummary();
        final User user = newUserWithPermissions(contentType,
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_PUBLISH);
        final String title = "imported-" + UUID.randomUUID();
        newContentWithSummary(contentType, title);
        newContentWithSummary(contentType, title);

        runImport(contentType, "title,summary\n" + title + ",\n", fieldId(contentType, "title"), user.getUserId());

        final List<Contentlet> matches = findByTitle(contentType, title);
        assertEquals(2, matches.size());
        for (final Contentlet match : matches) {
            assertFalse(APILocator.getVersionableAPI().isLocked(match));
        }
    }

    /**
     * Method to test: {@link ContentImportThread#execute(JobExecutionContext)}
     * Given: content the saving user had already locked, and a line for it that fails validation
     * Expected: the user keeps the lock
     */
    @Test
    public void test_execute_updateByKeyField_lockedByTheSameUser_shouldKeepTheLock() throws Exception {
        final ContentType contentType = newContentTypeWithRequiredSummary();
        final User user = newUserWithPermissions(contentType,
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_PUBLISH);
        final String title = "imported-" + UUID.randomUUID();
        final Contentlet existing = newContentWithSummary(contentType, title);
        APILocator.getContentletAPI().lock(existing, user, false);

        runImport(contentType, "title,summary\n" + title + ",\n", fieldId(contentType, "title"), user.getUserId());

        final List<Contentlet> matches = findByTitle(contentType, title);
        assertEquals(1, matches.size());
        assertEquals(Optional.of(user.getUserId()), APILocator.getVersionableAPI().getLockedBy(matches.get(0)));
    }

    private static ContentType newContentTypeWithRequiredSummary() {
        return new ContentTypeDataGen()
                .host(new SiteDataGen().nextPersisted())
                .field(new FieldDataGen().type(TextField.class).name("title").velocityVarName("title").indexed(true).next())
                .field(new FieldDataGen().type(TextField.class).name("summary").velocityVarName("summary").required(true).next())
                .nextPersisted();
    }

    private static Contentlet newContentWithSummary(final ContentType contentType, final String title) throws Exception {
        final Host site = APILocator.getHostAPI().find(contentType.host(), systemUser, false);
        return new ContentletDataGen(contentType).host(site).setProperty("title", title)
                .setProperty("summary", "original").nextPersisted();
    }

    private static ContentType newContentType() {
        return new ContentTypeDataGen()
                .host(new SiteDataGen().nextPersisted())
                .field(new FieldDataGen().type(TextField.class).name("title").velocityVarName("title").indexed(true).next())
                .field(new FieldDataGen().type(TagField.class).name("tags").velocityVarName("tags").indexed(true).next())
                .nextPersisted();
    }

    private static String fieldId(final ContentType contentType, final String variable) {
        return contentType.fields().stream().filter(field -> variable.equals(field.variable()))
                .map(Field::id).findFirst().orElseThrow();
    }

    private static List<String> tagNames(final Contentlet contentlet) throws Exception {
        return APILocator.getTagAPI().getTagsByInodeAndFieldVarName(contentlet.getInode(), "tags")
                .stream().map(Tag::getTagName).collect(Collectors.toList());
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
        return runImport(contentType, "title\n" + title + "\n", null, userId);
    }

    /**
     * Runs the job on the given CSV.
     *
     * @param keyFieldId the id of the field to update content by, or {@code null} to only create content
     * @return the CSV file the job was given
     */
    private static File runImport(final ContentType contentType, final String csvContent, final String keyFieldId,
            final String userId) throws Exception {
        final File folder = Files.createTempDirectory("content-import").toFile();
        final File csv = new File(folder, "import.csv");
        Files.writeString(csv.toPath(), csvContent, StandardCharsets.UTF_8);

        final Map<String, Object> properties = new HashMap<>();
        properties.put("structure", contentType.name());
        properties.put("path", csv.getAbsolutePath());
        properties.put("logpath", new File(folder, "log").getAbsolutePath());
        if (userId != null) {
            properties.put(ScheduledTask.SCHEDULED_BY_USER_ID, userId);
        }
        if (keyFieldId != null) {
            properties.put("updatebykeyfield", keyFieldId);
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
