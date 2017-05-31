package com.dotcms.publisher.endpoint.bean;

import java.io.Serializable;

import com.dotcms.enterprise.publishing.staticpublishing.AWSS3Publisher;
import com.dotcms.publisher.pusher.PushPublisher;

/**
 * Java Bean for publishing_end_point table
 *
 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
 *
 *         Oct 26, 2012 - 9:57:07 AM
 */
public class PublishingEndPoint implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -5224257190734104414L;
    private String id;
    private String groupId;
    private StringBuilder serverName;
    private String address;
    private String port;
    private String protocol;
    private boolean enabled;
    private StringBuilder authKey;
    private boolean sending;

    public PublishingEndPoint() {
        groupId = "";
        serverName = null;
        address = null;
        port = "";
        protocol = null;
        authKey = new StringBuilder();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public StringBuilder getServerName() {
        return serverName;
    }

    public void setServerName(StringBuilder serverName) {
        this.serverName = serverName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public StringBuilder getAuthKey() {
        return authKey;
    }

    public void setAuthKey(StringBuilder authKey) {
        this.authKey = authKey;
    }

    /**
     * Returns whether this endpoint is sending or receiving bundles.
     * <p>
     * 'true' means this endpoint sends, so it will be listed in the "Receive from" section.
     * 'false; means this endpoints receives, so it will be listed in the "Sent to" section.
     */
    public boolean isSending() {
        return sending;
    }

    /**
     * Sets whether this endpoint is sending or receiving bundles.
     * <p>
     * 'true' means this endpoint sends, so it will be listed in the "Receive from" section.
     * 'false; means this endpoints receives, so it will be listed in the "Sent to" section.
     *
     * @param sending the boolean value
     */
    public void setSending(boolean sending) {
        this.sending = sending;
    }

    public StringBuilder toURL() {
        StringBuilder sb = new StringBuilder();
        sb.append(protocol);
        sb.append("://");
        sb.append(address);
        if (!port.equals("80")) {
            sb.append(":");
            sb.append(port);
        }
        return sb;
    }
    
    /**
     * Get the endpoint publisher class
     * @return the endpoint publisher class
     */
    public Class getPublisher() {
        if ( AWSS3Publisher.PROTOCOL_AWS_S3.equals(this.getProtocol()) ) {
            return AWSS3Publisher.class;
        } else {
        	return PushPublisher.class;
        }
    }
}
