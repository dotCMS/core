package com.dotcms.rest.api.v1.workflow;

import java.util.Map;

public class ActionInputView {

    private final String id;
    private final Map<String, Object> body;

    public ActionInputView(final String id, final Map<String, Object> body) {
        this.id = id;
        this.body = body;
    }

    public String getId() {
        return id;
    }

    public Map<String, Object> getBody() {
        return body;
    }
}

