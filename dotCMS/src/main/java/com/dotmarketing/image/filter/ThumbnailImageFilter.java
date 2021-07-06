package com.dotmarketing.image.filter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import javax.imageio.ImageIO;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;

import com.dotmarketing.util.Logger;

public class ThumbnailImageFilter extends ImageFilter {
    public String[] getAcceptedParameters() {
        return new String[] {"w (int) specifies width", "h (int) specifies height",
                "bg (int) must be 9 digits of rgb (000000000=black, 255255255=white) for background color"

        };
    }

    public static final int DEFAULT_HEIGHT = Config.getIntProperty("DEFAULT_HEIGHT", 100);
    public static final int DEFAULT_WIDTH = Config.getIntProperty("DEFAULT_WIDTH", 100);
    public static final Color DEFAULT_BG_COLOR = new Color(Config.getIntProperty("DEFAULT_BG_R_COLOR"),
                    Config.getIntProperty("DEFAULT_BG_G_COLOR"), Config.getIntProperty("DEFAULT_BG_B_COLOR"));

    public File runFilter(File file, Map<String, String[]> parameters) {

        int height = parameters.get(getPrefix() + "h") != null ? Integer.parseInt(parameters.get(getPrefix() + "h")[0])
                        : 0;
        int width = parameters.get(getPrefix() + "w") != null ? Integer.parseInt(parameters.get(getPrefix() + "w")[0])
                        : 0;
        String rgb = parameters.get(getPrefix() + "bg") != null ? parameters.get(getPrefix() + "bg")[0] : "255255255";
        Color color = new Color(Integer.parseInt(rgb.substring(0, 3)), Integer.parseInt(rgb.substring(3, 6)),
                        Integer.parseInt(rgb.substring(6)));

        File resultFile = getResultsFile(file, parameters);

        if (!overwrite(resultFile, parameters)) {
            return resultFile;
        }

        resultFile.delete();
        try {

            if (height <= 0 && width <= 0) {
                height = DEFAULT_HEIGHT;
                width = DEFAULT_WIDTH;
            }

            if (color == null) {
                color = DEFAULT_BG_COLOR;
            }

            Dimension widthHeight = ImageFilterAPI.apiInstance.get().getWidthHeight(file);

            // determine thumbnail size from WIDTH and HEIGHT
            int imageWidth = widthHeight.width;
            int imageHeight = widthHeight.height;
            double imageRatio = (double) imageWidth / (double) imageHeight;

            int thumbWidth = width;
            int thumbHeight = height;
            if (thumbWidth <= 0)
                thumbWidth = (int) (thumbHeight * imageRatio);
            if (thumbHeight <= 0)
                thumbHeight = (int) (thumbWidth / imageRatio);
            double thumbRatio = (double) thumbWidth / (double) thumbHeight;

            if (thumbRatio < imageRatio) {
                thumbHeight = (int) Math.ceil((thumbWidth / imageRatio));
            } else {
                thumbWidth = (int) Math.ceil((thumbHeight * imageRatio));
            }

            if (thumbWidth == 0)
                thumbWidth = 1;
            if (thumbHeight == 0)
                thumbHeight = 1;

            if (width <= 0)
                width = (int) Math.ceil(height * imageRatio);
            if (height <= 0)
                height = (int) Math.ceil(width / imageRatio);

            // draw original image to thumbnail image object and
            // scale it to the new size on-the-fly
            BufferedImage bgImage = new BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
            Graphics2D resultGraphics = bgImage.createGraphics();
            resultGraphics.setColor(color);
            resultGraphics.fillRect(0, 0, width, height);

            BufferedImage thumbImage =
                            ImageFilterAPI.apiInstance.get().intelligentResize(file, thumbWidth, thumbHeight);

            // compute offsets to center image in its space
            int offsetX = (width - thumbImage.getWidth()) / 2;
            int offsetY = (height - thumbImage.getHeight()) / 2;

            resultGraphics.drawImage(thumbImage, null, offsetX, offsetY);
            resultGraphics.dispose();

            final File tempResultFile =
                            new File(resultFile.getAbsoluteFile() + "_" + System.currentTimeMillis() + ".tmp");

            ImageIO.write(bgImage, "png", tempResultFile);
            bgImage.flush();

            tempResultFile.renameTo(resultFile);
        } catch (Exception e) {
            throw new DotRuntimeException("unable to convert file:" + file + " : " + e.getMessage(), e);
        }

        return resultFile;

    }

}
