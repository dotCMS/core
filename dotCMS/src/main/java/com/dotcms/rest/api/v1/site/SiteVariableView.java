package com.dotcms.rest.api.v1.site;

import java.util.Date;

/**
 * Site variable View
 * @author jsanca
 */
public class SiteVariableView {

    private final String id;
    private final String hostId;
    private final String name;
    private final String key;
    private final String value;
    private final String lastModifierId;
    private final Date lastModDate;
    private final String lastModifierFullName;

    public SiteVariableView(String id, String hostId, String name, String key, String value, String lastModifierId, Date lastModDate, String lastModifierFullName) {
        this.id = id;
        this.hostId = hostId;
        this.name = name;
        this.key = key;
        this.value = value;
        this.lastModifierId = lastModifierId;
        this.lastModDate = lastModDate;
        this.lastModifierFullName = lastModifierFullName;
    }

    public String getId() {
        return id;
    }

    public String getHostId() {
        return hostId;
    }

    public String getName() {
        return name;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public String getLastModifierId() {
        return lastModifierId;
    }

    public Date getLastModDate() {
        return lastModDate;
    }

    public String getLastModifierFullName() {
        return lastModifierFullName;
    }
}
