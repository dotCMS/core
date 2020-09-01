package com.dotmarketing.portlets.containers.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.*;
import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.business.*;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.workflows.business.SystemWorkflowConstants;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.util.Constants;
import com.liferay.portal.model.User;
import java.io.File;
import java.io.IOException;
import java.util.*;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    /**
     * Creates a temporal file using a given content
     *
     * @param content
     * @return
     * @throws IOException
     */
    private File createTempFile (final String content ) throws IOException {

        final File tempTestFile = File.createTempFile( "tempfile_" + String.valueOf( new Date().getTime() ), ".vtl" );
        FileUtils.writeStringToFile( tempTestFile, content );
        return tempTestFile;
    }

    private void createTestFileContainer(final String containerPath, final Host host, final String containerContent)
            throws DotDataException, DotSecurityException, IOException {

        final User   user            = APILocator.systemUser();
        final Folder containerFolder = APILocator.getFolderAPI().createFolders(containerPath, host, user, false);
        final File   contentFile     = this.createTempFile(containerContent);
        new FileAssetDataGen(containerFolder, contentFile)
                .setProperty(FileAssetAPI.TITLE_FIELD, "container.vtl")
                .setProperty(FileAssetAPI.FILE_NAME_FIELD, "container.vtl")
                .nextPersisted();

    }

    @Test
    public void test_getContainerByFolder_cache() throws DotDataException, DotSecurityException, IOException {

        final String containerPath = Constants.CONTAINER_FOLDER_PATH + "/test-container" + System.currentTimeMillis();
        this.createTestFileContainer(containerPath, APILocator.getHostAPI()
                .findDefaultHost(APILocator.systemUser(), false),
                "$dotJSON.put(\"title\", \"Test Container\""+ System.currentTimeMillis() +")\n"
                        + "$dotJSON.put(\"description\", \"Test container\")\n"
                        + "$dotJSON.put(\"max_contentlets\", 25)\n");

        final User      user       = APILocator.systemUser();
        final Host      host       = APILocator.getHostAPI().findDefaultHost(user, false);
        final Folder    folder     = APILocator.getFolderAPI().findFolderByPath
                (containerPath, host, user, false);
        final Container container1 = APILocator.getContainerAPI().getContainerByFolder(folder, host, user, false);
        final Container container2 = APILocator.getContainerAPI().getContainerByFolder(folder, host, user, false);

        Assert.assertTrue(container1 == container2);

        CacheLocator.getContainerCache().remove(container1);

        final Container container3 = APILocator.getContainerAPI().getContainerByFolder(folder, host, user, false);

        Assert.assertTrue(container3 != container2);
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

    /**
     * Method to Test: {@link ContainerAPIImpl#findContainer(String, User, boolean, boolean)}
     * When: Find a {@link Container} not publish by Id and live equals to false
     * Should: return it
     */
    @Test
    public void whenFindContainerByIdAndExists() throws DotSecurityException, DotDataException {
        final Container container = new ContainerDataGen().nextPersisted();
        final Optional<Container> containerFromDatabase =
                APILocator.getContainerAPI().findContainer(container.getIdentifier(), APILocator.systemUser(), false, false);

        assertTrue(containerFromDatabase.isPresent());
        assertEquals(container.getIdentifier(), containerFromDatabase.get().getIdentifier());
    }

    /**
     * Method to Test: {@link ContainerAPIImpl#findContainer(String, User, boolean, boolean)}
     * When: Find a published {@link Container}  by Id and live equals to false
     * Should: return the working version
     */
    @Test
    public void whenFindPublishContainerByIdAndExistsAndLiveFalse() throws DotSecurityException, DotDataException, WebAssetException {
        final Host host = new SiteDataGen().nextPersisted();
        final Container container = new ContainerDataGen().site(host).nextPersisted();
        container.setTitle("Live Version");
        ContainerDataGen.publish(container);

        container.setTitle("Working Version");
        APILocator.getContainerAPI()
                .save(container, Collections.emptyList(), host, APILocator.getUserAPI().getSystemUser(), false);

        final Optional<Container> containerFromDatabase =
                APILocator.getContainerAPI().findContainer(container.getIdentifier(), APILocator.systemUser(), false, false);

        assertTrue(containerFromDatabase.isPresent());
        assertEquals(container.getIdentifier(), containerFromDatabase.get().getIdentifier());

        final VersionInfo versionInfo = APILocator.getVersionableAPI().getVersionInfo(container.getIdentifier());
        assertEquals(versionInfo.getWorkingInode(), containerFromDatabase.get().getInode());
    }

    /**
     * Method to Test: {@link ContainerAPIImpl#findContainer(String, User, boolean, boolean)}
     * When: Find a published {@link Container}  by Id and live equals to true
     * Should: return the live version
     */
    @Test
    public void whenFindPublishContainerByIdAndExistsAndLiveTrue() throws DotSecurityException, DotDataException, WebAssetException {
        final Host host = new SiteDataGen().nextPersisted();
        final Container container = new ContainerDataGen().site(host).nextPersisted();
        container.setTitle("Live Version");
        ContainerDataGen.publish(container);
        
        container.setTitle("Working Version");
        APILocator.getContainerAPI()
                .save(container, Collections.emptyList(), host, APILocator.getUserAPI().getSystemUser(), false);

        final VersionInfo versionInfo = APILocator.getVersionableAPI().getVersionInfo(container.getIdentifier());
        CacheLocator.getContainerCache().remove(versionInfo);

        final Optional<Container> containerFromDatabase =
                APILocator.getContainerAPI().findContainer(container.getIdentifier(), APILocator.systemUser(), true, false);

        assertTrue(containerFromDatabase.isPresent());
        assertEquals(container.getIdentifier(), containerFromDatabase.get().getIdentifier());

        assertEquals(versionInfo.getLiveInode(), containerFromDatabase.get().getInode());
    }

    /**
     * Method to Test: {@link ContainerAPIImpl#findContainer(String, User, boolean, boolean)}
     * When: Find a {@link Container} by Id and it not exists
     * Should: return a empty Optional
     */
    @Test
    public void whenFindContainerByIdAndNotExists() throws DotSecurityException, DotDataException {
        final Optional<Container> containerFromDatabase =
                APILocator.getContainerAPI().findContainer("not_exists", APILocator.systemUser(), false, false);

        assertFalse(containerFromDatabase.isPresent());
    }

    /**
     * Method to Test: {@link ContainerAPIImpl#findContainer(String, User, boolean, boolean)}
     * When: Find a not published {@link FileAssetContainer} by absolute path and id, wit Live equals to true and false
     * Should: for all the case should return the working version
     */
    @Test
    public void whenFindFileContainerByAbsolutePathAndExists() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final FileAssetContainer fileAssetContainer = new ContainerAsFileDataGen()
                .host(host)
                .nextPersisted();

        //Find by Id
        final Optional<Container> containerFromDatabase =
                APILocator.getContainerAPI().findContainer(fileAssetContainer.getIdentifier(), APILocator.systemUser(),
                        false, false);

        assertTrue(containerFromDatabase.isPresent());
        assertEquals(
                fileAssetContainer.getIdentifier(),
                containerFromDatabase.get().getIdentifier()
        );

        //Find by absolute path
        final Optional<Container> containerFromDatabaseByAbsolutePath =
                APILocator.getContainerAPI().findContainer(
                        FileAssetContainerUtil.getInstance().getFullPath((FileAssetContainer) containerFromDatabase.get()),
                        APILocator.systemUser(), false, false);

        assertTrue(containerFromDatabaseByAbsolutePath.isPresent());
        assertEquals(
                fileAssetContainer.getIdentifier(),
                containerFromDatabaseByAbsolutePath.get().getIdentifier()
        );

        //find with Live equals to true
        final Optional<Container> containerInLive = APILocator.getContainerAPI().findContainer(
                FileAssetContainerUtil.getInstance().getFullPath((FileAssetContainer) containerFromDatabaseByAbsolutePath.get()),
                APILocator.systemUser(),
                true, false);

        assertTrue(containerInLive.isPresent());

        final VersionInfo versionInfo = APILocator.getVersionableAPI().getVersionInfo(fileAssetContainer.getIdentifier());
        assertEquals(
                versionInfo.getWorkingInode(),
                containerInLive.get().getInode()
        );
    }

    /**
     * Method to Test: {@link ContainerAPIImpl#findContainer(String, User, boolean, boolean)}
     * When: Find a published {@link FileAssetContainer} by absolute path and id
     * Should: return it
     */
    @Test
    public void whenFindPublishedFileContainerByAbsolutePathAndExists() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final FileAssetContainer fileAssetContainer = new ContainerAsFileDataGen()
                .host(host)
                .nextPersisted();
        final Contentlet contentlet = APILocator.getContentletAPI().find(fileAssetContainer.getInode(),
                APILocator.systemUser(), false);
        publish(contentlet);

        //find by id
        final Optional<Container> containerFromDatabase =
                APILocator.getContainerAPI().findContainer(fileAssetContainer.getIdentifier(), APILocator.systemUser(),
                        true, false);

        assertTrue(containerFromDatabase.isPresent());
        assertEquals(
                fileAssetContainer.getIdentifier(),
                containerFromDatabase.get().getIdentifier()
        );

        //find by absolute path
        final Optional<Container> containerFromDatabaseWithAbsolutePath =
                APILocator.getContainerAPI().findContainer(
                        FileAssetContainerUtil.getInstance().getFullPath((FileAssetContainer) containerFromDatabase.get()),
                        APILocator.systemUser(), true, false);

        assertEquals(
                fileAssetContainer.getIdentifier(),
                containerFromDatabaseWithAbsolutePath.get().getIdentifier()
        );

        final VersionInfo versionInfo = APILocator.getVersionableAPI().getVersionInfo(fileAssetContainer.getIdentifier());

        assertTrue(containerFromDatabaseWithAbsolutePath.isPresent());
        assertEquals(
                versionInfo.getLiveInode(),
                containerFromDatabaseWithAbsolutePath.get().getInode()
        );

        //find with LIVE equals false, should return the working version
        final Optional<Container> workingContainerFromDatabase =
                APILocator.getContainerAPI().findContainer(fileAssetContainer.getIdentifier(), APILocator.systemUser(), false,
                        false);

        assertEquals(versionInfo.getWorkingInode(), workingContainerFromDatabase.get().getInode());
    }

    /**
     * Method to Test: {@link ContainerAPIImpl#findContainer(String, User, boolean, boolean)}
     * When: Find a {@link FileAssetContainer} that exists in current host by absolute path but using another host
     *       Also find the container with Relative Path
     * Should: return it
     */
    @Test
    public void whenFindFileContainerByAbsolutePathAndExistsInCurrentHost() throws DotDataException, DotSecurityException {
        final Host anotherHost = new SiteDataGen().nextPersisted();
        final Host currentHost = new SiteDataGen().nextPersisted();

        final HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        when(request.getParameter("host_id")).thenReturn(currentHost.getIdentifier());

        final FileAssetContainer fileAssetContainer = new ContainerAsFileDataGen()
                .host(currentHost)
                .nextPersisted();

        //find by id
        final Optional<Container> containerFromDatabase =
                APILocator.getContainerAPI().findContainer(fileAssetContainer.getIdentifier(), APILocator.systemUser(),
                        false, false);

        final String fullPathUsingAnotherHost = FileAssetContainerUtil.getInstance().getFullPath(
                anotherHost,
                ((FileAssetContainer) containerFromDatabase.get()).getPath());

        //find by absolute path using the another hist
        final Optional<Container> containerFromDatabaseWithAbsolutePath =
                APILocator.getContainerAPI().findContainer(fullPathUsingAnotherHost,
                        APILocator.systemUser(), true, false);

        final FileAssetContainer fileAssetContainerFromDataBase = (FileAssetContainer) containerFromDatabaseWithAbsolutePath.get();
        assertEquals(
                fileAssetContainer.getIdentifier(),
                fileAssetContainerFromDataBase.getIdentifier()
        );

        assertEquals(
                fileAssetContainerFromDataBase.getHost().getIdentifier(),
                currentHost.getIdentifier()
        );

        //using relative path
        final Optional<Container> containerFromDatabaseWithRelativePath =
                APILocator.getContainerAPI().findContainer(((FileAssetContainer) containerFromDatabase.get()).getPath(),
                        APILocator.systemUser(), true, false);

        final FileAssetContainer fileAssetContainerFromDataBaseWithRelativePath = (FileAssetContainer) containerFromDatabaseWithRelativePath.get();
        assertEquals(
                fileAssetContainer.getIdentifier(),
                fileAssetContainerFromDataBaseWithRelativePath.getIdentifier()
        );

        assertEquals(
                fileAssetContainerFromDataBaseWithRelativePath.getHost().getIdentifier(),
                currentHost.getIdentifier()
        );
    }

    /**
     * Method to Test: {@link ContainerAPIImpl#findContainer(String, User, boolean, boolean)}
     * When: Find a {@link FileAssetContainer} that exists in current host by absolute path but using another host
     *       Also find the container with Relative Path
     * Should: return it
     */
    @Test
    public void whenFindFileContainerByAbsolutePathAndExistsInDefaultHost() throws DotDataException, DotSecurityException {
        final Host anotherHost = new SiteDataGen().nextPersisted();
        final Host defaultHost = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        when(request.getParameter("host_id")).thenReturn(defaultHost.getIdentifier());

        final FileAssetContainer fileAssetContainer = new ContainerAsFileDataGen()
                .host(defaultHost)
                .nextPersisted();

        //find by id
        final Optional<Container> containerFromDatabase =
                APILocator.getContainerAPI().findContainer(fileAssetContainer.getIdentifier(), APILocator.systemUser(),
                        false, false);

        final String fullPathUsingAnotherHost = FileAssetContainerUtil.getInstance().getFullPath(
                anotherHost,
                ((FileAssetContainer) containerFromDatabase.get()).getPath());

        //find by absolute path using the another host
        final Optional<Container> containerFromDatabaseWithAbsolutePath =
                APILocator.getContainerAPI().findContainer(fullPathUsingAnotherHost,
                        APILocator.systemUser(), true, false);

        final FileAssetContainer fileAssetContainerFromDataBase = (FileAssetContainer) containerFromDatabaseWithAbsolutePath.get();
        assertEquals(
                fileAssetContainer.getIdentifier(),
                fileAssetContainerFromDataBase.getIdentifier()
        );

        assertEquals(
                fileAssetContainerFromDataBase.getHost().getIdentifier(),
                defaultHost.getIdentifier()
        );

        //using relative path
        final Optional<Container> containerFromDatabaseWithRelativePath =
                APILocator.getContainerAPI().findContainer(((FileAssetContainer) containerFromDatabase.get()).getPath(),
                        APILocator.systemUser(), true, false);

        final FileAssetContainer fileAssetContainerFromDataBaseWithRelativePath = (FileAssetContainer) containerFromDatabaseWithRelativePath.get();
        assertEquals(
                fileAssetContainer.getIdentifier(),
                fileAssetContainerFromDataBaseWithRelativePath.getIdentifier()
        );

        assertEquals(
                fileAssetContainerFromDataBaseWithRelativePath.getHost().getIdentifier(),
                defaultHost.getIdentifier()
        );
    }


    /**
     * Method to Test: {@link ContainerAPIImpl#findContainer(String, User, boolean, boolean)}
     * When: Find a {@link FileAssetContainer} by relative path and it not exists
     * Should: return Empty Optional
     */
    @Test
    public void whenFindFileContainerByRelativePathAndNotExists() throws DotSecurityException, DotDataException {
        final Optional<Container> containerFromDatabase =
                APILocator.getContainerAPI().findContainer("/not_exists", APILocator.systemUser(), false, false);

        assertFalse(containerFromDatabase.isPresent());
    }

    /**
     * Method to Test: {@link ContainerAPIImpl#findContainer(String, User, boolean, boolean)}
     * When: Find a {@link Container} by Id and it exists but the not have permission
     * Should: throw a {@link DotSecurityException}
     */
    @Test(expected = DotSecurityException.class)
    public void whenFindContainerByIdAndUserNotHasPermission() throws DotDataException, DotSecurityException {
        final User user = new UserDataGen().nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();
        final FileAssetContainer fileAssetContainer = new ContainerAsFileDataGen()
                .host(host)
                .nextPersisted();

        APILocator.getContainerAPI().findContainer(fileAssetContainer.getIdentifier(), user,
                false, false);
        throw new AssertionError("DotSecurityException expected");
    }

    /**
     * Method to Test: {@link ContainerAPIImpl#findContainer(String, User, boolean, boolean)}
     * When: the FileContainer exists but the user not have permission in Default host
     * Should: return it
     */
    @Test
    public void whenUserNotHavePermissionInDefaultHost() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen()
                .roles(APILocator.getRoleAPI().loadBackEndUserRole(), role)
                .nextPersisted();

        addPermission(role, host, PermissionLevel.READ.getType());

        final FileAssetContainer fileAssetContainer = new ContainerAsFileDataGen()
                .host(host)
                .nextPersisted();

        final Optional<Container> containerFromDatabase =
                APILocator.getContainerAPI().findContainer(fileAssetContainer.getIdentifier(), APILocator.systemUser(),
                        false, false);

        final String fullPathUsingAnotherHost = FileAssetContainerUtil.getInstance().getFullPath(
                host,
                ((FileAssetContainer) containerFromDatabase.get()).getPath());

        final Optional<Container> containerFromDatabaseWithAbsolutePath =
                APILocator.getContainerAPI().findContainer(fullPathUsingAnotherHost,
                        user, true, false);

        assertFalse(containerFromDatabaseWithAbsolutePath.isPresent());
    }

    /**
     * Method to Test: {@link ContainerAPIImpl#findContainer(String, User, boolean, boolean)}
     * When: the FileContainer exists but the user not have permission in Current host
     * Should: return it
     */
    @Test
    public void whenUserNotHavePermissionInCurrentHost() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();
        final Host currentHost = new SiteDataGen().nextPersisted();

        final HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        when(request.getParameter("host_id")).thenReturn(currentHost.getIdentifier());

        final Role role = new RoleDataGen().nextPersisted();
        final User backEndUser = new UserDataGen()
                .roles(APILocator.getRoleAPI().loadBackEndUserRole(), role)
                .nextPersisted();

        addPermission(role, host, PermissionLevel.READ.getType());

        final FileAssetContainer fileAssetContainer = new ContainerAsFileDataGen()
                .host(host)
                .nextPersisted();

        final Optional<Container> containerFromDatabase =
                APILocator.getContainerAPI().findContainer(fileAssetContainer.getIdentifier(), APILocator.systemUser(),
                        false, false);

        final String fullPathUsingAnotherHost = FileAssetContainerUtil.getInstance().getFullPath(
                host,
                ((FileAssetContainer) containerFromDatabase.get()).getPath());

        final Optional<Container> containerFromDatabaseWithAbsolutePath =
                APILocator.getContainerAPI().findContainer(fullPathUsingAnotherHost,
                        backEndUser, true, false);

        assertFalse(containerFromDatabaseWithAbsolutePath.isPresent());
    }

    @NotNull
    private void addPermission(
            final Role role,
            final Permissionable permissionable,
            final int permissionPublish) {

        final Permission publishPermission = getPermission(role, permissionable, permissionPublish);

        try {
            APILocator.getPermissionAPI().save(publishPermission, permissionable, APILocator.systemUser(), false);
        } catch (DotDataException | DotSecurityException e){
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private Permission getPermission(Role role, Permissionable permissionable, int permissionPublish) {
        final Permission publishPermission = new Permission();
        publishPermission.setInode(permissionable.getPermissionId());
        publishPermission.setRoleId(role.getId());
        publishPermission.setPermission(permissionPublish);
        return publishPermission;
    }
}
