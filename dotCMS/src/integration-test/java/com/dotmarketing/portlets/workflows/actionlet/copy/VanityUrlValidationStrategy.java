package com.dotmarketing.portlets.workflows.actionlet.copy;

import static org.junit.Assert.assertEquals;

import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.util.Map;

public class VanityUrlValidationStrategy extends AbstractContentletValidationStrategy {

    AssertionStrategy forwardToAssertion = (final Contentlet original, final Contentlet copy) -> {
        final String originalValue = (String) original.get("forwardTo");
        final String copyValue = (String) copy.get("forwardTo");
        assertEquals("forwardTo", originalValue, copyValue);
    };


    private final ImmutableMap<String, AssertionStrategy> assertionStrategyMap = ImmutableMap.<String, AssertionStrategy>builder()
            .put("host", hostAssertion)
            .put("uri", folderAssertion)
            .put("site", hostAssertion)
            .put("folder", folderAssertion)
            .put("title", titleAssertion)
            .put("forwardTo", forwardToAssertion)
            .build();

    @Override
    public Map<String, AssertionStrategy> getBaseTypeAssertionsToApply() {
        return assertionStrategyMap;
    }

}
