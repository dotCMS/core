package com.dotmarketing.portlets.contentlet.business;

import com.liferay.util.StringPool;
import java.util.Map;
import java.util.Objects;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;

public class DotJsonFieldException extends DotContentletStateException {

    public static final String INVALID_JSON_FIELD_PROVIDED_KEY_VALUE_FIELD_VARIABLE = "Invalid JSON field provided. Key Value Field variable: ";
    private final String invalidJson;
    private final int line;
    private final int column;
    private final String parseError;
    private final String field;

    public DotJsonFieldException(
            @NotNull String field, @NotNull String invalidJson,
            int line, int column, @NotNull String parseError) {
        super(INVALID_JSON_FIELD_PROVIDED_KEY_VALUE_FIELD_VARIABLE + field);
        // Null-safe assignments with meaningful defaults
        this.field = StringUtils.defaultIfBlank(field, StringPool.UNKNOWN);
        this.invalidJson = Objects.requireNonNullElse(invalidJson, "{}");
        this.line = Math.max(line, 0); // Ensure non-negative line numbers
        this.column = Math.max(column, 0); // Ensure non-negative column numbers
        this.parseError = StringUtils.defaultIfBlank(parseError, StringPool.UNKNOWN);
    }

    public String getAbbreviatedParseError() {
        return StringUtils.abbreviate(parseError, 200);
    }
    public String getInvalidJson() { return invalidJson; }
    public int getLine() { return line; }
    public int getColumn() { return column; }
    public String getParseError() { return parseError; }
    public String getField() { return field; }

    public String getDetailedMessage() {
        return String.format("JSON Validation Error:%n" +
                        "Field: %s%n" +
                        "Position: line %d, column %d%n" +
                        "Error: %s%n" +
                        "Invalid JSON: %s",
                field, line, column, parseError,
                invalidJson.length() > 200 ? invalidJson.substring(0, 200) + "..." : invalidJson);
    }

    public Map<String, Object> getContext() {
        return Map.of(
                "parseError", getAbbreviatedParseError(),
                "line", this.line,
                "column", this.column
        );
    }

}
