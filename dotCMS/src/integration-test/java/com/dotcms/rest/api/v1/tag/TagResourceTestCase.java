package com.dotcms.rest.api.v1.tag;

import java.util.List;

public class TagResourceTestCase {

    private String tagName;
    private String siteOrFolderId;
    private List<String> expectedTags;

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getSiteOrFolderId() {
        return siteOrFolderId;
    }

    public void setSiteOrFolderId(String siteOrFolderId) {
        this.siteOrFolderId = siteOrFolderId;
    }

    public List<String> getExpectedTags() {
        return expectedTags;
    }

    public void setExpectedTags(List<String> expectedTags) {
        this.expectedTags = expectedTags;
    }
}
