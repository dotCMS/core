package com.dotmarketing.portlets.containers.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.IntegrationTestBase;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.ContentType;
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
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
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
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.apache.felix.framework.OSGIUtil;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test of {@link ContainerAPIImpl}
 */
public class ContainerAPIImplTest extends IntegrationTestBase  {

    private static final String TEST_CONTAINER = "/testcontainer" + System.currentTimeMillis();
    private static ContentType newsLikeContentType,documentLikeContentType,productLikeContentType;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        OSGIUtil.getInstance().initializeFramework(Config.CONTEXT);
        final Host defaultHost  = APILocator.getHostAPI().findDefaultHost( APILocator.systemUser(), false );
        checkApplicationContainerFolder(defaultHost);
    }

    @Test
    public void getContentTypesInContainer() throws DotDataException, DotSecurityException {
        Container container = null;
        Host host = new SiteDataGen().nextPersisted();
        User user = TestUserUtils.getBillIntranetUser(host);

        Permission permissionWrite = new Permission(host.getPermissionId(),
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

        final Host defaultHost  = APILocator.getHostAPI().findDefaultHost( APILocator.systemUser(), false );
        final ContainerAPI containerAPI = APILocator.getContainerAPI();
        final FolderAPI folderAPI       = APILocator.getFolderAPI();
        final Folder    folder          = folderAPI.findFolderByPath
                (Constants.CONTAINER_FOLDER_PATH + TEST_CONTAINER, defaultHost,
                        APILocator.systemUser(), false);

        final Container container = containerAPI.getContainerByFolder(folder, APILocator.systemUser(), false);

        assertNotNull(container);
        assertNotNull(container.getInode());
        assertTrue   (container instanceof FileAssetContainer);
        assertEquals ("Test Container", container.getTitle());
        final List<FileAsset> fileAssets = FileAssetContainer.class.cast(container)
                .getContainerStructuresAssets();

        assertNotNull(fileAssets);
        assertEquals(3, fileAssets.size());
        assertTrue(fileAssets.stream().anyMatch(fileAsset -> fileAsset.getFileName().equals(String.format("%s.vtl",newsLikeContentType.name()))));
        assertTrue(fileAssets.stream().anyMatch(fileAsset -> fileAsset.getFileName().equals(String.format("%s.vtl",productLikeContentType.name()))));
        assertTrue(fileAssets.stream().anyMatch(fileAsset -> fileAsset.getFileName().equals(String.format("%s.vtl",documentLikeContentType.name()))));

        final List<ContainerStructure> containerStructures = containerAPI.getContainerStructures(container);
        assertNotNull(containerStructures);
        assertEquals(3, containerStructures.size());
    }

    @Test(expected = NotFoundInDbException.class)
    public void test_find_container_not_found() throws DotDataException, DotSecurityException {

        final Host defaultHost  = APILocator.getHostAPI().findDefaultHost( APILocator.systemUser(), false );
        final ContainerAPI containerAPI = APILocator.getContainerAPI();
        final FolderAPI folderAPI       = APILocator.getFolderAPI();
        final Folder    folder          = folderAPI.findFolderByPath
                (Constants.CONTAINER_FOLDER_PATH + "/doesnotexists", defaultHost,
                        APILocator.systemUser(), false);

        containerAPI.getContainerByFolder(folder, APILocator.systemUser(), false);
    }

    @Test
    public void test_find_all_containers_success() throws DotDataException, DotSecurityException {

        final Host defaultHost  = APILocator.getHostAPI().findDefaultHost( APILocator.systemUser(), false );
        final ContainerAPI containerAPI = APILocator.getContainerAPI();
        final List<Container> containers = containerAPI.findAllContainers(APILocator.systemUser(), false);

        assertNotNull(containers);
        assertTrue(containers.size() > 0);
        assertTrue(containers.stream().anyMatch(container -> container instanceof FileAssetContainer));
    }

    @Test (expected = NotFoundInDbException.class)
    public void test_get_live_not_found() throws DotDataException, DotSecurityException {

        final Host defaultHost  = APILocator.getHostAPI().findDefaultHost( APILocator.systemUser(), false );
        Contentlet contentlet = null;

        try {

            final ContainerAPI containerAPI = APILocator.getContainerAPI();
            final FolderAPI folderAPI       = APILocator.getFolderAPI();
            final Folder    folder          = folderAPI.findFolderByPath
                    (Constants.CONTAINER_FOLDER_PATH + TEST_CONTAINER, defaultHost,
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

        final Host defaultHost  = APILocator.getHostAPI().findDefaultHost( APILocator.systemUser(), false );
        final ContainerAPI containerAPI = APILocator.getContainerAPI();
        final FolderAPI folderAPI       = APILocator.getFolderAPI();
        final Folder    folder          = folderAPI.findFolderByPath
                (Constants.CONTAINER_FOLDER_PATH + TEST_CONTAINER, defaultHost,
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
                        .indexPolicy(IndexPolicy.FORCE)
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
                        .indexPolicy(IndexPolicy.FORCE)
                        .workflowActionId(unpublishAction)
                        .modUser(APILocator.systemUser())
                        .build());
    }


    @WrapInTransaction
    private static synchronized void checkApplicationContainerFolder (final Host defaultHost) {

        final FolderAPI folderAPI = APILocator.getFolderAPI();
        try {
            final Folder folder = folderAPI.findFolderByPath(Constants.CONTAINER_FOLDER_PATH,
                  defaultHost, APILocator.systemUser(), true
            );

            if (null == folder || !UtilMethods.isSet(folder.getIdentifier())) {

                creatApplicationContainerFolder(defaultHost);
                createFileAssetContainerForTesting(defaultHost);
            }

            if (!existsFileAssetContainerForTesting(defaultHost)) {

                createFileAssetContainerForTesting(defaultHost);
            }
        } catch (DotDataException | DotSecurityException e) {

            creatApplicationContainerFolder(defaultHost);
            createFileAssetContainerForTesting(defaultHost);
        }
    }

    private static boolean existsFileAssetContainerForTesting(final Host defaultHost) {

        final FolderAPI    folderAPI    = APILocator.getFolderAPI();
        final FileAssetAPI fileAssetAPI = APILocator.getFileAssetAPI();
        boolean exists = false;
        try {
            final Folder    folder    = folderAPI.findFolderByPath
                    (Constants.CONTAINER_FOLDER_PATH + TEST_CONTAINER, defaultHost, APILocator.systemUser(), true);

            if(null != folder && UtilMethods.isSet(folder.getIdentifier())) {

                final List<FileAsset> fileAssets = fileAssetAPI.findFileAssetsByFolder
                        (folder, null, true, APILocator.systemUser(), false);

                if (UtilMethods.isSet(fileAssets)) {

                   exists = fileAssets.stream().anyMatch(fileAsset -> "container.vtl".equalsIgnoreCase(fileAsset.getFileName()));
                }
            }
        } catch (DotDataException | DotSecurityException e) {

            return false;
        }

        return exists;
    }

    private static void creatApplicationContainerFolder(final Host defaultHost) {

        final FolderAPI folderAPI = APILocator.getFolderAPI();
        try {
            folderAPI.createFolders(Constants.CONTAINER_FOLDER_PATH, defaultHost, APILocator.systemUser(), true);
        } catch (DotDataException | DotSecurityException e) {
            fail("Couldn't create the " + Constants.CONTAINER_FOLDER_PATH);
        }
    }

    private static void createFileAssetContainerForTesting(final Host defaultHost) {

        final Folder testContainerFolder = createTestContainerFolder(defaultHost);

        try {
            //Don't forget these are file Assets
            //The container it self
            final Contentlet container = createContainerVTL(testContainerFolder, defaultHost);
            //And the containers mapped to contentTypes
            final Contentlet document  = createDocumentVTL (testContainerFolder, defaultHost);
            final Contentlet news      = createNewsVTL     (testContainerFolder, defaultHost);
            final Contentlet products  = createProductsVTL (testContainerFolder, defaultHost);

            assertNotNull(container);
            assertNotNull(document);
            assertNotNull(news);
            assertNotNull(products);
        } catch (DotSecurityException | DotDataException e) {
            fail(e.getMessage());
        }
    }

    private static Folder createTestContainerFolder(final Host defaultHost) {

        final FolderAPI folderAPI        = APILocator.getFolderAPI();
        try {
            return folderAPI.createFolders(Constants.CONTAINER_FOLDER_PATH + TEST_CONTAINER, defaultHost, APILocator.systemUser(), true);
        } catch (DotDataException | DotSecurityException e) {
            throw new DotRuntimeException("Couldn't create the " + Constants.CONTAINER_FOLDER_PATH + TEST_CONTAINER);
        }
    }

    private static Contentlet createContainerVTL(final Folder testContainerFolder, final Host defaultHost) throws DotSecurityException, DotDataException {

        try {

            final WorkflowAPI workflowAPI   = APILocator.getWorkflowAPI();
            final FileAsset fileAsset       = new FileAsset();
            final WorkflowAction saveAction = workflowAPI.findAction(SystemWorkflowConstants.WORKFLOW_SAVE_PUBLISH_ACTION_ID, APILocator.systemUser());
            final String title              = "container";

            fileAsset.setContentTypeId(APILocator.getContentTypeAPI(APILocator.systemUser()).find(FileAssetAPI.BINARY_FIELD).id());
            fileAsset.setHost(defaultHost.getIdentifier());
            fileAsset.setModUser(APILocator.systemUser().getUserId());
            fileAsset.setModDate(new Date());
            fileAsset.setTitle(title);
            fileAsset.setFriendlyName(title);
            fileAsset.setFolder(testContainerFolder.getInode());
            fileAsset.setBinary(FileAssetAPI.BINARY_FIELD, createTempFile(title,"$dotJSON.put(\"title\", \"Test Container\")\n" +
                                                    "$dotJSON.put(\"max_contentlets\", 25)\n" +
                                                    "$dotJSON.put(\"notes\", \"Medium Column:Blog,Events,Generic,Location,Media,News,Documents,Products\")\n"));
            fileAsset.setBoolProperty(Contentlet.IS_TEST_MODE, true);
            fileAsset.setIndexPolicy(IndexPolicy.FORCE);
            fileAsset.setIndexPolicyDependencies(IndexPolicy.FORCE);

            return workflowAPI.fireContentWorkflow(fileAsset,
                    new ContentletDependencies.Builder()
                            .indexPolicy(IndexPolicy.FORCE)
                            .indexPolicyDependencies(IndexPolicy.FORCE)
                            .workflowActionId(saveAction)
                            .modUser(APILocator.systemUser())
                            .build());
        } catch (IOException e) {
            fail(e.getMessage());
        }

        return null;
    }

    private static Contentlet createProductsVTL(final Folder testContainerFolder, final Host defaultHost) throws DotSecurityException, DotDataException {

        try {
            productLikeContentType = TestDataUtils.getProductLikeContentType();

            final WorkflowAPI workflowAPI   = APILocator.getWorkflowAPI();
            final FileAsset fileAsset       = new FileAsset();
            final WorkflowAction saveAction = workflowAPI.findAction(SystemWorkflowConstants.WORKFLOW_SAVE_PUBLISH_ACTION_ID, APILocator.systemUser());
            final String title              = productLikeContentType.name();

            fileAsset.setContentTypeId(APILocator.getContentTypeAPI(APILocator.systemUser()).find(FileAssetAPI.BINARY_FIELD).id());
            fileAsset.setHost(defaultHost.getIdentifier());
            fileAsset.setModUser(APILocator.systemUser().getUserId());
            fileAsset.setModDate(new Date());
            fileAsset.setTitle(title);
            fileAsset.setFriendlyName(title);
            fileAsset.setFolder(testContainerFolder.getInode());
            fileAsset.setBinary(FileAssetAPI.BINARY_FIELD, createTempFile(title,"<script>\n" +
                    "    $(document).ready(function() {\n" +
                    "\n" +
                    "        jQuery.getJSON(\"https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20yahoo.finance.quotes%20where%20symbol%20in%20(%22$!{tickerSymbol}%22)&format=json&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=?\",\n" +
                    "\t    function(data) {\n" +
                    "\t      var q = (data.query.results.quote.PreviousClose);\n" +
                    "\t      var x = (data.query.results.quote.Change);\n" +
                    "\t      var y = (data.query.results.quote.PercentChange);\n" +
                    "\t\n" +
                    "\t      jQuery(\"#price$!{tickerSymbol}MD\").append(q);\n" +
                    "\t\n" +
                    "\t      if (x.indexOf(\"-\") != -1) {\n" +
                    "\t        var x = \"<span class=\\\"down\\\">&nbsp;\" + x + \"&nbsp;(\" + y + \")</span>\";\n" +
                    "\t      } else if (x.indexOf(\"+\") != -1) {\n" +
                    "\t        var x = \"<span class=\\\"up\\\">&nbsp;\" + x + \"&nbsp;(\" + y + \")</span>\";\n" +
                    "\t      } else {\n" +
                    "\t        var x = \"<span>&nbsp;\" + x + \"&nbsp;(\" + y + \")</span>\";\n" +
                    "\t      }\n" +
                    "\t      jQuery(\"#change$!{tickerSymbol}MD\").append(x);\n" +
                    "\t    });\n" +
                    "\n" +
                    "\t});\n" +
                    "</script>\n" +
                    "<div class=\"row\">\n" +
                    "    <div class=\"col-sm-12\">\n" +
                    "    \t<span class=\"label label-default pull-right\">Product</span>\n" +
                    "\t    <div class=\"media-heading\"><a href=\"/products/$urlTitle\">$title ($!{tickerSymbol})</a></div>\n" +
                    "\t\t<img src=\"http://chart.finance.yahoo.com/c/5b/v/$!{tickerSymbol}?lang=en-US&region=US&width=300&height=180\" alt=\"Chart for $!{tickerSymbol}\" width=\"100%\">\n" +
                    "\t</div>\n" +
                    "</div>\n" +
                    "<div class=\"row\">\n" +
                    "\t<div class=\"col-sm-12\">\n" +
                    "\t\t<div class=\"media-body\">\n" +
                    "\t\t\t<table class=\"table product-info\">\n" +
                    "\t\t\t\t<tr>\n" +
                    "\t\t\t\t\t<td>$!{tickerSymbol}</td>\n" +
                    "\t\t\t\t\t<td><span class=\"price\" id=\"price$!{tickerSymbol}MD\"></span></td>\n" +
                    "                    <td><span id=\"change$!{tickerSymbol}MD\"></span></td>\n" +
                    "\t\t\t\t</tr>\n" +
                    "\t\t\t</table>\n" +
                    "\t\t</div>\n" +
                    "\t</div>\n" +
                    "</div>\n" +
                    "\n" +
                    "<hr>"));
            fileAsset.setFileName(title+".vtl");

            return workflowAPI.fireContentWorkflow(fileAsset,
                    new ContentletDependencies.Builder()
                            .indexPolicy(IndexPolicy.FORCE)
                            .modUser(APILocator.systemUser())
                            .workflowActionId(saveAction)
                            .build());
        } catch (IOException e) {
            fail(e.getMessage());
        }

        return null;
    }


    private static Contentlet createNewsVTL(final Folder testContainerFolder, final Host defaultHost) throws DotSecurityException, DotDataException {

        try {

            newsLikeContentType = TestDataUtils.getNewsLikeContentType();

            final WorkflowAPI workflowAPI   = APILocator.getWorkflowAPI();
            final FileAsset fileAsset       = new FileAsset();
            final WorkflowAction saveAction = workflowAPI.findAction(SystemWorkflowConstants.WORKFLOW_SAVE_PUBLISH_ACTION_ID, APILocator.systemUser());
            final String title              = newsLikeContentType.name(); //"news";

            fileAsset.setContentTypeId(APILocator.getContentTypeAPI(APILocator.systemUser()).find(FileAssetAPI.BINARY_FIELD).id());
            fileAsset.setHost(defaultHost.getIdentifier());
            fileAsset.setModUser(APILocator.systemUser().getUserId());
            fileAsset.setModDate(new Date());
            fileAsset.setTitle(title);
            fileAsset.setFriendlyName(title);
            fileAsset.setFolder(testContainerFolder.getInode());
            fileAsset.setBinary(FileAssetAPI.BINARY_FIELD, createTempFile(title,"<div class=\"media\">\n" +
                    "    <div class=\"media-body\">\n" +
                    "        <span class=\"label label-default pull-right\">News</span>\n" +
                    "    \t<div class=\"media-heading\"><a href=\"/news/$urlTitle\">$title</a></div>\n" +
                    "    \t<div class=\"media-subheading\">\n" +
                    "            <time datetime=\"$date.format('yyyy-MM-dd',$!{sysPublishDate})\">$date.format('MMM dd yyyy', ${sysPublishDate}) at $date.format('HH:mm z', $!{sysPublishDate})</time>\n" +
                    "        </div>\n" +
                    "\t</div>\n" +
                    "</div>\n" +
                    "\n" +
                    "<hr>"));
            fileAsset.setFileName(title+".vtl");

            return workflowAPI.fireContentWorkflow(fileAsset,
                    new ContentletDependencies.Builder()
                            .indexPolicy(IndexPolicy.FORCE)
                            .modUser(APILocator.systemUser())
                            .workflowActionId(saveAction)
                            .build());
        } catch (IOException e) {
            fail(e.getMessage());
        }

        return null;
    }

    private static Contentlet createDocumentVTL(final Folder testContainerFolder, final Host defaultHost) throws DotSecurityException, DotDataException {

        try {

            documentLikeContentType = TestDataUtils.getDocumentLikeContentType();

            final WorkflowAPI workflowAPI   = APILocator.getWorkflowAPI();
            final FileAsset fileAsset       = new FileAsset();
            final WorkflowAction saveAction = workflowAPI.findAction(SystemWorkflowConstants.WORKFLOW_SAVE_PUBLISH_ACTION_ID, APILocator.systemUser());
            final String title              =  documentLikeContentType.name(); //"document";

            fileAsset.setContentTypeId(APILocator.getContentTypeAPI(APILocator.systemUser()).find(FileAssetAPI.BINARY_FIELD).id());
            fileAsset.setHost(defaultHost.getIdentifier());
            fileAsset.setModDate(new Date());
            fileAsset.setTitle(title);
            fileAsset.setFriendlyName(title);
            fileAsset.setFolder(testContainerFolder.getInode());
            fileAsset.setBinary(FileAssetAPI.BINARY_FIELD, createTempFile(title,"<div class=\"media\">\n" +
                    "    <span class=\"label label-default pull-right\">Document</span>\n" +
                    "\n" +
                    "    <div class=\"media-heading\">\n" +
                    "        #if ($UtilMethods.isSet(${fileAssetBinaryFileURI})) \n" +
                    "        \t<a href=\"$!{fileAssetBinaryFileURI}?force_download=1&filename=$!{fileAssetBinaryFileTitle}\">${title}</a> \n" +
                    "\t\t#end \n" +
                    "    </div>\n" +
                    "    #if ($UtilMethods.isSet($description1))\n" +
                    "      <div class=\"media-subheading\">$UtilMethods.prettyShortenString(\"$description1\", 125)</div>\n" +
                    "    #end\n" +
                    "</div>\n" +
                    "\n" +
                    "<hr>"));
            fileAsset.setFileName(title+".vtl");

            return workflowAPI.fireContentWorkflow(fileAsset,
                    new ContentletDependencies.Builder()
                            .indexPolicy(IndexPolicy.FORCE)
                            .modUser(APILocator.systemUser())
                            .workflowActionId(saveAction)
                            .build());
        } catch (IOException e) {
            fail(e.getMessage());
        }

        return null;
    }

    private static File createTempFile(final String fileName, final String body) throws IOException {

        final String tempFolder = System.getProperty("java.io.tmpdir");
        File file = null;

        if (!UtilMethods.isSet(tempFolder)) {

            final File tempFile = File.createTempFile(fileName, ".vtl");
            final File newFile  = new File(tempFile.getParent(), fileName + ".vtl");

            if (!newFile.exists()) {
                FileUtil.write(tempFile, body);
                if (!tempFile.renameTo(newFile)) {

                    fail("Couldn't rename the file: " + tempFile + ", to: " + fileName + ".vtl");
                }
            } else {
                FileUtil.write(newFile, body);
            }

            file = newFile;
        } else {

            file = new File(tempFolder, fileName + ".vtl");
            FileUtil.write(file, body);
        }
        return file;
    }
}
