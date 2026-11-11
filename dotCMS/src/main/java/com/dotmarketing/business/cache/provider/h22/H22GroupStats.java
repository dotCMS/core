package com.dotmarketing.business.cache.provider.h22;

import java.io.Serializable;
import java.util.concurrent.atomic.LongAdder;

public class H22GroupStats implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	final long startTime = System.currentTimeMillis();
	final LongAdder hits = new LongAdder();
	final LongAdder misses = new LongAdder();
	final LongAdder writes = new LongAdder();
	final LongAdder totalTimeReading = new LongAdder();
	final LongAdder totalTimeWriting = new LongAdder();
	final LongAdder totalSize = new LongAdder();

	final String group;
	public H22GroupStats(String group){
		this.group=group + "H22";
	}
	/** Records whether a lookup returned a cached value. */
	void hitOrMiss(final Object obj) {
		if (obj == null) {
			misses.increment();
		} else {
			hits.increment();
		}
	}

	/** Adds serialized bytes written, including replacements of existing keys. */
	void writeSize(final long len) {
		totalSize.add(len);
	}

	/** Adds the elapsed read time in nanoseconds. */
	void readTime(final long len) {
		totalTimeReading.add(len);
	}
	
	/** Counts one successful write and adds its elapsed time in nanoseconds. */
	void writeTime(final long len) {
		writes.increment();
		totalTimeWriting.add(len);
	}
	
	


}
