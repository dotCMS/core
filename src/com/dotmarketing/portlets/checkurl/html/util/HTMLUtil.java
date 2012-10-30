package com.dotmarketing.portlets.checkurl.html.util;

import java.net.MalformedURLException;
import com.dotmarketing.portlets.checkurl.util.URL;
import org.apache.commons.httpclient.NameValuePair;

public class HTMLUtil {
	
	public static String ANCHOR = "a";	
	public static String HREF = "href";
	public static String TITLE = "title";
	public static String HTTPS = "https";
	public static String HTTP = "http";
	public static String PARAGRAPH = "#";
	
	public static URL getURLByString(String href){
		try {
			java.net.URL url = new java.net.URL(href);
			URL urlBean = new URL();
			if(url.getProtocol().equals(HTTPS))
				urlBean.setHttps(true);
			else
				urlBean.setHttps(false);
			urlBean.setHostname(url.getHost());			
			urlBean.setPort(url.getPort()<0?80:url.getPort());
			urlBean.setPath(url.getPath());
			if(null!=url.getQuery()){
				urlBean.setWithParameter(true);
				String[] query_string = null;
				if(url.getQuery().split("[&amp;]").length>0)
					query_string = url.getQuery().split("[&amp;]");
				else
					query_string = url.getQuery().split("[&]");
				NameValuePair[] params = new NameValuePair[query_string.length];
				for(int i=0; i<query_string.length; i++){
					String[] parametro_arr = query_string[i].split("[=]");
					params[i] = new NameValuePair(parametro_arr[0], parametro_arr[1]);
				}
				urlBean.setQueryString(params);				
			}
			return urlBean;
		} catch (MalformedURLException e) {
			return null;
		}
	}
}
