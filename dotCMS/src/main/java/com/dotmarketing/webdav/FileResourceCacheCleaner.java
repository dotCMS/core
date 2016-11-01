package com.dotmarketing.webdav;

import java.util.TimerTask;

public class FileResourceCacheCleaner extends TimerTask {

    public void run() {
    	DotWebdavHelper.getFileResourceCache().clearExpiredEntries();
    }
}
