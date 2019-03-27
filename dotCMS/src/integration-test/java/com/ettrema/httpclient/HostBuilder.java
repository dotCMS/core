package com.ettrema.httpclient;

import com.ettrema.cache.Cache;
import com.ettrema.httpclient.zsyncclient.FileSyncer;
import java.util.List;

/**
 *
 * @author brad
 */
public class HostBuilder {
    private String server;
    private int port;
    private String user;
    private String password;
    private String rootPath;
    private ProxyDetails proxy;
    private Cache<Folder, List<Resource>> cache;
    private int timeoutMillis;
    private FileSyncer fileSyncer;
    
    public Host buildHost() {
        return new Host(server, rootPath, port, user, password, proxy, timeoutMillis, cache, fileSyncer);
    }

    /**
     * @return the server
     */
    public String getServer() {
        return server;
    }

    /**
     * @param server the server to set
     */
    public void setServer(String server) {
        this.server = server;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the rootPath
     */
    public String getRootPath() {
        return rootPath;
    }

    /**
     * @param rootPath the rootPath to set
     */
    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public ProxyDetails getProxy() {
        return proxy;
    }

    public void setProxy(ProxyDetails proxy) {
        this.proxy = proxy;
    }

    public Cache<Folder, List<Resource>> getCache() {
        return cache;
    }

    public void setCache(Cache<Folder, List<Resource>> cache) {
        this.cache = cache;
    }

    public int getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public FileSyncer getFileSyncer() {
        return fileSyncer;
    }

    public void setFileSyncer(FileSyncer fileSyncer) {
        this.fileSyncer = fileSyncer;
    }

    
}
