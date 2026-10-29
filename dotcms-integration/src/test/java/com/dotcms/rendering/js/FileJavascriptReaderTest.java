package com.dotcms.rendering.js;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.rendering.util.ScriptingReaderParams;
import com.dotcms.rest.api.v1.HTTPMethod;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.Reader;
import java.nio.file.Files;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Covers which {@code get.js} files {@link FileJavascriptReader} hands over to be run, depending on
 * the user who last saved them.
 */
public class FileJavascriptReaderTest {

    private static final String SAVED_FILE_CODE = "'ready';";

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link FileJavascriptReader#getJavaScriptReader(ScriptingReaderParams)}
     * Given: a get.js last saved by a back-end user without the Scripting Developer role
     * When: the folder is requested
     * Should: refuse to hand the file over
     */
    @Test
    public void test_getJavaScriptReader_fileLastSavedByUserWithoutScriptingRole_refused() throws Exception {
        final User editor = new UserDataGen()
                .roles(APILocator.getRoleAPI().loadBackEndUserRole()).nextPersisted();

        assertThrows(DotSecurityException.class, () -> readFileSavedBy(editor));
    }

    /**
     * Method to test: {@link FileJavascriptReader#getJavaScriptReader(ScriptingReaderParams)}
     * Given: a get.js last saved by a back-end user holding the Scripting Developer role
     * When: the folder is requested
     * Should: hand over the file's code
     */
    @Test
    public void test_getJavaScriptReader_fileLastSavedByScriptingDeveloper_returnsCode() throws Exception {
        final User editor = new UserDataGen()
                .roles(APILocator.getRoleAPI().loadBackEndUserRole(), getScriptingDeveloperRole())
                .nextPersisted();

        assertEquals(SAVED_FILE_CODE, readFileSavedBy(editor));
    }

    /**
     * Method to test: {@link FileJavascriptReader#getJavaScriptReader(ScriptingReaderParams)}
     * Given: a get.js last saved by a CMS Administrator who does not hold the Scripting Developer role
     * When: the folder is requested
     * Should: hand over the file's code, since an administrator can grant themselves the role
     */
    @Test
    public void test_getJavaScriptReader_fileLastSavedByAdmin_returnsCode() throws Exception {
        final User editor = new UserDataGen()
                .roles(APILocator.getRoleAPI().loadCMSAdminRole()).nextPersisted();

        assertEquals(SAVED_FILE_CODE, readFileSavedBy(editor));
    }

    /**
     * Creates a get.js in a new folder of a new site, saves and publishes a new version of it as the
     * given user, and reads it back the way the endpoint does.
     *
     * @param editor the user recorded as the last one to modify the file
     * @return the code the reader handed over
     * @throws Exception if the file cannot be created or saved, or the reader refuses it
     */
    private String readFileSavedBy(final User editor) throws Exception {
        final Host site = new SiteDataGen().nextPersisted();
        final String folderName = System.currentTimeMillis() + "saved";
        final Folder folder = APILocator.getFolderAPI().createFolders(
                JsResource.JS_PATH + "/" + folderName, site, APILocator.systemUser(), false);
        try {
            saveAndPublishAs(createFile(folder), editor);

            final HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getServerName()).thenReturn(site.getHostname());
            when(request.getSession(false)).thenReturn(mock(HttpSession.class));
            when(request.getSession()).thenReturn(mock(HttpSession.class));

            final ScriptingReaderParams params = new ScriptingReaderParams.ScriptingReaderParamsBuilder()
                    .setFolderName(folderName)
                    .setHttpMethod(HTTPMethod.GET)
                    .setRequest(request)
                    .setUser(APILocator.getUserAPI().getAnonymousUser())
                    .setPageMode(PageMode.LIVE)
                    .build();
            try (Reader reader = new FileJavascriptReader().getJavaScriptReader(params)) {
                return new BufferedReader(reader).lines().collect(Collectors.joining("\n")).trim();
            }
        } finally {
            APILocator.getFolderAPI().delete(folder, APILocator.systemUser(), false);
        }
    }

    /**
     * Creates and publishes a get.js in the folder as the system user.
     *
     * @param folder the folder the file goes in
     * @return the published file asset
     * @throws Exception if the file cannot be written, saved or published
     */
    private Contentlet createFile(final Folder folder) throws Exception {
        final File source = new File(Files.createTempDirectory("dotcms-test").toFile(), "get.js");
        FileUtil.write(source, SAVED_FILE_CODE);

        final Contentlet file = new FileAssetDataGen(folder, source).nextPersisted();
        file.setIndexPolicy(IndexPolicy.FORCE);
        file.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
        APILocator.getContentletAPI().publish(file, APILocator.systemUser(), false);

        APILocator.getPermissionAPI().save(
                new Permission(file.getPermissionId(),
                        APILocator.getRoleAPI().loadCMSAnonymousRole().getId(),
                        PermissionAPI.PERMISSION_READ),
                file, APILocator.systemUser(), false);
        return file;
    }

    /**
     * Gives the user write and publish permission on the file, then checks out, checks in and
     * publishes a new version of it as that user, so the user becomes its last modifier.
     *
     * @param file   the file asset to save again
     * @param editor the user who saves it
     * @throws Exception if the permission or the new version cannot be saved
     */
    private void saveAndPublishAs(final Contentlet file, final User editor) throws Exception {
        APILocator.getPermissionAPI().save(
                new Permission(file.getPermissionId(),
                        APILocator.getRoleAPI().getUserRole(editor).getId(),
                        PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE
                                | PermissionAPI.PERMISSION_PUBLISH),
                file, APILocator.systemUser(), false);

        final Contentlet checkout = APILocator.getContentletAPI()
                .checkout(file.getInode(), editor, false);
        checkout.setIndexPolicy(IndexPolicy.FORCE);
        checkout.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
        final Contentlet saved = APILocator.getContentletAPI().checkin(checkout, editor, false);
        APILocator.getContentletAPI().publish(saved, editor, false);

        assertEquals(editor.getUserId(), saved.getModUser());
    }

    /**
     * Loads the Scripting Developer role, creating it when the test database does not have it yet.
     *
     * @return the role
     * @throws DotDataException if the role cannot be read or created
     */
    private static Role getScriptingDeveloperRole() throws DotDataException {
        final Role role = APILocator.getRoleAPI().loadRoleByKey(Role.SCRIPTING_DEVELOPER);
        return null != role ? role : new RoleDataGen().key(Role.SCRIPTING_DEVELOPER)
                .name(Role.SCRIPTING_DEVELOPER).nextPersisted();
    }
}
