package com.dotmarketing.business.cache.provider.h22;

import java.io.Serializable;
import java.io.StringWriter;

public class H22GroupStats implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	final long startTime = System.currentTimeMillis();
	long hits = 0;
	long misses = 0;
	long writes = 0;
	long totalTimeReading = 0;
	long totalTimeWriting = 0;
	long totalSize=0;

	final String group;
	public H22GroupStats(String group){
		this.group=group + "H22";
	}
	void hitOrMiss(final Object obj) {
		if (obj == null) {
			misses++;
		} else {
			hits++;
		}
	}

	void writeSize(long len) {
		totalSize += len;
	}

	void readTime(long len) {

		totalTimeReading += len;
	}
	
	void writeTime(long len) {
		writes++;
		totalTimeWriting += len;
	}
	
	


}
