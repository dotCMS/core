package com.dotcms.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Date;

public class TestMediaCreator {

    public static File lookupMOV ()  {

        return new File(ConfigTestHelper.getPathToTestResource("images/testmovie.mov"));
    }

    public static File lookupPNG ()  {

        return new File(ConfigTestHelper.getPathToTestResource("images/test.png"));
    }

    public static File lookupJPG ()  {

        return new File(ConfigTestHelper.getPathToTestResource("images/test.jpg"));
    }

    public static File createPNG () throws IOException  {

        final File tempImageTestFile = File
                .createTempFile("image" + new Date().getTime(), ".png");
        final int width         = 200;
        final int height        = 200;
        final BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D graphics2D     = bufferedImage.createGraphics();
        final Font font                   = new Font("TimesRoman", Font.BOLD, 20);
        final String message              = "Test";
        final FontMetrics fontMetrics           = graphics2D.getFontMetrics();
        graphics2D.setFont(font);
        final int stringWidth             = fontMetrics.stringWidth(message);
        final int stringHeight            = fontMetrics.getAscent();
        graphics2D.setPaint(Color.black);
        graphics2D.drawString(message, (width - stringWidth) / 2, height / 2 + stringHeight / 4);

        ImageIO.write(bufferedImage, "PNG", tempImageTestFile);

        return tempImageTestFile;
    }

    public static File createJPEG () throws IOException  {

        final File tempImageTestFile = File
                .createTempFile("image" + new Date().getTime(), ".jpeg");
        final int width         = 200;
        final int height        = 200;
        final BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D graphics2D     = bufferedImage.createGraphics();
        final Font font                   = new Font("TimesRoman", Font.BOLD, 20);
        final String message              = "Test";
        final FontMetrics fontMetrics     = graphics2D.getFontMetrics();
        graphics2D.setFont(font);
        final int stringWidth             = fontMetrics.stringWidth(message);
        final int stringHeight            = fontMetrics.getAscent();
        graphics2D.setPaint(Color.black);
        graphics2D.drawString(message, (width - stringWidth) / 2, height / 2 + stringHeight / 4);

        ImageIO.write(bufferedImage, "PNG", tempImageTestFile);

        return tempImageTestFile;
    }

}
