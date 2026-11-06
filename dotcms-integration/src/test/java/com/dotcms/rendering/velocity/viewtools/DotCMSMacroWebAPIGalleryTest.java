package com.dotcms.rendering.velocity.viewtools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link DotCMSMacroWebAPI#getMediaGalleryFolderFiles(String, Host)}: the folder and its files
 * are listed with the permissions of the user making the request.
 */
public class DotCMSMacroWebAPIGalleryTest {

    private static final List<Host> sites = new ArrayList<>();
    private static final List<Role> roles = new ArrayList<>();
    private static final List<User> users = new ArrayList<>();

    private static Role anonymousRole;
    private static Host site;

    @BeforeAll
    static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        anonymousRole = APILocator.getRoleAPI().loadCMSAnonymousRole();
        site = new SiteDataGen().nextPersisted();
        sites.add(site);
        grantRead(site, anonymousRole);
    }

    @AfterEach
    void clearThreadRequest() {
        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
    }

    @AfterAll
    static void cleanUp() {
        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
        users.forEach(user -> UserDataGen.remove(user, true));
        sites.forEach(each -> {
            Try.run(() -> APILocator.getHostAPI().archive(each, APILocator.systemUser(), false));
            Try.run(() -> APILocator.getHostAPI().delete(each, APILocator.systemUser(), false));
        });
        roles.forEach(role -> RoleDataGen.remove(role, true));
    }

    /**
     * When: the folder can only be read by a role the anonymous user does not have
     * Should: list nothing to the anonymous user
     */
    @Test
    void anonymousViewer_folderWithoutReadAccess_listsNothing() throws Exception {
        final Role readers = newRole();
        final Folder folder = newFolder(readers);
        publishedFile(folder, readers);

        final List<String> listed = fileNames(galleryFor(null).getMediaGalleryFolderFiles(folder.getPath(), site));

        assertTrue(listed.isEmpty(), "Expected no files, got " + listed);
    }

    /**
     * When: the folder and its file can be read by the anonymous role
     * Should: list the file to the anonymous user
     */
    @Test
    void anonymousViewer_readableFolder_listsItsFiles() throws Exception {
        final Folder folder = newFolder(anonymousRole);
        final Contentlet file = publishedFile(folder, anonymousRole);

        final List<String> listed = fileNames(galleryFor(null).getMediaGalleryFolderFiles(folder.getPath(), site));

        assertEquals(List.of(fileName(file)), listed);
    }

    /**
     * When: a logged-in user holds the role that can read the folder and its file
     * Should: list the file to that user
     */
    @Test
    void userWithReadAccess_listsFolderFiles() throws Exception {
        final Role readers = newRole();
        final Folder folder = newFolder(readers);
        final Contentlet file = publishedFile(folder, readers);
        final User reader = new UserDataGen().roles(readers).nextPersisted();
        users.add(reader);

        final List<String> listed = fileNames(galleryFor(reader).getMediaGalleryFolderFiles(folder.getPath(), site));

        assertEquals(List.of(fileName(file)), listed);
    }

    /**
     * When: the anonymous user can read the folder and one of its two files
     * Should: list only that file
     */
    @Test
    void anonymousViewer_onlyReadableFilesAreListed() throws Exception {
        final Role readers = newRole();
        final Folder folder = newFolder(anonymousRole);
        final Contentlet visible = publishedFile(folder, anonymousRole);
        final Contentlet hidden = publishedFile(folder, readers);

        final Set<String> listed = Set.copyOf(
                fileNames(galleryFor(null).getMediaGalleryFolderFiles(folder.getPath(), site)));

        assertEquals(Set.of(fileName(visible)), listed, "Expected only " + fileName(visible) + ", not "
                + fileName(hidden));
    }

    /**
     * When: the site can only be read by a role the anonymous user does not have, while the folder and the file can be
     * read by the anonymous role
     * Should: list nothing to the anonymous user
     */
    @Test
    void anonymousViewer_siteWithoutReadAccess_listsNothing() throws Exception {
        final Role readers = newRole();
        final Host closedSite = new SiteDataGen().nextPersisted();
        sites.add(closedSite);
        grantRead(closedSite, readers);
        final Folder folder = new FolderDataGen().site(closedSite).nextPersisted();
        grantRead(folder, anonymousRole);
        publishedFile(folder, anonymousRole);

        final List<String> listed = fileNames(
                galleryFor(null).getMediaGalleryFolderFiles(folder.getPath(), closedSite));

        assertTrue(listed.isEmpty(), "Expected no files, got " + listed);
    }

    private static Role newRole() {
        final Role role = new RoleDataGen().nextPersisted();
        roles.add(role);
        return role;
    }

    private static Folder newFolder(final Role reader) throws Exception {
        final Folder folder = new FolderDataGen().site(site).nextPersisted();
        grantRead(folder, reader);
        return folder;
    }

    private static Contentlet publishedFile(final Folder folder, final Role reader) throws Exception {
        final Contentlet file = new FileAssetDataGen(folder, "gallery file").nextPersisted();
        grantRead(file, reader);
        return ContentletDataGen.publish(file);
    }

    private static void grantRead(final Permissionable asset, final Role role) throws Exception {
        APILocator.getPermissionAPI().save(new Permission(asset.getPermissionId(), role.getId(),
                PermissionAPI.PERMISSION_READ), asset, APILocator.systemUser(), false);
    }

    private static String fileName(final Contentlet file) {
        return file.getStringProperty(FileAssetAPI.FILE_NAME_FIELD);
    }

    private static List<String> fileNames(final List<HashMap<String, IFileAsset>> files) {
        return files.stream().map(entry -> entry.get("photo").getFileName()).collect(Collectors.toList());
    }

    /**
     * Creates the tool as Velocity does for a request made by the given user, or by nobody when it is {@code null}.
     */
    private static DotCMSMacroWebAPI galleryFor(final User user) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(WebKeys.USER)).thenReturn(user);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        final ViewContext viewContext = mock(ViewContext.class);
        when(viewContext.getVelocityContext()).thenReturn(mock(Context.class));
        when(viewContext.getRequest()).thenReturn(request);
        final DotCMSMacroWebAPI macro = new DotCMSMacroWebAPI();
        macro.init(viewContext);
        return macro;
    }
}
