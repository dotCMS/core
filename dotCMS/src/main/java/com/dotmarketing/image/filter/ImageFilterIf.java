package com.dotmarketing.image.filter;

import com.dotmarketing.business.DotStateException;
import java.io.File;
import java.util.Map;

public interface ImageFilterIf {

  public File runFilter(File file, Map<String, String[]> parameters) throws DotStateException;

  public String[] getAcceptedParameters();
}
