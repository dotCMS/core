package com.dotcms.rest;

import java.util.List;

public class TagResourceTestCase {

    private String tagName;
    private String siteOrFolderId;
    private List<String> expectedTags;
    private List<String> unexpectedTags;

    public String getTagName() {
        return tagName;
    }

    public void setTagName(final String tagName) {
        this.tagName = tagName;
    }

    public String getSiteOrFolderId() {
        return siteOrFolderId;
    }

    public void setSiteOrFolderId(final String siteOrFolderId) {
        this.siteOrFolderId = siteOrFolderId;
    }

    public List<String> getExpectedTags() {
        return expectedTags;
    }

    public void setExpectedTags(final List<String> expectedTags) {
        this.expectedTags = expectedTags;
    }

    public List<String> getUnexpectedTags() {
        return unexpectedTags;
    }

    public void setUnexpectedTags(final List<String> unexpectedTags) {
        this.unexpectedTags = unexpectedTags;
    }
}
