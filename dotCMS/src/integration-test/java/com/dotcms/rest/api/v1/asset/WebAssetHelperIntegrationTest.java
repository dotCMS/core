package com.dotcms.rest.api.v1.asset;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.SiteDataGen;
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
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.RandomStringUtils;
import org.glassfish.jersey.internal.util.Base64;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
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
     * Method to test : {@link WebAssetHelper#saveUpdateAsset(HttpServletRequest, FileUploadData, User)}
     * Given Scenario: We submit a valid path and a file
     * Expected Result: We get the asset info back as proof of success
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     */
    @Test
    public void TestUploadFile() throws DotDataException, DotSecurityException, IOException {


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

        File newTestFile = FileUtil.createTemporaryFile("lol", ".txt", RandomStringUtils.random(1000));

        final FormDataContentDisposition formDataContentDisposition = FormDataContentDisposition
                .name(newTestFile.getName())
                .fileName(newTestFile.getName())
                .size(newTestFile.length())
                .build();

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
     * Given Scenario: We submit a valid path and an empty file
     * Expected Result: We get the asset info back as proof of success. We should be able to upload empty files
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     */
    @Test
    public void TestUploadEmptyFile() throws DotDataException, DotSecurityException, IOException {

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

        File newTestFile = FileUtil.createTemporaryFile("lol", ".txt");

        Assert.assertEquals(0, newTestFile.length());

        final FormDataContentDisposition formDataContentDisposition = FormDataContentDisposition
                .name(newTestFile.getName())
                .fileName(newTestFile.getName())
                .size(newTestFile.length())
                .build();

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

        //Now we create a new version
        final Contentlet checkout = ContentletDataGen.checkout(contentlet);
        checkout.getMap().put(Contentlet.TITTLE_KEY, "new title");

        final String newInode = UUIDGenerator.generateUuid();
        checkout.setInode(newInode);
        ContentletDataGen.checkin(checkout);

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
