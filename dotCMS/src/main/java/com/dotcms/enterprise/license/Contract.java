package com.dotcms.enterprise.license;

public class Contract {
    boolean site;
    String id;
    boolean perpetual;
    String level;
    String start;
    String end;
    public boolean isSite() {
        return site;
    }
    public void setSite(boolean site) {
        this.site = site;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public boolean isPerpetual() {
        return perpetual;
    }
    public void setPerpetual(boolean perpetual) {
        this.perpetual = perpetual;
    }
    public String getLevel() {
        return level;
    }
    public void setLevel(String level) {
        this.level = level;
    }
    public String getStart() {
        return start;
    }
    public void setStart(String start) {
        this.start = start;
    }
    public String getEnd() {
        return end;
    }
    public void setEnd(String end) {
        this.end = end;
    }
    
}