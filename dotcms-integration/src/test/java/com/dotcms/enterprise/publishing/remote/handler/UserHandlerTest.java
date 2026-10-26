package com.dotcms.enterprise.publishing.remote.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.enterprise.PasswordFactoryProxy;
import com.dotcms.enterprise.PasswordFactoryProxy.AuthenticationStatus;
import com.dotcms.enterprise.publishing.remote.bundler.UserBundler;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.UserWrapper;
import com.dotcms.publisher.receiver.BundleOrigin;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockedStatic;

/**
 * Tests for {@link UserHandler}: what happens on the receiving instance to the users carried by a
 * bundle. A user that already exists on the receiver, matched by user ID or by email address, is
 * kept as it is, and a new user is created without the password that came in the bundle. The
 * same applies to every bundle, whether a person uploaded it, a registered sender pushed it, or
 * its origin is unknown.
 *
 * <p>Every user and role a test creates has a unique ID and is removed in {@link #cleanup()}.
 */
public class UserHandlerTest extends IntegrationTestBase {

    private static final String KNOWN_PASSWORD = "Bundle-Pa55word!";

    private static final List<BundleOrigin> ORIGINS =
            Arrays.asList(BundleOrigin.USER_UPLOAD, BundleOrigin.REGISTERED_SENDER, null);

    private final List<String> userIds = new ArrayList<>();
    private final List<String> roleIds = new ArrayList<>();
    private final List<File> toDelete = new ArrayList<>();

    @FunctionalInterface
    private interface Call {
        void run() throws Exception;
    }

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        // Removing a user hands its references over to the system user, which needs its own
        // role; a fresh test database may not have created it yet, and the role cache may still
        // hold entries from before the test data was loaded.
        CacheLocator.getCmsRoleCache().clearCache();
        LocalTransaction.wrap(() -> APILocator.getRoleAPI().getUserRole(APILocator.systemUser()));
        CacheLocator.getCmsRoleCache().clearCache();
        assertNotNull("the system user must have its own role",
                APILocator.getRoleAPI().loadRoleByKey(APILocator.systemUser().getUserId()));
    }

    @After
    public void cleanup() {
        CacheLocator.getUserCache().clearCache();
        for (final String userId : userIds) {
            final User user = findById(userId);
            if (user != null) {
                Try.run(() -> UserDataGen.remove(user, Boolean.TRUE));
            }
        }
        userIds.clear();
        for (final String roleId : roleIds) {
            Try.run(() -> {
                final Role role = APILocator.getRoleAPI().loadRoleById(roleId);
                if (role != null) {
                    APILocator.getRoleAPI().delete(role);
                }
            });
        }
        roleIds.clear();
        toDelete.forEach(FileUtils::deleteQuietly);
        toDelete.clear();
    }

    // ---------------------------------------------------------------- existing users

    /**
     * Method to Test: {@link UserHandler#handle(File)}
     * Given: a user that exists on the receiver, and a bundle that carries the same user ID with
     * different names, email address, password and active flag
     * When: the bundle is handled, for every bundle origin and both login types
     * Should: keep the stored user exactly as it was.
     */
    @Test
    public void test_handle_existingUserMatchedById_isKeptAsItIs() throws Exception {
        for (final String authType : Arrays.asList(Company.AUTH_TYPE_EA, Company.AUTH_TYPE_ID)) {
            for (final BundleOrigin origin : ORIGINS) {
                final User existing = existingUser();
                final User fromBundle = copyOf(existing);
                fromBundle.setFirstName("Changed");
                fromBundle.setLastName("By Bundle");
                fromBundle.setEmailAddress(uniqueEmail());
                fromBundle.setPassword(PasswordFactoryProxy.generateHash(KNOWN_PASSWORD));
                fromBundle.setActive(false);

                withLoginBy(authType, () -> handle(origin,
                        bundleWith(fromBundle, personalRoleFor(fromBundle), Operation.PUBLISH)));

                assertKeptAsItIs(existing, describe(authType, origin));
            }
        }
    }

    /**
     * Method to Test: {@link UserHandler#handle(File)}
     * Given: a user that exists on the receiver, and a bundle that carries a different user ID
     * with the same email address
     * When: the bundle is handled, for every bundle origin and both login types
     * Should: keep the stored user exactly as it was, and not create a user with the bundle's ID.
     */
    @Test
    public void test_handle_existingUserMatchedByEmailOnly_isKeptAsItIs() throws Exception {
        for (final String authType : Arrays.asList(Company.AUTH_TYPE_EA, Company.AUTH_TYPE_ID)) {
            for (final BundleOrigin origin : ORIGINS) {
                final User existing = existingUser();
                // A user's ID cannot be changed once set, so the bundle's user is built anew
                final User fromBundle = new UserDataGen()
                        .id(uniqueId())
                        .emailAddress(existing.getEmailAddress())
                        .firstName("Changed")
                        .lastName("By Bundle")
                        .password(PasswordFactoryProxy.generateHash(KNOWN_PASSWORD))
                        .next();
                assertNotEquals(existing.getUserId(), fromBundle.getUserId());
                userIds.add(fromBundle.getUserId());

                withLoginBy(authType, () -> Try.run(() -> handle(origin,
                        bundleWith(fromBundle, personalRoleFor(fromBundle), Operation.PUBLISH))));

                final String context = describe(authType, origin);
                assertKeptAsItIs(existing, context);
                assertNull(context + ": no user may be created with the bundle's ID",
                        findById(fromBundle.getUserId()));
            }
        }
    }

    /**
     * Method to Test: {@link UserHandler#handle(File)}
     * Given: a user that exists on the receiver, and a bundle that asks to remove that user
     * When: the bundle is handled, for every bundle origin
     * Should: keep the stored user exactly as it was.
     */
    @Test
    public void test_handle_removeRequestForExistingUser_keepsTheUser() throws Exception {
        for (final BundleOrigin origin : ORIGINS) {
            final User existing = existingUser();
            final User fromBundle = copyOf(existing);

            withLoginBy(Company.AUTH_TYPE_EA, () -> handle(origin,
                    bundleWith(fromBundle, personalRoleFor(fromBundle), Operation.UNPUBLISH)));

            assertKeptAsItIs(existing, describe(Company.AUTH_TYPE_EA, origin));
        }
    }

    // ---------------------------------------------------------------- new users

    /**
     * Method to Test: {@link UserHandler#handle(File)}
     * Given: a bundle that carries a user that does not exist on the receiver, with a password
     * hash for a known password
     * When: the bundle is handled, for every bundle origin
     * Should: create the user with the bundle's ID, email address and names, but with a password
     * that is neither the bundle's value nor accepts the known password.
     */
    @Test
    public void test_handle_newUser_isCreatedWithoutTheBundlePassword() throws Exception {
        for (final BundleOrigin origin : ORIGINS) {
            final User fromBundle = new UserDataGen()
                    .id(uniqueId())
                    .emailAddress(uniqueEmail())
                    .firstName("Pushed")
                    .lastName("User")
                    .password(PasswordFactoryProxy.generateHash(KNOWN_PASSWORD))
                    .next();
            userIds.add(fromBundle.getUserId());

            withLoginBy(Company.AUTH_TYPE_EA, () -> handle(origin,
                    bundleWith(fromBundle, personalRoleFor(fromBundle), Operation.PUBLISH)));

            final String context = describe(Company.AUTH_TYPE_EA, origin);
            final User created = findById(fromBundle.getUserId());
            assertNotNull(context + ": the new user must be created", created);
            assertEquals(context, fromBundle.getEmailAddress(), created.getEmailAddress());
            assertEquals(context, fromBundle.getFirstName(), created.getFirstName());
            assertEquals(context, fromBundle.getLastName(), created.getLastName());
            assertNotEquals(context + ": the stored password must not be the bundle's value",
                    fromBundle.getPassword(), created.getPassword());
            assertEquals(context + ": the known password must not be accepted",
                    AuthenticationStatus.NOT_AUTHENTICATED,
                    PasswordFactoryProxy.authPassword(KNOWN_PASSWORD, created.getPassword()));
        }
    }

    /**
     * Method to Test: {@link UserHandler#handle(File)}
     * Given: a bundle that carries a user that does not exist on the receiver, together with a
     * personal role whose name, description, key, parent and flags differ from the ones dotCMS
     * gives a user's own role
     * When: the bundle is handled, for every bundle origin
     * Should: give the new user its own role with the bundle role's ID, so references by ID from
     * other pushed assets keep working, and with every other attribute set the way dotCMS sets it
     * for any user.
     */
    @Test
    public void test_handle_newUser_getsStandardPersonalRoleKeepingTheBundleRoleId() throws Exception {
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        final Role usersRoot = roleAPI.loadRoleByKey(RoleAPI.USERS_ROOT_ROLE_KEY);
        final Role otherParent = roleAPI.loadCMSAdminRole();

        for (final BundleOrigin origin : ORIGINS) {
            final User fromBundle = new UserDataGen()
                    .id(uniqueId())
                    .emailAddress(uniqueEmail())
                    .firstName("Pushed")
                    .lastName("User")
                    .password(PasswordFactoryProxy.generateHash(KNOWN_PASSWORD))
                    .next();
            userIds.add(fromBundle.getUserId());

            final Role fromBundleRole = new Role();
            fromBundleRole.setId(UUIDGenerator.generateUuid());
            fromBundleRole.setName("Role From Bundle " + UUIDGenerator.shorty());
            fromBundleRole.setDescription("Description from bundle");
            fromBundleRole.setRoleKey(uniqueId());
            fromBundleRole.setParent(otherParent.getId());
            fromBundleRole.setDBFQN(otherParent.getId() + " --> " + fromBundleRole.getId());
            fromBundleRole.setEditPermissions(false);
            fromBundleRole.setEditUsers(true);
            fromBundleRole.setEditLayouts(false);
            fromBundleRole.setLocked(true);
            fromBundleRole.setSystem(false);
            roleIds.add(fromBundleRole.getId());

            withLoginBy(Company.AUTH_TYPE_EA, () -> handle(origin,
                    bundleWith(fromBundle, fromBundleRole, Operation.PUBLISH)));

            final String context = describe(Company.AUTH_TYPE_EA, origin);
            final User created = findById(fromBundle.getUserId());
            assertNotNull(context + ": the new user must be created", created);
            CacheLocator.getCmsRoleCache().clearCache();
            final Role personal = roleAPI.getUserRole(created);
            assertNotNull(context + ": the new user must have its own role", personal);
            assertEquals(context + ": the bundle role's ID must be kept",
                    fromBundleRole.getId(), personal.getId());
            assertEquals(context, created.getUserId(), personal.getRoleKey());
            assertEquals(context, created.getFullName(), personal.getName());
            assertNotEquals(context, fromBundleRole.getDescription(), personal.getDescription());
            assertEquals(context, usersRoot.getId(), personal.getParent());
            assertEquals(context, usersRoot.getId() + " --> " + personal.getId(),
                    personal.getDBFQN());
            assertEquals(context, true, personal.isEditPermissions());
            assertEquals(context, false, personal.isEditUsers());
            assertEquals(context, true, personal.isEditLayouts());
            assertEquals(context, false, personal.isLocked());
            assertEquals(context, true, personal.isSystem());
        }
    }

    // ---------------------------------------------------------------- helpers

    private User existingUser() {
        final User user = new UserDataGen()
                .id(uniqueId())
                .emailAddress(uniqueEmail())
                .firstName("Stored")
                .lastName("Receiver User")
                .nextPersisted();
        userIds.add(user.getUserId());
        CacheLocator.getUserCache().clearCache();
        return findById(user.getUserId());
    }

    private void assertKeptAsItIs(final User expected, final String context) {
        final User stored = findById(expected.getUserId());
        assertNotNull(context + ": the existing user must still exist", stored);
        assertEquals(context, expected.getFirstName(), stored.getFirstName());
        assertEquals(context, expected.getLastName(), stored.getLastName());
        assertEquals(context, expected.getEmailAddress(), stored.getEmailAddress());
        assertEquals(context, expected.getPassword(), stored.getPassword());
        assertEquals(context, expected.isActive(), stored.isActive());
    }

    private static User findById(final String userId) {
        CacheLocator.getUserCache().clearCache();
        return Try.of(() -> APILocator.getUserAPI().loadUserById(userId)).getOrNull();
    }

    /**
     * Returns a detached copy of the given user, the way a sender would serialize it, so changes
     * to the copy never reach the cached instance.
     */
    private static User copyOf(final User user) throws Exception {
        final File file = Files.createTempFile("user-copy", ".xml").toFile();
        try {
            BundlerUtil.objectToXML(user, file);
            return (User) BundlerUtil.xmlToObject(file);
        } finally {
            FileUtils.deleteQuietly(file);
        }
    }

    /**
     * Builds the personal role a sender includes next to a user, with the same shape dotCMS gives
     * a user's own role.
     */
    private Role personalRoleFor(final User user) throws Exception {
        final Role parent = APILocator.getRoleAPI().loadRoleByKey(RoleAPI.USERS_ROOT_ROLE_KEY);
        final Role role = new Role();
        role.setId(UUIDGenerator.generateUuid());
        role.setName(user.getFullName());
        role.setDescription("");
        role.setRoleKey(user.getUserId());
        role.setParent(parent.getId());
        role.setDBFQN(parent.getId() + " --> " + role.getId());
        role.setEditPermissions(true);
        role.setEditUsers(false);
        role.setEditLayouts(true);
        role.setLocked(false);
        role.setSystem(true);
        roleIds.add(role.getId());
        return role;
    }

    private File bundleWith(final User user, final Role role, final Operation operation)
            throws Exception {
        final File bundleFolder = Files.createTempDirectory("bundle-folder").toFile();
        toDelete.add(bundleFolder);
        final File usersFolder = new File(bundleFolder, UserBundler.FOLDER_USERS);
        usersFolder.mkdirs();
        final UserWrapper wrapper = new UserWrapper(user, role);
        wrapper.setOperation(operation);
        BundlerUtil.objectToXML(wrapper,
                new File(usersFolder, user.getUserId() + UserBundler.WRAPPER_DESCRIPTOR_EXTENSION));
        return bundleFolder;
    }

    private static void handle(final BundleOrigin origin, final File bundleFolder) throws Exception {
        final PublisherConfig config = new PushPublisherConfig();
        config.setId("zz-test-" + UUIDGenerator.shorty() + ".tar.gz");
        if (origin != null) {
            config.put(PublisherConfig.Config.BUNDLE_ORIGIN.name(), origin.name());
        }
        if (origin == BundleOrigin.USER_UPLOAD) {
            config.put(PublisherConfig.Config.UPLOADED_BY.name(),
                    APILocator.systemUser().getUserId());
        }
        new UserHandler(config).handle(bundleFolder);
    }

    /**
     * Runs the call with the default company set to log users in by the given type, so the tests
     * do not depend on how the test instance is configured.
     */
    private static void withLoginBy(final String authType, final Call call) throws Exception {
        final Company company = spy(PublicCompanyFactory.getDefaultCompany());
        doReturn(authType).when(company).getAuthType();
        try (MockedStatic<PublicCompanyFactory> factory =
                Mockito.mockStatic(PublicCompanyFactory.class, Mockito.CALLS_REAL_METHODS)) {
            factory.when(PublicCompanyFactory::getDefaultCompany).thenReturn(company);
            call.run();
        }
    }

    private static String describe(final String authType, final BundleOrigin origin) {
        return "login by " + authType + ", origin " + origin;
    }

    private static String uniqueId() {
        return "zz-test-" + UUIDGenerator.shorty();
    }

    private static String uniqueEmail() {
        return "zz-test-" + UUIDGenerator.shorty() + "@example.com";
    }

}
