package com.dotmarketing.portlets.workflows.actionlet.copy;

import static org.junit.Assert.assertEquals;

import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.util.Map;

public class KeyValueValidationStrategy extends AbstractContentletValidationStrategy {

    AssertionStrategy valueAssertion = (final Contentlet original, final Contentlet copy) -> {
        final String originalValue = (String) original.get("value");
        final String copyValue = (String) copy.get("value");
        assertEquals("value", originalValue, copyValue);
    };

    AssertionStrategy keyAssertion = (final Contentlet original, final Contentlet copy) -> {
        final String originalValue = (String) original.get("key");
        final String copyValue = (String) copy.get("key");
        assertEquals("key", originalValue, copyValue);
    };

    private final ImmutableMap<String, AssertionStrategy> assertionStrategyMap = ImmutableMap.<String, AssertionStrategy>builder()
            .put("value", valueAssertion)
            .put("key", keyAssertion)
            .build();

    @Override
    public Map<String, AssertionStrategy> getBaseTypeAssertionsToApply() {
        return assertionStrategyMap;
    }

}
