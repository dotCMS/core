package com.dotmarketing.portlets.htmlpageasset.business.render;

public class PageLivePreviewVersionBean {

    private final String  renderLive;
    private final String  renderWorking;
    private final boolean diff;

    public PageLivePreviewVersionBean(final String renderLive,
                                      final String renderWorking,
                                      final boolean diff) {
        this.renderLive = renderLive;
        this.renderWorking = renderWorking;
        this.diff = diff;
    }

    public String getRenderLive() {
        return renderLive;
    }

    public String getRenderWorking() {
        return renderWorking;
    }

    public boolean isDiff() {
        return diff;
    }
}
