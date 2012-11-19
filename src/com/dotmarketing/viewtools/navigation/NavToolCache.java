package com.dotmarketing.viewtools.navigation;

import java.util.List;

import com.dotmarketing.business.Cachable;

public interface NavToolCache extends Cachable {
    public final String GROUP="NavTool";
    
    List<NavResult> getNav(String hostid, String folderInode);
    void putNav(String hostid, String folderInode, List<NavResult> items);
    void removeNav(String hostid, String folderInode);
    void removeNavByPath(String hostid, String path);
    void removeNavAndChildren(String hostid, String folderInode);
    void removeNav(String folderInode);
}
