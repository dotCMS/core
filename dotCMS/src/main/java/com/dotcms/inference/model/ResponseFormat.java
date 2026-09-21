package com.dotcms.inference.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.Serializable;

/**
 * The shape the caller wants the model's answer in.
 *
 * @param type   free text, an arbitrary JSON object, or JSON conforming to a schema; required
 * @param schema the JSON Schema the answer must satisfy; required when the type is
 *               {@link Type#JSON_SCHEMA} and null otherwise
 */
public record ResponseFormat(Type type, JsonNode schema) implements Serializable {

    /** The kind of output requested. */
    public enum Type {
        /** Ordinary prose. */
        TEXT,
        /** Any well-formed JSON object. */
        JSON_OBJECT,
        /** JSON conforming to a supplied schema. */
        JSON_SCHEMA
    }

    public ResponseFormat {
        if (type == null) {
            throw new IllegalArgumentException("ResponseFormat type is required");
        }
        if (type == Type.JSON_SCHEMA && schema == null) {
            throw new IllegalArgumentException("ResponseFormat type JSON_SCHEMA requires a schema");
        }
    }

    /** @return the default, free-text format */
    public static ResponseFormat text() {
        return new ResponseFormat(Type.TEXT, null);
    }
}
