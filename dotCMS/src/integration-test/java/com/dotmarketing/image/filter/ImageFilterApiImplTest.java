package com.dotmarketing.image.filter;

import static org.junit.Assert.*;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import org.apache.commons.collections.IteratorUtils;
import org.junit.BeforeClass;
import org.junit.Test;

public class ImageFilterApiImplTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        
        
        ImageIO.scanForPlugins();
        
    }

    @Test
    public void test_image_readers_loaded() {
        
        Iterator<ImageReader> readerIter = ImageIO.getImageReadersByFormatName("webp");
        
        List<ImageReader> readers = IteratorUtils.toList(readerIter);
        assert(readers.size()==2);
        
        
        fail("Not yet implemented");
    }

}
