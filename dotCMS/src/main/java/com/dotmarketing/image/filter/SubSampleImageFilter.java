package com.dotmarketing.image.filter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import javax.imageio.ImageIO;
import com.dotmarketing.exception.DotRuntimeException;

public class SubSampleImageFilter extends ImageFilter {


    public String[] getAcceptedParameters() {
        return new String[] {"w (int) specifies width", "h (int) specifies height",};
    }

    public File runFilter(final File file, Map<String, String[]> parameters) {
        final String[] widthParam = parameters.get(getPrefix() + "w");
        int width = widthParam != null ? Integer.parseInt(widthParam[0])
                        : 0;
        final String[] heightParam = parameters.get(getPrefix() + "h");
        int height = heightParam != null ? Integer.parseInt(heightParam[0])
                        : 0;



        File resultFile = getResultsFile(file, parameters, "png");

        if (!overwrite(resultFile, parameters)) {
            return resultFile;
        }
        resultFile.delete();

        // subsample from stream
        BufferedImage srcImage = ImageFilterAPI.apiInstance.get().subsampleImage(file, width, height);

        
        File tempResultFile = new File(resultFile.getAbsoluteFile() + "_" + System.currentTimeMillis() +".tmp");

        try{
            ImageIO.write(srcImage, "png", tempResultFile);
            tempResultFile.renameTo(resultFile);
        } catch (Exception e) {
            throw new DotRuntimeException("unable to convert file:" +file + " : " +  e.getMessage(),e);
        }

        return resultFile;

    }
    
    
    @Override
    protected File getResultsFile(File file, Map<String, String[]> parameters, String fileExtension) {
        try {
            return super.getResultsFile(file, parameters, fileExtension);
        }
        catch(Exception e) {
            return new File(System.getProperty("java.io.tmpdir") + file.separator + System.currentTimeMillis() + "." + fileExtension);
        }

    }
    

}
