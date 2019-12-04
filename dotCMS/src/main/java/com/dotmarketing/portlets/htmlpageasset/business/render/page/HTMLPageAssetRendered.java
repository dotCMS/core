package com.dotmarketing.portlets.htmlpageasset.business.render.page;


import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * It is a {@link PageView} rendered
 */
@JsonSerialize(using = HTMLPageAssetRenderedSerializer.class)
public class HTMLPageAssetRendered extends PageView {
    private final String html;

    public HTMLPageAssetRendered(final RenderedBuilder builder) {

        super(builder);
        this.html = builder.html;
    }

    public String getHtml() {
        return html;
    }

    public static class RenderedBuilder extends PageView.Builder {

        private String html;

        public RenderedBuilder html (final String html) {

            this.html = html;
            return this;
        }

        public HTMLPageAssetRendered build() {
            return new HTMLPageAssetRendered(this);
        }
    }



}
