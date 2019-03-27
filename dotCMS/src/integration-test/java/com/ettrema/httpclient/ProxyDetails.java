package com.ettrema.httpclient;

/**
 *
 * @author brad
 */
public class ProxyDetails {
    private boolean useSystemProxy;

    private String proxyHost;

    private int proxyPort;

    private String userName;

    private String password;

    /**
     * @return the useSystemProxy
     */
    public boolean isUseSystemProxy() {
        return useSystemProxy;
    }

    /**
     * @param useSystemProxy the useSystemProxy to set
     */
    public void setUseSystemProxy( boolean useSystemProxy ) {
        this.useSystemProxy = useSystemProxy;
    }

    /**
     * @return the proxyHost
     */
    public String getProxyHost() {
        return proxyHost;
    }

    /**
     * @param proxyHost the proxyHost to set
     */
    public void setProxyHost( String proxyHost ) {
        this.proxyHost = proxyHost;
    }

    /**
     * @return the proxyPort
     */
    public int getProxyPort() {
        return proxyPort;
    }

    /**
     * @param proxyPort the proxyPort to set
     */
    public void setProxyPort( int proxyPort ) {
        this.proxyPort = proxyPort;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword( String password ) {
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName( String userName ) {
        this.userName = userName;
    }

    public boolean hasAuth() {
        return (password != null && password.length() > 0 ) || (userName != null && userName.length() > 0);
    }
}
