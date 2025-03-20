package com.dotmarketing.servlets;

import com.dotcms.DataProviderWeldRunner;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableBinaryField;
import com.dotcms.contenttype.model.field.ImmutableImageField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.DotAssetContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.liferay.portal.model.User;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.ApplicationScoped;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@ApplicationScoped
@RunWith(DataProviderWeldRunner.class)
public class ShortyServletAndTitleImageTest {
    

    final static byte[] pngPixel=Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");
    final static byte[] gifPixel=Base64.getDecoder().decode("R0lGODlhAQABAIAAAP///wAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw==");
    final static byte[] txtBlah = "Here is a text blah".getBytes();
    private static User user ;
    private static Host host ;

    public static class Uri{
         final ShortyServlet servlet = new ShortyServlet();
         final String uri;
         final int      width;
         final int      height;
         final int      maxWidth;
         final int      maxHeight;
         final int      minWidth;
         final int      minHeight;
         final int      quality;
         final int      cropWidth;
         final int      cropHeight;
         int            expectedWidth;
         int            expectedHeight;
         int            expectedMaxWidth;
         int            expectedMaxHeight;
         int            expectedMinWidth;
         int            expectedMinHeight;
         int            expectedQuality;
         int            expectedCopWidth;
         int            expectedCropHeight;
         final boolean  jpeg;
         final boolean  webp;
         final boolean  isImage;

         boolean expectedIsImage;


        public Uri(String uri, int expectedWidth, int expectedHeight, int expectedMaxWidth, int expectedMaxHeight, int expectedMinWidth, int expectedMinHeight, int expectedQuality, int expectedCopWidth, int expectedCropHeight, boolean expectedIsImage) {
            this.uri = uri;
            this.expectedWidth = expectedWidth;
            this.expectedHeight = expectedHeight;
            this.expectedMaxWidth = expectedMaxWidth;
            this.expectedMaxHeight = expectedMaxHeight;
            this.expectedMinWidth = expectedMinWidth;
            this.expectedMinHeight = expectedMinHeight;
            this.expectedQuality = expectedQuality;
            this.expectedCopWidth = expectedCopWidth;
            this.expectedCropHeight = expectedCropHeight;
            this.expectedIsImage = expectedIsImage;
            width = servlet.getWidth(uri, 0);
            height = servlet.getHeight(uri, 0);
            maxWidth = servlet.getMaxWidth(uri);
            maxHeight = servlet.getMaxHeight(uri);
            minWidth = servlet.getMinWidth(uri);
            minHeight = servlet.getMinHeight(uri);
            quality = servlet.getQuality(uri, 0);
            cropWidth = servlet.cropWidth(uri);
            cropHeight = servlet.cropHeight(uri);
            jpeg    = uri.contains(".jpg");
            webp    = uri.contains(".webp");
            isImage = webp || jpeg || width+height+maxWidth+maxHeight +minHeight+minWidth> 0 || quality>0 || cropHeight>0 || cropWidth>0;
        }
    }


    @DataProvider
    public static Object[] uriTestCases() {
        return new Object[] {
                //file is an image and have different properties (width, height, maxWidth, etc)
                new Uri("https://www.dotcms.com/6a50a56c-c281-4282-b9ba-fa9f07fea4da/fileAsset/test.jpg", 0, 0, 0, 0, 0, 0, 0, 0, 0, true),
                new Uri("https://www.dotcms.com/6a50a56c-c281-4282-b9ba-fa9f07fea4da/fileAsset/100h/test.jpg", 0, 100, 0, 0, 0, 0, 0, 0, 0, true),
                new Uri("https://www.dotcms.com/6a50a56c-c281-4282-b9ba-fa9f07fea4da/fileAsset/100w/100h/test.jpg", 100, 100, 0, 0, 0, 0, 0, 0, 0, true),
                new Uri("https://www.dotcms.com/6a50a56c-c281-4282-b9ba-fa9f07fea4da/fileAsset/300maxw/test.jpg", 0, 0, 300, 0, 0, 0, 0, 0, 0, true),
                //file is not an image and has some properties in the name
                new Uri("https://www.dotcms.com/6a50a56c-c281-4282-b9ba-fa9f07fea4da/fileAsset/test.pdf", 0, 0, 0, 0, 0, 0, 0, 0, 0, false),
                new Uri("https://www.dotcms.com/6a50a56c-c281-4282-b9ba-fa9f07fea4da/fileAsset/100qtest.pdf", 0, 0, 0, 0, 0, 0, 0, 0, 0, false),

        };
    }

    
    @BeforeClass
    public static void prepare()  {
        try {
            IntegrationTestInitService.getInstance().init();
            user = APILocator.systemUser();
            host = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }


    }

    
    @Test
    public void test_Contentlet_getTitleImage_Returns_Proper_Field() throws Exception{
        final File txtFile=File.createTempFile("tmp", ".txt");
        Files.write(txtFile.toPath(), txtBlah);
        final File pngFile=File.createTempFile("tmp", ".png");
        Files.write(pngFile.toPath(), pngPixel);
        final File gifFile=File.createTempFile("tmp", ".gif");
        Files.write(gifFile.toPath(), gifPixel);
        final Folder folder = new FolderDataGen().nextPersisted();
        Contentlet fileAsset = new FileAssetDataGen(folder, pngFile).nextPersisted();
        
        assertTrue("we HAVE a title image  ", fileAsset.getTitleImage().isPresent());
        
        byte[] fileContents = Files.readAllBytes(fileAsset.getBinary(fileAsset.getTitleImage().get().variable()).toPath());

        assertTrue("TitleImage read correctly", Arrays.equals(fileContents ,pngPixel));
        
        fileAsset = APILocator.getContentletAPI().checkout(fileAsset.getInode(), user, false);
        
        fileAsset.setIndexPolicy(IndexPolicy.FORCE);
        fileAsset.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, txtFile.getName());
        fileAsset.setBinary(FileAssetAPI.BINARY_FIELD, txtFile);
        
        fileAsset = APILocator.getContentletAPI().checkin(fileAsset, user, false);
        

        assertTrue("we DO NOT HAVE a title image as a text file cannot have an image", fileAsset.getTitleImage().isEmpty());
        
        
        fileContents = Files.readAllBytes(fileAsset.getBinary(FileAssetAPI.BINARY_FIELD).toPath());
        
        assertTrue("TitleImage read correctly", Arrays.equals(fileContents ,txtBlah));

        
        
    }
    @Test
    public void test_ShortyServlet_Returns_Proper_Field() throws Exception{
        final File txtFile=File.createTempFile("tmp", ".txt");
        Files.write(txtFile.toPath(), txtBlah);
        final File pngFile=File.createTempFile("tmp", ".png");
        Files.write(pngFile.toPath(), pngPixel);
        final File gifFile=File.createTempFile("tmp", ".gif");
        Files.write(gifFile.toPath(), gifPixel);
        final ShortyServlet servlet = new ShortyServlet();
        Contentlet fileAsset = new FileAssetDataGen(host, pngFile).nextPersisted();
        APILocator.getContentletAPI().publish(fileAsset, user, false);
        fileAsset.setInode(null);
        fileAsset = APILocator.getContentletAPI().checkin(fileAsset, user, false);

        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        
        
        int i=0;

        
        Field binary1 = ImmutableBinaryField.builder()
                .contentTypeId(contentType.id())
                .name("binary" + (i))
                .sortOrder(i++)
                .variable("binary" +i).build();
               
        Field image2 = ImmutableImageField.builder()
                .contentTypeId(contentType.id())
                .name("image" + (i))
                .sortOrder(i++)
                .variable("image" +i).build();     
        
        
        Field binary3 = ImmutableBinaryField.builder()
                .contentTypeId(contentType.id())
                .name("binary" + (i))
                .sortOrder(i++)
                .variable("binary" +i).build();                  
           
        binary1 = APILocator.getContentTypeFieldAPI().save(binary1, user);
        image2 = APILocator.getContentTypeFieldAPI().save(image2, user);
        binary3 = APILocator.getContentTypeFieldAPI().save(binary3, user);


        // create new content
        Contentlet contentlet = new Contentlet();
        contentlet.setContentTypeId(contentType.id());
        contentlet = APILocator.getContentletAPI().checkin(contentlet, user, false);
        
        assertTrue("we DO NOT a title image  ",contentlet.getTitleImage().isEmpty());
        
        String path = servlet.inodePath(contentlet, null, false);
        assertTrue("shorty points to the inode", path.contains("/" + contentlet.getInode()));

        
        // Set binarys, first binary field is a text file, then an image field, then a binary
        contentlet = APILocator.getContentletAPI().checkout(contentlet.getInode(), user, false);
        contentlet.setBinary(binary1, txtFile);
        contentlet.setStringProperty(image2, fileAsset.getIdentifier());
        contentlet.setBinary(binary3, gifFile);

        contentlet = APILocator.getContentletAPI().checkin(contentlet, user, false);
        
        // title image is the image field
        assertTrue("we HAVE a title image  ", contentlet.getTitleImage().isPresent());
        assertTrue("TitleImage field includes the image field", image2.equals(contentlet.getTitleImage().get()));
        
        // if no field variable is passed in, return the first binary
        path = servlet.inodePath(contentlet, null, false);

        assertTrue("shorty points to the inode", path.contains("/" + contentlet.getInode()));
        assertTrue("empty field points to the first binary field", path.contains("/" + binary1.variable()));
        
        // if titleImage is passed in, return the first image or binary that is an image
        // in this case an image field
        Optional<ContentletVersionInfo> cvi = APILocator.getVersionableAPI()
                .getContentletVersionInfo(contentlet.getStringProperty(image2.variable()),
                        contentlet.getLanguageId());

        assertTrue(cvi.isPresent());

        // the path to the WORKING image will be returned (live = false)
        path = servlet.inodePath(contentlet, Contentlet.TITLE_IMAGE_KEY, false);
        assertTrue("the path to the WORKING image will be to the realted",
                path.contains("/" + cvi.get().getWorkingInode()));
        assertTrue(path.contains("/fileAsset"));
        
        // the path to the LIVE image will be returned (live = true)
        path = servlet.inodePath(contentlet, Contentlet.TITLE_IMAGE_KEY, true);
        assertTrue("the path to the LIVE image will be to the realted",
                path.contains("/" + cvi.get().getLiveInode()));
        assertTrue(path.contains("/fileAsset"));

        
        
        // null out image field
        contentlet = APILocator.getContentletAPI().checkout(contentlet.getInode(), user, false);
        contentlet.setStringProperty(image2, null);
        contentlet = APILocator.getContentletAPI().checkin(contentlet, user, false);
        
        // title image is now the last binary field (the first image on content)
        assertTrue("we HAVE a title image  ", contentlet.getTitleImage().isPresent());
        assertTrue("TitleImage field is correct", binary3.equals(contentlet.getTitleImage().get()));
        
        // if no field variable is passed in, return the first binary
        path = servlet.inodePath(contentlet, Contentlet.TITLE_IMAGE_KEY, false);

        assertTrue("shorty points to the inode", path.contains("/" + contentlet.getInode()));
        assertTrue("titleImage points to binary3", path.contains("/" + binary3.variable()));
    }





    /**
     * Method to test: {@link ShortyServlet#inodePath(Contentlet, String, boolean)}
     * Given Scenario: A contentlet with an image field referencing a dot asset is created in a secondary language,
     *                 while the actual asset only exists in the default language
     * ExpectedResult: The servlet should:
     *                 - Detect that the asset doesn't exist in the secondary language
     *                 - Fall back to the default language version
     *                 - Return the correct path for both live and working versions
     *                 - Include the correct field variable (DotAssetContentType.ASSET_FIELD_VAR)
     */
    @Test
    public void test_inodePath_ImageField_ShouldFallbackToDefaultLanguage() throws Exception {
        // Initialize servlet
        final ShortyServlet servlet = new ShortyServlet();

        // Create and publish dot asset in default language
        Contentlet dotAssset = TestDataUtils.getDotAssetLikeContentlet(true, 
                APILocator.getLanguageAPI().getDefaultLanguage().getId());
        APILocator.getContentletAPI().publish(dotAssset, user, false);

        // Create content type with image field
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        Field imageField = ImmutableImageField.builder()
                .contentTypeId(contentType.id())
                .name("image")
                .variable("image").build();
        imageField = APILocator.getContentTypeFieldAPI().save(imageField, user);

        // Setup languages
        final Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        final Language language2 = new LanguageDataGen().nextPersisted();

        // Create contentlet in secondary language
        Contentlet contentlet = new Contentlet();
        contentlet.setContentTypeId(contentType.id());
        contentlet.setStringProperty(imageField, dotAssset.getIdentifier());
        contentlet.setLanguageId(language2.getId());
        contentlet = APILocator.getContentletAPI().checkin(contentlet, user, false);

        // Verify asset doesn't exist in secondary language
        Optional<ContentletVersionInfo> cvi = APILocator.getVersionableAPI()
                .getContentletVersionInfo(contentlet.getStringProperty(imageField.variable()),
                        language2.getId());
        assertTrue(cvi.isEmpty());

        // Verify asset exists in default language
        Optional<ContentletVersionInfo> cvi2 = APILocator.getVersionableAPI()
                .getContentletVersionInfo(contentlet.getStringProperty(imageField.variable()),
                        defaultLanguage.getId());
        assertTrue(cvi2.isPresent());
        assertEquals(cvi2.get().getLang(), defaultLanguage.getId());

        // Verify live version path resolution
        String path = servlet.inodePath(contentlet, imageField.variable(), true);
        assertTrue("shorty points to the asset live inode", path.contains("/" + cvi2.get().getLiveInode()));
        assertTrue(path.contains("/" + DotAssetContentType.ASSET_FIELD_VAR));

        // Verify working version path resolution
        path = servlet.inodePath(contentlet, imageField.variable(), false);
        assertTrue("shorty points to the asset working inode", path.contains("/" + cvi2.get().getWorkingInode()));
        assertTrue(path.contains("/" + DotAssetContentType.ASSET_FIELD_VAR));
    }

    /**
     * Method to test: {@link ShortyServlet#doForward(HttpServletRequest, HttpServletResponse, String, String, boolean, Optional)}
     * Given Scenario: Different uri of files such as jpg, pdf, webp, etc are passed to the method
     * ExpectedResult: The method should check if the expected values are correct and if the file is an image or not
     *
     */

    @Test
    @UseDataProvider("uriTestCases")
    public void test_ShortyServlet_Process_Correct_Uri(Uri uri) {

        assertEquals(uri.width, uri.expectedWidth);
        assertEquals(uri.height, uri.expectedHeight);
        assertEquals(uri.maxWidth, uri.expectedMaxWidth);
        assertEquals(uri.maxHeight, uri.expectedMaxHeight);
        assertEquals(uri.minWidth, uri.expectedMinWidth);
        assertEquals(uri.minHeight, uri.expectedMinHeight);
        assertEquals(uri.quality, uri.expectedQuality);
        assertEquals(uri.cropWidth, uri.expectedCopWidth);
        assertEquals(uri.cropHeight, uri.expectedCropHeight);
        assertEquals(uri.isImage, uri.expectedIsImage);
    }

    /**
     * Method to test: {@link ShortyServlet#doForward(HttpServletRequest, HttpServletResponse, String, String, boolean, Optional)}
     * Given Scenario: A uri of a file that is called or contains webp is passed to the method
     * ExpectedResult: The method shouldn't consider the file as an image for containing the word webp, so the result should be false
     *
     */
    @Test
    public void test_webp_file_name(){
        Uri uri = new Uri("/data/shared/assets/tmp_upload/temp_2e1056205c/webPageContent.vtl", 0, 0, 0, 0, 0, 0, 0, 0, 0, false);
        assertEquals(uri.isImage, uri.expectedIsImage);
    }

    /**
     * Method to test: {@link ShortyServlet#serve(HttpServletRequest, HttpServletResponse)}
     * Given Scenario: A shorty URL is requested by an authenticated user
     * ExpectedResult: The method should forward the request for an authenticated user
     * For a non-authenticated user, the method should return a 401 status code
     */
    @Test
    public void test_ShortyServlet_With_AuthenticatedUser() throws Exception {

        final HttpServlet servlet = new ShortyServlet();
        ServletTestUtils.testServletWithAuthenticatedUser(
                servlet, assetId -> "/dA/"
                        + APILocator.getShortyAPI().shortify(assetId) + "/image/test.jpg");

    }

}
