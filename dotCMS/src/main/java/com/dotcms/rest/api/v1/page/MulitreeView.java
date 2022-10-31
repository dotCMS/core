package com.dotcms.rest.api.v1.page;

/**
 * Multitree view to render on the json.
 * @author jsanca
 */
public class MulitreeView {

    private final String pageId;

    private final String containerId;

    private final String contentId;

    private final String relationType;

    private final int treeOrder;

    private final String personalization;

    private final String variantId;

    public MulitreeView(String pageId, String containerId, String contentId, String relationType, int treeOrder, String personalization, String variantId) {
        this.pageId = pageId;
        this.containerId = containerId;
        this.contentId = contentId;
        this.relationType = relationType;
        this.treeOrder = treeOrder;
        this.personalization = personalization;
        this.variantId = variantId;
    }

    public String getPageId() {
        return pageId;
    }

    public String getContainerId() {
        return containerId;
    }

    public String getContentId() {
        return contentId;
    }

    public String getRelationType() {
        return relationType;
    }

    public int getTreeOrder() {
        return treeOrder;
    }

    public String getPersonalization() {
        return personalization;
    }

    public String getVariantId() {
        return variantId;
    }
}
