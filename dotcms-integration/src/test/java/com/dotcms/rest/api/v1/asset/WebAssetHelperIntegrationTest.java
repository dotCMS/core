package com.dotcms.rest.api.v1.asset;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.VariantDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.rest.api.v1.asset.view.AssetVersionsView;
import com.dotcms.rest.api.v1.asset.view.AssetView;
import com.dotcms.rest.api.v1.asset.view.FolderView;
import com.dotcms.rest.api.v1.asset.view.WebAssetView;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import io.vavr.control.Try;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.RandomStringUtils;
import org.glassfish.jersey.internal.util.Base64;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class WebAssetHelperIntegrationTest {

    public static final String ASSET_PATH_TEMPLATE = "//%s/%s/%s/%s";
    static Host host;
    static File testFile;
    static File empty;

    static Folder foo;
    static Folder bar;

    static Language language;
    static Language defaultLanguage;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        language = new LanguageDataGen().nextPersisted();

        host = new SiteDataGen().nextPersisted(true);
        foo = new FolderDataGen().site(host).name("foo").nextPersisted();
        bar = new FolderDataGen().parent(foo).name("bar").nextPersisted();

        testFile = FileUtil.createTemporaryFile("lol", ".txt", RandomStringUtils.random(1000));
        empty = FileUtil.createTemporaryFile("empty", ".txt");

        final Contentlet contentlet = new FileAssetDataGen(bar, testFile).languageId(
                defaultLanguage.getId()).nextPersisted();

        final Variant variant = new VariantDataGen().nextPersisted();

        FileAssetDataGen.createNewVersion(contentlet, variant, language, Map.of());

    }

    String assetPath(){
        return String.format(ASSET_PATH_TEMPLATE, host.getHostname(), foo.getName(),
                bar.getName(), testFile.getName());
    }

    String emptyFilePath(){
        return String.format(ASSET_PATH_TEMPLATE, host.getHostname(), foo.getName(),
                bar.getName(), empty.getName());
    }

    String parentFolderPath(){
        return String.format(ASSET_PATH_TEMPLATE, host.getHostname(), foo.getName(),
                bar.getName(), "");
    }

    String nonExistingFolderPath(){
        return String.format(ASSET_PATH_TEMPLATE, host.getHostname(), foo.getName(),
                "/lol", "");
    }

    String rootFolderPath(){
        return String.format("//%s/", host.getHostname());
    }

    /**
     *  Method to test :  {@link WebAssetHelper#getAssetInfo(String, User)}
     *  Given Scenario: We submit a valid path to an assets file in two versions each in a different language
     *  Expected Result: We get the asset info
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void TestGetFolderInfo() throws DotDataException, DotSecurityException {
        final String folderPath = parentFolderPath();
        Logger.info(this, "TestGetFolderInfo  ::  " +folderPath );
        WebAssetHelper webAssetHelper = WebAssetHelper.newInstance();
        final WebAssetView assetInfo = webAssetHelper.getAssetInfo(folderPath,
                APILocator.systemUser());
        Assert.assertNotNull(assetInfo);
        Assert.assertTrue(assetInfo instanceof FolderView);
        FolderView folderView = (FolderView) assetInfo;
        Assert.assertEquals(bar.getName(), folderView.name());

        final String langString1 = defaultLanguage.toString();

        Assert.assertNotNull(folderView.assets().versions());

        final long count = folderView.assets().versions().stream()
                .filter(version -> langString1.equalsIgnoreCase(version.lang()))
                .filter(version -> testFile.getName().equalsIgnoreCase(version.name())).count();

        Assert.assertEquals(1,count);

    }


    /**
     * Method to test :  {@link WebAssetHelper#getAssetInfo(String, User)}
     * Given Scenario: We submit an invalid path
     * Expected Result: 404 Not Found in the form of a NotFoundInDbException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test(expected = NotFoundInDbException.class)
    public void TestGetNonExistingFolderInfo() throws DotDataException, DotSecurityException {
        String folderPath = nonExistingFolderPath();
        Logger.info(this, "TestGetFolderInfo  ::  " +folderPath );
        WebAssetHelper webAssetHelper = WebAssetHelper.newInstance();
        final WebAssetView assetInfo = webAssetHelper.getAssetInfo(folderPath,
                APILocator.systemUser());
    }

    /**
     * Method to test :  {@link WebAssetHelper#getAssetInfo(String, User)}
     * Given Scenario: We submit a valid path using a limited user
     * Expected Result: We should not get the asset info back but a Security Exception
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void TestGetFolderInfoWithLimitedUser() throws DotDataException, DotSecurityException {
        final User chrisPublisherUser = TestUserUtils.getChrisPublisherUser(host);

        //Give him access to the site and parent folder
        final Permission siteReadPermissions = new Permission(host.getPermissionId(),
                APILocator.getRoleAPI().getUserRole(chrisPublisherUser).getId(), PermissionAPI.PERMISSION_READ );
        APILocator.getPermissionAPI().save(siteReadPermissions, host, APILocator.systemUser(), false);

        final Folder sub = new FolderDataGen().parent(bar).name("restricted-folder-1").nextPersisted();
        final Folder folder = new FolderDataGen().site(host).parent(sub)
                .name("restricted-sub-folder-1").nextPersisted();
        String folderPath = parentFolderPath() + sub.getName() + "/" + folder.getName() + "/";
        WebAssetHelper webAssetHelper = WebAssetHelper.newInstance();
        Exception exception = null;
        try {
            webAssetHelper.getAssetInfo(folderPath, chrisPublisherUser);
        }catch (Exception e){
            exception = e;
        }
        Assert.assertNotNull(exception);
        Assert.assertTrue(exception instanceof DotSecurityException);
    }


    /**
     * Method to test :  {@link WebAssetHelper#getAssetInfo(String, User)}
     * Given Scenario: We submit a valid path using a limited user
     * Expected Result: We should not get the asset info back but a Security Exception
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void TestGetSiteInfoLimitedUser() throws DotDataException, DotSecurityException {
        final Folder subBar = new FolderDataGen().parent(bar).name("bar2").nextPersisted();
        new FolderDataGen().site(host).parent(subBar).name("bar2-1").nextPersisted();
        final String folderPath = parentFolderPath() +  subBar.getName() + "/";
        final User chrisPublisherUser = TestUserUtils.getChrisPublisherUser(host);
        Logger.info(this, "TestGetFolderInfo  ::  " +folderPath );
        WebAssetHelper webAssetHelper = WebAssetHelper.newInstance();
        final WebAssetView assetInfo = webAssetHelper.getAssetInfo(folderPath, APILocator.systemUser());
        Assert.assertNotNull(assetInfo);

        final Permission siteReadPermissions = new Permission(host.getPermissionId(),
                APILocator.getRoleAPI().getUserRole(chrisPublisherUser).getId(), PermissionAPI.PERMISSION_READ );
        APILocator.getPermissionAPI().save(siteReadPermissions, host, APILocator.systemUser(), false);

        Exception exception = null;
        try {
            webAssetHelper.getAssetInfo(folderPath, chrisPublisherUser);
        }catch (Exception e){
            exception = e;
        }
        Assert.assertNotNull(exception);
        Assert.assertTrue(exception instanceof DotSecurityException);
    }

    /**
     * Method to test :  {@link WebAssetHelper#getAssetInfo(String, User)}
     * Given Scenario: Assuming a limited user has no access to sub-folders we want to test that they can't access them.
     * So we give our limited user Chris access to the parent folder /foo/bar/ but not to the sub-folder /foo/bar/bar2/
     * then we feed our helper method getAssetInfo with a valid folder path /foo/bar/
     * Expected Result: We should get access to the root folder but not to the sub-folders
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void TestLimitedUserHasNoAccessToSubFolders() throws DotDataException, DotSecurityException {
        //Create a sub-folder
        final String folderName = String.format("bar-%s", RandomStringUtils.randomAlphabetic(5));
        final Folder subBar = new FolderDataGen().parent(bar).name(folderName).nextPersisted();
        final String subFolderName = String.format("sub-bar-%s", RandomStringUtils.randomAlphabetic(5));
        new FolderDataGen().site(host).parent(subBar).name(subFolderName).nextPersisted();
        final String folderPath = parentFolderPath();
        //Bring in our limited user
        final User chrisPublisherUser = TestUserUtils.getChrisPublisherUser(host);
        //Give him access to the parent folder
        final Permission siteReadPermissions = new Permission(host.getPermissionId(),
                APILocator.getRoleAPI().getUserRole(chrisPublisherUser).getId(), PermissionAPI.PERMISSION_READ );
        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        permissionAPI.save(siteReadPermissions, host, APILocator.systemUser(), false);

        final Permission fooReadPermissions = new Permission(bar.getPermissionId(),
                APILocator.getRoleAPI().getUserRole(chrisPublisherUser).getId(), PermissionAPI.PERMISSION_READ );
        permissionAPI.save(fooReadPermissions,  bar, APILocator.systemUser(), false);
        //Here we break the chain by not giving him access to the sub-folder bar2
        permissionAPI.resetPermissionsUnder(bar);

        Logger.info(this, "TestGetFolderInfo  ::  " +folderPath );
        WebAssetHelper webAssetHelper = WebAssetHelper.newInstance();

        //Test we can access the parent folder
        final ResolvedAssetAndPath assetAndPath = AssetPathResolver.newInstance()
                .resolve(folderPath, chrisPublisherUser);

        //We should be able to access the parent folder
        //Now request the asset info
        final WebAssetView assetInfo = webAssetHelper.getAssetInfo(assetAndPath,false, chrisPublisherUser);
        // We should get the asset info back
        Assert.assertNotNull(assetInfo);
        Assert.assertTrue(assetInfo instanceof FolderView);
        FolderView folderView = (FolderView) assetInfo;
        //But no sub-folders should be returned
        Assert.assertNotNull(folderView.subFolders());
        Assert.assertTrue(folderView.subFolders().isEmpty());
    }

    /**
     * Method to test : {@link WebAssetHelper#saveUpdateAsset(HttpServletRequest, FileUploadData, User)}
     * Given Scenario: We submit a valid path and a file
     * Expected Result: We get the asset info back as proof of success
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     */
    @Test
    public void TestUploadFile() throws DotDataException, DotSecurityException, IOException {

        final MockHeaderRequest request = getMockHeaderRequest();

        File newTestFile = FileUtil.createTemporaryFile("lol", ".txt", RandomStringUtils.random(1000));

        final FormDataContentDisposition formDataContentDisposition = getFormDataContentDisposition(
                newTestFile);

        final String path = parentFolderPath() + newTestFile.getName();

        final FileUploadDetail detail = new FileUploadDetail(path, language.toString(), true);

        final String langString2 = language.toString();

        try(final InputStream inputStream = Files.newInputStream(newTestFile.toPath())){
            WebAssetHelper webAssetHelper = WebAssetHelper.newInstance();
            FileUploadData form = new FileUploadData();
            form.setFileInputStream(inputStream);
            form.setContentDisposition(formDataContentDisposition);
            form.setAssetPath(path);
            form.setDetail(detail);

            final WebAssetView assetInfo = webAssetHelper.saveUpdateAsset(request, form, APILocator.systemUser());
            Assert.assertNotNull(assetInfo);
            Assert.assertTrue(assetInfo instanceof AssetView);
            AssetView assetView = (AssetView) assetInfo;
            Assert.assertTrue(assetView.lang().equalsIgnoreCase(langString2));
            Assert.assertEquals(newTestFile.getName(), assetView.name());
            Assert.assertTrue(assetView.live());
        }

    }

    /**
     * Method to test : {@link WebAssetHelper#saveUpdateAsset(HttpServletRequest, FileUploadData, User)}
     * Given Scenario: We submit a valid path using a limited user with view permission
     * Expected Result: We should get the asset info back
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     */
    @Test
    public void TestGetFolderInfoOnLimitedUserWithViewPermission() throws DotDataException, DotSecurityException, IOException {

        final User chrisPublisherUser = TestUserUtils.getChrisPublisherUser(host);

        //Give him access to the site and parent folder
        final Permission siteReadPermissions = new Permission(host.getPermissionId(),
                APILocator.getRoleAPI().getUserRole(chrisPublisherUser).getId(), PermissionAPI.PERMISSION_READ );
        APILocator.getPermissionAPI().save(siteReadPermissions, host, APILocator.systemUser(), false);

        //We need to assign Chris Publisher view permissions to the parent folder.  But that's it.
        // He should not be able to create new folders
        final String subFolderName = String.format("sub-bar-%s", RandomStringUtils.randomAlphabetic(5));
        final Folder subBar = new FolderDataGen().parent(bar).name(subFolderName).nextPersisted();

        final Permission viewPermission = new Permission(subBar.getPermissionId(),
                APILocator.getRoleAPI().getUserRole(chrisPublisherUser).getId(), PermissionAPI.PERMISSION_READ );
        APILocator.getPermissionAPI().save(viewPermission, subBar, APILocator.systemUser(), false);

        final MockHeaderRequest request = getMockHeaderRequest();
        File newTestFile = FileUtil.createTemporaryFile("lol", ".txt", RandomStringUtils.random(1000));
        final FormDataContentDisposition formDataContentDisposition = getFormDataContentDisposition(newTestFile);
        //We feed the component with a non-existing path. it can create it when instructed to do so. But this can open a security hole if not properly handled

        final String path = parentFolderPath() + subBar.getName() + "/";
        final FileUploadDetail detail = new FileUploadDetail(path, language.toString(), true);
        WebAssetHelper webAssetHelper = WebAssetHelper.newInstance();
        FileUploadData form = new FileUploadData();
        // No file input stream means the request only contains a folder path and no file
        form.setFileInputStream(null);
        form.setContentDisposition(formDataContentDisposition);
        form.setAssetPath(path);
        form.setDetail(detail);

        final WebAssetView assetInfo = webAssetHelper.saveUpdateAsset(request, form, chrisPublisherUser);
        Assert.assertNotNull(assetInfo);
    }

    /**
     * Method to test : {@link WebAssetHelper#saveUpdateAsset(HttpServletRequest, FileUploadData, User)}
     * Given Scenario: We submit a valid path using a limited user with view permission
     * Expected Result: We should get a security exception since the user has no permission to create new folders
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     */
    @Test
    public void TestUploadFileWithUserWithViewPermission() throws DotDataException, DotSecurityException, IOException {

        final User chrisPublisherUser = TestUserUtils.getChrisPublisherUser(host);

        //Give him access to the site and parent folder
        final Permission siteReadPermissions = new Permission(host.getPermissionId(),
                APILocator.getRoleAPI().getUserRole(chrisPublisherUser).getId(), PermissionAPI.PERMISSION_READ );
        APILocator.getPermissionAPI().save(siteReadPermissions, host, APILocator.systemUser(), false);

        //We need to assign Chris Publisher view permissions to the parent folder.  But that's it.
        // He should not be able to create new folders
        final String subFolderName = String.format("sub-bar-%s", RandomStringUtils.randomAlphabetic(5));
        final Folder subBar = new FolderDataGen().parent(bar).name(subFolderName).nextPersisted();

        final Permission viewPermission = new Permission(subBar.getPermissionId(),
                APILocator.getRoleAPI().getUserRole(chrisPublisherUser).getId(), PermissionAPI.PERMISSION_PUBLISH );
        APILocator.getPermissionAPI().save(viewPermission, subBar, APILocator.systemUser(), false);

        final MockHeaderRequest request = getMockHeaderRequest();
        File newTestFile = FileUtil.createTemporaryFile("lol", ".txt", RandomStringUtils.random(1000));
        final FormDataContentDisposition formDataContentDisposition = getFormDataContentDisposition(newTestFile);
        //We feed the component with a non-existing path. it can create it when instructed to do so. But this can open a security hole if not properly handled

        final String path = parentFolderPath() + subBar.getName() + "/" ;
        final FileUploadDetail detail = new FileUploadDetail(path, language.toString(), true);

        try(final InputStream inputStream = Files.newInputStream(newTestFile.toPath())){
            final WebAssetHelper webAssetHelper = WebAssetHelper.newInstance();
            final FileUploadData form = new FileUploadData();
            form.setFileInputStream(inputStream);
            form.setContentDisposition(formDataContentDisposition);
            form.setAssetPath(path);
            form.setDetail(detail);
            Exception exception = null;
            try {
                 webAssetHelper.saveUpdateAsset(request, form, chrisPublisherUser);
            }catch (Exception e){
                exception = e;
            }
            Assert.assertNotNull(exception);
            Assert.assertTrue(exception instanceof DotSecurityException);
        }

    }


    /**
     * We're testing that even when no System Workflow is assigned to FileAsset we can still save content
     * Given scenario:  We remove system wf from FileAsset then we create a new file using WebAssetHelper
     * Expected Results: Even when no system-workflow is available we should be able to save content
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     */
    @Test
    public void Test_Save_FilAsset_No_System_Workflow() throws DotDataException, DotSecurityException, IOException {
        final User user = APILocator.systemUser();
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);
        ContentType fileAssetContentType = contentTypeAPI.find("FileAsset");
        final WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
        final List<WorkflowScheme> schemesForContentType = workflowAPI.findSchemesForContentType(fileAssetContentType);

        try {
            workflowAPI.saveSchemeIdsForContentType(fileAssetContentType, Set.of());
            WebAssetHelper webAssetHelper = WebAssetHelper.newInstance();

            final File newTestFile = FileUtil.createTemporaryFile("lol", ".txt",
                    RandomStringUtils.random(1000));
            final Folder folder = new FolderDataGen().site(host).nextPersisted();
            final Language lang = new LanguageDataGen().nextPersisted();

            final Contentlet contentlet = webAssetHelper.makeFileAsset(newTestFile, host, folder, user, lang);
            final Contentlet savedAsset = webAssetHelper.checkinOrPublish(contentlet, user, true);
            Assert.assertTrue(savedAsset.isLive());
        }finally {
            workflowAPI.saveSchemeIdsForContentType(fileAssetContentType,
                    schemesForContentType.stream().map(WorkflowScheme::getId).collect(Collectors.toSet())
            );
        }
    }

        /**
         * Helper method to create a FormDataContentDisposition object
         * @param newTestFile
         * @return
         */
    private static FormDataContentDisposition getFormDataContentDisposition(File newTestFile) {
        return FormDataContentDisposition
                .name(newTestFile.getName())
                .fileName(newTestFile.getName())
                .size(newTestFile.length())
                .build();
    }

    /**
     * Helper method to create a MockHeaderRequest object
     * @return
     */
    @NotNull
    private static MockHeaderRequest getMockHeaderRequest() {
        final MockHeaderRequest request = new MockHeaderRequest(
                (
                        new MockSessionRequest(new MockAttributeRequest(new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
                ).request()
        );

        request.setHeader("Authorization", "Basic " + new String(Base64.encode("admin@dotcms.com:admin".getBytes())));
        request.setHeader("User-Agent", "Fake-Agent");
        request.setHeader("Host", "localhost");
        request.setHeader("Origin", "localhost");
        request.setAttribute(WebKeys.USER, APILocator.systemUser());
        return request;
    }


    /**
     * Scenario  We had a bug where stuff sent to live under site's root folder would end up in the system folder
     * Expected Result: We send a file to live under the root folder, and it ends up there and not in the system folder.
     * We validate checking the info saved on the identifier table verifying the host_inode is the same host inode and never SYSTEM_HOST
     * Method to test : {@link WebAssetHelper#saveUpdateAsset(HttpServletRequest, FileUploadData, User)}
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     */
     @Test
     public void Test_Upload_File_Under_Root() throws DotDataException, DotSecurityException, IOException {
        final MockHeaderRequest request = getMockHeaderRequest();
        final File newTestFile = FileUtil.createTemporaryFile("robots", ".txt", RandomStringUtils.random(1000));
        final FormDataContentDisposition formDataContentDisposition = getFormDataContentDisposition(newTestFile);
        final String path = rootFolderPath() + newTestFile.getName();

        final FileUploadDetail detail = new FileUploadDetail(path, language.toString(), true);

        try(final InputStream inputStream = Files.newInputStream(newTestFile.toPath())){
            final WebAssetHelper webAssetHelper = WebAssetHelper.newInstance();
            final FileUploadData form = new FileUploadData();
            form.setFileInputStream(inputStream);
            form.setContentDisposition(formDataContentDisposition);
            form.setAssetPath(path);
            form.setDetail(detail);

            final WebAssetView assetInfo = webAssetHelper.saveUpdateAsset(request, form, APILocator.systemUser());
            Assert.assertNotNull(assetInfo);
            Assert.assertTrue(assetInfo instanceof AssetView);
            AssetView assetView = (AssetView) assetInfo;
            Assert.assertTrue(assetView.live());

            @SuppressWarnings("unchecked")
            final List<Map<String,String>> results = new DotConnect().setSQL(
                        "select * from identifier i where i.id = ?"
                    ).addParam(assetView.identifier())
                    .loadResults();

            Assert.assertEquals(1, results.size());
            final Map<String, String> map = results.get(0);
            Assert.assertEquals(newTestFile.getName(), map.get("asset_name"));
            Assert.assertEquals(host.getIdentifier(), map.get("host_inode"));
        }
     }

    /**
     * Method to test : {@link WebAssetHelper#saveUpdateAsset(HttpServletRequest, FileUploadData, User)}
     * Given Scenario: We submit a valid path and a file in two different languages
     * Expected Result: We get the asset info back as proof of success and no exception should arise
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     */
    @Test
    public void Test_Upload_File_In_Multiple_Languages() throws DotDataException, DotSecurityException, IOException {

        final Language secondLang = new LanguageDataGen().nextPersisted();
        final File newTestFile = FileUtil.createTemporaryFile("multi-lang-example", ".txt", RandomStringUtils.random(1000));
        final String path = rootFolderPath() + newTestFile.getName();
        final boolean live = true;

        pushFileForLanguageThenValidate(newTestFile, path, language.toString(), live);
        pushFileForLanguageThenValidate(newTestFile, path, secondLang.toString(), live);
    }

    /**
     * Method to test : {@link WebAssetHelper#saveUpdateAsset(HttpServletRequest, FileUploadData, User)}
     * Given Scenario: We submit a valid path and a file then we archive it
     * Expected Result: We get the asset info back as proof of success and the asset should be unarchived
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     */
    @Test
    public void Test_Upload_File_Archive_Then_Update() throws DotDataException, DotSecurityException, IOException {

        final File newTestFile = FileUtil.createTemporaryFile("archive-me", ".txt", RandomStringUtils.random(1000));
        final String path = rootFolderPath() + newTestFile.getName();
        final boolean live = true;

        pushFileForLanguageThenValidate(newTestFile, path, language.toString(), live);

        final WebAssetHelper webAssetHelper = WebAssetHelper.newInstance();
        final WebAssetView assetInfo = webAssetHelper.getAssetInfo(path, APILocator.systemUser());

        Assert.assertTrue(assetInfo instanceof AssetVersionsView);
        final AssetVersionsView assetVersionsView = (AssetVersionsView) assetInfo;
        AssetView assetView = assetVersionsView.versions().get(0);

        final ContentletAPI contentletAPI = APILocator.getContentletAPI();

        final Contentlet fileAsset = contentletAPI.findContentletByIdentifierAnyLanguage(assetView.identifier()) ;
        contentletAPI.archive(fileAsset, APILocator.systemUser(), false);

        //Pushing again should unarchive
        pushFileForLanguageThenValidate(newTestFile, path, language.toString(), live);

        final Contentlet unarchived = contentletAPI.findContentletByIdentifierAnyLanguage(
                assetView.identifier());
        Assert.assertFalse(unarchived.isArchived());

    }

    /**
     * Method to test : {@link WebAssetHelper#saveUpdateAsset(HttpServletRequest, FileUploadData, User)}
     * Given Scenario: This should emulate the scenario where a file is uploaded and then published several times
     * What we intend to create is several versions of the same asset and then archive it then publish it again
     * Expected Result: We get the asset info back as proof of success and the asset should be unarchived. No version should remain archived
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     */
    @Test
    public void Test_Upload_File_Archive_Then_Publish_Several_Times() throws DotDataException, DotSecurityException, IOException {

        final WebAssetHelper webAssetHelper = WebAssetHelper.newInstance();
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        final File newTestFile = FileUtil.createTemporaryFile("also-archive-me", ".txt", RandomStringUtils.random(1000));
        final String path = rootFolderPath() + newTestFile.getName();

        final Contentlet contentlet = new FileAssetDataGen(newTestFile).title("lol")
                .languageId(language.getId()).host(host).nextPersisted();

        final WebAssetView assetInfo = webAssetHelper.getAssetInfo(path, APILocator.systemUser());
        Assert.assertTrue(assetInfo instanceof AssetVersionsView);
        final AssetVersionsView assetVersionsView = (AssetVersionsView) assetInfo;
        AssetView assetView = assetVersionsView.versions().get(0);
        Assert.assertEquals(contentlet.getIdentifier(), assetView.identifier());

        pushFileForLanguageThenValidate(newTestFile, path, language.toString(), false);
        pushFileForLanguageThenValidate(newTestFile, path, language.toString(), true);

        contentletAPI.archive(contentlet, APILocator.systemUser(), false);

        final List<Contentlet> allVersions = contentletAPI.findAllVersions(
                new Identifier(contentlet.getIdentifier()), APILocator.systemUser(), false);

        //Not all versions here are live or working old versions are neither live nor working, but they all should be archived
        Assert.assertTrue(allVersions.stream().allMatch(c-> Try.of(c::isArchived).getOrElse(false)));
        //Test at least one version is neither live nor working. This is an obvious scenario since we have several version of the same asset
        Assert.assertTrue(allVersions.stream().anyMatch(c-> Try.of(()->!c.isWorking() && !c.isLive()).getOrElse(false)));
        //Now this should generate a new version (obviously unarchived) and should succeed
        pushFileForLanguageThenValidate(newTestFile, path, language.toString(), true);
        //Test nothing remains archived
        //We can still use the same collection of versions since the archive method is calculated on the fly
        Assert.assertTrue(allVersions.stream().noneMatch(c-> Try.of(c::isArchived).getOrElse(false)));
    }



    /**
     * Helper method to Send a file in a given language and validate the result
     * @param newTestFile
     * @param path
     * @param langString
     * @throws IOException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private static void pushFileForLanguageThenValidate(final File newTestFile, final String path, final String langString, final boolean live) throws IOException, DotDataException, DotSecurityException {
        final FormDataContentDisposition formDataContentDisposition = getFormDataContentDisposition(newTestFile);
        try(final InputStream inputStream = Files.newInputStream(newTestFile.toPath())){
            final WebAssetHelper webAssetHelper = WebAssetHelper.newInstance();
            final MockHeaderRequest request = getMockHeaderRequest();
            final FileUploadData form = new FileUploadData();
            form.setFileInputStream(inputStream);
            form.setContentDisposition(formDataContentDisposition);
            form.setAssetPath(path);
            form.setDetail(new FileUploadDetail(path, langString, live));

            final WebAssetView assetInfo = webAssetHelper.saveUpdateAsset(request, form, APILocator.systemUser());
            Assert.assertNotNull(assetInfo);
            Assert.assertTrue(assetInfo instanceof AssetView);
            AssetView assetView = (AssetView) assetInfo;
            Assert.assertEquals(live, assetView.live());
        }
    }

    /**
     * Method to test : {@link WebAssetHelper#saveUpdateAsset(HttpServletRequest, FileUploadData, User)}
     * Given Scenario: We submit a valid path and an empty file
     * Expected Result: We get the asset info back as proof of success. We should be able to upload empty files
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     */
    @Test
    public void TestUploadEmptyFile() throws DotDataException, DotSecurityException, IOException {

        final MockHeaderRequest request = getMockHeaderRequest();

        File newTestFile = FileUtil.createTemporaryFile("lol", ".txt");

        Assert.assertEquals(0, newTestFile.length());

        final FormDataContentDisposition formDataContentDisposition = getFormDataContentDisposition(
                newTestFile);

        final String path = parentFolderPath() + newTestFile.getName();

        final FileUploadDetail detail = new FileUploadDetail(path, language.toString(), true);

        final String langString2 = language.toString();

        try(final InputStream inputStream = Files.newInputStream(newTestFile.toPath())){
            WebAssetHelper webAssetHelper = WebAssetHelper.newInstance();
            FileUploadData form = new FileUploadData();
            form.setFileInputStream(inputStream);
            form.setContentDisposition(formDataContentDisposition);
            form.setAssetPath(path);
            form.setDetail(detail);

            final WebAssetView assetInfo = webAssetHelper.saveUpdateAsset(request, form, APILocator.systemUser());
            Assert.assertNotNull(assetInfo);
            Assert.assertTrue(assetInfo instanceof AssetView);
            AssetView assetView = (AssetView) assetInfo;
            Assert.assertTrue(assetView.lang().equalsIgnoreCase(langString2));
            Assert.assertEquals(newTestFile.getName(), assetView.name());
            Assert.assertTrue(assetView.live());
            final long size = (long)assetView.metadata().get("size");
            Assert.assertEquals(0L, size);

        }

    }

    /**
     * Method to test : {@link WebAssetHelper#getAsset(AssetsRequestForm, User)}
     * Given Scenario: We submit a valid path to retrieve a file
     * Expected Result: We get the asset content back as proof of success
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     */
    @Test
    public void TestDownloadFile() throws DotDataException, DotSecurityException {
        final AssetsRequestForm form = AssetsRequestForm.builder()
                .assetPath(assetPath()).language(defaultLanguage.toString()).live(false)
                .build();
        WebAssetHelper webAssetHelper = WebAssetHelper.newInstance();
        final FileAsset asset = webAssetHelper.getAsset(form, APILocator.systemUser());
        Assert.assertNotNull(asset);
        Assert.assertTrue(asset.getFileAsset().exists());
        Assert.assertEquals(testFile.length(), asset.getFileAsset().length());
    }

    /**
     * Method to test : {@link WebAssetHelper#getAsset(AssetsRequestForm, User)}
     * Given Scenario: We submit a valid path to retrieve a file
     * Expected Result: We get the asset content back as proof of success
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     */
    @Test(expected = NotFoundInDbException.class)
    public void TestDownloadFileDifferentLang() throws DotDataException, DotSecurityException {

        final Language anyLang = new LanguageDataGen().nextPersisted();

        final AssetsRequestForm form = AssetsRequestForm.builder()
                .assetPath(assetPath()).language(anyLang.toString()).live(false)
                .build();
        WebAssetHelper webAssetHelper = WebAssetHelper.newInstance();
        final FileAsset assetContent = webAssetHelper.getAsset(form, APILocator.systemUser());
        Assert.assertNotNull(assetContent);
        Assert.assertTrue(assetContent.getFileAsset().exists());
        Assert.assertEquals(testFile.length(), assetContent.getFileAsset().length());
    }

    /**
     * Method to test : {@link WebAssetHelper#getAsset(AssetsRequestForm, User)}
     * Given Scenario: We submit a valid path for file but using an invalid language
     * Expected Result: We should get BadRequestException
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     */
    @Test(expected = IllegalArgumentException.class)
    public void TestDownloadFileInvalidLanguage() throws DotDataException, DotSecurityException {
        final AssetsRequestForm form = AssetsRequestForm.builder()
                .assetPath(assetPath()).language("nonExisting_lang").live(false)
                .build();
        WebAssetHelper webAssetHelper = WebAssetHelper.newInstance();
        webAssetHelper.getAsset(form, APILocator.systemUser());
    }

    /**
     * Method to test : {@link WebAssetHelper#parseLang(String, boolean)}
     * Given Scenario: We're testing various scenarios here,
     *       First we test sending an invalid lang expect a default lang
     *       Send an empty lang and expect an empty optional
     *       Send a valid lang with no country and expect the same lang back
     * Expected Results: When defaultLangFallback is true, we should get the default lang back
     *                   When defaultLangFallback is false, we should get an empty optional
     *                   When we send a valid lang with no country, we should get the same lang back
     *                   When we send a valid lang with a country, we should get the same lang back
     */
    @Test
    public void Test_Parse_Language(){

        final WebAssetHelper webAssetHelper = WebAssetHelper.newInstance();

        //Upon invalid language when defaultLangFallback is true, we should get the default language back
        final Optional<Language> def = webAssetHelper.parseLang("",true);
        Assert.assertFalse(def.isEmpty());
        Assert.assertEquals(def.get(), defaultLanguage);

        //Upon invalid language when defaultLangFallback is false, we should get an empty optional
        final Optional<Language> empty1 = webAssetHelper.parseLang("",false);
        Assert.assertTrue(empty1.isEmpty());

        final Language countryLessLang = new LanguageDataGen().languageCode("lol").languageName("lol").countryCode("").country("").nextPersisted();
        final Language language1 = new LanguageDataGen().nextPersisted();
        try {
            //Test upon passing a language lacking country code nothing breaks
            final Optional<Language> countryLess = webAssetHelper.parseLang(
                    countryLessLang.getLanguageCode(), false);
            Assert.assertFalse(countryLess.isEmpty());
            Assert.assertEquals(countryLess.get(), countryLessLang);

            //Test that upon passing a valid language with country  we get the same language back
            final Optional<Language> fullLang = webAssetHelper.parseLang(language1.toString(), false);
            Assert.assertEquals(fullLang.get(), language1);

        }finally {
            LanguageDataGen.remove(countryLessLang);
            LanguageDataGen.remove(language1);
        }

    }

    /**
     * Method to test : {@link WebAssetHelper#archiveAsset(String, User)}
     * Given Scenario: We submit a valid path to delete a file
     * Expected Result: We get the asset content back as proof of success
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Archive_File() throws DotDataException, DotSecurityException {
        final WebAssetHelper webAssetHelper = WebAssetHelper.newInstance();

        Folder foo2 = new FolderDataGen().site(host).name("foo2").nextPersisted();
        Folder bar2 = new FolderDataGen().parent(foo2).name("bar2").nextPersisted();

        new FileAssetDataGen(bar2, testFile).languageId(defaultLanguage.getId()).nextPersisted();

        String assetPath = String.format(ASSET_PATH_TEMPLATE, host.getHostname(), foo2.getName(),
                bar2.getName(), testFile.getName());

        webAssetHelper.archiveAsset(assetPath, APILocator.systemUser());

    }


    /**
     * Method to test : {@link WebAssetHelper#deleteAsset(String, User)}
     * Given Scenario: We submit a valid path to delete a file
     * Expected Result: We get the asset content back as proof of success
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Test_Delete_File_Then_Delete_Folder() throws DotDataException, DotSecurityException {
        final WebAssetHelper webAssetHelper = WebAssetHelper.newInstance();

        Folder foo3 = new FolderDataGen().site(host).name("foo3").nextPersisted();
        Folder bar3 = new FolderDataGen().parent(foo3).name("bar3").nextPersisted();

        new FileAssetDataGen(bar3, testFile).languageId(defaultLanguage.getId()).nextPersisted();

        String assetPath = String.format(ASSET_PATH_TEMPLATE, host.getHostname(), foo3.getName(),
                bar3.getName(), testFile.getName());


        webAssetHelper.deleteAsset(assetPath, APILocator.systemUser());

        final String folderPath = assetPath.replaceFirst(testFile.getName(), "");
        webAssetHelper.deleteFolder(folderPath, APILocator.systemUser());

    }


    /**
     * Method to test : {@link WebAssetHelper#getAssetInfo(String, User)}
     * Given Scenario: First we create a file asset then we publish it making it be live and working at the same time.
     * Expected Result: We should one single version of the asset with the same inode for both working and live.
     * Once more versions get added we should see the inode change for the working version but not for the live version.
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     */
    @Test
    public void Test_Retrieve_All_Versions()
            throws DotDataException, DotSecurityException, IOException {

        final File testFile2 = FileUtil.createTemporaryFile("lol", ".txt", RandomStringUtils.random(1000));
        final String path = String.format(ASSET_PATH_TEMPLATE, host.getHostname(), foo.getName(),
                bar.getName(), testFile2.getName());

        final Contentlet contentlet = new FileAssetDataGen(bar, testFile2).languageId(
                defaultLanguage.getId()).nextPersisted();

        ContentletDataGen.publish(contentlet);

        final WebAssetHelper webAssetHelper = WebAssetHelper.newInstance();
        final AssetVersionsView assetInfo = (AssetVersionsView) webAssetHelper.getAssetInfo(path, APILocator.systemUser());

        //At this point both working in and live are the same  inode therefore we should get back only 1 version
        Assert.assertEquals(1, assetInfo.versions().size());
        final AssetView singleVersionedAsset = assetInfo.versions().get(0);
        Assert.assertTrue(singleVersionedAsset.live());
        Assert.assertTrue(singleVersionedAsset.working());

        //Now we create a new version with a brand-new file different from the previous one
        final File testFile3 = FileUtil.createTemporaryFile("new-lol", ".txt", RandomStringUtils.random(1000));
        final Contentlet checkout = ContentletDataGen.checkout(contentlet);
        checkout.getMap().put(Contentlet.TITTLE_KEY, "new title");
        checkout.getMap().put(FileAssetAPI.BINARY_FIELD,testFile3);

        final String newInode = UUIDGenerator.generateUuid();
        checkout.setInode(newInode);
        final Contentlet checkin = ContentletDataGen.checkin(checkout);
        System.out.println(checkin);

        final AssetVersionsView withMultipleVersions = (AssetVersionsView) webAssetHelper.getAssetInfo(path, APILocator.systemUser());
        Assert.assertEquals(2, withMultipleVersions.versions().size());

        Assert.assertEquals(1,
                withMultipleVersions.versions().stream().filter(AssetView::live).count());

        Assert.assertEquals(1,
                withMultipleVersions.versions().stream().filter(assetView -> !assetView.live()).count());

        //Now validate the working state
        Assert.assertEquals(1,
                withMultipleVersions.versions().stream().filter(AssetView::working).count());

        Assert.assertEquals(1,
                withMultipleVersions.versions().stream().filter(assetView -> !assetView.working()).count());

    }

}
