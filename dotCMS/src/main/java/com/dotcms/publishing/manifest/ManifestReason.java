package com.dotcms.publishing.manifest;

import com.dotcms.publisher.util.dependencies.DependencyManager;
import com.dotcms.publishing.PublisherConfig;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ManifestReason {

    INCLUDE_BY_USER ("Add directly by User"),
    INCLUDE_DEPENDENCY_FROM("Dependency from: %s", argument -> String.class.isInstance(argument) ?
            argument : ((ManifestItem) argument).getManifestInfo().id()),
    INCLUDE_AUTOMATIC_BY_DOTCMS("Add Automatic by dotcms"),
    EXCLUDE_SYSTEM_OBJECT("Exclude System Folder/Host"),
    EXCLUDE_BY_FILTER("Exclude by filter"),
    EXCLUDE_BY_MOD_DATE("Exclude by mod_date"),
    EXCLUDE_BY_OPERATION("Exclude by Operation: %s", argument -> argument.toString());

    private String messageTemplate;
    private Function<Object, Object> transformer = argument -> argument;

    ManifestReason(final String messageTemplate) {
        this.messageTemplate = messageTemplate;
    }

    ManifestReason(final String messageTemplate,  Function<Object, Object> transformer) {
        this.messageTemplate = messageTemplate;
        this.transformer = transformer;
    }

    public String getMessage(final Object... arguments){
        final List<Object> argumentsTransformed = Arrays.stream(arguments).sequential().map(this.transformer)
                .collect(Collectors.toList());
        return String.format(this.messageTemplate, argumentsTransformed.toArray());
    }
}
