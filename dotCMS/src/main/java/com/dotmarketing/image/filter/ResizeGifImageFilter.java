package com.dotmarketing.image.filter;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

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

  public File runFilter(final File incomingFile, final Map<String, String[]> parameters) {
    double w = Try.of(() -> Integer.parseInt(parameters.get(getPrefix() + "w")[0])).getOrElse(0);
    double h = Try.of(() -> Integer.parseInt(parameters.get(getPrefix() + "h")[0])).getOrElse(0);

    final boolean loop = Try.of(() -> Boolean.parseBoolean(parameters.get(getPrefix() + "loop")[0])).getOrElse(true);
    final int maxFrames = Try.of(() -> Integer.parseInt(parameters.get(getPrefix() + "frames")[0])).getOrElse(Integer.MAX_VALUE);

    final File resultFile = getResultsFile(incomingFile, parameters);

    if (!overwrite(resultFile, parameters)) {
      return resultFile;
    }
    resultFile.delete();
    final Optional<ImageFrame> masterFrameOpt = getMasterFrame(incomingFile);
    if(!masterFrameOpt.isPresent()) {
      return incomingFile;
    }
    final ImageFrame masterFrame = masterFrameOpt.get();
    
    if (w == 0 && h == 0) {
      return incomingFile;
    } else if (w == 0 && h > 0) {
      w = Math.round(h * masterFrame.width / masterFrame.height);
    } else if (w > 0 && h == 0) {
      h = Math.round(w * masterFrame.height / masterFrame.width);
    }

    final BufferedImageOp resampler = new ResampleOp((int) w, (int) h, ResampleOp.FILTER_TRIANGLE); // A good default filter, see class

    try {
      final ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
      try (ImageInputStream stream = ImageIO.createImageInputStream(incomingFile)) {
        reader.setInput(stream);
        try (ImageOutputStream output = new FileImageOutputStream(resultFile)) {
          ImageFrame previousFrame = masterFrame;
          for (int i = 0; i < maxFrames; i++) {
            final Optional<ImageFrame> frameOpt = imageToFrame(reader, i, previousFrame);
            if(!frameOpt.isPresent()) {
              break;
            }
            previousFrame = frameOpt.get();
            try (final GifSequenceWriter writer = new GifSequenceWriter(output, masterFrame.image.getType(), masterFrame.delay, loop)) {
              writer.writeToSequence(resampler.filter(frameOpt.get().image, null));
            }
          }
        }
      }
    }
    catch (Exception e) {
      Logger.warnAndDebug(this.getClass(), e);
    }

    return resultFile;
  }

  private BufferedImage getImageFromGif(final ImageReader reader, final int frameIndex) throws IOException {
    try {
      return reader.read(frameIndex);
    } catch (IndexOutOfBoundsException io) {
      return null;
    }
  }

  private Optional<ImageFrame> getMasterFrame(final File gifFile)  {
    int width = -1;
    int height = -1;
    final ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
    try (ImageInputStream stream = ImageIO.createImageInputStream(gifFile)) {
      reader.setInput(stream);
      final IIOMetadata metadata = reader.getStreamMetadata();

      final IIOMetadataNode globalRoot = (IIOMetadataNode) metadata.getAsTree(metadata.getNativeMetadataFormatName());

      final NodeList globalScreenDescriptor = globalRoot.getElementsByTagName("LogicalScreenDescriptor");

      if (globalScreenDescriptor != null && globalScreenDescriptor.getLength() > 0) {
        final IIOMetadataNode screenDescriptor = (IIOMetadataNode) globalScreenDescriptor.item(0);

        if (screenDescriptor != null) {
          width = Integer.parseInt(screenDescriptor.getAttribute("logicalScreenWidth"));
          height = Integer.parseInt(screenDescriptor.getAttribute("logicalScreenHeight"));
        }
      }

      if (width == -1 || height == -1) {
        BufferedImage image = this.getImageFromGif(reader, 0);
        width = image.getWidth();
        height = image.getHeight();
      }

      IIOMetadataNode root = (IIOMetadataNode) reader.getImageMetadata(0).getAsTree("javax_imageio_gif_image_1.0");
      IIOMetadataNode gce = (IIOMetadataNode) root.getElementsByTagName("GraphicControlExtension").item(0);
   
      final int delay = Integer.valueOf(gce.getAttribute("delayTime"));
      final String disposalMethod = gce.getAttribute("disposalMethod");
      reader.dispose();

      return Optional.of(new ImageFrame(null, delay, disposalMethod, width, height, null));

    }
    catch(Exception e) {
      Logger.warnAndDebug(this.getClass(), "unable to get image meta, " + e.getMessage(),e);
    }
    return Optional.empty();

  }

  private Optional<ImageFrame> imageToFrame(final ImageReader reader, final int frameIndex, final ImageFrame previousFrame) throws IOException {

    BufferedImage image = getImageFromGif(reader, frameIndex);
    if(image==null) return Optional.empty();
    BufferedImage master = previousFrame.image;
    Graphics2D masterGraphics = previousFrame.masterGraphics;

    int x = 0;
    int y = 0;

    IIOMetadataNode root = (IIOMetadataNode) reader.getImageMetadata(frameIndex).getAsTree("javax_imageio_gif_image_1.0");
    NodeList children = root.getChildNodes();
    for (int nodeIndex = 0; nodeIndex < children.getLength(); nodeIndex++) {
      Node nodeItem = children.item(nodeIndex);
      if (nodeItem.getNodeName().equals("ImageDescriptor")) {
        NamedNodeMap map = nodeItem.getAttributes();
        x = Integer.valueOf(map.getNamedItem("imageLeftPosition").getNodeValue());
        y = Integer.valueOf(map.getNamedItem("imageTopPosition").getNodeValue());
      }
    }

    masterGraphics.drawImage(image, x, y, null);

    final BufferedImage copy = new BufferedImage(master.getColorModel(), master.copyData(null), master.isAlphaPremultiplied(), null);

    if (previousFrame.disposal.equals("restoreToPrevious")) {
      BufferedImage from = previousFrame.image;
      master = new BufferedImage(from.getColorModel(), from.copyData(null), from.isAlphaPremultiplied(), null);
      masterGraphics = master.createGraphics();
      masterGraphics.setBackground(new Color(0, 0, 0, 0));
    } else if (previousFrame.disposal.equals("restoreToBackgroundColor")) {
      masterGraphics.clearRect(x, y, image.getWidth(), image.getHeight());
    }
    ImageFrame frame =
        new ImageFrame(copy, previousFrame.delay, previousFrame.disposal, previousFrame.width, previousFrame.height, masterGraphics);
    return Optional.of(frame);
  }

  private class ImageFrame {
    final int delay;
    final BufferedImage image;
    final String disposal;
    final int width;
    final int height;
    final Graphics2D masterGraphics;

    public ImageFrame(BufferedImage image, int incomingDelay, String disposal, int width, int height, Graphics2D incomingMaster) {
      this.delay = incomingDelay < 1 ? 100 : incomingDelay < 30 ? incomingDelay * 10 : incomingDelay;
      this.disposal = disposal;
      this.width = width;
      this.height = height;
      this.image = initMasterImage(image);

      this.masterGraphics = initMasterGraphics(incomingMaster);
    }

    private BufferedImage initMasterImage(BufferedImage incomingMaster) {
      if (incomingMaster == null) {
        incomingMaster = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);
      }
      return incomingMaster;
    }

    private Graphics2D initMasterGraphics(Graphics2D incomingMaster) {
      if (incomingMaster == null) {
        BufferedImage master = this.image;
        incomingMaster = master.createGraphics();
        incomingMaster.setBackground(new Color(0, 0, 0, 0));
      }
      return incomingMaster;
    }
  }
  public class GifSequenceWriter implements AutoCloseable {
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
     * @author Elliot Kroo (elliot[at]kroo[dot]net) and licensed under
     *         https://creativecommons.org/licenses/by/3.0/
     */
    public GifSequenceWriter(final ImageOutputStream outputStream, final int imageType, final int timeBetweenFramesMS,
        final boolean loopContinuously) throws IIOException, IOException {
      // my method to create a writer
      gifWriter = getWriter();
      imageWriteParam = gifWriter.getDefaultWriteParam();
      final ImageTypeSpecifier imageTypeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(imageType);

      imageMetaData = gifWriter.getDefaultImageMetadata(imageTypeSpecifier, imageWriteParam);

      final String metaFormatName = imageMetaData.getNativeMetadataFormatName();

      final IIOMetadataNode root = (IIOMetadataNode) imageMetaData.getAsTree(metaFormatName);

      final IIOMetadataNode graphicsControlExtensionNode = getNode(root, "GraphicControlExtension");

      graphicsControlExtensionNode.setAttribute("disposalMethod", "none");
      graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE");
      graphicsControlExtensionNode.setAttribute("transparentColorFlag", "FALSE");
      graphicsControlExtensionNode.setAttribute("delayTime", Integer.toString(timeBetweenFramesMS / 10));
      graphicsControlExtensionNode.setAttribute("transparentColorIndex", "0");

      final IIOMetadataNode commentsNode = getNode(root, "CommentExtensions");
      commentsNode.setAttribute("CommentExtension", "Created by MAH");

      final IIOMetadataNode appEntensionsNode = getNode(root, "ApplicationExtensions");

      final IIOMetadataNode child = new IIOMetadataNode("ApplicationExtension");

      child.setAttribute("applicationID", "NETSCAPE");
      child.setAttribute("authenticationCode", "2.0");

      final int loop = loopContinuously ? 0 : 1;

      child.setUserObject(new byte[] {0x1, (byte) (loop & 0xFF), (byte) ((loop >> 8) & 0xFF)});
      appEntensionsNode.appendChild(child);

      imageMetaData.setFromTree(metaFormatName, root);

      gifWriter.setOutput(outputStream);

      gifWriter.prepareWriteSequence(null);
    }

    public void writeToSequence(final RenderedImage img) throws IOException {
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
      final Iterator<ImageWriter> iter = ImageIO.getImageWritersBySuffix("gif");
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
      final int nNodes = rootNode.getLength();
      for (int i = 0; i < nNodes; i++) {
        if (rootNode.item(i).getNodeName().compareToIgnoreCase(nodeName) == 0) {
          return ((IIOMetadataNode) rootNode.item(i));
        }
      }
      final IIOMetadataNode node = new IIOMetadataNode(nodeName);
      rootNode.appendChild(node);
      return (node);
    }

  }
  @Override
  public String[] getAcceptedParameters() {
    return new String[] {"w (int) specifies width", "h (int) specifies height, loop=true|false, maxFrames (int)",};
  }

  @Override
  protected String getPrefix() {
    return "resize_";
  }

}
