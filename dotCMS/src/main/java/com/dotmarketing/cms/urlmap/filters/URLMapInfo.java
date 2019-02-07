package com.dotmarketing.cms.urlmap.filters;

public class URLMapInfo {
    
    final protected String identifier, inode, detailPagePath, mappedUrl;

    public URLMapInfo(String identifier, String inode, String detailPagePath, String mappedUrl) {
        super();
        this.identifier = identifier;
        this.inode = inode;
        this.detailPagePath = detailPagePath;
        this.mappedUrl = mappedUrl;
        
    }
    
}
