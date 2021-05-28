package com.dotmarketing.image.filter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Map;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.luciad.imageio.webp.WebPWriteParam;

public class SubSampleImageFilter extends ImageFilter {

    private final static int maxSize = Config.getIntProperty("IMAGE_MAX_IMAGE_SIZE_PIXELS", 5000);

    public String[] getAcceptedParameters() {
        return new String[] {"w (int) specifies width", "h (int) specifies height",};
    }

    public File runFilter(final File file, Map<String, String[]> parameters) {
        int width = parameters.get(getPrefix() + "w") != null ? Integer.parseInt(parameters.get(getPrefix() + "w")[0])
                        : 0;
        int height = parameters.get(getPrefix() + "h") != null ? Integer.parseInt(parameters.get(getPrefix() + "h")[0])
                        : 0;

        width = Math.min(maxSize, width);
        height = Math.min(maxSize, height);

        File resultFile = getResultsFile(file, parameters, "webp");

        if (!overwrite(resultFile, parameters)) {
            return resultFile;
        }
        resultFile.delete();

        // subsample from stream
        BufferedImage srcImage = ImageFilterAPI.apiInstance.get().subsampleImage(file, width, height);

        final ImageWriter writer = ImageIO.getImageWritersByMIMEType("image/webp").next();
        final WebPWriteParam writeParam = new WebPWriteParam(writer.getLocale());

        writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        writeParam.setCompressionType("Lossless");
        
        try (OutputStream out =Files.newOutputStream(resultFile.toPath())){
            writer.setOutput(out);
            writer.write(null, new IIOImage(srcImage, null, null), writeParam);
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        } finally {
            writer.dispose();
        }

        return resultFile;

    }

}
