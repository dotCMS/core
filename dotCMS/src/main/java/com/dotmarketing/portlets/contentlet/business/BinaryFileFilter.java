package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.util.Config;
import java.io.FileFilter;

public class BinaryFileFilter implements FileFilter {

  public boolean accept(java.io.File pathname) {
    if (pathname.getName().contains(Config.GENERATED_FILE)) {
      return false;
    } else if (pathname.getName().startsWith(".")) {
      return false;
    } else {
      return true;
    }
  }
}
