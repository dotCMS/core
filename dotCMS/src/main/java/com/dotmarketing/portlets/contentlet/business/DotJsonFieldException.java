package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.util.importer.ImportLineValidationCodes;
import com.dotmarketing.util.importer.exception.ImportLineError;
import com.liferay.util.StringPool;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;

public class DotJsonFieldException extends DotContentletStateException implements
        ImportLineError {

    public static final String INVALID_JSON_FIELD_FOR_KEY_VALUE = "Invalid JSON field provided. Key Value Field variable: ";
    public static final String INVALID_JSON_FIELD = "Invalid JSON field provided. Field variable: ";

    private final String invalidJson;
    private final int line;
    private final int column;
    private final String parseError;
    private final String field;

    public static String cleanJsonErrorMessage(String errorMessage) {
        String pattern = "\\[([^\\]]*?)(?:;\\s*)?(line:\\s*\\d+,\\s*column:\\s*\\d+)\\]";
        return errorMessage.replaceAll(pattern, "[$2]");
    }

    DotJsonFieldException( @NotNull String message,
            @NotNull String field, @NotNull String invalidJson,
            int line, int column, @NotNull String parseError) {
        super(message);
        // Null-safe assignments with meaningful defaults
        this.field = StringUtils.defaultIfBlank(field, StringPool.UNKNOWN);
        this.invalidJson = Objects.requireNonNullElse(invalidJson, StringPool.BLANK);
        this.line = Math.max(line, 0); // Ensure non-negative line numbers
        this.column = Math.max(column, 0); // Ensure non-negative column numbers
        this.parseError = cleanJsonErrorMessage(StringUtils.defaultIfBlank(parseError, StringPool.UNKNOWN));
    }

    public String getAbbreviatedParseError() {
        return StringUtils.abbreviate(parseError, 200);
    }
    public String getInvalidJson() { return invalidJson; }
    public int getLine() { return line; }
    public int getColumn() { return column; }
    public String getParseError() { return parseError; }

    @Override
    public Optional<String> getField() { return Optional.ofNullable(field); }

    public String getDetailedMessage() {
        return String.format("JSON Validation Error:%n" +
                        "Field: %s%n" +
                        "Position: line %d, column %d%n" +
                        "Error: %s%n" +
                        "Invalid JSON: %s",
                field, line, column, parseError,
                invalidJson.length() > 200 ? invalidJson.substring(0, 200) + "..." : invalidJson);
    }

    @Override
    public Optional<Map<String, ?>> getContext() {
        return Optional.of(Map.of(
                "errorHint", getAbbreviatedParseError(),
                "line", this.line,
                "column", this.column
        ));
    }

    @Override
    public String getCode() {
        return ImportLineValidationCodes.INVALID_JSON.name();
    }

    @Override
    public Optional<String> getValue() { return Optional.ofNullable(getInvalidJson()); }

    /**
     * Factory to build the JsonStateException passing the KeyValue Message
     * @param field
     * @param invalidJson
     * @param line
     * @param column
     * @param parseError
     * @return
     */
    public static DotJsonFieldException keyValueJsonException(
            @NotNull String field, @NotNull String invalidJson,
            int line, int column, @NotNull String parseError) {
        return new DotJsonFieldException(INVALID_JSON_FIELD_FOR_KEY_VALUE + field, field, invalidJson, line, column, parseError);
    }

    /**
     * Factory to build the JsonStateException for regular json fields
     * @param field
     * @param invalidJson
     * @param line
     * @param column
     * @param parseError
     * @return
     */

    public static DotJsonFieldException jsonFieldException(
            @NotNull String field, @NotNull String invalidJson,
            int line, int column, @NotNull String parseError) {
        return new DotJsonFieldException(INVALID_JSON_FIELD + field, field, invalidJson, line, column, parseError);
    }


}
