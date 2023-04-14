/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
*/

package com.dotcms.enterprise.linkchecker;

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
