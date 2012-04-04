package com.dotmarketing.portlets.contentlet.model;

import java.io.Serializable;

public class ContentletLangVersionInfoId implements Serializable {
    private String identifier;
    private long lang;
    
    public String getIdentifier() {
        return identifier;
    }
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
    public long getLang() {
        return lang;
    }
    public void setLang(long langId) {
        lang = langId;
    }
    
    @Override
    public boolean equals(Object obj) {
        ContentletLangVersionInfoId x=(ContentletLangVersionInfoId)obj;
        return lang==x.getLang() && identifier.equals(x.getIdentifier());
    }
    
    @Override
    public int hashCode() {
        return identifier.hashCode()+new Long(lang).hashCode();
    }
}
