package com.dotcms.system.event.local.domain;

import com.dotcms.system.event.local.type.OrphanEvent;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

/**
 * The default subscriber to handle the ophan events.
 * By default it is just doing a log
 * @author jsanca
 */
public class DefaultOrphanEventSubscriber implements EventSubscriber<OrphanEvent> {

    public static final String DEBUG_LEVEL = "debug";
    public static final String INFO_LEVEL  = "info";
    public static final String ORPHANEVENTSUBSCRIBER_DEBUGLEVEL = "orphaneventsubscriber.debuglevel";
    private final String logLevel;

    public DefaultOrphanEventSubscriber() {
        this (Config.getStringProperty(ORPHANEVENTSUBSCRIBER_DEBUGLEVEL, DEBUG_LEVEL));
    }

    public DefaultOrphanEventSubscriber(final String logLevel) {
        this.logLevel = logLevel;
    }

    @Override
    public void notify(final OrphanEvent event) {

        if (INFO_LEVEL.equals(logLevel)) {

            Logger.info(this, "The Event: " + event + ", has not any subscribers associated");
        } else {
            Logger.debug(this, "The Event: " + event + ", has not any subscribers associated");
        }
    } // notify.
} // E:O:F:DefaultOrphanEventSubscriber.
