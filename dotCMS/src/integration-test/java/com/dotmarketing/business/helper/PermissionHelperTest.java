package com.dotmarketing.business.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionAPI.PermissionableType;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotDataValidationException;
import com.liferay.portal.model.User;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class PermissionHelperTest {

    public static class TestCase{
        List<Integer> permissions;
        List<String> permissionableTypes;

        public TestCase(final List<Integer> permissions, final List<String> permissionableTypes){
            this.permissions = permissions;
            this.permissionableTypes = permissionableTypes;
        }
    }

    @DataProvider
    public static Object[] getTestCases() {
        return new TestCase[] {
                new TestCase(null, null),

                new TestCase(null, CollectionsUtils.list(
                        PermissionableType.HTMLPAGES.name(), PermissionableType.TEMPLATES.name())),

                new TestCase(CollectionsUtils.list(PermissionAPI.PERMISSION_READ), null),
                new TestCase(CollectionsUtils.list(PermissionAPI.PERMISSION_WRITE), null),
                new TestCase(CollectionsUtils.list(PermissionAPI.PERMISSION_READ, PermissionAPI.PERMISSION_WRITE), null),

                new TestCase(CollectionsUtils.list(PermissionAPI.PERMISSION_READ), CollectionsUtils.list(
                        PermissionableType.HTMLPAGES.name(), PermissionableType.TEMPLATES.name())),
                new TestCase(CollectionsUtils.list(PermissionAPI.PERMISSION_WRITE), CollectionsUtils.list(
                        PermissionableType.HTMLPAGES.name(), PermissionableType.TEMPLATES.name())),
                new TestCase(CollectionsUtils.list(PermissionAPI.PERMISSION_READ, PermissionAPI.PERMISSION_WRITE), CollectionsUtils.list(
                        PermissionableType.HTMLPAGES.name(), PermissionableType.TEMPLATES.name()))

        };
    }

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * <b>Method to test:</b> {@link PermissionHelper#getPermissionsByPermissionType(User, List, List)}<p></p>
     * <b>Test Case:</b> Gets permissions on each permissionable type for system user<p></p>
     * <b>Expected Results:</b> It should succeed
     * @param testCase
     * @throws DotDataException
     */
    @Test
    @UseDataProvider("getTestCases")
    public void test_getPermissionsByPermissionType_with_system_user_should_succeed(TestCase testCase)
            throws DotDataException {

        final Map<String, Map<String, Boolean>> result = PermissionHelper.getInstance()
                .getPermissionsByPermissionType(APILocator.systemUser(), testCase.permissions,
                        testCase.permissionableTypes);

        assertNotNull(result);

        final List<String> permissionsToValidate = testCase.permissionableTypes == null ? Arrays
                .asList(PermissionableType.values()).stream().map(value -> value.name())
                .collect(
                        Collectors.toList()) : testCase.permissionableTypes;

        assertEquals(result.size(), permissionsToValidate.size());

        for (final String type : permissionsToValidate) {

            if (testCase.permissions == null) {
                //it validates all permissions
                assertTrue(result.get(type).get("canRead"));
                assertTrue(result.get(type).get("canWrite"));
            } else {
                assertEquals(result.get(type).size(), testCase.permissions.size());
                //otherwise, it validates the permissions set
                testCase.permissions.forEach(perm -> {
                    if (perm == PermissionAPI.PERMISSION_READ) {
                        assertTrue(result.get(type).get("canRead"));
                    } else {
                        assertTrue(result.get(type).get("canWrite"));
                    }
                });
            }
        }
    }

    /**
     * <b>Method to test:</b> {@link PermissionHelper#getPermissionsByPermissionType(User, List, List)}<p></p>
     * <b>Test Case:</b> Gets permissions for each permissionable type given a limited user<p></p>
     * <b>Expected Results:</b> It should succeed
     * @param testCase
     * @throws DotDataException
     */
    @Test
    @UseDataProvider("getTestCases")
    public void test_getPermissionsByPermissionType_with_limited_user_should_succeed(TestCase testCase)
            throws DotDataException {

        final User limitedUser = new UserDataGen().roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).nextPersisted();

        final Map<String, Map<String, Boolean>> result = PermissionHelper.getInstance()
                .getPermissionsByPermissionType(limitedUser, testCase.permissions,
                        testCase.permissionableTypes);

        assertNotNull(result);

        final List<String> permissionsToValidate = testCase.permissionableTypes == null ? Arrays
                .asList(PermissionableType.values()).stream().map(value -> value.name())
                .collect(
                        Collectors.toList()) : testCase.permissionableTypes;

        assertEquals(result.size(), permissionsToValidate.size());

        for (final String type : permissionsToValidate) {

            if (testCase.permissions == null) {
                //it validates all permissions
                assertFalse(result.get(type).get("canRead"));
                assertFalse(result.get(type).get("canWrite"));
            } else {
                assertEquals(result.get(type).size(), testCase.permissions.size());
                //otherwise, it validates the permissions set
                testCase.permissions.forEach(perm -> {
                    if (perm == PermissionAPI.PERMISSION_READ) {
                        assertFalse(result.get(type).get("canRead"));
                    } else {
                        assertFalse(result.get(type).get("canWrite"));
                    }
                });
            }
        }
    }

    /**
     * <b>Method to test:</b> {@link PermissionHelper#getPermissionsByPermissionType(User, List, List)}<p></p>
     * <b>Test Case:</b> Try to get permissions for each permissionable type given an invalid user<p></p>
     * <b>Expected Results:</b> It should fail
     * @param testCase
     * @throws DotDataException
     */
    @Test(expected = DotDataValidationException.class)
    @UseDataProvider("getTestCases")
    public void test_getPermissionsByPermissionType_with_invalid_user_should_fail(TestCase testCase)
            throws DotDataException {
        PermissionHelper.getInstance()
                .getPermissionsByPermissionType(null, testCase.permissions, testCase.permissionableTypes);
    }
}
