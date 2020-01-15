package com.dotcms.rest.api.v1.secret.view;

public class HostView {

    private String hostId;
    private String hostName;

    public HostView(final String hostId, final String hostName) {
        this.hostId = hostId;
        this.hostName = hostName;
    }

    public String getHostId() {
        return hostId;
    }

    public String getHostName() {
        return hostName;
    }

}
