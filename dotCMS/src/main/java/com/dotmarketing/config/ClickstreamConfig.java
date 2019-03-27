package com.dotmarketing.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Clickstream configuration data.
 *
 * @author 
 */
public class ClickstreamConfig {
    private String loggerClass;
    private List botAgents = new ArrayList();
    private List botHosts = new ArrayList();

    public String getLoggerClass() {
        return loggerClass;
    }

    public void setLoggerClass(String loggerClass) {
        this.loggerClass = loggerClass;
    }

    public void addBotAgent(String agent) {
        botAgents.add(agent);
    }

    public void addBotHost(String host) {
        botHosts.add(host);
    }

    public List getBotAgents() {
        return botAgents;
    }

    public List getBotHosts() {
        return botHosts;
    }
}
