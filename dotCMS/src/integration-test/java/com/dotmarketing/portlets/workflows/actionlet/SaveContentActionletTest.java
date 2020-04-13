package com.dotmarketing.portlets.workflows.actionlet;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.*;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.*;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
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

import static com.dotcms.util.CollectionsUtils.list;

@RunWith(DataProviderRunner.class)
public class SaveContentActionletTest extends BaseWorkflowIntegrationTest {

    private static WorkflowAPI workflowAPI = null;
    private static ContentTypeAPI contentTypeAPI = null;
    private static ContentType customContentType = null;
    private static User systemUser = APILocator.systemUser();

    private static class TestCase {
        final private boolean respectFrontendRoles;
        final private User user;
        final private boolean shouldThrowException;

        private TestCase(final boolean respectFrontendRoles, final User user, final boolean shouldThrowException) {
            this.respectFrontendRoles = respectFrontendRoles;
            this.user = user;
            this.shouldThrowException = shouldThrowException;
        }
    }

    @DataProvider
    public static Object[] users() throws Exception {
        prepare();
        final User userWithPermission = createUserWithPermission();
        final User userWithoutPermission = new UserDataGen().nextPersisted();

        final List<User> users = list(/*APILocator.systemUser(),*/ userWithPermission);
        final List<TestCase> testCases = new ArrayList<>();

        for (final User user : users) {
            testCases.add(new TestCase(true, user, false));
            testCases.add(new TestCase(false, user, false));
        }

        //testCases.add(new TestCase(true, userWithoutPermission, true));
        //testCases.add(new TestCase(false, userWithoutPermission, true));

        return  testCases.toArray();
    }

    private static User createUserWithPermission() throws DotDataException {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen().roles(role).nextPersisted();

        addPermissionToAddContentlet(role);
        return user;
    }

    public static void prepare() throws Exception {
        //Setting web app environment

        IntegrationTestInitService.getInstance().init();
        workflowAPI = APILocator.getWorkflowAPI();
        contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());

        // creates the type to trigger the scheme
        customContentType = createTestType(contentTypeAPI);

        // associated the scheme to the type
        final WorkflowScheme systemWorkflowScheme = workflowAPI.findSystemWorkflowScheme();
        workflowAPI.saveSchemesForStruct(new StructureTransformer(customContentType).asStructure(),
                Collections.singletonList(systemWorkflowScheme));

        setDebugMode(false);
    }

    private static ContentType createTestType(final ContentTypeAPI contentTypeAPI)
            throws DotDataException, DotSecurityException {

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

        if (null != customContentType) {

            final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
            contentTypeAPI.delete(customContentType);
        }

        cleanupDebug(SaveContentActionletTest.class);
    } // cleanup

    @Test
    @UseDataProvider("users")
    public void test_Publish_With_Save_Contentlet_Actionlet_Tag (final TestCase testCase) throws DotSecurityException, DotDataException {

        final Contentlet contentlet = new Contentlet();
        contentlet.setContentType(customContentType);
        contentlet.setProperty("title", "Test");
        contentlet.setProperty("txt", "Test");
        contentlet.setProperty("tag", "test");

        try {
            final Contentlet contentletSaved =
                    workflowAPI.fireContentWorkflow(contentlet,
                            new ContentletDependencies.Builder()
                                    .modUser(testCase.user)
                                    .respectAnonymousPermissions(testCase.respectFrontendRoles)
                                    .workflowActionId(SystemWorkflowConstants.WORKFLOW_SAVE_ACTION_ID)
                                    .build());

            Assert.assertNotNull(contentletSaved);
            Assert.assertEquals("Test", contentletSaved.getStringProperty("title"));
            Assert.assertEquals("Test", contentletSaved.getStringProperty("txt"));

            final List<TagInode> tagInodes = APILocator.getTagAPI().getTagInodesByInode(contentletSaved.getInode());
            Assert.assertNotNull(tagInodes);
            Assert.assertFalse(tagInodes.isEmpty());

            contentletSaved.setTags();
            final Contentlet contentletPublished =
                    workflowAPI.fireContentWorkflow(contentletSaved,
                            new ContentletDependencies.Builder()
                                    .modUser(APILocator.systemUser())
                                    .workflowActionId(SystemWorkflowConstants.WORKFLOW_PUBLISH_ACTION_ID)
                                    .respectAnonymousPermissions(testCase.respectFrontendRoles)
                                    .build());

            Assert.assertNotNull(contentletPublished);
            Assert.assertEquals("Test", contentletPublished.getStringProperty("title"));
            Assert.assertEquals("Test", contentletPublished.getStringProperty("txt"));
            contentletPublished.setTags();
            Assert.assertEquals("test", contentletPublished.getStringProperty("tag"));
            Assert.assertTrue(contentletPublished.isLive());

            Assert.assertFalse(testCase.shouldThrowException);
        } catch(Exception e) {
            if (ExceptionUtil.causedBy(e, DotSecurityException.class)) {
                throw e;
                //Assert.assertTrue(testCase.shouldThrowException);
            } else {
                throw e;
            }
        }
    }

    @NotNull
    private static void addPermissionToAddContentlet(final Role role) throws DotDataException {

        final WorkflowAction action = FactoryLocator.getWorkFlowFactory().findAction(SystemWorkflowConstants.WORKFLOW_PUBLISH_ACTION_ID);
        final Permission actionPermission = getPermission(role, action, PermissionLevel.USE.getType());

        final Permission contentTypePermission = getPermission(role, customContentType, PermissionLevel.CAN_ADD_CHILDREN.getType());

        try {
            APILocator.getPermissionAPI().save(contentTypePermission, customContentType, systemUser, false);
            APILocator.getPermissionAPI().save(actionPermission, action, systemUser, false);
        } catch (DotDataException | DotSecurityException e){
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private static Permission getPermission(Role role, Permissionable permissionable, int permissionPublish) {
        final Permission permission = new Permission();
        permission.setInode(permissionable.getPermissionId());
        permission.setRoleId(role.getId());
        permission.setPermission(permissionPublish);
        return permission;
    }

}
