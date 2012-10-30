package com.dotmarketing.portlets.checkurl.util;

/**
 * Classe contenente le proprietˆ per la Connection. 
 * 
 * @author 	Aliberti Graziano - Engineering Ingegneria Informatica
 * @date	01/02/2012
 *
 */
public class Connection {
	
	private boolean proxy;
	private String proxyHost;
	private Integer proxyPort;
	private boolean proxyRequiredAuth;
	private String proxyUsername;
	private String proxyPassword;
	
	public boolean isProxy() {
		return proxy;
	}
	public void setProxy(boolean proxy) {
		this.proxy = proxy;
	}
	public String getProxyHost() {
		return proxyHost;
	}
	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}
	public Integer getProxyPort() {
		return proxyPort;
	}
	public void setProxyPort(Integer proxyPort) {
		this.proxyPort = proxyPort;
	}
	public boolean isProxyRequiredAuth() {
		return proxyRequiredAuth;
	}
	public void setProxyRequiredAuth(boolean proxyRequiredAuth) {
		this.proxyRequiredAuth = proxyRequiredAuth;
	}
	public String getProxyUsername() {
		return proxyUsername;
	}
	public void setProxyUsername(String proxyUsername) {
		this.proxyUsername = proxyUsername;
	}
	public String getProxyPassword() {
		return proxyPassword;
	}
	public void setProxyPassword(String proxyPassword) {
		this.proxyPassword = proxyPassword;
	}
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(500);
		sb.append("[Connection: ");
		sb.append("proxyEnable: ");
		sb.append(proxy);
		sb.append("; ");
		if(proxy){
			sb.append("proxyHost: ");
			sb.append(proxyHost);
			sb.append("; ");
			sb.append("proxyPort: ");
			sb.append(proxyPort);
			sb.append("; ");
			sb.append("proxyRequiredAuth: ");
			sb.append(proxyRequiredAuth);
			sb.append("; ");
			if(proxyRequiredAuth){
				sb.append("proxyUsername: ");
				sb.append(proxyUsername);
				sb.append("; ");
				sb.append("proxyPassword: ");
				sb.append(proxyPassword);
				sb.append("; ");				
			}
		}
		sb.append("]");
		return sb.toString();
	}
	
	
	
}
