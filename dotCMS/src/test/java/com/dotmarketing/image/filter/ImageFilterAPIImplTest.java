package com.dotmarketing.image.filter;

import com.google.common.io.Files;
import org.apache.commons.collections.IteratorUtils;
import org.junit.Assert;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.fail;

public class ImageFilterAPIImplTest {

    final static String[] extensions = new String[]{"webp", "png", "gif", "jpg"};

    static ImageFilterApiImpl imageApi = ImageFilterAPI.apiInstance.apply();

    @Test
    public void test_no_VP8_WEBP_loaded() {

        Iterator<ImageReader> readerIter = ImageIO.getImageReadersByFormatName("webp");

        IteratorUtils.toList(readerIter).forEach(r -> {
            Assert.assertFalse(r instanceof net.sf.javavp8decoder.imageio.WebPImageReaderSpi);
            Assert.assertFalse(r instanceof net.sf.javavp8decoder.imageio.WebPImageReader);
        });

    }

    @Test
    public void test_image_readers_loaded() {

        Iterator<ImageReader> readerIter = ImageIO.getImageReadersByFormatName("webp");

        List<ImageReader> readers = IteratorUtils.toList(readerIter);
        assert (readers.size() > 0);

    }

    @Test
    public void test_image_readers_for_image_files() {
        for (final String ext : extensions) {
            final URL url = getClass().getResource("/images/test." + ext);

            final File incomingFile = new File(url.getFile());
            try (ImageInputStream inputStream = ImageIO.createImageInputStream(incomingFile)) {
                final ImageReader reader = imageApi.getReader(incomingFile, inputStream);
                assert (reader != null);
                //assert (!(reader instanceof net.sf.javavp8decoder.imageio.WebPImageReader));

            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());

            }
        }
    }


    // GH Issue 25861
    @Test
    public void test_broken_webp_reader() {
        final URL url = getClass().getResource("/images/test-webp-issue-25861.webp");
        File incomingFile = new File(url.getFile());
        Dimension dim = imageApi.getWidthHeight(incomingFile);

        Assert.assertEquals((int) dim.getWidth() , 320);
        Assert.assertEquals((int) dim.getHeight() , 240);

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

        url = getClass().getResource("/images/test.svg");
        incomingFile = new File(url.getFile());

        dim = imageApi.getWidthHeight(incomingFile);
        assert (dim.getHeight() > 0);
        assert (dim.getWidth() > 0);

        //We should get dimensions even when the svg file is actually a png
        url = getClass().getResource("/images/png_as_svg.svg");
        incomingFile = new File(url.getFile());

        dim = imageApi.getWidthHeight(incomingFile);
        assert (dim.getHeight() > 0);
        assert (dim.getWidth() > 0);

    }


    @Test
    public void test_svg_dimensions() {

        URL url = getClass().getResource("/images/test.svg");
        File incomingFile = new File(url.getFile());

        Dimension dim = imageApi.getWidthHeight(incomingFile);
        assert (dim.getHeight() == 103);
        assert (dim.getWidth() == 203);

    }


    @Test
    public void test_resizeImage() {

        URL url = getClass().getResource("/images/10by10000.png");
        File incomingFile = new File(url.getFile());

        Dimension dim = imageApi.getWidthHeight(incomingFile);
        assert (dim.getHeight() == 10d);
        assert (dim.getWidth() == 10000d);

        BufferedImage newFile = imageApi.resizeImage(incomingFile, 1000, 10);
        assert (newFile.getWidth() == 1000);
        assert (newFile.getHeight() == 10);
        newFile = null;


    }

    @Test
    public void test_intelligentResize() {

        URL url = getClass().getResource("/images/10by10000.png");
        File incomingFile = new File(url.getFile());

        Dimension dim = imageApi.getWidthHeight(incomingFile);
        assert (dim.getHeight() == 10d);
        assert (dim.getWidth() == 10000d);

        BufferedImage newFile = imageApi.intelligentResize(incomingFile, 7000, 10);
        assert (newFile.getWidth() == 5000);
        assert (newFile.getHeight() == 10);
        newFile = null;


    }


    @Test
    public void test_resizeImage_change_algo() throws Exception {
        URL url = getClass().getResource("/images/test.webp");
        File incomingFile = new File(url.getFile());


        BufferedImage newFile = imageApi.resizeImage(incomingFile, 150, 150, 1);
        File tempResultFile1 = new File("/tmp", System.currentTimeMillis() + ".png");
        ImageIO.write(newFile, "png", tempResultFile1);


        BufferedImage newFile2 = imageApi.resizeImage(incomingFile, 150, 150, 15);
        File tempResultFile2 = new File("/tmp", System.currentTimeMillis() + "2.png");
        ImageIO.write(newFile2, "png", tempResultFile2);

        // tests that the resample opts are getting read and result in different files.
        assert (!Files.equal(tempResultFile2, tempResultFile1));


    }


}
