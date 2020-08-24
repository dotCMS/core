package com.dotcms.rest.api.v1.workflow;

import java.util.Map;

/**
 * This class encapsulates the WF action inputs
 * this input will be taken by the FE and show some input (such as dialog)
 * There are a few known dialogs such as PP, Assign or Comment that are included on dotCMS
 * But the user might describe a set of inputs into the body in order to create a form input
 * @author jsanca
 */
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

