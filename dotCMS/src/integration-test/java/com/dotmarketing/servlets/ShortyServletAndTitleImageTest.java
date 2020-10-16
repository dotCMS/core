package com.dotmarketing.servlets;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;

import java.util.Optional;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableBinaryField;
import com.dotcms.contenttype.model.field.ImmutableImageField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;

public class ShortyServletAndTitleImageTest {
    

    final static byte[] pngPixel=Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");
    final static byte[] gifPixel=Base64.getDecoder().decode("R0lGODlhAQABAIAAAP///wAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw==");
    final static byte[] txtBlah = "Here is a text blah".getBytes();
    private static User user ;
    private static Host host ;
    
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
        

        assertTrue("we DO NOT HAVE a title image as a text file cannot have an image", !fileAsset.getTitleImage().isPresent());
        
        
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
        
        assertTrue("we DO NOT a title image  ",! contentlet.getTitleImage().isPresent());
        
        String path = servlet.inodePath(contentlet, null, false);
        assertTrue("shorty points to the inode", path.contains("/" + contentlet.getInode()));

        
        // Set binarys, first binary field is a text file, then an image field, then a binary
        contentlet.setBinary(binary1, txtFile);
        contentlet.setStringProperty(image2, fileAsset.getIdentifier());
        contentlet.setBinary(binary3, gifFile);
        contentlet.setInode(null);
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
        contentlet.setStringProperty(image2, null);
        contentlet.setInode(null);
        contentlet = APILocator.getContentletAPI().checkin(contentlet, user, false);
        
        // title image is now the last binary field (the first image on content)
        assertTrue("we HAVE a title image  ", contentlet.getTitleImage().isPresent());
        assertTrue("TitleImage field is correct", binary3.equals(contentlet.getTitleImage().get()));
        
        // if no field variable is passed in, return the first binary
        path = servlet.inodePath(contentlet, Contentlet.TITLE_IMAGE_KEY, false);

        assertTrue("shorty points to the inode", path.contains("/" + contentlet.getInode()));
        assertTrue("titleImage points to binary3", path.contains("/" + binary3.variable()));
        
        
        
        
        
        
        
        
    }
    
    
}
