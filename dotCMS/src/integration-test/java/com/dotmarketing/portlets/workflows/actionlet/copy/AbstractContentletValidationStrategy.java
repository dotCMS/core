package com.dotmarketing.portlets.workflows.actionlet.copy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import com.dotcms.repackage.org.apache.commons.io.FilenameUtils;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;

public abstract class AbstractContentletValidationStrategy {

    private static final String SYSTEM = "system";

    public final void apply(final Contentlet original, final Contentlet copy)
            throws DotDataException {
        final Map<String, AssertionStrategy> assertionsMap = getAssertionsToApply();
        final Set<String> propertyNames = assertionsMap.keySet();
        for (final String propertyName : propertyNames) {
            Logger.info(this, "Property:" + propertyName);
            final AssertionStrategy strategy = assertionsMap.get(propertyName);
            strategy.apply(original, copy);
        }
    }

    private Map<String, AssertionStrategy> getAssertionsToApply(){
        final Map<String, AssertionStrategy> assertionstoApply = new HashMap<>();
        assertionstoApply.putAll(commonAssertionsMap);
        assertionstoApply.putAll(getBaseTypeAssertionsToApply());
        return assertionstoApply;
    }

    abstract Map<String, AssertionStrategy> getBaseTypeAssertionsToApply();

    // If we wanted to have a validation specific for a sub type. We would need to implement a method like:
    //Map<String, AssertionStrategy> getSubTypeAssertionsToApply(String subTypeName);

    /* common assertion strategies shared across classes */

    AssertionStrategy notEqualIdentifierAssertion = (final Contentlet original, final Contentlet copy) -> {
        final String originalIdentifier = (String) original.get(Contentlet.IDENTIFIER_KEY);
        final String copyIdentifier = (String) copy.get(Contentlet.IDENTIFIER_KEY);
        assertNotEquals(originalIdentifier, copyIdentifier);
    };

    AssertionStrategy equalInodeAssertion = (final Contentlet original, final Contentlet copy) -> {
        final String originalInode = (String) original.get(Contentlet.INODE_KEY);
        final String copyInode = (String) copy.get(Contentlet.INODE_KEY);
        assertNotEquals(originalInode, copyInode);
    };

    AssertionStrategy languageAssertion = (final Contentlet original, final Contentlet copy) -> {
        final Number originalLangId = (Number) original.get("languageId");
        final Number copyLangId = (Number) copy.get("languageId");
        assertEquals("languageId", originalLangId, copyLangId);
    };

    AssertionStrategy modUserAssertion = (final Contentlet original, final Contentlet copy) -> {
        final String originalModUser = (String) original.get("modUser");
        final String copyModUser = (String) copy.get("modUser");
        assertEquals(copyModUser, "system");
        if (!SYSTEM.equals(originalModUser)) {
            assertNotEquals("modUser", originalModUser, copyModUser);
        }
    };

    AssertionStrategy sortOrderAssertion = (final Contentlet original, final Contentlet copy) -> {
        final Number originalValue = (Number) original.get("sortOrder");
        final Number copyValue = (Number) copy.get("sortOrder");
        assertEquals("copyValue", originalValue, copyValue);
    };

    AssertionStrategy modDateAssertion = (final Contentlet original, final Contentlet copy) -> {
        final Date originalModDate = (Date) original.get("modDate");
        final Date copyModDate = (Date) copy.get("modDate");
        assertNotEquals("modDate", originalModDate, copyModDate);
    };

    AssertionStrategy ownerUserAssertion = (final Contentlet original, final Contentlet copy) -> {
        final String originalOwner = (String) original.get("owner");
        final String copyOwner = (String) copy.get("owner");
        assertEquals("owner", originalOwner, copyOwner);
    };

    AssertionStrategy structureAssertion = (final Contentlet original, final Contentlet copy) -> {
        final String originalValue = (String) original.get("stInode");
        final String copyValue = (String) copy.get("stInode");
        assertEquals("stInode", originalValue, copyValue);
    };

    AssertionStrategy publishedAssertion = (final Contentlet original, final Contentlet copy) -> {
        final String originalValue = (String) original.get("published");
        final String copyValue = (String) copy.get("published");
        assertEquals("published", originalValue, copyValue);
    };

    AssertionStrategy folderAssertion = (final Contentlet original, final Contentlet copy) -> {
        final String originalValue = (String) original.get("folder");
        final String copyValue = (String) copy.get("folder");
        assertEquals("folder", originalValue, copyValue);
    };

    AssertionStrategy hostAssertion = (final Contentlet original, final Contentlet copy) -> {
        final String originalValue = (String) original.get("host");
        final String copyValue = (String) copy.get("host");
        assertEquals("host", originalValue, copyValue);
    };

    AssertionStrategy disabledWysiwygAssertion = (final Contentlet original, final Contentlet copy) -> {
        final List originalValue = (List) original.get("disabledWYSIWYG");
        final List copyValue = (List) copy.get("disabledWYSIWYG");
        assertEquals("host", originalValue, copyValue);
    };

    AssertionStrategy titleAssertion = (final Contentlet original, final Contentlet copy) -> {
        final String originalValue = (String) original.get("title");
        final String copyValue = (String) copy.get("title");
        assertEquals("title", originalValue, copyValue);
    };

    AssertionStrategy emailAssertion = (final Contentlet original, final Contentlet copy) -> {
        final String originalValue = (String) original.get("email");
        final String copyValue = (String) copy.get("email");
        assertEquals("email", originalValue, copyValue);
    };

    private final ImmutableMap<String, AssertionStrategy> commonAssertionsMap = ImmutableMap.<String, AssertionStrategy>builder()
            .put(Contentlet.IDENTIFIER_KEY, notEqualIdentifierAssertion)
            .put(Contentlet.INODE_KEY, equalInodeAssertion)
            .put(Contentlet.LANGUAGEID_KEY, languageAssertion)
            .put(Contentlet.OWNER_KEY, ownerUserAssertion)
            .put(Contentlet.SORT_ORDER_KEY, sortOrderAssertion)
            .put(Contentlet.MOD_DATE_KEY, modDateAssertion)
            .put(Contentlet.MOD_USER_KEY, modUserAssertion)
            .put(Contentlet.STRUCTURE_INODE_KEY, structureAssertion)
            .put("published", publishedAssertion)
            .put("disabledWYSIWYG",disabledWysiwygAssertion)
            .build();

    void assertPaths(final String originalFileName, final String copyFileName) {
        final String ext = FilenameUtils.getExtension(originalFileName);
        final String extPattern = StringUtils.isNotBlank(ext) ? "\\." + ext : ""; // if the original file has an extension add a regex portion to validate the copy has it too.
        final Pattern pattern = Pattern.compile("^[\\w.\\s-]+_copy(_?(\\d+))*?" + extPattern + "$");
        final Matcher matcher = pattern.matcher(copyFileName);
        assertTrue("Copy file name '" + copyFileName + "' does not match.", matcher.matches());
    }


}
