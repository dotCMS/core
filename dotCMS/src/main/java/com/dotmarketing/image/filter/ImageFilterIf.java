package com.dotmarketing.image.filter;

import java.io.File;
import java.util.Map;

import com.dotmarketing.business.DotStateException;

/**
 * Encapsulates the basic interface to do a filter over image.
 */
public interface ImageFilterIf {

	/**
	 * Applies the filter over a file based on the parameters
	 * @param file {@link File} original file
	 * @param parameters {@link Map} parameters
	 * @return File with the filter applied
	 * @throws DotStateException
	 */
	public File runFilter(File file,   Map<String, String[]> parameters) throws DotStateException;

	/**
	 * Return an array with guide or help
	 * @return String
	 */
	public String[] getAcceptedParameters();	
	
	
}
