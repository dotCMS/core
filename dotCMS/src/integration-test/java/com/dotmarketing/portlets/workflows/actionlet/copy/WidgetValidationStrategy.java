package com.dotmarketing.portlets.workflows.actionlet.copy;

import static org.junit.Assert.assertEquals;

import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.util.Map;

public class WidgetValidationStrategy extends AbstractContentletValidationStrategy {

    AssertionStrategy vtlFileAssertion = (final Contentlet original, final Contentlet copy) -> {
        final String originalValue = (String) original.get("vtlFile");
        final String copyValue = (String) copy.get("vtlFile");
        assertEquals("vtlFile", originalValue, copyValue);
    };

    AssertionStrategy widgetTitleAssertion = (final Contentlet original, final Contentlet copy) -> {
        final String originalValue = (String) original.get("widgetTitle");
        final String copyValue = (String) copy.get("widgetTitle");
        assertEquals("widgetTitle", originalValue, copyValue);
    };

    private final ImmutableMap<String, AssertionStrategy> assertionStrategyMap = ImmutableMap.<String, AssertionStrategy>builder()
            .put("vtlFile", vtlFileAssertion)
            .put("widgetTitle", widgetTitleAssertion)
            .put("host", hostAssertion)
            .put("folder", folderAssertion)
            .put("widgetCode", hostAssertion)
            .build();

    @Override
    public Map<String, AssertionStrategy> getBaseTypeAssertionsToApply() {
        return assertionStrategyMap;
    }

}
