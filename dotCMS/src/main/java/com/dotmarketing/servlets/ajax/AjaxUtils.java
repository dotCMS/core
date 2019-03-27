package com.dotmarketing.servlets.ajax;

import java.util.HashMap;
import java.util.Map;

public class AjaxUtils {

	


	public Map<String,String> urlAsMap(String url){
		
		url = (url.startsWith("/")) ? url.substring(1, url.length()) : url;
		String p[] = url.split("/");
		Map<String, String> map = new HashMap<String, String>();
		
		String key =null;
		for(String x : p){
			if(key ==null){
				key = x;
			}
			else{
				map.put(key, x);
				key = null;
			}
		}
		
		return map;	
	}
}
