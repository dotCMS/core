package com.dotmarketing.beans;

import java.io.Serializable;

/**
 * This event is being triggered when a site is created
 * @author jsanca
 */
public class SiteCreatedEvent implements Serializable {

    private String siteIdentifier;
    public SiteCreatedEvent() {
    }

    public String getSiteIdentifier() {
        return siteIdentifier;
    }

    public void setSiteIdentifier(String siteIdentifier) {
        this.siteIdentifier = siteIdentifier;
    }
}
