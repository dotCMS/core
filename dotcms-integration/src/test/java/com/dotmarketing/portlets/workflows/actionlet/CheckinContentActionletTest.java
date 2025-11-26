package com.dotmarketing.portlets.workflows.actionlet;

import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotLockException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class CheckinContentActionletTest {

    private static class TestCase {
        Contentlet contentlet;
        User user;

        boolean hasWritePermission;

        public TestCase(Contentlet contentlet, User user, boolean hasWritePermission) {
            this.contentlet = contentlet;
            this.user = user;
            this.hasWritePermission = hasWritePermission;
        }
    }

    @DataProvider
    public static Object[] dataProviderSaveLanguage() throws DotSecurityException, DotDataException {
        final ContentType contentType = new ContentTypeDataGen()
                .fields(ImmutableList.of(ImmutableTextField.builder().name("Name").variable("name").build()))
                .nextPersisted();

        final Contentlet notLockContentlet = new ContentletDataGen(contentType.id())
                .setProperty("name", "testName")
                .nextPersisted();
        final User limitedUser = new UserDataGen().next();

        final Contentlet lockContentlet = new ContentletDataGen(contentType.id())
                .setProperty("name", "testName")
                .nextPersisted();

        final User userWithPermission = createUserWithPermission(lockContentlet);
        APILocator.getContentletAPI().lock(lockContentlet, userWithPermission, false);

        return new TestCase[]{
                new TestCase(notLockContentlet, limitedUser, false),
                new TestCase(lockContentlet, limitedUser, false),
                new TestCase(notLockContentlet, userWithPermission, true)
        };
    }

    private static User createUserWithPermission(final Contentlet lockContentlet) throws DotDataException {
        final Role backEndUserRole = APILocator.getRoleAPI().loadBackEndUserRole();
        final Role role = new RoleDataGen().nextPersisted();
        final User userWithPermission = new UserDataGen().roles(role, backEndUserRole).nextPersisted();
        addWritePermissionToContentlet(role, lockContentlet);
        return userWithPermission;
    }

    private static void addWritePermissionToContentlet(final Role role, final Contentlet contentlet) throws DotDataException {
        final Permission permission = getPermission(role, contentlet, PermissionLevel.EDIT.getType());

        try {
            APILocator.getPermissionAPI().save(permission, contentlet, APILocator.systemUser(), false);

        } catch (DotDataException | DotSecurityException e){
            throw new RuntimeException(e);
        }
    }

    private static Permission getPermission(
            final Role role,
            final Permissionable permissionable,
            final int permissionLevel) {

        final Permission permission = new Permission();
        permission.setInode(permissionable.getPermissionId());
        permission.setRoleId(role.getId());
        permission.setPermission(permissionLevel);
        return permission;
    }

    /**
     * Method to test: {@link CheckinContentActionlet#executeAction(WorkflowProcessor, Map)}
     * Given Scenario: User without WRITE permission over the {@link Contentlet} try to unlock a not lock contentlet
     * Expected Result: should do anything
     *
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    @UseDataProvider("dataProviderSaveLanguage")
    public void tryToUnlockContentlet (final TestCase testCase) throws DotSecurityException, DotDataException {

        final WorkflowProcessor workflowProcessor = mock(WorkflowProcessor.class);
        final Map<String, WorkflowActionClassParameter> params = new HashMap<>();

        final ContentletDependencies contentletDependencies = mock(ContentletDependencies.class);

        when(workflowProcessor.getContentlet()).thenReturn(testCase.contentlet);
        when(workflowProcessor.getUser()).thenReturn(testCase.user);
        when(workflowProcessor.getContentletDependencies()).thenReturn(contentletDependencies);
        when(contentletDependencies.isRespectAnonymousPermissions()).thenReturn(false);

        final CheckinContentActionlet checkinContentActionlet = new CheckinContentActionlet();

        try {
            final boolean locked = testCase.contentlet.isLocked();
            checkinContentActionlet.executeAction(workflowProcessor, params);
            assertTrue(testCase.hasWritePermission || !locked);

            final Optional<ContentletVersionInfo> info = APILocator.getVersionableAPI().
                    getContentletVersionInfo(testCase.contentlet.getIdentifier(), testCase.contentlet.getLanguageId());

            assertTrue(info.isPresent());
            assertNull(info.get().getLockedBy());
        } catch (WorkflowActionFailureException e) {
            if (ExceptionUtil.causedBy(e, DotLockException.class)) {
                assertTrue(!testCase.hasWritePermission && testCase.contentlet.isLocked());
            } else {
                throw e;
            }
        }
    }

}
