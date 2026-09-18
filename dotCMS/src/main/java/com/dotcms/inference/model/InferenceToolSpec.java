package com.dotcms.inference.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.Serializable;

/**
 * A tool the caller declares the model may ask to execute.
 *
 * <p>{@code parameters} is a JSON Schema document describing the tool's arguments. It is held as
 * a JSON tree rather than a bound type because dotCMS neither interprets nor validates it — the
 * schema is the caller's contract with their own tool, and it is passed to the provider as
 * given.</p>
 *
 * @param name        the tool's name, as the model will refer to it; required
 * @param description what the tool does; optional but strongly advised, the model reads it
 * @param parameters  JSON Schema for the arguments; required
 */
public record InferenceToolSpec(String name, String description, JsonNode parameters)
        implements Serializable {

    public InferenceToolSpec {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("InferenceToolSpec name is required");
        }
        if (parameters == null) {
            throw new IllegalArgumentException(
                    "InferenceToolSpec parameters is required; use an empty object schema for a tool taking none");
        }
    }
}
