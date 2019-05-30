package com.dotmarketing.portlets.workflows;

import java.util.HashMap;

public class LargeMessageMap extends HashMap<String, Object> {

    public LargeMessageMap title (final String title) {

        this.put("title", title);
        return this;
    }

    public LargeMessageMap width (final String width) {

        this.put("width", width);
        return this;
    }

    public LargeMessageMap height (final String height) {

        this.put("height", height);
        return this;
    }


    public LargeMessageMap body (final String body) {

        this.put("body", body);
        return this;
    }

    public LargeMessageMap code (final CodeMessage code) {

        this.put("code", code);
        return this;
    }


    public static class CodeMessage {

        private String lang;
        private String content;

        public CodeMessage(final String lang, final String content) {
            this.lang = lang;
            this.content = content;
        }

        public String getLang() {
            return lang;
        }

        public String getContent() {
            return content;
        }
    }
}
