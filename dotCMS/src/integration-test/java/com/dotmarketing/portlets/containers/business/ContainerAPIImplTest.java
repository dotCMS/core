package com.dotmarketing.portlets.containers.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
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
import org.apache.felix.framework.OSGIUtil;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test of {@link ContainerAPIImpl}
 */
public class ContainerAPIImplTest extends IntegrationTestBase  {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        OSGIUtil.getInstance().initializeFramework(Config.CONTEXT);
    }

    @Test
    public void getContentTypesInContainer() throws DotDataException, DotSecurityException {
        Container container = null;
        ContentType contentType = null;

        User user = APILocator.getUserAPI().getUsersByNameOrEmail("bill@dotcms.com", 0, 1).get(0);
        User adminUser = APILocator.getUserAPI().getUsersByNameOrEmail("admin@dotcms.com", 0, 1).get(0);

        try {
            contentType = createContentType(adminUser);
            container = createContainer(user, adminUser, contentType);

            ContainerAPIImpl containerAPI = new ContainerAPIImpl();
            List<ContentType> contentTypesInContainer = containerAPI.getContentTypesInContainer(user, container);

            assertEquals(1, contentTypesInContainer.size());
            assertEquals("Document", ((ContentType) contentTypesInContainer.get(0)).name());
        } finally {
            HibernateUtil.startTransaction();
            if (container != null) {
                APILocator.getContainerAPI().delete(container, adminUser, false);
            }

            if (contentType != null) {
                APILocator.getContentTypeAPI(adminUser).delete(contentType);
            }
            HibernateUtil.commitTransaction();
        }
    }

    private Container createContainer(User user, User adminUser, ContentType contentType) throws DotDataException, DotSecurityException {
        HibernateUtil.startTransaction();
        Host defaultHost = APILocator.getHostAPI().findDefaultHost( user, false );
        ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);

        Container container = new Container();
        container.setFriendlyName("test container");
        container.setTitle("this is the title");
        container.setMaxContentlets(5);
        container.setPreLoop("preloop code");
        container.setPostLoop("postloop code");

        List<ContainerStructure> containerStructures = new ArrayList<ContainerStructure>();

        ContainerStructure containerStructure = new ContainerStructure();
        containerStructure.setStructureId(contentTypeAPI.find("Document").inode());
        containerStructure.setCode("this is the code");

        ContainerStructure containerStructure2 = new ContainerStructure();
        containerStructure2.setStructureId(contentType.inode());
        containerStructure2.setCode("this is the code");

        containerStructures.add(containerStructure);
        containerStructures.add(containerStructure2);

        Container containerSaved = APILocator.getContainerAPI().save(container, containerStructures, defaultHost, adminUser, false);

        HibernateUtil.commitTransaction();

        return containerSaved;
    }

    private ContentType createContentType(User user) throws DotSecurityException, DotDataException {
        Host host = APILocator.getHostAPI().findDefaultHost(user, false);

        Folder folder = APILocator.getFolderAPI()
                .createFolders("/folderMoveSourceTest"+System.currentTimeMillis(), host, user, false);

        ContentType contentType = ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                .description("description")
                .folder(folder.getInode()).host(host.getInode())
                .name("ContentTypeTesting")
                .owner("owner")
                .variable("velocityVarNameTesting")
                .build();


        return APILocator.getContentTypeAPI(user).save(contentType);
    }

    @Test
    public void test_get_container_by_folder_found() throws DotDataException, DotSecurityException {

        final Host defaultHost  = APILocator.getHostAPI().findDefaultHost( APILocator.systemUser(), false );
        this.checkApplicationContainerFolder(defaultHost);
        final ContainerAPI containerAPI = APILocator.getContainerAPI();
        final FolderAPI folderAPI       = APILocator.getFolderAPI();
        final Folder    folder          = folderAPI.findFolderByPath
                (Constants.CONTAINER_FOLDER_PATH + "/testcontainer", defaultHost,
                        APILocator.systemUser(), false);

        final Container container = containerAPI.getContainerByFolder(folder, APILocator.systemUser(), false);

        assertNotNull(container);
        assertNotNull(container.getInode());
        assertTrue   (container instanceof FileAssetContainer);
        assertEquals ("Test Container", container.getTitle());
        final List<FileAsset> fileAssets =  FileAssetContainer.class.cast(container)
                .getContainerStructuresAssets();

        assertNotNull(fileAssets);
        assertEquals(3, fileAssets.size());
        assertTrue(fileAssets.stream().anyMatch(fileAsset -> fileAsset.getFileName().equals("news.vtl")));
        assertTrue(fileAssets.stream().anyMatch(fileAsset -> fileAsset.getFileName().equals("products.vtl")));
        assertTrue(fileAssets.stream().anyMatch(fileAsset -> fileAsset.getFileName().equals("document.vtl")));

        final List<ContainerStructure> containerStructures = containerAPI.getContainerStructures(container);
        assertNotNull(containerStructures);
        assertEquals(3, containerStructures.size());
    }

    @Test(expected = NotFoundInDbException.class)
    public void test_find_container_not_found() throws DotDataException, DotSecurityException {

        final Host defaultHost  = APILocator.getHostAPI().findDefaultHost( APILocator.systemUser(), false );
        this.checkApplicationContainerFolder(defaultHost);
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
        this.checkApplicationContainerFolder(defaultHost);
        final ContainerAPI containerAPI = APILocator.getContainerAPI();
        final List<Container> containers = containerAPI.findAllContainers(APILocator.systemUser(), false);

        assertNotNull(containers);
        assertTrue(containers.size() > 0);
        assertTrue(containers.stream().anyMatch(container -> container instanceof FileAssetContainer));
    }

    @Test (expected = NotFoundInDbException.class)
    public void test_get_live_not_found() throws DotDataException, DotSecurityException {

        final Host defaultHost  = APILocator.getHostAPI().findDefaultHost( APILocator.systemUser(), false );
        this.checkApplicationContainerFolder(defaultHost);

        Contentlet contentlet = null;

        try {

            final ContainerAPI containerAPI = APILocator.getContainerAPI();
            final FolderAPI folderAPI       = APILocator.getFolderAPI();
            final Folder    folder          = folderAPI.findFolderByPath
                    (Constants.CONTAINER_FOLDER_PATH + "/testcontainer", defaultHost,
                            APILocator.systemUser(), false);

            final Container container = containerAPI.getContainerByFolder(folder, APILocator.systemUser(), false);

            if (container.isLive()) {

                contentlet = this.unpublish (container.getInode());
            }

            containerAPI.getLiveContainerById(container.getIdentifier(), APILocator.systemUser(), false);
        } finally {

            this.publish(contentlet);
        }
    }

    @Test
    public void test_get_working_found() throws DotDataException, DotSecurityException {

        final Host defaultHost  = APILocator.getHostAPI().findDefaultHost( APILocator.systemUser(), false );
        this.checkApplicationContainerFolder(defaultHost);

        final ContainerAPI containerAPI = APILocator.getContainerAPI();
        final FolderAPI folderAPI       = APILocator.getFolderAPI();
        final Folder    folder          = folderAPI.findFolderByPath
                (Constants.CONTAINER_FOLDER_PATH + "/testcontainer", defaultHost,
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

        final WorkflowAPI workflowAPI        = APILocator.getWorkflowAPI();
        final ContentletAPI contentletAPI    = APILocator.getContentletAPI();
        final WorkflowAction unpublishAction = workflowAPI.findAction
                (SystemWorkflowConstants.WORKFLOW_UNPUBLISH_ACTION_ID, APILocator.systemUser());

        final Contentlet contentlet = contentletAPI.find(containerInode, APILocator.systemUser(), false);

        return workflowAPI.fireContentWorkflow(contentlet,
                new ContentletDependencies.Builder()
                        .indexPolicy(IndexPolicy.FORCE)
                        .workflowActionId(unpublishAction)
                        .modUser(APILocator.systemUser())
                        .build());
    }

    @Test
    public void test_find_live_found() throws DotDataException, DotSecurityException {

        final Host defaultHost  = APILocator.getHostAPI().findDefaultHost( APILocator.systemUser(), false );
        this.checkApplicationContainerFolder(defaultHost);
        // todo: check containers

    }

    @WrapInTransaction
    private  void checkApplicationContainerFolder (final Host defaultHost) {

        final FolderAPI folderAPI = APILocator.getFolderAPI();
        try {
            final Folder    folder    = folderAPI.findFolderByPath
                    (Constants.CONTAINER_FOLDER_PATH, defaultHost, APILocator.systemUser(), true);

            if (null == folder || !UtilMethods.isSet(folder.getIdentifier())) {

                this.creatApplicationContainerFolder(defaultHost);
                this.createFileAssetContainerForTesting(defaultHost);
            }

            if (!this.existsFileAssetContainerForTesting(defaultHost)) {

                this.createFileAssetContainerForTesting(defaultHost);
            }
        } catch (DotDataException | DotSecurityException e) {

            this.creatApplicationContainerFolder(defaultHost);
            this.createFileAssetContainerForTesting(defaultHost);
        }
    }

    private boolean existsFileAssetContainerForTesting(final Host defaultHost) {

        final FolderAPI    folderAPI    = APILocator.getFolderAPI();
        final FileAssetAPI fileAssetAPI = APILocator.getFileAssetAPI();
        boolean exists = false;
        try {
            final Folder    folder    = folderAPI.findFolderByPath
                    (Constants.CONTAINER_FOLDER_PATH + "/testcontainer", defaultHost, APILocator.systemUser(), true);

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

    private void creatApplicationContainerFolder(final Host defaultHost) {

        final FolderAPI folderAPI = APILocator.getFolderAPI();

        try {

            folderAPI.createFolders(Constants.CONTAINER_FOLDER_PATH, defaultHost, APILocator.systemUser(), true);
        } catch (DotDataException | DotSecurityException e) {
            fail("Couldn't create the " + Constants.CONTAINER_FOLDER_PATH);
        }
    }

    private void createFileAssetContainerForTesting(final Host defaultHost) {

        final Folder testContainerFolder = this.createTestContainerFolder(defaultHost);

        try {
            final Contentlet container = this.createContainerVTL(testContainerFolder, defaultHost);
            final Contentlet document  = this.createDocumentVTL (testContainerFolder, defaultHost);
            final Contentlet news      = this.createNewsVTL     (testContainerFolder, defaultHost);
            final Contentlet products  = this.createProductsVTL (testContainerFolder, defaultHost);

            assertNotNull(container);
            assertNotNull(document);
            assertNotNull(news);
            assertNotNull(products);
        } catch (DotSecurityException | DotDataException e) {
            fail(e.getMessage());
        }
    }

    private Folder createTestContainerFolder(final Host defaultHost) {

        final FolderAPI folderAPI        = APILocator.getFolderAPI();
        try {
            return folderAPI.createFolders(Constants.CONTAINER_FOLDER_PATH + "/testcontainer", defaultHost, APILocator.systemUser(), true);
        } catch (DotDataException | DotSecurityException e) {
            fail("Couldn't create the " + Constants.CONTAINER_FOLDER_PATH + "/testcontainer");
        }
        return null;
    }

    private Contentlet createContainerVTL(final Folder testContainerFolder, final Host defaultHost) throws DotSecurityException, DotDataException {

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
            fileAsset.setBinary(FileAssetAPI.BINARY_FIELD, this.createTempFile(title,"$dotJSON.put(\"title\", \"Test Container\")\n" +
                                                    "$dotJSON.put(\"max_contentlets\", 25)\n" +
                                                    "$dotJSON.put(\"notes\", \"Medium Column:Blog,Events,Generic,Location,Media,News,Documents,Products\")\n"));

            return workflowAPI.fireContentWorkflow(fileAsset,
                    new ContentletDependencies.Builder()
                            .indexPolicy(IndexPolicy.FORCE)
                            .workflowActionId(saveAction)
                            .modUser(APILocator.systemUser())
                            .build());
        } catch (IOException e) {
            fail(e.getMessage());
        }

        return null;
    }

    private Contentlet createProductsVTL(final Folder testContainerFolder, final Host defaultHost) throws DotSecurityException, DotDataException {

        try {

            final WorkflowAPI workflowAPI   = APILocator.getWorkflowAPI();
            final FileAsset fileAsset       = new FileAsset();
            final WorkflowAction saveAction = workflowAPI.findAction(SystemWorkflowConstants.WORKFLOW_SAVE_PUBLISH_ACTION_ID, APILocator.systemUser());
            final String title              = "products";

            fileAsset.setContentTypeId(APILocator.getContentTypeAPI(APILocator.systemUser()).find(FileAssetAPI.BINARY_FIELD).id());
            fileAsset.setHost(defaultHost.getIdentifier());
            fileAsset.setModUser(APILocator.systemUser().getUserId());
            fileAsset.setModDate(new Date());
            fileAsset.setTitle(title);
            fileAsset.setFriendlyName(title);
            fileAsset.setFolder(testContainerFolder.getInode());
            fileAsset.setBinary(FileAssetAPI.BINARY_FIELD, this.createTempFile(title,"<script>\n" +
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


    private Contentlet createNewsVTL(final Folder testContainerFolder, final Host defaultHost) throws DotSecurityException, DotDataException {

        try {

            final WorkflowAPI workflowAPI   = APILocator.getWorkflowAPI();
            final FileAsset fileAsset       = new FileAsset();
            final WorkflowAction saveAction = workflowAPI.findAction(SystemWorkflowConstants.WORKFLOW_SAVE_PUBLISH_ACTION_ID, APILocator.systemUser());
            final String title              = "news";

            fileAsset.setContentTypeId(APILocator.getContentTypeAPI(APILocator.systemUser()).find(FileAssetAPI.BINARY_FIELD).id());
            fileAsset.setHost(defaultHost.getIdentifier());
            fileAsset.setModUser(APILocator.systemUser().getUserId());
            fileAsset.setModDate(new Date());
            fileAsset.setTitle(title);
            fileAsset.setFriendlyName(title);
            fileAsset.setFolder(testContainerFolder.getInode());
            fileAsset.setBinary(FileAssetAPI.BINARY_FIELD, this.createTempFile(title,"<div class=\"media\">\n" +
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

    private Contentlet createDocumentVTL(final Folder testContainerFolder, final Host defaultHost) throws DotSecurityException, DotDataException {

        try {

            final WorkflowAPI workflowAPI   = APILocator.getWorkflowAPI();
            final FileAsset fileAsset       = new FileAsset();
            final WorkflowAction saveAction = workflowAPI.findAction(SystemWorkflowConstants.WORKFLOW_SAVE_PUBLISH_ACTION_ID, APILocator.systemUser());
            final String title              = "document";

            fileAsset.setContentTypeId(APILocator.getContentTypeAPI(APILocator.systemUser()).find(FileAssetAPI.BINARY_FIELD).id());
            fileAsset.setHost(defaultHost.getIdentifier());
            fileAsset.setModDate(new Date());
            fileAsset.setTitle(title);
            fileAsset.setFriendlyName(title);
            fileAsset.setFolder(testContainerFolder.getInode());
            fileAsset.setBinary(FileAssetAPI.BINARY_FIELD, this.createTempFile(title,"<div class=\"media\">\n" +
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

    private File createTempFile(final String fileName, final String body) throws IOException {

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
