package com.dotmarketing.image.filter;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.dotmarketing.util.Logger;
import com.twelvemonkeys.image.ResampleOp;

import io.vavr.control.Try;

public class ResizeGifImageFilter extends ImageFilter {
  public String[] getAcceptedParameters() {
    return new String[] {"w (int) specifies width", "h (int) specifies height, loop=true|false, maxFrames (int)",};
  }

  @Override
  protected String getPrefix() {
    // TODO Auto-generated method stub
    return "resize_";
  }

  public File runFilter(final File file, final Map<String, String[]> parameters) {
    double w = Try.of(() -> Integer.parseInt(parameters.get(getPrefix() + "w")[0])).getOrElse(0);
    double h = Try.of(() -> Integer.parseInt(parameters.get(getPrefix() + "h")[0])).getOrElse(0);
    
    final boolean loop = Try.of(() -> Boolean.parseBoolean(parameters.get(getPrefix() + "loop")[0])).getOrElse(true);    
    final int maxFrames = Try.of(() -> Integer.parseInt(parameters.get(getPrefix() + "frames")[0])).getOrElse(-1);
    
    final File resultFile = getResultsFile(file, parameters);

    if (!overwrite(resultFile, parameters)) {
      return resultFile;
    }
    resultFile.delete();

    try {
      BufferedImage src = ImageIO.read(file);
      if (w == 0 && h == 0) {
        return file;
      }
      else if (w == 0 && h > 0) {
        w = Math.round(h * src.getWidth() / src.getHeight());
      }
      else if (w > 0 && h == 0) {
        h = Math.round(w * src.getHeight() / src.getWidth());
      }

      final int width = (int) w;
      final int height = (int) h;

      final ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
      final BufferedImageOp resampler = new ResampleOp(width, height, ResampleOp.FILTER_TRIANGLE); // A good default filter, see class
      try(ImageInputStream stream = ImageIO.createImageInputStream(file)){
        reader.setInput(stream);
        try(ImageOutputStream output = new FileImageOutputStream(resultFile)){
          final ImageFrame[] frames = readGIF(reader, maxFrames);
          final int delay = frames[0].delay < 1 ? 100 : frames[0].delay < 30 ? frames[0].delay * 10 : frames[0].delay;
          final GifSequenceWriter writer = new GifSequenceWriter(output, frames[0].image.getType(), delay, loop);
          for (ImageFrame frame : frames) {
            
            writer.writeToSequence(resampler.filter(frame.image, null));
          }
          writer.close();
        }
      }
    } catch (IOException e) {
      Logger.warnAndDebug(this.getClass(), e);
    }

    return resultFile;
  }

  private ImageFrame[] readGIF(final ImageReader reader, final int maxFrames) throws IOException {
    ArrayList<ImageFrame> frames = new ArrayList<ImageFrame>(2);

    int width = -1;
    int height = -1;

    IIOMetadata metadata = reader.getStreamMetadata();
    if (metadata != null) {
      IIOMetadataNode globalRoot = (IIOMetadataNode) metadata.getAsTree(metadata.getNativeMetadataFormatName());

      NodeList globalScreenDescriptor = globalRoot.getElementsByTagName("LogicalScreenDescriptor");

      if (globalScreenDescriptor != null && globalScreenDescriptor.getLength() > 0) {
        IIOMetadataNode screenDescriptor = (IIOMetadataNode) globalScreenDescriptor.item(0);

        if (screenDescriptor != null) {
          width = Integer.parseInt(screenDescriptor.getAttribute("logicalScreenWidth"));
          height = Integer.parseInt(screenDescriptor.getAttribute("logicalScreenHeight"));
        }
      }
    }

    BufferedImage master = null;
    Graphics2D masterGraphics = null;

    for (int frameIndex = 0;; frameIndex++) {
      BufferedImage image;
      if(maxFrames>0 && frameIndex>maxFrames) {
        break;
      }
      try {
        image = reader.read(frameIndex);
      } catch (IndexOutOfBoundsException io) {
        break;
      }

      if (width == -1 || height == -1) {
        width = image.getWidth();
        height = image.getHeight();
      }

      IIOMetadataNode root = (IIOMetadataNode) reader.getImageMetadata(frameIndex).getAsTree("javax_imageio_gif_image_1.0");
      IIOMetadataNode gce = (IIOMetadataNode) root.getElementsByTagName("GraphicControlExtension").item(0);
      int delay = Integer.valueOf(gce.getAttribute("delayTime"));
      String disposal = gce.getAttribute("disposalMethod");

      int x = 0;
      int y = 0;

      if (master == null) {
        master = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        masterGraphics = master.createGraphics();
        masterGraphics.setBackground(new Color(0, 0, 0, 0));
      } else {
        NodeList children = root.getChildNodes();
        for (int nodeIndex = 0; nodeIndex < children.getLength(); nodeIndex++) {
          Node nodeItem = children.item(nodeIndex);
          if (nodeItem.getNodeName().equals("ImageDescriptor")) {
            NamedNodeMap map = nodeItem.getAttributes();
            x = Integer.valueOf(map.getNamedItem("imageLeftPosition").getNodeValue());
            y = Integer.valueOf(map.getNamedItem("imageTopPosition").getNodeValue());
          }
        }
      }
      masterGraphics.drawImage(image, x, y, null);

      BufferedImage copy = new BufferedImage(master.getColorModel(), master.copyData(null), master.isAlphaPremultiplied(), null);
      frames.add(new ImageFrame(copy, delay, disposal));

      if (disposal.equals("restoreToPrevious")) {
        BufferedImage from = null;
        for (int i = frameIndex - 1; i >= 0; i--) {
          if (!frames.get(i).disposal.equals("restoreToPrevious") || frameIndex == 0) {
            from = frames.get(i).image;
            break;
          }
        }

        master = new BufferedImage(from.getColorModel(), from.copyData(null), from.isAlphaPremultiplied(), null);
        masterGraphics = master.createGraphics();
        masterGraphics.setBackground(new Color(0, 0, 0, 0));
      } else if (disposal.equals("restoreToBackgroundColor")) {
        masterGraphics.clearRect(x, y, image.getWidth(), image.getHeight());
      }
    }
    reader.dispose();

    return frames.toArray(new ImageFrame[frames.size()]);
  }

  private class ImageFrame {
     final int delay;
     final BufferedImage image;
     final String disposal;

    public ImageFrame(BufferedImage image, int delay, String disposal) {
      this.image = image;
      this.delay = delay;
      this.disposal = disposal;
    }


  }
  public class GifSequenceWriter {
    protected ImageWriter gifWriter;
    protected ImageWriteParam imageWriteParam;
    protected IIOMetadata imageMetaData;

    /**
     * Creates a new GifSequenceWriter
     * 
     * @param outputStream the ImageOutputStream to be written to
     * @param imageType one of the imageTypes specified in BufferedImage
     * @param timeBetweenFramesMS the time between frames in miliseconds
     * @param loopContinuously wether the gif should loop repeatedly
     * @throws IIOException if no gif ImageWriters are found
     *
     * @author Elliot Kroo (elliot[at]kroo[dot]net)
     * and licensed under https://creativecommons.org/licenses/by/3.0/
     */
    public GifSequenceWriter(final ImageOutputStream outputStream, final int imageType, final int timeBetweenFramesMS, final boolean loopContinuously)
        throws IIOException, IOException {
      // my method to create a writer
      gifWriter = getWriter();
      imageWriteParam = gifWriter.getDefaultWriteParam();
      ImageTypeSpecifier imageTypeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(imageType);

      imageMetaData = gifWriter.getDefaultImageMetadata(imageTypeSpecifier, imageWriteParam);

      String metaFormatName = imageMetaData.getNativeMetadataFormatName();

      IIOMetadataNode root = (IIOMetadataNode) imageMetaData.getAsTree(metaFormatName);

      IIOMetadataNode graphicsControlExtensionNode = getNode(root, "GraphicControlExtension");

      graphicsControlExtensionNode.setAttribute("disposalMethod", "none");
      graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE");
      graphicsControlExtensionNode.setAttribute("transparentColorFlag", "FALSE");
      graphicsControlExtensionNode.setAttribute("delayTime", Integer.toString(timeBetweenFramesMS / 10));
      graphicsControlExtensionNode.setAttribute("transparentColorIndex", "0");

      IIOMetadataNode commentsNode = getNode(root, "CommentExtensions");
      commentsNode.setAttribute("CommentExtension", "Created by MAH");

      IIOMetadataNode appEntensionsNode = getNode(root, "ApplicationExtensions");

      IIOMetadataNode child = new IIOMetadataNode("ApplicationExtension");

      child.setAttribute("applicationID", "NETSCAPE");
      child.setAttribute("authenticationCode", "2.0");

      int loop = loopContinuously ? 0 : 1;

      child.setUserObject(new byte[] {0x1, (byte) (loop & 0xFF), (byte) ((loop >> 8) & 0xFF)});
      appEntensionsNode.appendChild(child);

      imageMetaData.setFromTree(metaFormatName, root);

      gifWriter.setOutput(outputStream);

      gifWriter.prepareWriteSequence(null);
    }

    public void writeToSequence(RenderedImage img) throws IOException {
      gifWriter.writeToSequence(new IIOImage(img, null, imageMetaData), imageWriteParam);
    }

    /**
     * Close this GifSequenceWriter object. This does not close the underlying stream, just finishes off
     * the GIF.
     */
    public void close() throws IOException {
      gifWriter.endWriteSequence();
    }

    /**
     * Returns the first available GIF ImageWriter using ImageIO.getImageWritersBySuffix("gif").
     * 
     * @return a GIF ImageWriter object
     * @throws IIOException if no GIF image writers are returned
     */
    private ImageWriter getWriter() throws IIOException {
      Iterator<ImageWriter> iter = ImageIO.getImageWritersBySuffix("gif");
      if (!iter.hasNext()) {
        throw new IIOException("No GIF Image Writers Exist");
      } else {
        return iter.next();
      }
    }

    /**
     * Returns an existing child node, or creates and returns a new child node (if the requested node
     * does not exist).
     * 
     * @param rootNode the <tt>IIOMetadataNode</tt> to search for the child node.
     * @param nodeName the name of the child node.
     * 
     * @return the child node, if found or a new node created with the given name.
     */
    private IIOMetadataNode getNode(IIOMetadataNode rootNode, String nodeName) {
      int nNodes = rootNode.getLength();
      for (int i = 0; i < nNodes; i++) {
        if (rootNode.item(i).getNodeName().compareToIgnoreCase(nodeName) == 0) {
          return ((IIOMetadataNode) rootNode.item(i));
        }
      }
      IIOMetadataNode node = new IIOMetadataNode(nodeName);
      rootNode.appendChild(node);
      return (node);
    }

  }

}
