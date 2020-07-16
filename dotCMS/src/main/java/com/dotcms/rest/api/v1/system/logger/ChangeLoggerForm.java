package com.dotcms.rest.api.v1.system.logger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Form to change logger level
 * @author jsanca
 */
public class ChangeLoggerForm {

    /**
     * Name of the logger
     */
    private final String name;

    /**
     * New level for the logger
     */
    private final String level;

    @JsonCreator
    public ChangeLoggerForm(@JsonProperty("name")  final String name,
                            @JsonProperty("level") final String level) {
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
