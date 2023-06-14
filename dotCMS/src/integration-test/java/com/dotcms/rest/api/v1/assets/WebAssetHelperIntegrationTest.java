package com.dotcms.rest.api.v1.assets;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.VariantDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.rest.api.v1.asset.AssetsRequestForm;
import com.dotcms.rest.api.v1.asset.FileUploadData;
import com.dotcms.rest.api.v1.asset.FileUploadDetail;
import com.dotcms.rest.api.v1.asset.WebAssetHelper;
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
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;
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
     * Method to test : {@link WebAssetHelper#getAssetContent(AssetsRequestForm, User)}
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
        final File assetContent = webAssetHelper.getAssetContent(form, APILocator.systemUser());
        Assert.assertNotNull(assetContent);
        Assert.assertTrue(assetContent.exists());
        Assert.assertEquals(testFile.length(), assetContent.length());
    }

    /**
     * Method to test : {@link WebAssetHelper#getAssetContent(AssetsRequestForm, User)}
     * Given Scenario: We submit a valid path for file but using a different language
     * Expected Result: We should get 404 since the file does not exist in that language
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws IOException
     */
    @Test(expected = NotFoundInDbException.class)
    public void TestDownloadFileDifferentLanguage() throws DotDataException, DotSecurityException {
        final AssetsRequestForm form = AssetsRequestForm.builder()
                .assetPath(assetPath()).language("nonExisting_lang").live(false)
                .build();
        WebAssetHelper webAssetHelper = WebAssetHelper.newInstance();
        webAssetHelper.getAssetContent(form, APILocator.systemUser());

    }

}
