package com.dotmarketing.image.filter;

import java.awt.Dimension;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
public class ResizeCalcTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {}

    
    Dimension square = new Dimension(500,500);
    
    Dimension landscape = new Dimension(1000,500);
    
    Dimension portrait = new Dimension(500,1000);
    
    Dimension squareSmall = new Dimension(50,50);
    
    Dimension portraitSmall = new Dimension(50,100);
    
    Dimension landscapeSmall = new Dimension(100,50);
    
    
    @Test
    public void test_square_resize_both() {
        
        ResizeCalc calc = new ResizeCalc.Builder(square)
                        .desiredHeight(700)
                        .desiredWidth(700)
                        .build();
        
        calc = new ResizeCalc.Builder(square)
                        .desiredHeight(240)
                        .desiredWidth(780)
                        .build();
                
        
        assertEquals(780,calc.getDim().width);
        assertEquals(240,calc.getDim().height);
        
    }

    @Test
    public void test_square_resize_width() {
        
        ResizeCalc calc = new ResizeCalc.Builder(square)
                        .desiredWidth(700)
                        .build();
        
        assertEquals(700,calc.getDim().width);
        assertEquals(700,calc.getDim().height);
        
        calc = new ResizeCalc.Builder(square)
                        .desiredWidth(240)
                        .build();
        
        assertEquals(240,calc.getDim().width);
        assertEquals(240,calc.getDim().height);
    }
    
    @Test
    public void test_square_resize_height() {
        
        ResizeCalc calc = new ResizeCalc.Builder(square)
                        .desiredHeight(700)
                        .build();
        
        assertEquals(700,calc.getDim().width);
        assertEquals(700,calc.getDim().height);
        calc = new ResizeCalc.Builder(square)
                        .desiredHeight(240)
                        .build();
        
        assertEquals(240,calc.getDim().width);
        assertEquals(240,calc.getDim().height);
        
        
        calc = new ResizeCalc.Builder(squareSmall)
                        .desiredHeight(700)
                        .build();
        
        assertEquals(700,calc.getDim().width);
        assertEquals(700,calc.getDim().height);
    }
    
    
    @Test
    public void test_landscape_resize() {
        
        ResizeCalc calc = new ResizeCalc.Builder(landscape)
                        .desiredHeight(1000)
                        .build();
        
        assertEquals(2000,calc.getDim().width);
        assertEquals(1000,calc.getDim().height);

        
        calc = new ResizeCalc.Builder(landscape)
                        .desiredHeight(250)
                        .build();
        
        assertEquals(500,calc.getDim().width);
        assertEquals(250,calc.getDim().height);

        
    }
    
    @Test
    public void test_portrait_resize() {
        
        ResizeCalc calc = new ResizeCalc.Builder(portrait)
                        .desiredHeight(2000)
                        .build();
        
        assertEquals(1000,calc.getDim().width);
        assertEquals(2000,calc.getDim().height);

        
        calc = new ResizeCalc.Builder(portrait)
                        .desiredHeight(500)
                        .build();
        
        assertEquals(250,calc.getDim().width);
        assertEquals(500,calc.getDim().height);

        
    }
    
    @Test
    public void test_portrait_maxWidth() {
        
        ResizeCalc calc = new ResizeCalc.Builder(portrait)
                        .maxWidth(250)
                        .build();
        
        assertEquals(250,calc.getDim().width);
        assertEquals(500, calc.getDim().height);

        
    }
    
    @Test
    public void test_portrait_maxHeight() {
        
        ResizeCalc calc = new ResizeCalc.Builder(portrait)
                        .maxHeight(500)
                        .build();
        
        assertEquals(250,calc.getDim().width);
        assertEquals(500,calc.getDim().height);

        
    }
    
    @Test
    public void test_smallerThan_max() {
        
        ResizeCalc calc = new ResizeCalc.Builder(portraitSmall)
                        .maxHeight(500)
                        .build();
        
        assertEquals(portraitSmall,calc.getDim());

        
        calc = new ResizeCalc.Builder(portraitSmall)
                        .maxWidth(500)
                        .build();
        
        assertEquals(portraitSmall,calc.getDim());

    }
    

    
    /**
     * If both a max width and max height are passed, we will resize the image proportionally in the
     * most restrictive (smallest) dimension - thus respecting both max parameters.
     */
    @Test
    public void test_bothMaxParams() {
        
        ResizeCalc calc = new ResizeCalc.Builder(portrait)
                        .maxWidth(100)
                        .maxHeight(500)
                        .build();
        
        assertEquals(100,calc.getDim().width);
        assertEquals(200,calc.getDim().height);
        
        calc = new ResizeCalc.Builder(landscape)
                        .maxWidth(500)
                        .maxHeight(100)
                        .build();

        assertEquals(200,calc.getDim().width);
        assertEquals(100,calc.getDim().height);
        
    }
    
    
    
}
