package com.dotmarketing.portlets.workflows.actionlet;

import com.dotcms.LicenseTestUtil;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.CategoryDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.workflows.business.BaseWorkflowIntegrationTest;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for the {@link com.dotmarketing.portlets.workflows.actionlet.EmailActionlet} class
 */
public class EmailActionletTest extends BaseWorkflowIntegrationTest {

    public static final int WRITE_PERMISSION =
            PermissionAPI.PERMISSION_READ |
                PermissionAPI.PERMISSION_WRITE;
    public static final int ALL_PERMISSIONS =
            PermissionAPI.PERMISSION_READ |
                PermissionAPI.PERMISSION_WRITE |
                PermissionAPI.PERMISSION_PUBLISH |
                PermissionAPI.PERMISSION_EDIT_PERMISSIONS |
                PermissionAPI.PERMISSION_CAN_ADD_CHILDREN;

    @BeforeClass
    public static void prepare() throws Exception {

        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();

    }

    /**
     * Method to test: {@link WorkflowAPI#fireContentWorkflow(Contentlet, ContentletDependencies)}
     * The workflow action to be executed includes the {@link EmailActionlet}
     * actionlet, so it will execute the {@link EmailActionlet#executeAction(WorkflowProcessor, java.util.Map)} method.
     * Given Scenario: Execute a workflow action that includes the {@link EmailActionlet} actionlet.
     * The email body field will include velocity code that will be executed.
     * Expected Result: The email should be sent with the expected content. The velocity context
     * should be populated with the user that executed the action, so a call to the
     * $dotcontent.find() should return the content if the user has read permissions for it.
     */
    @Test
    public void testExecuteWorkflowAction() throws Exception {

        WorkflowScheme workflowScheme = null;
        Category rootCategory = null;
        Host site = null;
        ContentType contentType = null;
        Role testRole = null;
        User testUser = null;
        Contentlet testContent = null;
        try {
            // creates the test categories
            final Category childCategory1 = createTestCategory(
                    "EmailActionletTestChildCat1", 1).next();
            final Category childCategory2 = createTestCategory(
                    "EmailActionletTestChildCat2", 2).next();
            rootCategory = createTestCategory(
                    "EmailActionletTestRootCat", 0)
                    .children(childCategory1, childCategory2).nextPersisted();

            // creates the workflow scheme and actions
            final String schemeName = "EmailActionletTestScheme" + UUIDGenerator.generateUuid();
            final CreateSchemeStepActionResult schemeStepFirstActionResult =
                    createSchemeStepActionActionlet(
                            schemeName, "step1", "action1",
                            SaveContentAsDraftActionlet.class);
            workflowScheme = schemeStepFirstActionResult.getScheme();
            final WorkflowStep workflowStep = schemeStepFirstActionResult.getStep();
            final WorkflowAction saveDraftAction = schemeStepFirstActionResult.getAction();

            final CreateSchemeStepActionResult schemeStepEmailActionResult =
                    createActionActionlet(workflowScheme.getId(),
                            workflowStep.getId(), "action2",
                            EmailActionlet.class);

            saveEmailActionletBodyCode(schemeStepEmailActionResult);

            final String emailActionId = schemeStepEmailActionResult.getAction().getId();
            addActionletToAction(emailActionId, SaveContentActionlet.class, 1);

            // create test user
            final Pair<Role, User> userAndRole = createUserWithActionPermission(
                    saveDraftAction, schemeStepEmailActionResult.getAction());
            testRole = userAndRole.getLeft();
            testUser = userAndRole.getRight();
            addPermissionToCategory(rootCategory, testRole);
            addPermissionToCategory(childCategory1, testRole);
            addPermissionToCategory(childCategory2, testRole);


            // creates content type with categories
            site = createTestSite(testRole);
            contentType = createTestTypeWithCategories(
                    "EmailActionletTestType" + System.currentTimeMillis(),
                    workflowScheme.getId(),
                    rootCategory.getInode());
            addPermissionToAddChildren(testRole, contentType);

            HttpServletRequest workflowRequest = new FakeHttpRequest(
                    site.getHostname(), null).request();
            workflowRequest.setAttribute(WebKeys.USER, testUser);
            HttpServletRequestThreadLocal.INSTANCE.setRequest(workflowRequest);

            // create test content
            final String contentId = createTestContentletWithCategories(contentType,
                    new ArrayList<>(List.of(childCategory1, childCategory2)),
                    saveDraftAction.getId(), testUser, site).getIdentifier();
            testContent = APILocator.getContentletAPI()
                    .findContentletByIdentifierAnyLanguage(contentId);

            final boolean anonymousAccess = APILocator.getPermissionAPI().doesRoleHavePermission(
                    testContent, PermissionAPI.PERMISSION_READ,
                    APILocator.getRoleAPI().loadCMSAnonymousRole());
            assertFalse(anonymousAccess);

            // execute the workflow action
            APILocator.getWorkflowAPI().fireContentWorkflow(testContent,
                    new ContentletDependencies.Builder()
                            .modUser(testUser)
                            .respectAnonymousPermissions(false)
                            .workflowActionId(emailActionId)
                            .indexPolicy(IndexPolicy.FORCE)
                            .indexPolicyDependencies(IndexPolicy.FORCE)
                            .build());

            // Check that result body in contentlet property contains category names
            final Contentlet resultContent = APILocator.getContentletAPI()
                    .findContentletByIdentifierAnyLanguage(
                            testContent.getIdentifier());
            assertNotNull(resultContent);
            final Object resultObj = resultContent.get ("result");
            assertNotNull(resultObj);
            final Map<?, ?> resultCodeMap = (Map<?, ?>) resultObj;
            final Object outputCodeObj = resultCodeMap.get("output");
            assertNotNull(outputCodeObj);
            final String resultCode = outputCodeObj.toString();

            assertTrue(resultCode.contains(childCategory1.getCategoryName()));
            assertTrue(resultCode.contains(childCategory2.getCategoryName()));

        } finally {
            if (null != testContent) {
                ContentletDataGen.remove(testContent);
            }
            if (null != contentType) {
                ContentTypeDataGen.remove(contentType);
            }
            if (null != testUser) {
                UserDataGen.remove(testUser);
            }
            if (null != testRole) {
                RoleDataGen.remove(testRole);
            }
            if (null != site) {
                APILocator.getHostAPI().archive(site, APILocator.systemUser(), false);
                APILocator.getHostAPI().delete(site, APILocator.systemUser(), false);
            }
            if (null != rootCategory) {
                APILocator.getCategoryAPI().delete(
                        rootCategory, APILocator.systemUser(), false);
            }
            if (null != workflowScheme) {
                cleanScheme(workflowScheme);
            }
        }
    }

    /**
     * Saves the velocity code for the email actionlet body.
     *
     * @param schemeStepActionResult The result of creating the scheme step.
     * @throws Exception An error occurred when adding the velocity code for the email body.
     */
    private void saveEmailActionletBodyCode(
            final CreateSchemeStepActionResult schemeStepActionResult) throws Exception {

        final String code = FileUtil.getFileContentFromResourceContext(
            "com/dotmarketing/portlets/workflows/actionlet/list-categories.vtl");

        final WorkflowActionClass workflowActionClass = schemeStepActionResult.getActionClass();
        final List<WorkflowActionClassParameter> params = new ArrayList<>();
        final User user = APILocator.systemUser();

        final WorkflowActionClassParameter bodyParameter = new WorkflowActionClassParameter();
        bodyParameter.setActionClassId(workflowActionClass.getId());
        bodyParameter.setKey("emailBody");
        bodyParameter.setValue(code);
        params.add(bodyParameter);

        final WorkflowActionClassParameter toParameter = new WorkflowActionClassParameter();
        toParameter.setActionClassId(workflowActionClass.getId());
        toParameter.setKey("toEmail");
        toParameter.setValue("test-user@dotcms.com");
        params.add(toParameter);

        final WorkflowActionClassParameter fromParameter = new WorkflowActionClassParameter();
        fromParameter.setActionClassId(workflowActionClass.getId());
        fromParameter.setKey("fromEmail");
        fromParameter.setValue("support@dotcms.com");
        params.add(fromParameter);

        APILocator.getWorkflowAPI().saveWorkflowActionClassParameters(params, user);

    }

    /**
     * Creates a test category.
     * @param categoryNamePrefix The prefix for the category name.
     * @param sortOrder The sort order for the category.
     * @return The category data generator.
     */
    private CategoryDataGen createTestCategory(
            final String categoryNamePrefix, final int sortOrder) {
        final String categoryName = categoryNamePrefix + System.currentTimeMillis();
        final String categoryKey = categoryName.toLowerCase().replace("\\s", "");

        return new CategoryDataGen().setCategoryName(categoryName)
                .setKey(categoryKey).setCategoryVelocityVarName(categoryKey)
                .setSortOrder(sortOrder);

    }

    /**
     * Adds permissions to a category for a role.
     * @param category The category.
     * @param role   The role.
     * @throws Exception An error occurred trying to add permissions to the category.
     */
    private void addPermissionToCategory(
            final Category category, final Role role) throws Exception {

        APILocator.getPermissionAPI().removePermissions(category);

        final int permission = PermissionAPI.PERMISSION_USE |
                PermissionAPI.PERMISSION_WRITE;

        final Permission categoryPermission = new Permission(
                PermissionAPI.PermissionableType.CONTENTLETS.getCanonicalName(),
                category.getPermissionId(), role.getId(),
                permission, true);
        final Permission usePermission = new Permission(
                PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                category.getPermissionId(), role.getId(),
                permission, true);

        final List<Permission> permissions = new ArrayList<>();
        permissions.add(categoryPermission);
        permissions.add(usePermission);

        APILocator.getPermissionAPI().save(permissions,
                category, APILocator.systemUser(), false);

    }

    /**
     * Creates a test site with permissions assigned to the given role.
     * @param role The role to assign permissions to the site.
     * @return The created site.
     * @throws Exception An error occurred trying to create the site.
     */
    private Host createTestSite(final Role role) throws Exception {
        final Host site = new SiteDataGen()
                .name("EmailActionletTestSite" + System.currentTimeMillis())
                .nextPersisted();
        APILocator.getPermissionAPI().removePermissions(site);

        final List<Permission> permissions = getPermissions(role, site);

        APILocator.getPermissionAPI().save(permissions,
                site, APILocator.systemUser(), false);

        return site;
    }

    /**
     * Gets the permissions for a role and site.
     * @param role The role.
     * @param site The site.
     * @return The list of permissions.
     */
    private List<Permission> getPermissions(Role role, Host site) {
        final Permission sitePermission = new Permission(
                PermissionAPI.PermissionableType.CONTENTLETS.getCanonicalName(),
                site.getPermissionId(), role.getId(),
                WRITE_PERMISSION, true);
        final Permission contentPermission = new Permission(
                PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                site.getPermissionId(), role.getId(),
                WRITE_PERMISSION, true);

        final List<Permission> permissions = new ArrayList<>();
        permissions.add(sitePermission);
        permissions.add(contentPermission);
        return permissions;
    }

    /**
     * Creates a content type with categories and assigns it to a workflow scheme.
     *
     * @param contentTypeName  The name of the content type.
     * @param workflowSchemeId The ID of the workflow scheme.
     * @param categoryId       The ID of the root category.
     * @return The created content type.
     * @throws Exception An error occurred trying to create the content type.
     */
    private ContentType createTestTypeWithCategories(
            final String contentTypeName,
            final String workflowSchemeId, final String categoryId)
            throws Exception {

        // Create content type
        ContentType contentType = new ContentTypeDataGen()
                .name(contentTypeName)
                .baseContentType(BaseContentType.CONTENT)
                .workflowId(workflowSchemeId)
                .nextPersisted();

        // Add fields to the contentType
        final Field titleField =
                FieldBuilder.builder(TextField.class).name("Title")
                        .contentTypeId(contentType.id())
                        .variable("title")
                        .indexed(true)
                        .required(true)
                        .dataType(DataTypes.TEXT).build();

        final Field categoryField =
                FieldBuilder.builder(CategoryField.class).name("TestCategory")
                        .contentTypeId(contentType.id())
                        .variable("testCategory")
                        .indexed(true)
                        .required(true)
                        .values(categoryId).build();

        final Field resultField =
                FieldBuilder.builder(KeyValueField.class).name("Result")
                        .contentTypeId(contentType.id())
                        .variable("result")
                        .required(false)
                        .indexed(false)
                        .build();

        contentType = APILocator.getContentTypeAPI(APILocator.systemUser()).save(
                contentType, List.of(titleField, categoryField, resultField));

        return contentType;

    }

    /**
     * Creates a user with permissions to execute the given actions.
     * @param actions The actions to assign permissions to the user.
     * @return The created user and role.
     * @throws Exception An error occurred trying to create the user.
     */
    private Pair<Role, User> createUserWithActionPermission(
            final WorkflowAction ...actions) throws Exception {

        final Role role = new RoleDataGen().nextPersisted();
        final Role backendRole = APILocator.getRoleAPI().loadBackEndUserRole();
        final User user = new UserDataGen().roles(role, backendRole).nextPersisted();

        for (final WorkflowAction action : actions) {
            final Permission actionPermission = getExecuteActionPermission(role, action);
            APILocator.getPermissionAPI().save(actionPermission,
                    action, APILocator.systemUser(), false);
        }
        return Pair.of(role, user);

    }

    /**
     * Gets the permission to execute an action for a role.
     * @param role The role.
     * @param permissionable The permissionable object.
     * @return The permission.
     */
    private Permission getExecuteActionPermission(
            final Role role,
            final Permissionable permissionable) {

        final Permission permission = new Permission();
        permission.setType(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE);
        permission.setInode(permissionable.getPermissionId());
        permission.setRoleId(role.getId());
        permission.setPermission(PermissionAPI.PERMISSION_USE);
        permission.setBitPermission(true);
        return permission;
    }

    /**
     * Adds permissions to a content type to allow adding children.
     * @param role The role.
     * @param contentType The content type.
     * @throws Exception An error occurred trying to add permissions to the content type.
     */
    private void addPermissionToAddChildren(
            final Role role, final ContentType contentType) throws Exception {

        APILocator.getPermissionAPI().removePermissions(contentType);

        final List<Permission> permissions = getPermissions(role, contentType);

        APILocator.getPermissionAPI().save(permissions,
                contentType, APILocator.systemUser(), false);

    }

    /**
     * Gets the permissions for a role and content type.
     *
     * @param role        The role.
     * @param contentType The content type.
     * @return The list of permissions.
     */
    private List<Permission> getPermissions(Role role, ContentType contentType) {
        final Permission contentTypePermission = new Permission(
                PermissionAPI.PermissionableType.STRUCTURES.getCanonicalName(),
                contentType.getPermissionId(), role.getId(),
                ALL_PERMISSIONS, true);
        final Permission addChildrenPermission = new Permission(
                PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                contentType.getPermissionId(), role.getId(),
                ALL_PERMISSIONS, true);

        final List<Permission> permissions = new ArrayList<>();
        permissions.add(contentTypePermission);
        permissions.add(addChildrenPermission);
        return permissions;
    }

    /**
     * Creates a test contentlet with categories.
     * @param contentType The content type.
     * @param categoryList The list of categories.
     * @param actionId The ID of the action to be executed.
     * @return The created contentlet.
     * @throws Exception An error occurred trying to create the contentlet.
     */
    private Contentlet createTestContentletWithCategories(
            final ContentType contentType, final List<Category> categoryList,
            final String actionId, final User user, final Host site) throws Exception {

        final Contentlet testContent = new ContentletDataGen(contentType.id())
                .languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                .setProperty("title", "EmailActionletTestContent" + System.currentTimeMillis())
                .host(site)
                .next();

        return APILocator.getWorkflowAPI().fireContentWorkflow(testContent,
                new ContentletDependencies.Builder()
                        .modUser(user)
                        .respectAnonymousPermissions(false)
                        .workflowActionId(actionId)
                        .categories(categoryList)
                        .indexPolicy(IndexPolicy.FORCE)
                        .indexPolicyDependencies(IndexPolicy.FORCE)
                        .build());

    }

}
