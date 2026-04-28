package com.dotcms.ai.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

/**
 * Represents a collection of OpenAI models with details such as the type of object and the list of models.
 * This class is immutable and uses Jackson annotations for JSON serialization and deserialization.
 *
 * @author vico
 */
public class OpenAIModels implements Serializable {

    private final String object;
    private final List<OpenAIModel> data;
    private final OpenAIError error;

    @JsonCreator
    public OpenAIModels(@JsonProperty("object") final String object,
                        @JsonProperty("data") final List<OpenAIModel> data,
                        @JsonProperty("error") final OpenAIError error) {
        this.object = object;
        this.data = data;
        this.error = error;
    }

    public String getObject() {
        return object;
    }

    public List<OpenAIModel> getData() {
        return data;
    }

    public OpenAIError getError() {
        return error;
    }

    public static class OpenAIError {

        private final String message;
        private final String type;
        private final String param;
        private final String code;

        @JsonCreator
        public OpenAIError(@JsonProperty("object") final String message,
                           @JsonProperty("type") final String type,
                           @JsonProperty("param") final String param,
                           @JsonProperty("code") final String code) {
            this.message = message;
            this.type = type;
            this.param = param;
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public String getType() {
            return type;
        }

        public String getParam() {
            return param;
        }

        public String getCode() {
            return code;
        }
    }

}
