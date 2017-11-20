package com.dotmarketing.webdav2;

import java.util.TimerTask;

public class FileResourceCacheCleaner extends TimerTask {

    public void run() {
    	DotWebDavObject.getFileResourceCache().clearExpiredEntries();
    }
}
