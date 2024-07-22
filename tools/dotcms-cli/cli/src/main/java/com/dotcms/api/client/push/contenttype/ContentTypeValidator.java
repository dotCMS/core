package com.dotcms.api.client.push.contenttype;

import com.dotcms.contenttype.model.field.FieldLayoutRow;
import com.dotcms.contenttype.model.type.ContentType;

import java.util.List;
import java.util.Objects;
/**
 * A utility class for validating {@link ContentType} instances.
 * <p>
 * This class provides static methods to validate various constraints on {@link ContentType} objects. Currently,
 * it includes validation to ensure that no row in a {@link ContentType} has more than a specified number of columns.
 * Additional validation methods can be added in the future to extend functionality.
 * </p>
 * <p>
 * This class is not intended to be instantiated. All methods are static and can be accessed directly from the class.
 * </p>
 */
public final class ContentTypeValidator {

    private static final String CONTENT_TYPE_NOT_NULL_MESSAGE = "ContentType must not be null.";
    private static final int COLUMNS_PER_ROW_LIMIT = 4;
    private static final String COLUMNS_PER_ROW_LIMIT_MESSAGE =
            "The maximum number of columns per row is limited to four.";

    /**
     * Private constructor to prevent instantiation.
     * <p>
     * This class is a utility class and should not be instantiated.
     * </p>
     */
    private ContentTypeValidator() {
    }

    /**
     * Validates the given {@link ContentType} to ensure it meets all defined constraints.
     * <p>
     * This method currently performs the following validation:
     * <ul>
     *     <li>Ensures that the {@link ContentType} is not {@code null}.</li>
     *     <li>Ensures that no row in the {@link ContentType} has more columns than the allowed limit.</li>
     * </ul>
     * </p>
     * <p>
     * Additional validation checks may be added in the future to handle other constraints. If any validation fails,
     * an {@link IllegalArgumentException} is thrown with an appropriate error message.
     * </p>
     *
     * @param contentType the {@link ContentType} to be validated
     * @throws NullPointerException if {@code contentType} is {@code null}
     * @throws IllegalArgumentException if any row has more than the allowed columns
     */
    public static void validate(ContentType contentType) {
        Objects.requireNonNull(contentType, CONTENT_TYPE_NOT_NULL_MESSAGE);
        if (!hasValidColumnCountPerRow(contentType.layout())) {
            throw new IllegalArgumentException(COLUMNS_PER_ROW_LIMIT_MESSAGE);
        }
    }

    /**
     * Validates that no row in the given list of {@link FieldLayoutRow} has more than four columns.
     *
     * @param layoutRows the list of {@link FieldLayoutRow} to be validated
     * @return {@code true} if all rows have four or fewer columns, {@code false} otherwise
     */
    private static boolean hasValidColumnCountPerRow( List<FieldLayoutRow> layoutRows) {
        if (layoutRows == null) {
            return true;
        }
        for (FieldLayoutRow row : layoutRows) {
            if (row.columns().size() > COLUMNS_PER_ROW_LIMIT) {
                return false;
            }
        }
        return true;
    }

}
