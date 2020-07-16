package com.dotcms.rest.api.v1.system.logger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ChangeLoggerForm {

    private final String name;
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
