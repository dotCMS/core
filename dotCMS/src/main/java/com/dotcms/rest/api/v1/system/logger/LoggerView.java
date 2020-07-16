package com.dotcms.rest.api.v1.system.logger;

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
