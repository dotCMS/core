package com.dotcms.publishing;

public class BundlerStatus {
	private long total = 0;
	private long count = 0;
	private long failures = 0;
	private String bundlerClass;
	public String getBundlerClass() {
		return bundlerClass;
	}
	public BundlerStatus(String className){
		
		setBundlerClass(className);
	}
	public void setBundlerClass(String bundlerClass) {
		this.bundlerClass = bundlerClass;
	}

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
	 * @param count
	 *            the count to set
	 */
	public void addCount(int x) {
		count = count + x;
	}
	/**
	 * @return the failuers
	 */
	public long getFailures() {
		return failures;
	}

	/**
	 * @param failuers
	 *            the failuers to set
	 */
	public void addFailure() {
		failures = failures + 1;
	}

}
