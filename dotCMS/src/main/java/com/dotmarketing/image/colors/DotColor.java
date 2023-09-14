package com.dotmarketing.image.colors;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Objects;

public class DotColor implements Comparable<DotColor>{


    public final Color color;
    public final String major;
    public final String minor;

    DotColor(Color initial, String major, String minor) {

        this.color = initial;
        this.major = major;
        this.minor = minor;

    }
    DotColor(Color initial){
        this(initial, null, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DotColor)) return false;
        DotColor dotColor = (DotColor) o;
        return Objects.equals(color, dotColor.color) ;
    }

    @Override
    public int compareTo(@NotNull DotColor o) {
        if (Objects.equals(this, o)) return 0;
        if (o.getRGB() == this.getRGB()) return 0;
        return o.getRGB() > this.getRGB() ? 1 : -1;
    }

    @Override
    public int hashCode() {
        return Objects.hash( color, major, minor);
    }

    public int getRGB(){
        return this.color.getRGB();
    }
}
