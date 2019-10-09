package com.dotmarketing.portlets.containers.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContainerAsFileDataGen;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.TestWorkflowUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.workflows.business.SystemWorkflowConstants;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Constants;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Optional;
import org.apache.felix.framework.OSGIUtil;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test of {@link ContainerAPIImpl}
 */
public class ContainerAPIImplTest extends IntegrationTestBase  {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }


    @Test
    public void test_containerapi_find_by_inode() throws DotDataException, DotSecurityException {
      String title = "myContainer" + System.currentTimeMillis();
      final Host daHost = new SiteDataGen().nextPersisted();
      Container container =  new ContainerDataGen().site(daHost).title(title).nextPersisted();

      assertNotNull(container);
      assertNotNull(container.getInode());
      Container containerFromDB = APILocator.getContainerAPI().find(container.getInode(), APILocator.systemUser(), false);
      assertEquals(containerFromDB.getTitle(), container.getTitle());
      assertEquals(containerFromDB.getInode(), container.getInode());

      Container nullContainer = APILocator.getContainerAPI().find("nope", APILocator.systemUser(), false);
      assertNull(nullContainer);

    }

    @Test
    public void getContentTypesInContainer() throws DotDataException, DotSecurityException {
        Container container = null;
        Host host = new SiteDataGen().nextPersisted();

        final Permission permissionWrite = new Permission(host.getPermissionId(),
                        TestUserUtils.getOrCreateIntranetRole(host).getId(),
                PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_READ);
        APILocator.getPermissionAPI().save(permissionWrite, host, APILocator.systemUser(), false);

        try {
            final ContentType contentType1 = TestDataUtils
                    .getBlogLikeContentType("Blog" + System.currentTimeMillis(), host);
            final ContentType contentType2 = TestDataUtils
                    .getBannerLikeContentType("Banner" + System.currentTimeMillis(), host);
            container = new ContainerDataGen().site(host)
                    .withContentType(contentType1, "")
                    .withContentType(contentType2, "")
                    .nextPersisted();

            ContainerAPIImpl containerAPI = new ContainerAPIImpl();
            List<ContentType> contentTypesInContainer = containerAPI
                    .getContentTypesInContainer(APILocator.systemUser(), container);

            assertEquals(2, contentTypesInContainer.size());

            Optional optionalContentType1 = contentTypesInContainer.stream().filter(contentType -> contentType.name().equals(contentType1.name())).findFirst();
            Optional optionalContentType2 = contentTypesInContainer.stream().filter(contentType -> contentType.name().equals(contentType2.name())).findFirst();

            assertTrue("Blog like CT was expected", optionalContentType1.isPresent());
            assertTrue("Banner Like CT was expected", optionalContentType2.isPresent());

        } finally {
            HibernateUtil.startTransaction();
            if (container != null) {
                APILocator.getContainerAPI().delete(container, APILocator.systemUser(), false);
            }

            HibernateUtil.commitTransaction();
        }
    }

    @Test
    public void test_get_container_by_folder_found() throws DotDataException, DotSecurityException {
        final String testContainer = "/test-get-container" + System.currentTimeMillis();
        final Host daHost = new SiteDataGen().nextPersisted();
        final ContainerAsFileDataGen containerAsFileDataGen = new ContainerAsFileDataGen().host(
                daHost).folderName(testContainer);
        containerAsFileDataGen.nextPersisted();
        final ContainerAPI containerAPI = APILocator.getContainerAPI();
        final FolderAPI folderAPI       = APILocator.getFolderAPI();
        final Folder    folder          = folderAPI.findFolderByPath
                (Constants.CONTAINER_FOLDER_PATH + testContainer, daHost,
                        APILocator.systemUser(), false);

        final Container container = containerAPI.getContainerByFolder(folder, APILocator.systemUser(), false);

        assertNotNull(container);
        assertNotNull(container.getInode());
        assertTrue   (container instanceof FileAssetContainer);
        assertEquals ("Test Container", container.getTitle());
        final List<FileAsset> fileAssets = FileAssetContainer.class.cast(container)
                .getContainerStructuresAssets();

        final List<ContentType> contentTypes = containerAsFileDataGen.getContentTypes();
        assertNotNull(fileAssets);
        assertEquals(contentTypes.size(), fileAssets.size());
        for(ContentType contentType:contentTypes) {
            assertTrue(fileAssets.stream().anyMatch(fileAsset -> fileAsset.getFileName()
                    .equals(String.format("%s.vtl", contentType.name()))));
        }

        final List<ContainerStructure> containerStructures = containerAPI.getContainerStructures(container);
        assertNotNull(containerStructures);
        assertEquals(contentTypes.size(), containerStructures.size());
    }

    @Test(expected = NotFoundInDbException.class)
    public void test_find_container_not_found() throws DotDataException, DotSecurityException {
        final Host daHost = new SiteDataGen().nextPersisted();
        final ContainerAPI containerAPI = APILocator.getContainerAPI();
        final FolderAPI folderAPI       = APILocator.getFolderAPI();
        final Folder    folder          = folderAPI.findFolderByPath
                (Constants.CONTAINER_FOLDER_PATH + "/doesnotexists", daHost,
                        APILocator.systemUser(), false);

        containerAPI.getContainerByFolder(folder, APILocator.systemUser(), false);
    }

    @Test
    public void test_find_all_containers_success() throws DotDataException, DotSecurityException {

        new ContainerAsFileDataGen().host(APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false)).nextPersisted();

        final ContainerAPI containerAPI = APILocator.getContainerAPI();
        final List<Container> containers = containerAPI.findAllContainers(APILocator.systemUser(), false);

        assertNotNull(containers);
        assertTrue(containers.size() > 0);
        assertTrue(containers.stream().anyMatch(container -> container instanceof FileAssetContainer));
    }

    @Test (expected = NotFoundInDbException.class)
    public void test_get_live_not_found() throws DotDataException, DotSecurityException {
        final String testContainer = "/test-get-container" + System.currentTimeMillis();
        final Host defaultHost  = APILocator.getHostAPI().findDefaultHost( APILocator.systemUser(), false );
        Contentlet contentlet = null;

        try {

            final ContainerAPI containerAPI = APILocator.getContainerAPI();
            final FolderAPI folderAPI       = APILocator.getFolderAPI();
            final Folder    folder          = folderAPI.findFolderByPath
                    (Constants.CONTAINER_FOLDER_PATH + testContainer, defaultHost,
                            APILocator.systemUser(), false);

            final Container container = containerAPI.getContainerByFolder(folder, APILocator.systemUser(), false);

            if (container.isLive()) {

                contentlet = this.unpublish (container.getInode());
            }

            containerAPI.getLiveContainerById(container.getIdentifier(), APILocator.systemUser(), false);
        } finally {

            try {
                this.publish(contentlet);
            } catch (Exception e) {
                // quiet
            }
        }
    }

    @Test
    public void test_get_working_found() throws DotDataException, DotSecurityException {
        final String testContainer = "/test_get_working_found" + System.currentTimeMillis();
        final Host daHost = new SiteDataGen().nextPersisted();
        new ContainerAsFileDataGen().host(daHost).folderName(testContainer).nextPersisted();
        final ContainerAPI containerAPI = APILocator.getContainerAPI();
        final FolderAPI folderAPI       = APILocator.getFolderAPI();
        final Folder    folder          = folderAPI.findFolderByPath
                (Constants.CONTAINER_FOLDER_PATH + testContainer, daHost,
                        APILocator.systemUser(), false);

        final Container container = containerAPI.getContainerByFolder(folder, APILocator.systemUser(), false);

        assertNotNull(container);
        assertNotNull(container.getIdentifier());

        final Container workingContainer =
                containerAPI.getWorkingContainerById(container.getIdentifier(), APILocator.systemUser(), false);

        assertNotNull(workingContainer);
        assertNotNull(workingContainer.getIdentifier());
    }

    private Contentlet publish(final Contentlet contentlet) throws DotSecurityException, DotDataException {

        final WorkflowAPI workflowAPI        = APILocator.getWorkflowAPI();
        final WorkflowAction unpublishAction = workflowAPI.findAction
                (SystemWorkflowConstants.WORKFLOW_PUBLISH_ACTION_ID, APILocator.systemUser());

        return null == contentlet? contentlet: workflowAPI.fireContentWorkflow(contentlet,
                new ContentletDependencies.Builder()
                        .indexPolicy(IndexPolicy.WAIT_FOR)
                        .workflowActionId(unpublishAction)
                        .modUser(APILocator.systemUser())
                        .build());
    }

    private Contentlet unpublish(final String containerInode) throws DotSecurityException, DotDataException {

        List<WorkflowAction> actions = APILocator.getWorkflowAPI().findActions(TestWorkflowUtils.getSystemWorkflow(), APILocator.systemUser());
        final Optional<WorkflowAction> optionalAction = actions.stream().filter(workflowAction -> "Unpublish".equalsIgnoreCase(workflowAction.getName())).findFirst();

        final WorkflowAPI workflowAPI        = APILocator.getWorkflowAPI();
        final ContentletAPI contentletAPI    = APILocator.getContentletAPI();
        assertTrue("Unable to locate Unpublish action on system workflow.", optionalAction.isPresent());

        final WorkflowAction unpublishAction = optionalAction.get();
        final Contentlet contentlet = contentletAPI.find(containerInode, APILocator.systemUser(), false);

        return workflowAPI.fireContentWorkflow(contentlet,
                new ContentletDependencies.Builder()
                        .indexPolicy(IndexPolicy.WAIT_FOR)
                        .workflowActionId(unpublishAction)
                        .modUser(APILocator.systemUser())
                        .build());
    }

}
