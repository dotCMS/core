package com.dotmarketing.image.colors;

import com.dotmarketing.image.filter.ResizeImageFilter;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.util.Map;

public class ColorTagger {


    public ColorTagger(File imageFile) {
        this.ogImage = imageFile;
    }

    final File ogImage;
    final Multiset<DotColor> colorTags = HashMultiset.create();
    private int width;
    private int height;
    private boolean hasAlphaChannel;
    private int pixelLength;
    private byte[] pixels;


    public void readImage() throws Exception {

        final BufferedImage image = ImageIO.read(resizeForReading());

        pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        width = image.getWidth();
        height = image.getHeight();


        hasAlphaChannel = image.getAlphaRaster() != null;
        pixelLength = 3;
        if (hasAlphaChannel) {
            pixelLength = 4;
        }


        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = new Color(getRGB(x, y));
                DotColor dotColor = ColorModel.nearestColor(color);
                if(dotColor.color.equals(Color.BLACK)){
                    continue;
                }

                    colorTags.add(dotColor);

            }


        }

        System.out.println("Top colors for :" + ogImage);
        System.out.println("Found : " + colorTags.size() + " colors");
        int i = 0;
        for (DotColor key : Multisets.copyHighestCountFirst(colorTags).elementSet()) {
            System.out.println(i++ + " " + key.major + " : " + key.minor + " count: " + colorTags.count(key));

        }

    }


    int getRGB(int x, int y) {
        int pos = (y * pixelLength * width) + (x * pixelLength);

        int argb = -16777216; // 255 alpha
        if (hasAlphaChannel) {
            argb = (((int) pixels[pos++] & 0xff) << 24); // alpha
        }

        argb += ((int) pixels[pos++] & 0xff); // blue
        argb += (((int) pixels[pos++] & 0xff) << 8); // green
        argb += (((int) pixels[pos++] & 0xff) << 16); // red
        return argb;
    }


    File resizeForReading(){
        return ogImage;
       //Map<String,String[]> params = ImmutableMap.of("maxw", new String[]{"500"}, "maxh", new String[]{"500"});
       // return new ResizeImageFilter().runFilter(ogImage,params);



    }





}
