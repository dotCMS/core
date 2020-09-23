package com.dotmarketing.util;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import java.io.File;
import java.util.Optional;
import org.junit.Test;

public class FileUtilTest extends UnitTestBase {

    /**
     * Given scenario: I have a file that belongs under my realAssets path (somewhere)
     * Expected Result: The method gets me the relative part of the file's absolute path that is under the real asset path.
     */
    @Test
    public void Test_Get_Relative_Path_Expect_Match(){
        final String relativePart = "/4/1/419bacda-1e1d-412e-b73e-db514289b71f/fileAsset/test_image11600880647045.jpg";
        final String realAssetPath = "/Users/fabrizzio/code/servers/server1/assets";
        final String absolutePath = realAssetPath + relativePart;
        final File file = mock(File.class);
        when(file.getAbsolutePath()).thenReturn(absolutePath);
        final Optional<String> optional = FileUtil
                .getRealAssetsPathRelativePiece(file, () -> realAssetPath);
        assertTrue(optional.isPresent());
        assertEquals(relativePart,optional.get());
    }

    /**
     * Given scenario: I am passing a file that doesnt belong under real-assets-path
     * Expected Result: an empty option since there is no relative part
     */
    @Test
    public void Test_Get_Relative_Path_Expect_Mismatch(){
        final String realAssetPath = "/Users/fabrizzio/code/servers/server1/assets";
        final String absolutePath = "/etc/class/xyz/image.png";
        final File file = mock(File.class);
        when(file.getAbsolutePath()).thenReturn(absolutePath);
        final Optional<String> optional = FileUtil
                .getRealAssetsPathRelativePiece(file, () -> realAssetPath);
        assertFalse(optional.isPresent());
    }

}
