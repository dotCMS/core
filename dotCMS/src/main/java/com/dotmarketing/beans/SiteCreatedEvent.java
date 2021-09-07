package com.dotmarketing.beans;

/**
 * This event is being triggered when a site is created
 * @author jsanca
 */
public class SiteCreatedEvent {

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
