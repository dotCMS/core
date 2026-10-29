package com.dotmarketing.factories;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.LinkDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TemplateLayoutDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link PublishFactory#publishAsset(Inode, User, boolean, boolean)} and the
 * template publish in {@link com.dotmarketing.portlets.templates.business.TemplateAPI}: the asset
 * handed in goes live only for a user holding PUBLISH on it, and the containers a template drags
 * along go live only where the user holds PUBLISH on each one. The users can read the site and
 * everything in it, which is what any back-end user working on the site can do.
 */
public class PublishAssetPermissionIT {

    private static User systemUser;
    private static Host site;
    private static final List<User> usersToClean = new ArrayList<>();

    @BeforeAll
    static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.systemUser();
        site = new SiteDataGen().nextPersisted();
    }

    /** Deletes the test users. */
    @AfterAll
    static void cleanUp() {
        usersToClean.forEach(user -> UserDataGen.remove(user, true));
    }

    /**
     * Method to test: {@link PublishFactory#publishAsset(Inode, User, boolean, boolean)}
     * Given Scenario: A back-end user who can read the site but holds no PUBLISH on the link
     * ExpectedResult: The call is refused and the link stays unpublished
     */
    @Test
    void test_publishAsset_link_userWithoutPublish_isRefused() throws Exception {
        final User user = backendUser();
        final Folder folder = new FolderDataGen().site(site).nextPersisted();
        final Link link = new LinkDataGen(folder).nextPersisted(false);

        assertThrows(DotSecurityException.class,
                () -> PublishFactory.publishAsset(link, user, false, false));

        assertFalse(isLive(link));
    }

    /**
     * Method to test: {@link PublishFactory#publishAsset(Inode, User, boolean, boolean)}
     * Given Scenario: A back-end user holding PUBLISH on the link's folder
     * ExpectedResult: The link goes live
     */
    @Test
    void test_publishAsset_link_userWithPublish_publishes() throws Exception {
        final User user = backendUser();
        final Folder folder = new FolderDataGen().site(site).nextPersisted();
        grant(folder, user, PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_PUBLISH);
        final Link link = new LinkDataGen(folder).nextPersisted(false);

        assertTrue(PublishFactory.publishAsset(link, user, false, false));

        assertTrue(isLive(link));
    }

    /**
     * Method to test: {@link PublishFactory#publishAsset(Inode, User, boolean, boolean)}
     * Given Scenario: No user at all
     * ExpectedResult: The call is refused and the link stays unpublished
     */
    @Test
    void test_publishAsset_link_noUser_isRefused() throws Exception {
        final Folder folder = new FolderDataGen().site(site).nextPersisted();
        final Link link = new LinkDataGen(folder).nextPersisted(false);

        assertThrows(DotSecurityException.class,
                () -> PublishFactory.publishAsset(link, null, false, false));

        assertFalse(isLive(link));
    }

    /**
     * Method to test: {@link PublishFactory#publishAsset(Inode, User, boolean, boolean)}
     * Given Scenario: A user holding PUBLISH on a template but not on the container in its layout
     * ExpectedResult: The template goes live and the container is left as it was
     */
    @Test
    void test_publishAsset_template_leavesContainerUserCannotPublish() throws Exception {
        final User user = backendUser();
        final Container container = new ContainerDataGen().site(site).nextPersisted();
        final Template template = unpublishedTemplateWith(container);
        grant(template, user, PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_PUBLISH);

        assertTrue(PublishFactory.publishAsset(template, user, false, false));

        assertTrue(isLive(template));
        assertFalse(isLive(container));
    }

    /**
     * Method to test:
     * {@link com.dotmarketing.portlets.templates.business.TemplateAPI#publishTemplate(Template, User, boolean)}
     * Given Scenario: A user holding PUBLISH on a template but not on the container in its layout
     * ExpectedResult: The template goes live and the container is left as it was
     */
    @Test
    void test_templateAPI_publishTemplate_leavesContainerUserCannotPublish() throws Exception {
        final User user = backendUser();
        final Container container = new ContainerDataGen().site(site).nextPersisted();
        final Template template = unpublishedTemplateWith(container);
        grant(template, user, PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_PUBLISH);

        APILocator.getTemplateAPI().publishTemplate(template, user, false);

        assertTrue(isLive(template));
        assertFalse(isLive(container));
    }

    /**
     * A drawed template whose layout holds the given unpublished container, with its own live
     * version removed so that publishing it is observable.
     */
    private static Template unpublishedTemplateWith(final Container container) throws Exception {
        final TemplateLayout layout = new TemplateLayoutDataGen().withContainer(container).next();
        final Template template = new TemplateDataGen().site(site).drawedBody(layout)
                .nextPersisted();
        APILocator.getVersionableAPI().removeLive(template.getIdentifier());
        assertFalse(isLive(template));
        assertFalse(isLive(container));
        assertTrue(APILocator.getTemplateAPI().getContainersInTemplate(template, systemUser, false)
                .stream().anyMatch(inTemplate ->
                        inTemplate.getIdentifier().equals(container.getIdentifier())));
        return template;
    }

    /**
     * A back-end user who can read the test site and whatever is created in it afterwards, and
     * holds nothing else there. Created before the test's folders and links so that they inherit
     * that READ.
     */
    private static User backendUser() throws DotDataException, DotSecurityException {
        final User user = new UserDataGen().roles(TestUserUtils.getBackendRole()).nextPersisted();
        usersToClean.add(user);
        grant(site, user, PermissionAPI.PERMISSION_READ);
        return user;
    }

    /**
     * Gives the user's own role the permission bits on the asset and, when the asset is a site or
     * a folder, on the folders and links created under it.
     */
    private static void grant(final Permissionable asset, final User user, final int permission)
            throws DotDataException, DotSecurityException {
        final Role role = APILocator.getRoleAPI().getUserRole(user);
        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        permissionAPI.save(new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                asset.getPermissionId(), role.getId(), permission, true), asset, systemUser, false);
        if (asset.isParentPermissionable()) {
            for (final Class<?> childType : List.of(Folder.class, Link.class)) {
                permissionAPI.save(new Permission(childType.getCanonicalName(),
                        asset.getPermissionId(), role.getId(), permission, true), asset,
                        systemUser, false);
            }
        }
    }

    private static boolean isLive(final Versionable asset) throws Exception {
        return APILocator.getVersionableAPI().isLive(asset);
    }
}
