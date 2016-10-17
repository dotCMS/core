/**
 * 
 */
package com.dotmarketing.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RegionLock {
	private static RegionLock instance=new RegionLock();
	public static RegionLock getInstance() {
		return instance;
	}
	
	private RegionLock() {
		list=Collections.synchronizedList(new ArrayList<String>());
	}
	
	List<String> list;
	
	public boolean isLocked(String keyName) {
		if (list.contains("/")) {
			return true;
		}
		if (list.contains(keyName)) {
			return true;
		}
		return false;
	}
	public  void  lock(String regionName) {
		if (regionName!=null) {
			list.add(regionName);
		}
	}
	
	
	public  void  unlock(String regionName) {
		if (regionName!=null) {
			list.remove(regionName);
		}
	}
}