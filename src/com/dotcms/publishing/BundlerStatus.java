package com.dotcms.publishing;

public class BundlerStatus {

	private long count = 0;
	private long failures = 0;
	/**
	 * @return the count
	 */
	public long getCount() {
		return count;
	}
	/**
	 * @param count the count to set
	 */
	public void addCount() {
		count= count +1;
	}
	/**
	 * @return the failuers
	 */
	public long getFailuers() {
		return failures;
	}
	/**
	 * @param failuers the failuers to set
	 */
	public void addFailuer() {
		failures = failures+ 1;
	}
	
}
