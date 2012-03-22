package com.dotmarketing.business.mbeans;

import java.text.NumberFormat;
import java.util.Set;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotJBCacheAdministratorImpl;
import com.dotmarketing.util.Logger;

public class CacheInfo implements CacheInfoMBean {
	private DotJBCacheAdministratorImpl cache;

	public CacheInfo(DotJBCacheAdministratorImpl cache) {
		super();
		this.cache = cache;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.dotmarketing.loggers.mbeans.CacheRegionInfoMBean#getRegionInfo()
	 */
	public String printRegionInfo() {
		return cache.getCacheStats();
	}

	public String printKeys(String group) {
		String result = "";
		Set<String> ss = cache.getKeys(group);
		for (String s : ss) {
			result += s + "\r\n";
		}
		return result;
	}

	public String regionCacheMemorySize(String group) {

		Set<String> keys = cache.getKeys(group);

		long startMemoryUse = getMemoryUse();

		int numberOfObjs = keys.size();


		CacheLocator.getCacheAdministrator().flushGroupLocalOnly(group);

		long endMemoryUse = getMemoryUse();
		
		String str = NumberFormat.getInstance().format((startMemoryUse - endMemoryUse) / 1024);
		String message = group + " using :" + str + "k with " + numberOfObjs + " objects";
		Logger.info(this, message);
		return message;

	}

	// PRIVATE //
	private static int fSAMPLE_SIZE = 100;
	private static long fSLEEP_INTERVAL = 100;

	private static long getMemoryUse() {
		putOutTheGarbage();
		long totalMemory = Runtime.getRuntime().totalMemory();

		putOutTheGarbage();
		long freeMemory = Runtime.getRuntime().freeMemory();

		return (totalMemory - freeMemory);
	}

	private static void putOutTheGarbage() {
		collectGarbage();
		collectGarbage();
	}

	private static void collectGarbage() {
		try {
			System.gc();
			Thread.currentThread().sleep(fSLEEP_INTERVAL);
			System.runFinalization();
			Thread.currentThread().sleep(fSLEEP_INTERVAL);
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		}
	}

}
