package com.dotcms.rest;

import com.dotmarketing.util.json.JSONObject;
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
