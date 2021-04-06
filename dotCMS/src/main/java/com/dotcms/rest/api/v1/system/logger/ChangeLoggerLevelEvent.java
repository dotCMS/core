package com.dotcms.rest.api.v1.system.logger;

/**
 * Event to notify when a level has changed
 * Note: this event is mostly to notify cluster wide, rather than the current node.
 * @author jsanca
 */
public class ChangeLoggerLevelEvent {

    /**
     * Name of the logger
     */
    private  String name;

    /**
     * New level for the logger
     */
    private  String level;

    public ChangeLoggerLevelEvent() {
    }

    public ChangeLoggerLevelEvent(final String name, final String level) {
        this.name = name;
        this.level = level;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }
}
