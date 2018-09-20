package com.dotmarketing.portlets.contentlet.model;

import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.util.UtilMethods;
import java.io.Serializable;

public class ContentletVersionInfo extends VersionInfo implements Serializable {
    private static final long serialVersionUID = 8952464908349482530L;

    private long lang;
    public long getLang() {
        return lang;
    }
    public void setLang(long lang) {
        this.lang = lang;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof ContentletVersionInfo) {
            ContentletVersionInfo vinfo=(ContentletVersionInfo)obj;
            return UtilMethods.isSet(this.getIdentifier()) && UtilMethods.isSet(vinfo.getIdentifier())
                    && this.getIdentifier().equals(vinfo.getIdentifier()) && lang==vinfo.getLang();
        }
        else
            return false;
    }

    @Override
    public int hashCode() {
        int langx=(int)lang;
        return getIdentifier().hashCode()+17*(langx+1);
    }

    @Override
    public String toString() {
        return "ContentletVersionInfo{" +
                "identifier='" + getIdentifier() + '\'' +
                ", workingInode='" + getWorkingInode() + '\'' +
                ", liveInode='" + getLiveInode() + '\'' +
                '}';
    }
}
