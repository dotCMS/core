package com.dotmarketing.portlets.workflows.actionlet;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.*;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.*;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.workflows.business.BaseWorkflowIntegrationTest;
import com.dotmarketing.portlets.workflows.business.SystemWorkflowConstants;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.tag.model.TagInode;
import com.liferay.portal.model.User;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.jetbrains.annotations.NotNull;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(DataProviderRunner.class)
public class SaveContentActionletTest extends BaseWorkflowIntegrationTest {


    private static User systemUser = APILocator.systemUser();
    private static List<ContentType> contentTypes = new ArrayList<>();

    private static class TestCase {
        private final boolean respectFrontendRoles;
        private final User user;
        private final boolean hasSaveActionPermission;
        private final boolean hasContentTypeAddChildrenPermission;
        private final ContentType contentType;

        private TestCase(
                final boolean respectFrontendRoles,
                final User user,
                final ContentType contentType,
                final boolean hasSaveActionPermission,
                final boolean hasContentTypeAddChildrenPermission) {
            this.respectFrontendRoles = respectFrontendRoles;
            this.user = user;
            this.hasSaveActionPermission = hasSaveActionPermission;
            this.hasContentTypeAddChildrenPermission = hasContentTypeAddChildrenPermission;
            this.contentType = contentType;
        }
    }

    @DataProvider
    public static Object[] usersAndContentTypeWithoutHostField() throws Exception {
        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());

        // creates the type to trigger the scheme
        final ContentType contentType = createTestType();
        contentTypes.add(contentType);

        // associated the scheme to the type
        final WorkflowScheme systemWorkflowScheme = workflowAPI.findSystemWorkflowScheme();
        workflowAPI.saveSchemesForStruct(new StructureTransformer(contentType).asStructure(),
                Collections.singletonList(systemWorkflowScheme));

        final User userWithPermission = createUserWithPermission(contentType);
        final User userWithoutPermission = new UserDataGen().nextPersisted();
        final User userWithJustActionPermission = createUserWithJustActionPermission();

        final ContentType frontendContentType = createTestType();
        contentTypes.add(frontendContentType);

        final User frontEndUserWithPermission = createFrontendUser(frontendContentType);
        final User frontendUserWithoutPermission = createFrontendUserWithoutPermission();

        return new TestCase[]{
                new TestCase(true, APILocator.systemUser(), contentType, true, true),
                new TestCase(false, APILocator.systemUser(), contentType, true, true),
                new TestCase(true, userWithPermission, contentType, true, true),
                new TestCase(false, userWithPermission, contentType, true, true),
                new TestCase(true, userWithoutPermission, contentType, false, false),
                new TestCase(false, userWithoutPermission, contentType, false, false),
                new TestCase(true, userWithJustActionPermission, contentType, true, false),
                new TestCase(false, userWithJustActionPermission, contentType, true, false),
                new TestCase(true, frontEndUserWithPermission, frontendContentType, true, true),
                new TestCase(false, frontEndUserWithPermission, frontendContentType, true, true),
                new TestCase(true, frontendUserWithoutPermission, frontendContentType, false, false),
                new TestCase(false, frontendUserWithoutPermission, frontendContentType, false, false)
        };
    }

    private static User createFrontendUser(final ContentType contentType) {

        try {
            final Role role = APILocator.getRoleAPI().loadFrontEndUserRole();
            final User user = new UserDataGen().roles(role).nextPersisted();

            addPermissionToAddChildren(role, contentType);
            addPermissionToSaveAction(role);

            return user;
        } catch (DotDataException e) {
            throw  new RuntimeException(e);
        }
    }

    private static User createFrontendUserWithoutPermission() {

        try {
            final Role role = APILocator.getRoleAPI().loadFrontEndUserRole();
            return new UserDataGen().roles(role).nextPersisted();
        } catch (DotDataException e) {
            throw  new RuntimeException(e);
        }
    }

    private static User createUserWithPermission(final ContentType contentType) throws DotDataException {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();

        addPermissionToAddChildren(role, contentType);
        addPermissionToSaveAction(role);
        return user;
    }

    private static User createUserWithJustActionPermission() throws DotDataException {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();

        addPermissionToSaveAction(role);
        return user;
    }

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment

        IntegrationTestInitService.getInstance().init();

        setDebugMode(false);
    }

    private static ContentType createTestType()
            throws DotDataException, DotSecurityException {

        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());

        final ContentType type = contentTypeAPI.save(
                ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                        .expireDateVar(null).folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                        .description("Dot..")
                        .name("DotSaveActionletTest" + System.currentTimeMillis())
                        .owner(APILocator.systemUser().toString())
                        .variable("DotSaveActionletTest" + System.currentTimeMillis()).build());

        final List<Field> fields = new ArrayList<>(type.fields());

        fields.add(FieldBuilder.builder(TextField.class).name("title").variable("title")
                .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());
        fields.add(FieldBuilder.builder(TextField.class).name("txt").variable("txt")
                .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());
        fields.add(FieldBuilder.builder(TagField.class).name("tag").variable("tag").required(true)
                .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());

        return contentTypeAPI.save(type, fields);
    }

    /**
     * Remove the content type and workflows created
     */
    @AfterClass
    public static void cleanup()
            throws DotDataException, DotSecurityException {
        for (final ContentType contentType : contentTypes) {
            if (null != contentType) {

                final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
                contentTypeAPI.delete(contentType);
            }
        }

        cleanupDebug(SaveContentActionletTest.class);
    } // cleanup

    /**
     * Method to test: {@link WorkflowAPI#fireContentWorkflow(Contentlet, ContentletDependencies)}
     * Given Scenario: Try to Save and publish a {@link Contentlet} with different users, when the ContentType does not
     * have a 'Site or folder' field
     * Expected Result: The follow are the expected result according to the user
     * - If the user is system should save and publish the contentlet
     * - If the user have {@link PermissionLevel#CAN_ADD_CHILDREN} over the ContentType
     * - If the user doesn't have any permission should throw a DotSecurityException when try to save it
     * - If the user just has {@link PermissionLevel#CAN_ADD_CHILDREN} over the save {@link ContentType} then should throw a DotSecurityException when try to save it
     */
    @Test
    @UseDataProvider("usersAndContentTypeWithoutHostField")
    public void test_Publish_With_Save_Contentlet (final TestCase testCase) throws DotSecurityException, DotDataException {
        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();

        final Contentlet contentlet = new ContentletDataGen(testCase.contentType.id())
            .setProperty("title", "Test")
            .setProperty("txt", "Test")
            .setProperty("tag", "test")
            .next();

        Contentlet contentletSaved = null;

        try {
            contentletSaved =
                    workflowAPI.fireContentWorkflow(contentlet,
                            new ContentletDependencies.Builder()
                                    .modUser(testCase.user)
                                    .respectAnonymousPermissions(testCase.respectFrontendRoles)
                                    .workflowActionId(SystemWorkflowConstants.WORKFLOW_SAVE_ACTION_ID)
                                    .build());

            checkContentSaved(contentletSaved);

            Assert.assertTrue(testCase.hasContentTypeAddChildrenPermission && testCase.hasSaveActionPermission);
        } catch(Exception e) {
            if (ExceptionUtil.causedBy(e, DotSecurityException.class)) {
                Assert.assertTrue(
                        !testCase.hasContentTypeAddChildrenPermission ||
                                !testCase.hasSaveActionPermission ||
                                (systemUser.isFrontendUser() && !testCase.respectFrontendRoles)
                );
                return;
            } else {
                throw e;
            }
        }

        final Contentlet contentletPublished =
                workflowAPI.fireContentWorkflow(contentletSaved,
                        new ContentletDependencies.Builder()
                                .modUser(APILocator.systemUser())
                                .workflowActionId(SystemWorkflowConstants.WORKFLOW_PUBLISH_ACTION_ID)
                                .respectAnonymousPermissions(testCase.respectFrontendRoles)
                                .build());

        checkPublishContent(contentletPublished);
    }

    private void checkPublishContent(final Contentlet contentletPublished) throws DotDataException, DotSecurityException {
        Assert.assertNotNull(contentletPublished);
        Assert.assertEquals("Test", contentletPublished.getStringProperty("title"));
        Assert.assertEquals("Test", contentletPublished.getStringProperty("txt"));
        contentletPublished.setTags();
        Assert.assertEquals("test", contentletPublished.getStringProperty("tag"));
        Assert.assertTrue(contentletPublished.isLive());
    }

    private void checkContentSaved(final Contentlet contentletSaved) throws DotDataException {
        Assert.assertNotNull(contentletSaved);
        Assert.assertEquals("Test", contentletSaved.getStringProperty("title"));
        Assert.assertEquals("Test", contentletSaved.getStringProperty("txt"));

        final List<TagInode> tagInodes = APILocator.getTagAPI().getTagInodesByInode(contentletSaved.getInode());
        Assert.assertNotNull(tagInodes);
        Assert.assertFalse(tagInodes.isEmpty());

        contentletSaved.setTags();
    }

    @NotNull
    private static void addPermissionToAddChildren(final Role role, final ContentType contentType) throws DotDataException {

        final Permission contentTypePermission = getPermission(role, contentType, PermissionLevel.WRITE.getType());

        try {

            APILocator.getPermissionAPI().save(contentTypePermission, contentType, systemUser, false);

        } catch (DotDataException | DotSecurityException e){
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private static void addPermissionToSaveAction(final Role role) throws DotDataException {

        final WorkflowAction action = FactoryLocator.getWorkFlowFactory().findAction(SystemWorkflowConstants.WORKFLOW_SAVE_ACTION_ID);
        final Permission actionPermission = getPermission(role, action, PermissionLevel.USE.getType());

        try {
             APILocator.getPermissionAPI().save(actionPermission, action, systemUser, false);

        } catch (DotDataException | DotSecurityException e){
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private static Permission getPermission(
            final Role role,
            final Permissionable permissionable,
            final int permissionPublish) {

        final Permission permission = new Permission();
        permission.setInode(permissionable.getPermissionId());
        permission.setRoleId(role.getId());
        permission.setPermission(permissionPublish);
        return permission;
    }

}
