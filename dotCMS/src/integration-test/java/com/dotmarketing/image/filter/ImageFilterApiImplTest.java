package com.dotmarketing.image.filter;

import static org.junit.Assert.fail;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.apache.commons.collections.IteratorUtils;
import org.junit.BeforeClass;
import org.junit.Test;

public class ImageFilterApiImplTest {

    final static String[] extensions = new String[] {"webp", "png", "gif", "jpg"};

    static ImageFilterApiImpl imageApi = ImageFilterAPI.apiInstance.apply();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        ImageIO.scanForPlugins();

    }

    @Test
    public void test_image_readers_loaded() {

        Iterator<ImageReader> readerIter = ImageIO.getImageReadersByFormatName("webp");

        List<ImageReader> readers = IteratorUtils.toList(readerIter);
        assert (readers.size() == 2);

    }

    @Test
    public void test_image_readers_for_image_files() {
        for (final String ext : extensions) {
            final URL url = getClass().getResource("/images/test." + ext);

            final File incomingFile = new File(url.getFile());
            try (ImageInputStream inputStream = ImageIO.createImageInputStream(incomingFile)) {
                final ImageReader reader = imageApi.getReader(incomingFile,inputStream);
                assert (reader != null);
                assert (!(reader instanceof net.sf.javavp8decoder.imageio.WebPImageReader));

            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());

            }
        }
    }
    
    
    @Test
    public void test_dimensions() {

        URL url = getClass().getResource("/images/10by10000.png");
        File incomingFile = new File(url.getFile());

        Dimension dim = imageApi.getWidthHeight(incomingFile);
        assert (dim.getHeight() == 10d);
        assert (dim.getWidth() == 10000d);

        // 1024x772
        url = getClass().getResource("/images/test.webp");
        incomingFile = new File(url.getFile());

        dim = imageApi.getWidthHeight(incomingFile);
        assert (dim.getHeight() == 772d);
        assert (dim.getWidth() == 1024d);

    }
    

    
    @Test
    public void test_resizeImage() {

        URL url = getClass().getResource("/images/10by10000.png");
        File incomingFile = new File(url.getFile());
        
        Dimension dim = imageApi.getWidthHeight(incomingFile);
        assert (dim.getHeight() == 10d);
        assert (dim.getWidth() == 10000d);
        
        BufferedImage newFile = imageApi.resizeImage(incomingFile,1000,10);
        assert(newFile.getWidth() == 1000);
        assert(newFile.getHeight() == 10);
        newFile=null;


    }
    
    @Test
    public void test_intelligentResize() {

        URL url = getClass().getResource("/images/10by10000.png");
        File incomingFile = new File(url.getFile());
        
        Dimension dim = imageApi.getWidthHeight(incomingFile);
        assert (dim.getHeight() == 10d);
        assert (dim.getWidth() == 10000d);
        
        BufferedImage newFile = imageApi.resizeImage(incomingFile,7000,10);
        assert(newFile.getWidth() == 5000);
        assert(newFile.getHeight() == 10);
        newFile=null;


    }
    

}
