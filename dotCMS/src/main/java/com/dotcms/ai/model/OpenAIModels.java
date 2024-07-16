package com.dotcms.ai.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

public class OpenAIModels implements Serializable {

    private final String object;
    private final List<OpenAIModel> data;

    @JsonCreator
    public OpenAIModels(@JsonProperty("object") final String object,
                        @JsonProperty("data") final List<OpenAIModel> data) {
        this.object = object;
        this.data = data;
    }

    public String getObject() {
        return object;
    }

    public List<OpenAIModel> getData() {
        return data;
    }

}
