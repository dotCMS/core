package com.dotcms.util;

import com.dotcms.UnitTestBase;
import com.dotcms.contenttype.business.ContentTypeAPIImpl;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.portlets.workflows.actionlet.PublishContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.SaveContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.SaveContentAsDraftActionlet;
import com.dotmarketing.portlets.workflows.actionlet.UnpublishContentActionlet;
import com.dotmarketing.portlets.workflows.business.BaseWorkflowIntegrationTest;
import com.dotmarketing.portlets.workflows.model.WorkflowState;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.util.StringPool;
import com.twelvemonkeys.imageio.plugins.jpeg.JPEGImageWriter;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.dotcms.util.CollectionsUtils.*;
import static org.junit.Assert.*;

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
        FontMetrics fontMetrics           = graphics2D.getFontMetrics();
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