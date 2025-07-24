package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.util.importer.ImportLineValidationCodes;
import com.dotmarketing.util.importer.exception.ImportLineErrorAware;
import com.liferay.util.StringPool;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;

public class DotJsonFieldException extends DotContentletStateException implements
        ImportLineErrorAware {

    public static final String INVALID_JSON_FIELD_PROVIDED_KEY_VALUE_FIELD_VARIABLE = "Invalid JSON field provided. Key Value Field variable: ";
    private final String invalidJson;
    private final int line;
    private final int column;
    private final String parseError;
    private final String field;

    //Let's reduce noise from our error messages
    private static final Pattern JACKSON_SOURCE_PATTERN =
            Pattern.compile("Source: REDACTED \\(`StreamReadFeature\\.INCLUDE_SOURCE_IN_LOCATION` disabled\\);\\s*");

    private static String cleanJacksonMessage(@NotNull String message) {
        return JACKSON_SOURCE_PATTERN.matcher(message).replaceAll("").trim();
    }

    public DotJsonFieldException(
            @NotNull String field, @NotNull String invalidJson,
            int line, int column, @NotNull String parseError) {
        super(INVALID_JSON_FIELD_PROVIDED_KEY_VALUE_FIELD_VARIABLE + field);
        // Null-safe assignments with meaningful defaults
        this.field = StringUtils.defaultIfBlank(field, StringPool.UNKNOWN);
        this.invalidJson = Objects.requireNonNullElse(invalidJson, StringPool.BLANK);
        this.line = Math.max(line, 0); // Ensure non-negative line numbers
        this.column = Math.max(column, 0); // Ensure non-negative column numbers
        this.parseError = cleanJacksonMessage(StringUtils.defaultIfBlank(parseError, StringPool.UNKNOWN));
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
    public Optional<Map<String, ? extends Object>> getContext() {
        return Optional.of(Map.of(
                "parseError", getAbbreviatedParseError(),
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

}
