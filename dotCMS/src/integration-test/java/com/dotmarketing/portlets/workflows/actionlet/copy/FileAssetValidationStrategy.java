package com.dotmarketing.portlets.workflows.actionlet.copy;

import static org.junit.Assert.assertEquals;

import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.io.File;
import java.util.Map;

public class FileAssetValidationStrategy extends AbstractContentletValidationStrategy {

    AssertionStrategy metadataAssertion = (final Contentlet original, final Contentlet copy) -> {
        final String originalValue = (String) original.getMap().get("metaData");
        final String copyValue = (String) copy.getMap().get("metaData");
        assertEquals("metadata", originalValue, copyValue);
    };

    AssertionStrategy fileAssetAssertion = (final Contentlet original, final Contentlet copy) -> {
        final File originalValue = (File) original.get("fileAsset");
        final File copyValue = (File) copy.get("fileAsset");
        assertPaths(originalValue.getName(), copyValue.getName());
    };

    private final ImmutableMap<String, AssertionStrategy> assertionStrategyMap = ImmutableMap.<String, AssertionStrategy>builder()
            .put("folder", folderAssertion)
            .put("metaData", metadataAssertion)
            .put("fileAsset", fileAssetAssertion)
            .build();

    @Override
    public Map<String, AssertionStrategy> getBaseTypeAssertionsToApply() {
        return assertionStrategyMap;
    }
}
