package com.dotmarketing.portlets.workflows.actionlet.copy;

import static com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI.FRIENDLY_NAME_FIELD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import com.dotcms.repackage.org.apache.commons.io.FilenameUtils;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import java.io.File;
import java.util.Map;

public class HtmlPageValidationStrategy extends AbstractContentletValidationStrategy {

    AssertionStrategy titleAssertion = (final Contentlet original, final Contentlet copy) -> {
        final String originalValue = (String) original.get("title");
        final String copyValue = (String) copy.get("title");
        assertEquals("title", originalValue, copyValue);
    };

    AssertionStrategy subTitleAssertion = (final Contentlet original, final Contentlet copy) -> {
        final String originalValue = (String) original.get("subtitle");
        final String copyValue = (String) copy.get("subtitle");
        assertEquals("subtitle", originalValue, copyValue);
    };

    AssertionStrategy pageUrlAssertion = (final Contentlet original, final Contentlet copy) -> {
        final String originalUrl = (String) original.get("url");
        final String copyUrl = (String) copy.get("url");
        if(originalUrl != null){
          assertNull("url", copyUrl);
        }
    };

    AssertionStrategy bannerAssertion = (final Contentlet original, final Contentlet copy) -> {
        final File originalValue = (File) original.get("banner");
        final File copyValue = (File) copy.get("banner");
        if(originalValue != null){
            final String file1 = FilenameUtils.getName(originalValue.getAbsolutePath());
            final String file2 = FilenameUtils.getName(copyValue.getAbsolutePath());
            assertEquals("banner", file1, file2);
        }
    };

    AssertionStrategy friendlyAssertion = (final Contentlet original, final Contentlet copy) -> {
        final String originalValue = (String) original.get(FRIENDLY_NAME_FIELD);
        final String copyValue = (String) copy.get(FRIENDLY_NAME_FIELD);
        assertEquals(FRIENDLY_NAME_FIELD, originalValue, copyValue);
    };

    AssertionStrategy seoKeywordAssertion = (final Contentlet original, final Contentlet copy) -> {
        final String originalValue = (String) original.get("seokeywords");
        final String copyValue = (String) copy.get("seokeywords");
        assertEquals("seokeywords", originalValue, copyValue);
    };

    AssertionStrategy seoDescriptionAssertion = (final Contentlet original, final Contentlet copy) -> {
        final String originalValue = (String) original.get("seodescription");
        final String copyValue = (String) copy.get("seodescription");
        assertEquals("seodescription", originalValue, copyValue);

    };

    AssertionStrategy templateAssertion = (final Contentlet original, final Contentlet copy) -> {
        final String originalValue = (String) original.get("template");
        final String copyValue = (String) copy.get("template");
        assertEquals("template", originalValue, copyValue);

    };

    AssertionStrategy assetAssertion = (final Contentlet original, final Contentlet copy) -> {
        final String originalIdentifierValue = (String) original.get("identifier");
        final String copyIdentifierValue = (String) copy.get("identifier");
        assertNotEquals("identifier", originalIdentifierValue, copyIdentifierValue);
        try{
            final Identifier identifier1 = APILocator.getIdentifierAPI().find( originalIdentifierValue );
            final Identifier identifier2 = APILocator.getIdentifierAPI().find( copyIdentifierValue );
            assertNotEquals("asset", identifier1.getAssetName(), identifier2.getAssetName());
            assertPaths(identifier1.getAssetName(), identifier2.getAssetName());
        }catch (Exception e){
            Logger.debug(this,"error on asset assertion ", e);
        }
    };


    private final ImmutableMap<String, AssertionStrategy> assertionStrategyMap = ImmutableMap.<String, AssertionStrategy>builder()
            .put("identifier", assetAssertion)
            .put("title", titleAssertion)
            .put("subTitle", subTitleAssertion)
            .put("url", pageUrlAssertion)
            .put("host", hostAssertion)
            .put("friendlyName", friendlyAssertion)
            .put("seokeywords", seoKeywordAssertion)
            .put("seodescription", seoDescriptionAssertion)
            .put("template", templateAssertion)
            .put("folder", folderAssertion)
            .put("banner",bannerAssertion)
            .build();

    @Override
    public Map<String, AssertionStrategy> getBaseTypeAssertionsToApply() {
        return assertionStrategyMap;
    }
}
