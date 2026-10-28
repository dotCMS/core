package com.dotcms.enterprise.publishing.remote.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.RoleDataGen;
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

                withLoginBy(authType, () -> handle(origin,
                        bundleWith(fromBundle, personalRoleFor(fromBundle), Operation.PUBLISH)));

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

    /**
     * Method to Test: {@link UserHandler#handle(File)}
     * Given: a user that exists on the receiver, and a bundle that asks to remove a user with a
     * different user ID and the same email address
     * When: the bundle is handled, for every bundle origin
     * Should: keep the stored user exactly as it was, and not create a user with the bundle's ID.
     */
    @Test
    public void test_handle_removeRequestMatchedByEmailOnly_keepsTheUser() throws Exception {
        for (final BundleOrigin origin : ORIGINS) {
            final User existing = existingUser();
            final User fromBundle = new UserDataGen()
                    .id(uniqueId())
                    .emailAddress(existing.getEmailAddress())
                    .next();
            userIds.add(fromBundle.getUserId());

            withLoginBy(Company.AUTH_TYPE_EA, () -> handle(origin,
                    bundleWith(fromBundle, personalRoleFor(fromBundle), Operation.UNPUBLISH)));

            final String context = describe(Company.AUTH_TYPE_EA, origin);
            assertKeptAsItIs(existing, context);
            assertNull(context + ": no user may be created with the bundle's ID",
                    findById(fromBundle.getUserId()));
        }
    }

    /**
     * Method to Test: {@link UserHandler#handle(File)}
     * Given: a bundle that asks to remove a user that does not exist on the receiver
     * When: the bundle is handled, for every bundle origin
     * Should: complete without creating the user or its role.
     */
    @Test
    public void test_handle_removeRequestForUnknownUser_createsNothing() throws Exception {
        for (final BundleOrigin origin : ORIGINS) {
            final User fromBundle = newUserFromBundle();
            final Role fromBundleRole = personalRoleFor(fromBundle);

            withLoginBy(Company.AUTH_TYPE_EA, () -> handle(origin,
                    bundleWith(fromBundle, fromBundleRole, Operation.UNPUBLISH)));

            final String context = describe(Company.AUTH_TYPE_EA, origin);
            assertNull(context + ": the user must not be created",
                    findById(fromBundle.getUserId()));
            CacheLocator.getCmsRoleCache().clearCache();
            assertNull(context + ": the user's role must not be created",
                    APILocator.getRoleAPI().loadRoleById(fromBundleRole.getId()));
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

    /**
     * Method to Test: {@link UserHandler#handle(File)}
     * Given: a role that exists on the receiver, and a bundle that carries a new user whose ID is
     * that role's key
     * When: the bundle is handled, for every bundle origin
     * Should: not create the user, and keep the existing role with its name and without the
     * user, since a user's own role is found by the user's ID.
     */
    @Test
    public void test_handle_newUserWithIdOfAnExistingRoleKey_isNotCreated() throws Exception {
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        for (final BundleOrigin origin : ORIGINS) {
            final String sharedKey = uniqueId();
            final Role existingRole = new RoleDataGen()
                    .name("Existing Role " + UUIDGenerator.shorty())
                    .key(sharedKey)
                    .nextPersisted();
            roleIds.add(existingRole.getId());

            final User fromBundle = new UserDataGen()
                    .id(sharedKey)
                    .emailAddress(uniqueEmail())
                    .firstName("Pushed")
                    .lastName("User")
                    .next();
            userIds.add(fromBundle.getUserId());

            withLoginBy(Company.AUTH_TYPE_EA, () -> handle(origin,
                    bundleWith(fromBundle, personalRoleFor(fromBundle), Operation.PUBLISH)));

            final String context = describe(Company.AUTH_TYPE_EA, origin);
            assertNull(context + ": no user may be created with the ID of an existing role key",
                    findById(fromBundle.getUserId()));
            CacheLocator.getCmsRoleCache().clearCache();
            final Role stored = roleAPI.loadRoleById(existingRole.getId());
            assertNotNull(context + ": the existing role must still exist", stored);
            assertEquals(context + ": the existing role must keep its name",
                    existingRole.getName(), stored.getName());
            assertEquals(context, sharedKey, stored.getRoleKey());
        }
    }

    /**
     * Method to Test: {@link UserHandler#handle(File)}
     * Given: a role that exists on the receiver, and a bundle that carries a new user whose ID is
     * that role's key written in upper case or with surrounding spaces
     * When: the bundle is handled, for every bundle origin
     * Should: not create the user, since dotCMS stores user IDs trimmed and in lower case, and keep
     * the existing role with its name and key.
     */
    @Test
    public void test_handle_newUserWithIdOfAnExistingRoleKeyInAnotherForm_isNotCreated()
            throws Exception {
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        for (final BundleOrigin origin : ORIGINS) {
            final String sharedKey = uniqueId();
            final Role existingRole = new RoleDataGen()
                    .name("Existing Role " + UUIDGenerator.shorty())
                    .key(sharedKey)
                    .nextPersisted();
            roleIds.add(existingRole.getId());
            userIds.add(sharedKey);

            for (final String bundleUserId : Arrays.asList(sharedKey.toUpperCase(),
                    " " + sharedKey + " ")) {
                final User fromBundle = new UserDataGen()
                        .id(bundleUserId)
                        .emailAddress(uniqueEmail())
                        .firstName("Pushed")
                        .lastName("User")
                        .next();

                withLoginBy(Company.AUTH_TYPE_EA, () -> handle(origin,
                        bundleWith(fromBundle, personalRoleFor(fromBundle), Operation.PUBLISH)));

                final String context = describe(Company.AUTH_TYPE_EA, origin)
                        + ", bundle user ID '" + bundleUserId + "'";
                assertNull(context + ": no user may be stored under the key of an existing role",
                        findById(sharedKey));
                CacheLocator.getCmsRoleCache().clearCache();
                final Role stored = roleAPI.loadRoleById(existingRole.getId());
                assertNotNull(context + ": the existing role must still exist", stored);
                assertEquals(context + ": the existing role must keep its name",
                        existingRole.getName(), stored.getName());
                assertEquals(context, sharedKey, stored.getRoleKey());
            }
        }
    }

    /**
     * Method to Test: {@link UserHandler#handle(File)}
     * Given: a bundle that carries a new user without its own role, or with a role that has no ID
     * When: the bundle is handled, for every bundle origin
     * Should: create the user with the own role dotCMS gives any user.
     */
    @Test
    public void test_handle_newUserWithoutUsableRole_getsStandardPersonalRole() throws Exception {
        final Role roleWithoutId = new Role();
        roleWithoutId.setName("Role From Bundle " + UUIDGenerator.shorty());
        for (final Role fromBundleRole : Arrays.asList(null, roleWithoutId)) {
            for (final BundleOrigin origin : ORIGINS) {
                final User fromBundle = newUserFromBundle();

                withLoginBy(Company.AUTH_TYPE_EA, () -> handle(origin,
                        bundleWith(fromBundle, fromBundleRole, Operation.PUBLISH)));

                final String context = describe(Company.AUTH_TYPE_EA, origin)
                        + ", bundle role " + (fromBundleRole == null ? "missing" : "without ID");
                final User created = findById(fromBundle.getUserId());
                assertNotNull(context + ": the new user must be created", created);
                assertStandardPersonalRole(created, context);
            }
        }
    }

    /**
     * Method to Test: {@link UserHandler#handle(File)}
     * Given: a role that exists on the receiver, and a bundle that carries a new user whose own
     * role has that role's ID
     * When: the bundle is handled, for every bundle origin
     * Should: create the user with a standard own role under a different ID, and keep the
     * existing role with its name and key and without the new user.
     */
    @Test
    public void test_handle_newUserWithRoleIdOfAnExistingRole_keepsTheExistingRole() throws Exception {
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        for (final BundleOrigin origin : ORIGINS) {
            final Role existingRole = new RoleDataGen()
                    .name("Existing Role " + UUIDGenerator.shorty())
                    .key(uniqueId())
                    .nextPersisted();
            roleIds.add(existingRole.getId());

            final User fromBundle = newUserFromBundle();
            final Role fromBundleRole = personalRoleFor(fromBundle);
            fromBundleRole.setId(existingRole.getId());

            withLoginBy(Company.AUTH_TYPE_EA, () -> handle(origin,
                    bundleWith(fromBundle, fromBundleRole, Operation.PUBLISH)));

            final String context = describe(Company.AUTH_TYPE_EA, origin);
            final User created = findById(fromBundle.getUserId());
            assertNotNull(context + ": the new user must be created", created);
            final Role personal = assertStandardPersonalRole(created, context);
            assertNotEquals(context + ": the user's own role must not take the existing role's ID",
                    existingRole.getId(), personal.getId());
            final Role stored = roleAPI.loadRoleById(existingRole.getId());
            assertNotNull(context + ": the existing role must still exist", stored);
            assertEquals(context, existingRole.getName(), stored.getName());
            assertEquals(context, existingRole.getRoleKey(), stored.getRoleKey());
            assertEquals(context + ": the new user must not get the existing role", false,
                    roleAPI.doesUserHaveRole(created, stored));
        }
    }

    /**
     * Method to Test: {@link UserHandler#handle(File)}
     * Given: a user that exists on the receiver, and a bundle that carries that same user together
     * with a personal role that does not exist on the receiver
     * When: the bundle is handled, for every bundle origin
     * Should: keep the stored user as it was, and not create the role carried by the bundle.
     */
    @Test
    public void test_handle_existingUserMatchedById_doesNotCreateTheBundleRole() throws Exception {
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        for (final BundleOrigin origin : ORIGINS) {
            final User existing = existingUser();
            final User fromBundle = copyOf(existing);
            final Role fromBundleRole = personalRoleFor(fromBundle);

            withLoginBy(Company.AUTH_TYPE_EA, () -> handle(origin,
                    bundleWith(fromBundle, fromBundleRole, Operation.PUBLISH)));

            final String context = describe(Company.AUTH_TYPE_EA, origin);
            assertKeptAsItIs(existing, context);
            CacheLocator.getCmsRoleCache().clearCache();
            assertNull(context + ": the role carried by the bundle must not be created",
                    roleAPI.loadRoleById(fromBundleRole.getId()));
        }
    }

    /**
     * Method to Test: {@link UserHandler#handle(File)}
     * Given: a company that logs users in by user ID, and a bundle that carries a user that does
     * not exist on the receiver
     * When: the bundle is handled, for every bundle origin
     * Should: create the user with the bundle's ID, email address and names.
     */
    @Test
    public void test_handle_newUser_isCreatedWithUserIdLogin() throws Exception {
        for (final BundleOrigin origin : ORIGINS) {
            final User fromBundle = newUserFromBundle();

            withLoginBy(Company.AUTH_TYPE_ID, () -> handle(origin,
                    bundleWith(fromBundle, personalRoleFor(fromBundle), Operation.PUBLISH)));

            final String context = describe(Company.AUTH_TYPE_ID, origin);
            final User created = findById(fromBundle.getUserId());
            assertNotNull(context + ": the new user must be created", created);
            assertEquals(context, fromBundle.getEmailAddress(), created.getEmailAddress());
            assertEquals(context, fromBundle.getFirstName(), created.getFirstName());
            assertEquals(context, fromBundle.getLastName(), created.getLastName());
        }
    }

    /**
     * Method to Test: {@link UserHandler#handle(File)}
     * Given: a single bundle that carries two users that do not exist on the receiver, both with
     * the same password hash
     * When: the bundle is handled, for every bundle origin
     * Should: create both users, each with a different stored password, and neither the bundle's.
     */
    @Test
    public void test_handle_twoNewUsersInOneBundle_getDifferentPasswords() throws Exception {
        for (final BundleOrigin origin : ORIGINS) {
            final User first = newUserFromBundle();
            final User second = newUserFromBundle();

            withLoginBy(Company.AUTH_TYPE_EA, () -> handle(origin, bundleWith(Arrays.asList(
                    wrapperFor(first, Operation.PUBLISH), wrapperFor(second, Operation.PUBLISH)))));

            final String context = describe(Company.AUTH_TYPE_EA, origin);
            final User createdFirst = findById(first.getUserId());
            final User createdSecond = findById(second.getUserId());
            assertNotNull(context + ": the first user must be created", createdFirst);
            assertNotNull(context + ": the second user must be created", createdSecond);
            assertNotEquals(context + ": the two users must not share a stored password",
                    createdFirst.getPassword(), createdSecond.getPassword());
            assertNotEquals(context + ": the stored password must not be the bundle's value",
                    first.getPassword(), createdFirst.getPassword());
        }
    }

    /**
     * Method to Test: {@link UserHandler#handle(File)}
     * Given: a single bundle that carries an existing user, a new user, and a remove request for a
     * user that does not exist on the receiver
     * When: the bundle is handled, for every bundle origin
     * Should: keep the existing user, create the new one, and create nothing for the remove request.
     */
    @Test
    public void test_handle_bundleWithSeveralUsers_handlesEachOne() throws Exception {
        for (final BundleOrigin origin : ORIGINS) {
            final User existing = existingUser();
            final User existingInBundle = copyOf(existing);
            existingInBundle.setFirstName("Changed");
            final User newUser = newUserFromBundle();
            final User unknownToRemove = newUserFromBundle();

            withLoginBy(Company.AUTH_TYPE_EA, () -> handle(origin, bundleWith(Arrays.asList(
                    wrapperFor(existingInBundle, Operation.PUBLISH),
                    wrapperFor(newUser, Operation.PUBLISH),
                    wrapperFor(unknownToRemove, Operation.UNPUBLISH)))));

            final String context = describe(Company.AUTH_TYPE_EA, origin);
            assertKeptAsItIs(existing, context);
            assertNotNull(context + ": the new user must be created",
                    findById(newUser.getUserId()));
            assertNull(context + ": the remove request for an unknown user must create nothing",
                    findById(unknownToRemove.getUserId()));
        }
    }

    // ---------------------------------------------------------------- helpers

    /**
     * Builds a user that does not exist on the receiver, with a password hash for a known
     * password, and registers it for cleanup.
     */
    private User newUserFromBundle() throws Exception {
        final User user = new UserDataGen()
                .id(uniqueId())
                .emailAddress(uniqueEmail())
                .firstName("Pushed")
                .lastName("User")
                .password(PasswordFactoryProxy.generateHash(KNOWN_PASSWORD))
                .next();
        userIds.add(user.getUserId());
        return user;
    }

    /**
     * Checks that the user has its own role with the key, name, parent and flags dotCMS gives
     * any user, and returns that role.
     */
    private static Role assertStandardPersonalRole(final User user, final String context)
            throws Exception {
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        CacheLocator.getCmsRoleCache().clearCache();
        final Role personal = roleAPI.loadRoleByKey(user.getUserId());
        assertNotNull(context + ": the user must have its own role", personal);
        final Role usersRoot = roleAPI.loadRoleByKey(RoleAPI.USERS_ROOT_ROLE_KEY);
        assertEquals(context, user.getFullName(), personal.getName());
        assertEquals(context, usersRoot.getId(), personal.getParent());
        assertEquals(context, true, personal.isSystem());
        assertEquals(context, false, personal.isEditUsers());
        assertEquals(context + ": the user must be in its own role", true,
                roleAPI.doesUserHaveRole(user, personal));
        return personal;
    }

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

    /**
     * Builds a bundle folder holding several user files, one per wrapper, the way a bundle with
     * more than one user arrives.
     */
    private File bundleWith(final List<UserWrapper> wrappers) throws Exception {
        final File bundleFolder = Files.createTempDirectory("bundle-folder").toFile();
        toDelete.add(bundleFolder);
        final File usersFolder = new File(bundleFolder, UserBundler.FOLDER_USERS);
        usersFolder.mkdirs();
        for (final UserWrapper wrapper : wrappers) {
            BundlerUtil.objectToXML(wrapper, new File(usersFolder,
                    wrapper.getUser().getUserId() + UserBundler.WRAPPER_DESCRIPTOR_EXTENSION));
        }
        return bundleFolder;
    }

    /**
     * Wraps a user with its personal role and the given operation, for a multi-user bundle.
     */
    private UserWrapper wrapperFor(final User user, final Operation operation) throws Exception {
        final UserWrapper wrapper = new UserWrapper(user, personalRoleFor(user));
        wrapper.setOperation(operation);
        return wrapper;
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
