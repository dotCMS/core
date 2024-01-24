package com.ettrema.httpclient.zsyncclient;

import com.bradmcevoy.http.Range;
import java.util.List;

/**
 * Used to load selected range data to satisfy the zsync process
 *
 * @author brad
 */
public interface RangeLoader {

	/**
	 * Fetch a set of ranges, usually over HTTP
	 * 
	 * @param rangeList
	 * @return
	 * @throws Exception 
	 */
	public byte[] get(List<Range> rangeList) throws Exception;
	
}
