package com.dotmarketing.viewtools.navigation;

import com.dotmarketing.business.Cachable;

public interface NavToolCache extends Cachable {
    public final String GROUP="navCache";
    
    NavResult getNav(String hostid, String folderInode);
    void putNav(String hostid, String folderInode, NavResult result);
    void removeNav(String hostid, String folderInode);
    void removeNavByPath(String hostid, String path);
    void removeNav(String folderInode);
}
