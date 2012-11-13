package com.dotmarketing.viewtools.navigation;

import java.util.List;

public class NavResult {
    private String title;
    private String href;
    private int order;
    private boolean active;
    private List<NavResult> children;
    
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<NavResult> getChildren() {
        return children;
    }

    public void setChildren(List<NavResult> children) {
        this.children = children;
    }

    public String toString(){
        //implement a real toString builder
        return "<a href='"+href+"' title='"+title+"'>"+title+"</a>";
    }    
}
