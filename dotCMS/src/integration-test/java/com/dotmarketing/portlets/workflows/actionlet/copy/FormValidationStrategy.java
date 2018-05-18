package com.dotmarketing.portlets.workflows.actionlet.copy;

import static org.junit.Assert.assertEquals;

import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.util.Map;

public class FormValidationStrategy extends AbstractContentletValidationStrategy {

    AssertionStrategy firstNameAssertion = (final Contentlet original, final Contentlet copy) -> {
        final String originalValue = (String) original.get("firstName");
        final String copyValue = (String) copy.get("firstName");
        assertEquals("firstName", originalValue, copyValue);
    };

    AssertionStrategy middleNameAssertion = (final Contentlet original, final Contentlet copy) -> {
        final String originalValue = (String) original.get("middleName");
        final String copyValue = (String) copy.get("middleName");
        assertEquals("middleName", originalValue, copyValue);
    };

    AssertionStrategy commentsAssertion = (final Contentlet original, final Contentlet copy) -> {
        final String originalValue = (String) original.get("comments");
        final String copyValue = (String) copy.get("comments");
        assertEquals("comments", originalValue, copyValue);
    };

    AssertionStrategy phoneAssertion = (final Contentlet original, final Contentlet copy) -> {
        final String originalValue = (String) original.get("phone");
        final String copyValue = (String) copy.get("phone");
        assertEquals("phone", originalValue, copyValue);
    };

    private final ImmutableMap<String, AssertionStrategy> assertionStrategyMap = ImmutableMap.<String, AssertionStrategy>builder()
            .put("host", hostAssertion)
            .put("email", emailAssertion)
            .put("firstName", firstNameAssertion)
            .put("middleName", middleNameAssertion)
            .put("comments", commentsAssertion)
            .put("phone", phoneAssertion)
            .build();

    @Override
    public Map<String, AssertionStrategy> getBaseTypeAssertionsToApply() {
        return assertionStrategyMap;
    }

}
