package com.dotcms.datacreator;

import java.util.Map;

public interface DataCreatorCommand {

    default String getName () {

        return this.getClass().getSimpleName();
    }

    void execute(final Map<String, Object> contextMap);
}
