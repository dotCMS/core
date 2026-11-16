package com.dotmarketing.tag.business;

import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the users, content and tags the tag permission tests need, and removes them all in
 * {@link #cleanUp()}. Each test class keeps one instance and calls {@code cleanUp()} from its
 * {@code @AfterClass}.
 */
public class TagTestFixtures {

    private final List<User> users = new ArrayList<>();
    private final List<Role> roles = new ArrayList<>();
    private final List<Layout> layouts = new ArrayList<>();
    private final List<String> tagIds = new ArrayList<>();
    private final List<Contentlet> contentlets = new ArrayList<>();

    /**
     * Creates a back-end user that is not an admin and holds the given portlets (none when no id
     * is passed).
     *
     * @param portletIds ids of the portlets the user can access
     * @return the persisted {@link User}
     */
    public User backEndUser(final String... portletIds) throws Exception {
        final RoleDataGen roleDataGen = new RoleDataGen();
        if (portletIds.length > 0) {
            final Layout layout = new LayoutDataGen().name("tags-test-" + UUIDGenerator.shorty())
                    .portletIds(portletIds).nextPersisted();
            layouts.add(layout);
            roleDataGen.layout(layout);
        }
        final Role role = roleDataGen.nextPersisted();
        roles.add(role);
        final User user = new UserDataGen().roles(role, TestUserUtils.getBackendRole()).nextPersisted();
        users.add(user);
        return user;
    }

    /**
     * Creates a contentlet and gives the user the given permission on it (no permission when
     * {@code permission} is 0).
     *
     * @param user       the {@link User} that gets the permission
     * @param permission a {@code PermissionAPI.PERMISSION_*} value, or 0
     * @return the persisted {@link Contentlet}
     */
    public Contentlet contentWithPermission(final User user, final int permission) throws Exception {
        final Contentlet contentlet = TestDataUtils.getGenericContentContent(true,
                APILocator.getLanguageAPI().getDefaultLanguage().getId());
        contentlets.add(contentlet);
        if (permission > 0) {
            final Role userRole = APILocator.getRoleAPI().getUserRole(user);
            APILocator.getPermissionAPI().save(
                    new Permission(contentlet.getPermissionId(), userRole.getId(), permission),
                    contentlet, APILocator.systemUser(), false);
        }
        return contentlet;
    }

    /**
     * Creates a tag on the system host and keeps it for clean-up.
     *
     * @param prefix start of the tag name; a random suffix is added
     * @return the persisted {@link Tag}
     */
    public Tag tag(final String prefix) throws Exception {
        final Tag tag = APILocator.getTagAPI().getTagAndCreate(prefix + "-" + UUIDGenerator.shorty(),
                APILocator.systemUser().getUserId(), Host.SYSTEM_HOST);
        tagIds.add(tag.getTagId());
        return tag;
    }

    /**
     * Keeps a tag created by the code under test, so it is removed in {@link #cleanUp()}.
     *
     * @param tag the {@link Tag} to remove later; ignored when {@code null}
     */
    public void track(final Tag tag) {
        if (tag != null && UtilMethods.isSet(tag.getTagId())) {
            tagIds.add(tag.getTagId());
        }
    }

    /**
     * Removes every tag, contentlet, user, role and layout created through this instance. Each
     * removal is independent, so one failure doesn't leave the rest behind.
     */
    public void cleanUp() {
        tagIds.forEach(tagId -> quietly(() -> APILocator.getTagAPI().deleteTag(tagId)));
        contentlets.forEach(contentlet -> quietly(() -> ContentletDataGen.remove(contentlet)));
        users.forEach(user -> quietly(() -> UserDataGen.remove(user)));
        roles.forEach(role -> quietly(() -> RoleDataGen.remove(role)));
        final LayoutAPI layoutAPI = APILocator.getLayoutAPI();
        layouts.forEach(layout -> quietly(() -> {
            // Delete the copy the Hibernate session already holds; a layout that is gone comes
            // back as an empty Layout, not null
            final Layout attached = layoutAPI.findLayout(layout.getId());
            if (attached != null && UtilMethods.isSet(attached.getId())) {
                layoutAPI.removeLayout(attached);
            }
        }));
        tagIds.clear();
        contentlets.clear();
        users.clear();
        roles.clear();
        layouts.clear();
        // the deletions above cleared the role cache; load the back-end role again so the next
        // class doesn't start against a cold cache
        quietly(() -> APILocator.getRoleAPI().loadBackEndUserRole());
    }

    /**
     * A clean-up step that may throw.
     */
    @FunctionalInterface
    private interface CleanUpStep {
        void run() throws Exception;
    }

    /**
     * Runs a clean-up step and logs any failure instead of stopping the rest of the clean-up.
     *
     * @param step the step to run
     */
    private static void quietly(final CleanUpStep step) {
        try {
            step.run();
        } catch (final Exception e) {
            Logger.warn(TagTestFixtures.class, "Could not remove a test fixture: " + e.getMessage());
        }
    }
}
