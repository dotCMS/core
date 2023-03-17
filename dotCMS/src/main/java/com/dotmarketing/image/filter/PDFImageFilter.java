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
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import org.apache.pdfbox.io.MemoryUsageSetting;

public class PDFImageFilter extends ImageFilter {
  static final long PDF_RENDERER_MAX_MEMORY_BYTES = Config.getLongProperty("PDF_RENDERER_MAX_MEMORY_BYTES", 1024L * 1024 * 50);


  public File runFilter(File file, Map<String, String[]> parameters) {


    File resultFile = getResultsFile(file, parameters);

    if (!overwrite(resultFile, parameters)) {
      return resultFile;
    }
    int page = parameters.get(getPrefix() + "page") != null ? Integer.parseInt(parameters.get(getPrefix() + "page")[0]) : 1;

    float dpi = parameters.get(getPrefix() + "dpi") != null ? Float.parseFloat(parameters.get(getPrefix() + "dpi")[0]) : 72f;

    float scale = dpi / 72f;

    final File tempResultFile = new File(resultFile.getAbsoluteFile() + "_" + System.currentTimeMillis() + ".tmp.png");

    try (PDDocument document = PDDocument.load(file, MemoryUsageSetting.setupMixed(PDF_RENDERER_MAX_MEMORY_BYTES))) {
      PDFRenderer pdfRenderer = new PDFRenderer(document);

      pdfRenderer.setSubsamplingAllowed(true);
      BufferedImage bim = pdfRenderer.renderImage(--page, scale);
      ImageIO.write(bim, "PNG", tempResultFile);
      if (!tempResultFile.renameTo(resultFile)) {
        throw new DotRuntimeException("unable to create file:" + resultFile);
      }
      return resultFile;

    } catch (Exception e) {
      throw new DotRuntimeException("unable to convert file:" + file + " : " + e.getMessage(), e);
    }




  }


}
