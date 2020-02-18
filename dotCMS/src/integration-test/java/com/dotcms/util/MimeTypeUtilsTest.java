package com.dotcms.util;

import com.dotcms.UnitTestBase;
import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * MimeTypeUtils unit test.
 * @author jsanca
 */
public class MimeTypeUtilsTest extends UnitTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void test_match_MimeType_all() throws IOException {

        boolean match = MimeTypeUtils.match("*/*", "image/jpeg");

        Assert.assertTrue("* must match with image/jpeg", match);

        match = MimeTypeUtils.match("*/*", "application/pdf");

        Assert.assertTrue("* must match with application/pdf", match);

        match = MimeTypeUtils.match("*/*", "text/plain");

        Assert.assertTrue("* must match with text/plain", match);
    }

    @Test
    public void test_match_MimeType_partial_image() throws IOException {

        boolean match = MimeTypeUtils.match("image/*", "image/jpeg");

        Assert.assertTrue("image/* must match with image/jpeg", match);

        match = MimeTypeUtils.match("image/*", "image/webp");

        Assert.assertTrue("image/* must match with image/webp", match);

        match = MimeTypeUtils.match("image/*", "image/png");

        Assert.assertTrue("image/* must match with image/png", match);

        match = MimeTypeUtils.match("image/*", "application/vnd.hzn-3d-crossword");

        Assert.assertFalse("image/* must not match with application/vnd.hzn-3d-crossword", match);
    }

    @Test
    public void test_match_MimeType_partial_application() throws IOException {

        boolean match = MimeTypeUtils.match("application/*", "application/vnd.hzn-3d-crossword");

        Assert.assertTrue("application/* must match with application/vnd.hzn-3d-crossword", match);

        match = MimeTypeUtils.match("application/*", "application/x-7z-compressed");

        Assert.assertTrue("application/* must match with application/x-7z-compressed", match);

        match = MimeTypeUtils.match("application/*", "application/pdf");

        Assert.assertTrue("application/* must match with application/pdf", match);

        match = MimeTypeUtils.match("application/*", "image/png");

        Assert.assertFalse("application/* must not match with image/png", match);
    }

    @Test
    public void test_match_MimeType_partial_text() throws IOException {

        boolean match = MimeTypeUtils.match("text/*", "text/x-asm");

        Assert.assertTrue("text/* must match with text/x-asm", match);

        match = MimeTypeUtils.match("text/*", "text/css");

        Assert.assertTrue("text/* must match with text/css", match);

        match = MimeTypeUtils.match("text/*", "text/csv");

        Assert.assertTrue("text/* must match with text/csv", match);

        match = MimeTypeUtils.match("text/*", "image/png");

        Assert.assertFalse("text/* must not match with image/png", match);
    }

    @Test
    public void test_match_MimeType_total_text() throws IOException {

        boolean match = MimeTypeUtils.match("text/x-asm", "text/x-asm");

        Assert.assertTrue("text/x-asm must match with text/x-asm", match);

        match = MimeTypeUtils.match("text/css", "text/css");

        Assert.assertTrue("text/css must match with text/css", match);

        match = MimeTypeUtils.match("text/csv", "text/csv");

        Assert.assertTrue("text/csv must match with text/csv", match);

        match = MimeTypeUtils.match("text/csv", "image/png");

        Assert.assertFalse("text/csv must not match with image/png", match);

        match = MimeTypeUtils.match("aplication/pdf", "image/png");

        Assert.assertFalse("aplication/pdf must not match with image/png", match);
    }

    @Test
    public void test_getMimeType_text_plain() throws IOException {

        final File tempTestFile = File
                .createTempFile("csvTest_" + new Date().getTime(), ".txt");
        FileUtils.writeStringToFile(tempTestFile, "Test");

        final String mimeType = MimeTypeUtils.getMimeType(tempTestFile);

        Assert.assertEquals("The mime type should be application/text","text/plain", mimeType);
    }

    @Test
    public void test_getMimeType_image_png() throws IOException {

        final File tempTestFile = File
                .createTempFile("image" + new Date().getTime(), ".png");
        final int width         = 200;
        final int height        = 200;
        final BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D   graphics2D     = bufferedImage.createGraphics();
        final Font font                   = new Font("TimesRoman", Font.BOLD, 20);
        final String message              = "Test";
        final FontMetrics fontMetrics           = graphics2D.getFontMetrics();
        graphics2D.setFont(font);
        final int stringWidth             = fontMetrics.stringWidth(message);
        final int stringHeight            = fontMetrics.getAscent();
        graphics2D.setPaint(Color.black);
        graphics2D.drawString(message, (width - stringWidth) / 2, height / 2 + stringHeight / 4);

        ImageIO.write(bufferedImage, "PNG", tempTestFile);

        final String mimeType = MimeTypeUtils.getMimeType(tempTestFile);

        Assert.assertEquals("The mime type should be image/png","image/png", mimeType);
    }
}
