package com.dotcms.rest.api.v1.system.logger;

/**
 * Logger view to show just what is needed from a logger
 * @author jsanca
 */
public class LoggerView {

    private final String name;
    private final String level;

    public LoggerView(final String name, final String level) {

        this.name = name;
        this.level = level;
    }

    public String getName() {
        return name;
    }

    public String getLevel() {
        return level;
    }
}
