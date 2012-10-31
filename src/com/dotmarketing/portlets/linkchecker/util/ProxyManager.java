package com.dotmarketing.portlets.linkchecker.util;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

public class ProxyManager {
	
	public static ProxyManager INSTANCE = new ProxyManager();
	private static int DEFAULT_PROXY_PORT = 8080;  
	
	private ProxyManager(){}
	
	private Connection connection;
	
	private synchronized void load() {
	    Logger.info(this, "Load singleton proxy configuration...");
        Connection connection = new Connection();
        
        //load plugin properties
        boolean proxy = Config.getBooleanProperty("urlcheck.connection.proxy", false);
        String proxyHost = Config.getStringProperty("urlcheck.connection.proxyHost");
        int proxyPort = 0;
        try{
            proxyPort = Config.getIntProperty("urlcheck.connection.proxyPort",8080);
        }catch(NumberFormatException e){
            proxyPort = DEFAULT_PROXY_PORT;
        }
        boolean proxyAuth = Config.getBooleanProperty("urlcheck.connection.proxyRequiredAuth", false);          
        String proxyUsername = Config.getStringProperty("urlcheck.connection.proxyUsername");
        String proxyPassword = Config.getStringProperty("urlcheck.connection.proxyPassword");
        connection.setProxy(proxy);
        connection.setProxyHost(proxyHost);
        connection.setProxyPassword(proxyPassword);
        connection.setProxyPort(proxyPort);
        connection.setProxyRequiredAuth(proxyAuth);
        connection.setProxyUsername(proxyUsername);
        Logger.info(this, connection.toString());
        ProxyManager.INSTANCE.setConnection(connection);
        Logger.info(this, "...singleton proxy configuration loaded successfully");
	}

	public Connection getConnection() {
	    if(!isLoaded()) load();
		return connection;
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}

	public boolean isLoaded(){
		if(null==connection)
			return false;
		else
			return true;
	}
	
	
	public static class Connection {
	    
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


}
