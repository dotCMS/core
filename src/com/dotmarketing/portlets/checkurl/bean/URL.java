package com.dotmarketing.portlets.checkurl.bean;

import org.apache.commons.httpclient.NameValuePair;

/**
 * Classe contenente le propriet� per la URL. 
 * 
 * @author 	Aliberti Graziano - Engineering Ingegneria Informatica
 * @date	01/02/2012
 *
 */
public class URL {
	
	private String hostname;
	private Integer port;
	private boolean https;
	private String path;
	private boolean withParameter;
	private NameValuePair[] queryString;
	
	public String getHostname() {
		return hostname;
	}
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}
	public Integer getPort() {
		return port;
	}
	public void setPort(Integer port) {
		this.port = port;
	}
	public boolean isHttps() {
		return https;
	}
	public void setHttps(boolean https) {
		this.https = https;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public boolean isWithParameter() {
		return withParameter;
	}
	public void setWithParameter(boolean withParameter) {
		this.withParameter = withParameter;
	}
	public NameValuePair[] getQueryString() {
		return queryString;
	}
	public void setQueryString(NameValuePair[] queryString) {
		this.queryString = queryString;
	}	
	
	public String completeURL(){
		StringBuilder sb = new StringBuilder(500);
		sb.append(https?"https://":"http://");
		sb.append(hostname);
		sb.append(port!=80?":"+port:"");
		sb.append(path);
		if(withParameter){
			sb.append("?");
			for(int i=0; i<queryString.length; i++){
				sb.append(queryString[i].getName());
				sb.append("=");
				sb.append(queryString[i].getValue());
				if(queryString.length-i>1)
					sb.append("&");
			}
		}
		return sb.toString();
	}
	
	public String absoluteURL(){
		StringBuilder sb = new StringBuilder(500);
		sb.append(https?"https://":"http://");
		sb.append(hostname);
		sb.append(port!=80?":"+port:"");
		sb.append(path);
		return sb.toString();
	}
}
