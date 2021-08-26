package com.dotcms.publishing.manifest;

import com.dotcms.publisher.util.dependencies.DependencyManager;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.manifest.ManifestItem.ManifestInfo;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.util.StringPool;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Build manifest register reason for INCLUDE or EXCLUDE registers
 *
 * @see {@link CSVManifestBuilder}
 */
public enum ManifestReason {

    INCLUDE_BY_USER ("Added directly by User"),
    INCLUDE_DEPENDENCY_FROM("Dependency from: %s", argument -> getDependencyFromArgument(argument)),
    INCLUDE_AUTOMATIC_BY_DOTCMS("Added Automatically by dotCMS"),
    EXCLUDE_SYSTEM_OBJECT("Excluded System Folder/Host"),
    EXCLUDE_BY_FILTER("Excluded by filter"),
    EXCLUDE_BY_MOD_DATE("Excluded by mod_date"),
    EXCLUDE_BY_OPERATION("Excluded by Operation: %s", argument -> argument.toString());

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

    private static String getDependencyFromArgument(final Object argument) {
        if (String.class.isInstance(argument)) {
            return argument.toString();
        } else if (ManifestItem.class.isInstance(argument))  {
            final ManifestInfo manifestInfo = ManifestItem.class.cast(argument).getManifestInfo();
            return String.format("ID: %s Title: %s", manifestInfo.id(), manifestInfo.title());
        } else {
            return StringPool.BLANK;
        }
    }
}
