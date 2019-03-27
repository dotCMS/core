package com.dotmarketing.image.filter;

import java.io.File;
import java.util.Map;

import com.dotmarketing.business.DotStateException;

public interface ImageFilterIf {

	
	public File runFilter(File file,   Map<String, String[]> parameters) throws DotStateException;
	

	public String[] getAcceptedParameters();	
	
	
}
