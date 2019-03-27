package com.dotmarketing.business.cache.provider.h22;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.concurrent.ConcurrentHashMap;

public class H2GroupStatsList extends ConcurrentHashMap<String, H22GroupStats> implements Serializable{

	

	private static final long serialVersionUID = 1L;

	H22GroupStats group(String group){
		H22GroupStats stats = get(group);
		return (stats == null) ? getOrDefaultWithPut(group) : stats;
	}
	
	private H22GroupStats getOrDefaultWithPut(String group) {
		H22GroupStats defaultValue = new H22GroupStats(group);
		H22GroupStats stats = putIfAbsent(group, defaultValue);
		return stats == null ? defaultValue : stats;
	}

	@Override
	public String toString() {
		StringWriter sw = new StringWriter();
		
		for(String group: keySet() ){

			H22GroupStats stats = get(group);

			sw.append("[\n");
			sw.append("\t'group':");
			sw.append("'" + group + "',\n");
			sw.append("\t'stats':");
			sw.append(stats.toString());
			sw.append("]");
	


		}

		return super.toString();
	}
	
	
	
	
}
