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
	long reads = 0;
	long readTiming = 0;
	long writeTiming = 0;
	long totalSize=0;
	long avgEntrySize=0;
	long avgReadTime=0;
	long avgWriteTime=0;
	final String group;
	public H22GroupStats(String group){
		this.group=group;
	}
	void hitOrMiss(Object obj) {
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
		reads++;
		readTiming += len;
	}
	
	void writeTime(long len) {
		writes++;
		writeTiming += len;
	}
	
	
	void compute(){
		avgEntrySize = writes>0 ? totalSize/writes : 0;
		avgReadTime = reads> 0 ? readTiming / reads:0;
		avgWriteTime = writes>0 ? writeTiming/writes : 0;
	}
	@Override
	public String toString() {
		StringWriter sw = new StringWriter();
		sw.append("{\n");
		sw.append("\t'hits':");
		sw.append(hits + ",\n");
		sw.append("\'misses':");
		sw.append(misses + ",\n");
		sw.append("\'avgEntrySize':");
		sw.append(avgEntrySize + ",\n");
		sw.append("\'avgWriteTime':");
		sw.append(avgWriteTime + ",\n");
		sw.append("\'avgReadTime':");
		sw.append(avgReadTime + ",\n");
		sw.append("}");
		return super.toString();
	}
}
