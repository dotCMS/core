package com.dotmarketing.image.focalpoint;

import java.io.Serializable;
import io.vavr.control.Try;

public class FocalPoint implements Serializable {

    private static final long serialVersionUID = 1L;
    public final float x, y;

    public FocalPoint(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    public FocalPoint(String focalPointStr) {
        this.x=Try.of(()-> Float.parseFloat(focalPointStr.split(",")[0])).getOrElse(0f);
        this.y=Try.of(()-> Float.parseFloat(focalPointStr.split(",")[1])).getOrElse(0f);
    }
    
    @Override
    public String toString() {
        return this.x + "," + this.y;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Float.floatToIntBits(x);
        result = prime * result + Float.floatToIntBits(y);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FocalPoint other = (FocalPoint) obj;
        if (Float.floatToIntBits(x) != Float.floatToIntBits(other.x))
            return false;
        if (Float.floatToIntBits(y) != Float.floatToIntBits(other.y))
            return false;
        return true;
    }

    
    
}
