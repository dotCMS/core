package com.dotcms.uuid.shorty;

import java.io.Serializable;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.UtilMethods;

public class ShortyId implements Serializable {


    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public final String shortId;
    public final String longId;
    public final ShortType type;
    public final ShortType subType;

    public ShortyId(String ShortyId, String longId, ShortType type, ShortType subtype) {
        super();
        if (!UtilMethods.isSet(ShortyId))
            throw new DotStateException("cannot create an empty ShortyId");
        this.shortId = ShortyId;
        this.longId = longId;
        this.type = type;
        this.subType = subtype;
    }

    @Override
    public String toString() {
        return "ShortyId [shortId=" + shortId + ", longId=" + longId + ", type=" + type
                + ", subType=" + subType + "]";
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ShortyId)) {
            return false;
        }
        ShortyId newShorty = (ShortyId) obj;
        return (newShorty.longId == this.longId && newShorty.type == this.type
                && newShorty.subType == this.subType && newShorty.shortId == this.shortId);


    }



}
