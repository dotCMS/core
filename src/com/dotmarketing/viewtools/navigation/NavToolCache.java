package com.dotmarketing.viewtools.navigation;

import com.dotmarketing.business.Cachable;

public interface NavToolCache extends Cachable {
    
    NavResult getNav(String hostid, String folderInode, long languageId);

    void putNav(String hostid, String folderInode, NavResult result, long languageId);

    void removeNav(String hostid, String folderInode);

    void removeNav(String hostid, String folderInode, long languageId);

    void removeNavByPath(String hostid, String path);

    void removeNavByPath(String hostid, String path, long languageId);
}
