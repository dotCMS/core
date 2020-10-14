package com.dotcms.rest;

import com.dotcms.repackage.org.codehaus.jettison.json.JSONObject;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using = JsonObjectViewSerializer.class)
public class JsonObjectView {

    private final JSONObject jsonObject;

    public JsonObjectView(final JSONObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public JSONObject getJsonObject() {
        return jsonObject;
    }
}
