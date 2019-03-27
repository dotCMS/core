package com.dotmarketing.portlets.workflows.actionlet.copy;

import static org.junit.Assert.assertEquals;

import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.util.Map;

public class ContentValidationStrategy extends AbstractContentletValidationStrategy {

    AssertionStrategy bodyAssertion = (final Contentlet original, final Contentlet copy) -> {
        final String originalValue = (String) original.get("body");
        final String copyValue = (String) copy.get("body");
        assertEquals("body", originalValue, copyValue);
    };

    AssertionStrategy commentAssertion = (final Contentlet original, final Contentlet copy) -> {
        final String originalValue = (String) original.get("comment");
        final String copyValue = (String) copy.get("comment");
        assertEquals("comment", originalValue, copyValue);
    };

    private final ImmutableMap<String, AssertionStrategy> assertionStrategyMap = ImmutableMap.<String, AssertionStrategy>builder()
            .put("folder",folderAssertion)
            .put("title",titleAssertion)
            .put("comment",commentAssertion)
            .put("email",emailAssertion)
            .put("body",bodyAssertion)
            .build();

    @Override
    public Map<String, AssertionStrategy> getBaseTypeAssertionsToApply() {
        return assertionStrategyMap;
    }
}
