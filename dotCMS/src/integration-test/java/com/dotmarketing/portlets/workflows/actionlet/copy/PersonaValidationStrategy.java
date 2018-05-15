package com.dotmarketing.portlets.workflows.actionlet.copy;

import static org.junit.Assert.assertEquals;

import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.util.Map;

public class PersonaValidationStrategy extends AbstractContentletValidationStrategy {

    AssertionStrategy photoAssertion = (final Contentlet original, final Contentlet copy) -> {
        final String originalValue = (String) original.get("photo");
        final String copyValue = (String) copy.get("photo");
        assertEquals("value", originalValue, copyValue);
    };

    AssertionStrategy descriptionAssertion = (final Contentlet original, final Contentlet copy) -> {
        final String originalValue = (String) original.get("description");
        final String copyValue = (String) copy.get("description");
        assertEquals("description", originalValue, copyValue);
    };

    AssertionStrategy nameAssertion = (final Contentlet original, final Contentlet copy) -> {
        final String originalValue = (String) original.get("name");
        final String copyValue = (String) copy.get("name");
        assertEquals("name", originalValue, copyValue);
    };

    private final ImmutableMap<String, AssertionStrategy> assertionStrategyMap = ImmutableMap.<String, AssertionStrategy>builder()
            .put("photo", photoAssertion)
            .put("description", descriptionAssertion)
            .put("name", nameAssertion)
            .put("folder", folderAssertion)

            .build();

    @Override
    public Map<String, AssertionStrategy> getBaseTypeAssertionsToApply() {
        return assertionStrategyMap;
    }

}
