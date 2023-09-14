package com.dotmarketing.image.colors;

import org.junit.Test;

import java.awt.*;
import java.io.File;

import static org.junit.Assert.*;

public class ColorTaggerTest {


    File file = new File("/Users/will/Desktop/test_image.webp");


    @Test
    public void test_read_image() throws Exception {



        ColorTagger tagger = new ColorTagger(file);

        tagger.readImage();

    }





    @Test
    public void test_color_model() throws Exception {

        Color beige = new Color(0x00F5F3DC);


        System.out.println(ColorModel.nearestColor(beige));

    }
}
