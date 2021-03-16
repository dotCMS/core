package com.dotmarketing.image.focalpoint;

import static com.dotcms.datagen.TestDataUtils.getFileAssetContent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.TestDataUtils.TestFile;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UUIDGenerator;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class FocalPointAPITest {

    final static String[] extensions = new String[] {"webp", "png", "gif", "jpg"};

    static FocalPointAPIImpl focalPointAPI;

    @BeforeClass
    public static void setup() throws Exception {
        IntegrationTestInitService.getInstance().init();
        focalPointAPI = new FocalPointAPIImpl(
                APILocator.getFileMetadataAPI(),
                APILocator.getTempFileAPI(),
                APILocator.getContentletAPI(),
                APILocator::systemUser);
    }

    /**
     * we should read and write the same values when a focal point is set
     * @throws Exception
     */
    @Ignore
    @Test
    public void Test_Reading_and_Writing_From_FocalPoint_File() throws Exception {
        final String fieldVarToTest = "fileAsset";
        for (final String ext : extensions) {

            final URL url = FocalPointAPITest.class.getResource("/images/test." + ext);

            final File incomingFile = new File(url.getFile());

            final String inode = UUIDGenerator.generateUuid();

            final File testFile = new File("/tmp/testing/assets/" + inode + "/" + inode.charAt(0) + "/" + inode.charAt(1) + "/"
                            + fieldVarToTest + "/" + incomingFile.getName());
            testFile.getParentFile().mkdirs();
            FileUtils.copyFile(incomingFile, testFile);

            assert (testFile.exists());

            final FocalPoint focalPoint = new FocalPoint(.2f, .3f);

            focalPointAPI.writeFocalPoint(inode, fieldVarToTest, focalPoint);

            final Optional<FocalPoint> writtenFp = focalPointAPI.readFocalPoint(inode, fieldVarToTest);
            assertTrue("Focal points read", writtenFp.isPresent());
            assertEquals("Focal points do not match", focalPoint, writtenFp.get());

        }

    }

    /**
     * we should read and write the same values when a focal point is set
     * @throws Exception
     */
    @Test
    public void Test_Reading_and_Writing_From_FocalPoint_For_Given_Conentlet() throws Exception {
            final String fieldVarToTest = "fileAsset";

            final Contentlet fileAssetContent = getFileAssetContent(true, 1L, TestFile.PNG);

            final FocalPoint focalPoint = new FocalPoint(.2f, .3f);

            focalPointAPI.writeFocalPoint(fileAssetContent.getInode(), fieldVarToTest, focalPoint);

            final Optional<FocalPoint> writtenFp = focalPointAPI.readFocalPoint(fileAssetContent.getInode(), fieldVarToTest);
            assertTrue("Focal points read", writtenFp.isPresent());
            assertEquals("Focal points do not match", focalPoint, writtenFp.get());

    }
    
    /**
     * we should return an empty Optional if there is no focal point
     * @throws Exception
     */
    @Test
    public void Test_When_No_Focal_Point() throws Exception {
        final String fieldVarToTest = "fileAsset";
        for (final String ext : extensions) {
            final URL url = FocalPointAPITest.class.getResource("/images/test." + ext);

            final File incomingFile = new File(url.getFile());

            final String inode = UUIDGenerator.generateUuid();

            final File testFile = new File("/tmp/testing/assets/" + inode + "/" + inode.charAt(0) + "/" + inode.charAt(1) + "/"
                            + fieldVarToTest + "/" + incomingFile.getName());
            testFile.getParentFile().mkdirs();
            FileUtils.copyFile(incomingFile, testFile);

            assert (testFile.exists());

            final Optional<FocalPoint> writtenFp = focalPointAPI.readFocalPoint(inode, fieldVarToTest);
            assertFalse("There is no focal point", writtenFp.isPresent());

        }

    }
    
    
    
    
    /**
     * parses a focal point from a String
     * @throws Exception
     */
    @Test
    public void Test_Parsing_a_Focal_Point_From_String() throws Exception {

        String fp1 = ".555,.666";

        Optional<FocalPoint> test = focalPointAPI.parseFocalPoint(fp1);
        assertTrue("we have focal point", test.isPresent());
        assertTrue("we have focal point1", test.get().x == .555f);
        assertTrue("we have focal point1", test.get().y == .666f);

        fp1 = ".dasdas,.asdas";
        test = focalPointAPI.parseFocalPoint(fp1);
        assertTrue("we have no  focal point", !test.isPresent());

    }
    
    
    /**
     * Reads a focal point from a parameter map
     * @throws Exception
     */
    @Test
    public void Test_Reading_a_Focal_Point_From_Params() throws Exception {
        String fp1 = ".555,.666";
        Map<String,String[]> params = ImmutableMap.of("fp", new String[] {fp1});

        Optional<FocalPoint> test = focalPointAPI.parseFocalPointFromParams(params);
        assertTrue("we have focal point", test.isPresent());
        assertTrue("we have focal point1", test.get().x == .555f);
        assertTrue("we have focal point1", test.get().y == .666f);
        
        
        fp1 = ".dasdas,.asdas";
        params = ImmutableMap.of("fp", new String[] {fp1});
        test= focalPointAPI.parseFocalPointFromParams(params);
        assertTrue("we have no  focal point", !test.isPresent());

    }
    
    
}
