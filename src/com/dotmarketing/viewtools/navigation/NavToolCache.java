package com.dotmarketing.viewtools.navigation;

import java.util.List;

import com.dotmarketing.business.Cachable;

public interface NavToolCache extends Cachable {
    List<NavResult> getNav(String hostid, String path);
    void putNav(String hostid, String path, List<NavResult> items);
    void removeNav(String hostid, String path);
    void removeNavAndChildren(String hostid, String path);
}
