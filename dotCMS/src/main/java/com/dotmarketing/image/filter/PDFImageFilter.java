package com.dotmarketing.image.filter;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

public class PDFImageFilter extends ImageFilter {
  public String[] getAcceptedParameters() {
    return new String[] {


    };
  }

  public File runFilter(File file, Map<String, String[]> parameters) {


    File resultFile = getResultsFile(file, parameters);

    if (!overwrite(resultFile, parameters)) {
      return resultFile;
    }
    int page = parameters.get(getPrefix() + "page") != null ? Integer.parseInt(parameters.get(getPrefix() + "page")[0]) : 1;

    int dpi = parameters.get(getPrefix() + "dpi") != null ? Integer.parseInt(parameters.get(getPrefix() + "dpi")[0]) : 72;

        
        
        
    resultFile.delete();
    try {
      
      System.setProperty("sun.java2d.cmm", Config.getStringProperty("IMAGE_COLOR_MANAGEMENT_SYSTEM",  "sun.java2d.cmm.kcms.KcmsServiceProvider"));
      PDDocument document = PDDocument.load(file);
      PDFRenderer pdfRenderer = new PDFRenderer(document);

      BufferedImage bim = pdfRenderer.renderImageWithDPI(--page, dpi, ImageType.RGB);
      Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("png");
      ImageWriter writer = iter.next();
      ImageWriteParam iwp = writer.getDefaultWriteParam();
      BufferedImage dst = new BufferedImage(bim.getWidth(), bim.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
      Graphics2D graphics = dst.createGraphics();

      // graphics.fillRect(0, 0, src.getWidth(), src.getHeight());
      graphics.drawImage(bim, 0, 0, bim.getWidth(), bim.getHeight(), null);
      ImageOutputStream ios = ImageIO.createImageOutputStream(resultFile);
      writer.setOutput(ios);
      writer.write(null, new IIOImage(dst, null, null), iwp);
      ios.flush();
      writer.dispose();
      ios.close();

      document.close();


    } catch (FileNotFoundException e) {
      Logger.error(this.getClass(), e.getMessage());
    } catch (IOException e) {
      Logger.error(this.getClass(), e.getMessage());
    }



    return resultFile;
  }


}
