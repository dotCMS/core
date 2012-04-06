package com.dotcms.publishing;

public class BundlerStatus {
	private long total = 0;
	private long count = 0;
	private long failures = 0;

	/**
	 * @return the count
	 */
	public long getCount() {
		return count;
	}

	/**
	 * get the total
	 * 
	 * @return
	 */
	public long getTotal() {
		return total;
	}
	/**
	 * sets the total 
	 * @param tot
	 */
	public void setTotal(long tot) {
		this.total = tot;
	}

	/**
	 * @param count
	 *            the count to set
	 */
	public void addCount() {
		count = count + 1;
	}

	/**
	 * @return the failuers
	 */
	public long getFailuers() {
		return failures;
	}

	/**
	 * @param failuers
	 *            the failuers to set
	 */
	public void addFailuer() {
		failures = failures + 1;
	}

}
