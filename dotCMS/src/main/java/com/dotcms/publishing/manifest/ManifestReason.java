package com.dotcms.publishing.manifest;

import com.dotcms.publisher.util.dependencies.DependencyManager;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ManifestReason {
    INCLUDE_BY_USER ("Add directly by User"),
    DEPENDENCY_FROM("Dependency from: %s", argument -> DependencyManager.getBundleKey(argument));

    private String messageTemplate;
    private Function<Object, Object> transformer = argument -> argument;

    ManifestReason(final String messageTemplate) {
        this.messageTemplate = messageTemplate;
    }

    ManifestReason(final String messageTemplate,  Function<Object, Object> transformer) {
        this.messageTemplate = messageTemplate;
        this.transformer = transformer;
    }

    public String toString(final Object... arguments){
        final List<Object> argumentsTransformed = Arrays.stream(arguments).sequential().map(this.transformer)
                .collect(Collectors.toList());
        return String.format(this.messageTemplate, argumentsTransformed);
    }
}
