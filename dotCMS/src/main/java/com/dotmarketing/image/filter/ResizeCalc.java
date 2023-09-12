package com.dotmarketing.image.filter;

import java.awt.Dimension;
import javax.annotation.Nonnull;


public class ResizeCalc {

    private final int originalWidth;
    private final int originalHeight;
    private final int desiredWidth;
    private final int desiredHeight;
    private final int maxWidth;
    private final int maxHeight;
    private final int minWidth;
    private final int minHeight;

    private ResizeCalc(Builder builder) {
        this.originalWidth = builder.originalWidth;
        this.originalHeight = builder.originalHeight;
        this.desiredWidth = builder.desiredWidth;
        this.desiredHeight = builder.desiredHeight;
        this.maxWidth = builder.maxWidth;
        this.maxHeight = builder.maxHeight;
        this.minWidth = builder.minWidth;
        this.minHeight = builder.minHeight;

    }


    public Dimension getDim() {

        
        
        // if we have a width and/or height, respect it and ignore maxw and maxh
        if (desiredWidth > 0 || desiredHeight > 0) {
            return doResize();
        }
        
        if (maxWidth <= 0 && maxHeight <= 0 && minWidth <= 0 && minHeight <= 0) {
            return doNothing();
        }
        
        // if the source is smaller than maxw && maxh, ignore
        if (maxWidth >= originalWidth && maxHeight >= originalHeight) {
            return doNothing();
        }

        // if both maxw and maxh are set, figure out which to respect
        if (maxWidth > 0 && maxHeight > 0) {
            return doMaxWidthAndHeight();
        }

        // only max width
        if (maxWidth > 0) {
            return doMaxWidth();
        }
        
        // only max height
        if (maxHeight > 0) {
            return doMaxHeight();
        }
        
        // if the source is smaller than minw && minh, ignore
        if (minWidth <= originalWidth && minHeight <= originalHeight) {
            return doNothing();
        }
        
        // if both minw and minh are set, figure out which to respect
        if (minWidth > 0 && minHeight > 0) {
            return doMinWidthAndHeight();
        }

        // only max width
        if (minWidth > 0) {
            return doMinWidth();
        }
        
        // only max height
        if (minHeight > 0) {
            return doMinHeight();
        }
        
        
        return doNothing();
    }


    private Dimension doResize() {
        int finalWidth = desiredWidth == 0 && desiredHeight > 0 ? desiredHeight * originalWidth / originalHeight : desiredWidth;
        int finalHeight = desiredWidth > 0 && desiredHeight == 0 ? desiredWidth * originalHeight / originalWidth : desiredHeight;
        return new Dimension(finalWidth, finalHeight);
    }

    private Dimension doNothing() {

        return new Dimension(originalWidth, originalHeight);
    }

    private Dimension doMaxWidthAndHeight() {

        int testHeight = (maxWidth * originalHeight) / originalWidth;
        int testWidth = ((maxHeight * originalWidth) / originalHeight);
        int finalWidth = maxWidth > testWidth ? testWidth : maxWidth;
        int finalHeight = maxHeight > testHeight ? testHeight : maxHeight;
        return new Dimension(finalWidth, finalHeight);
    }

    private Dimension doMinWidthAndHeight() {

        int testHeight = (minWidth * originalHeight) / originalWidth;
        int testWidth = ((minHeight * originalWidth) / originalHeight);
        int finalWidth = minWidth < testWidth ? testWidth : minWidth;
        int finalHeight = minHeight < testHeight ? testHeight : minHeight;
        return new Dimension(finalWidth, finalHeight);
    }
    
    private Dimension doMinWidth() {

        int finalWidth = minWidth > originalWidth ? minWidth : originalWidth;
        int finalHeight = (finalWidth * originalHeight) / originalWidth;
        return new Dimension(finalWidth, finalHeight);
    }
    
    private Dimension doMinHeight() {

        int finalHeight = minHeight > originalHeight ? minHeight : originalHeight ;
        int finalWidth = (finalHeight * originalWidth) / originalHeight;
        return new Dimension(finalWidth, finalHeight);
    }
    
    private Dimension doMaxWidth() {

        int finalWidth = maxWidth > originalWidth ? originalWidth : maxWidth;
        int finalHeight = (finalWidth * originalHeight) / originalWidth;
        return new Dimension(finalWidth, finalHeight);
    }

    private Dimension doMaxHeight() {

        int finalHeight = maxHeight > originalHeight ? originalHeight : maxHeight;
        int finalWidth = (finalHeight * originalWidth) / originalHeight;
        return new Dimension(finalWidth, finalHeight);
    }

    public static final class Builder {
        private int originalWidth;
        private int originalHeight;
        private int desiredWidth;
        private int desiredHeight;
        private int maxWidth;
        private int maxHeight;
        private int minWidth;
        private int minHeight;

        public Builder(int originalWidth, int originalHeight) {
            this.originalWidth = originalWidth;
            this.originalHeight = originalHeight;
        }

        public Builder(Dimension dim) {
            this.originalWidth = dim.width;
            this.originalHeight = dim.height;
        }


        public Builder desiredWidth(@Nonnull int desiredWidth) {
            this.desiredWidth = desiredWidth;
            return this;
        }

        public Builder desiredHeight(@Nonnull int desiredHeight) {
            this.desiredHeight = desiredHeight;
            return this;
        }

        public Builder maxWidth(@Nonnull int maxWidth) {
            this.maxWidth = maxWidth;
            return this;
        }

        public Builder maxHeight(@Nonnull int maxHeight) {
            this.maxHeight = maxHeight;
            return this;
        }
        public Builder minWidth(@Nonnull int minWidth) {
            this.minWidth = minWidth;
            return this;
        }

        public Builder minHeight(@Nonnull int minHeight) {
            this.minHeight = minHeight;
            return this;
        }
        public ResizeCalc build() {
            return new ResizeCalc(this);
        }
    }



}
