package com.dotmarketing.image.focalpoint;

import static org.junit.Assert.assertTrue;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPIImpl;
import com.dotmarketing.util.UUIDGenerator;
import com.google.common.collect.ImmutableMap;

public class FocalPointAPITest {


    final static String[] extensions = new String[] {"webp", "png", "gif", "jpg"};
    static List<File> testFiles = new ArrayList<>();


    static FocalPointAPIImpl fpAPI;



    @BeforeClass
    public static void setup() throws Exception {
        IntegrationTestInitService.getInstance().init();
        fpAPI = new FocalPointAPIImpl(new FileAssetAPIImpl(null, null, null, null, null), null);
    }

    /**
     * we should read and write the same values when a focal point is set
     * @throws Exception
     */
    @Test
    public void test_reading_and_writing_from_focalpoint_file() throws Exception {

        for (String ext : extensions) {
            URL url = FocalPointAPITest.class.getResource("/images/test." + ext);

            File incomingFile = new File(url.getFile());

            String inode = UUIDGenerator.generateUuid();
            String fieldVarToTest = "fileAsset";


            File testFile = new File("/tmp/testing/assets/" + inode + "/" + inode.charAt(0) + "/" + inode.charAt(1) + "/"
                            + fieldVarToTest + "/" + incomingFile.getName());
            testFile.getParentFile().mkdirs();
            FileUtils.copyFile(incomingFile, testFile);

            assert (testFile.exists());


            FocalPoint focalPoint = new FocalPoint(.2f, .3f);


            fpAPI.writeFocalPoint(inode, fieldVarToTest, focalPoint);

            Optional<FocalPoint> writtenFp = fpAPI.readFocalPoint(inode, fieldVarToTest);
            assertTrue("Focal points read", writtenFp.isPresent());
            assertTrue("Focal points do not match", focalPoint.equals(writtenFp.get()));



        }

    }

    
    /**
     * we should return an empty Optional if there is no focal point
     * @throws Exception
     */
    @Test
    public void test_when_no_focal_point() throws Exception {

        for (String ext : extensions) {
            URL url = FocalPointAPITest.class.getResource("/images/test." + ext);

            File incomingFile = new File(url.getFile());

            String inode = UUIDGenerator.generateUuid();
            String fieldVarToTest = "fileAsset";


            File testFile = new File("/tmp/testing/assets/" + inode + "/" + inode.charAt(0) + "/" + inode.charAt(1) + "/"
                            + fieldVarToTest + "/" + incomingFile.getName());
            testFile.getParentFile().mkdirs();
            FileUtils.copyFile(incomingFile, testFile);

            assert (testFile.exists());



            Optional<FocalPoint> writtenFp = fpAPI.readFocalPoint(inode, fieldVarToTest);
            assertTrue("There is no focal point", !writtenFp.isPresent());




        }

    }
    
    
    
    
    /**
     * parses a focal point from a String
     * @throws Exception
     */
    @Test
    public void test_parsing_a_focal_point_from_string() throws Exception {

        String fp1 = ".555,.666";

        
        Optional<FocalPoint> test = fpAPI.parseFocalPoint(fp1);
        assertTrue("we have focal point", test.isPresent());
        assertTrue("we have focal point1", test.get().x == .555f);
        assertTrue("we have focal point1", test.get().y == .666f);
        
        
        fp1 = ".dasdas,.asdas";
        test = fpAPI.parseFocalPoint(fp1);
        assertTrue("we have no  focal point", !test.isPresent());

    }
    
    
    /**
     * Reads a focal point from a parameter map
     * @throws Exception
     */
    @Test
    public void test_reading_a_focal_point_from_params() throws Exception {
        String fp1 = ".555,.666";
        Map<String,String[]> params = ImmutableMap.of("fp", new String[] {fp1});

        
        Optional<FocalPoint> test = fpAPI.parseFocalPointFromParams(params);
        assertTrue("we have focal point", test.isPresent());
        assertTrue("we have focal point1", test.get().x == .555f);
        assertTrue("we have focal point1", test.get().y == .666f);
        
        
        fp1 = ".dasdas,.asdas";
        params = ImmutableMap.of("fp", new String[] {fp1});
        test= fpAPI.parseFocalPointFromParams(params);
        assertTrue("we have no  focal point", !test.isPresent());

    }
    
    
}
