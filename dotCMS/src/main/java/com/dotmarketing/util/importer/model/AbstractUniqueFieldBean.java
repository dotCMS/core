package com.dotmarketing.util.importer.model;

import com.dotmarketing.portlets.structure.model.Field;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import org.immutables.value.Value;

/**
 * Represents validation information for a unique field during content import.
 * This immutable data structure tracks information needed to validate uniqueness
 * constraints across multiple lines of imported content, including field definition,
 * value, line number, and language context.
 *
 * <p>Unique fields require special handling during import to ensure values remain
 * unique across both the imported content and existing content in the system.
 * This class provides the context needed for that validation.</p>
 *
 * <p>The interface uses Immutables.org to generate an immutable implementation
 * with a builder pattern. The generated class will be named {@code UniqueFieldBean}.</p>
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonDeserialize(as = UniqueFieldBean.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractUniqueFieldBean extends Serializable {

    /**
     * Gets the field definition for the unique field being validated.
     * This contains metadata about the field including its name, type,
     * and validation rules.
     *
     * @return The Field object representing the unique field
     */
    Field field();

    /**
     * Gets the value being validated for uniqueness.
     * This is the raw value from the import file that needs to be
     * checked against existing content.
     *
     * @return The field value to validate
     */
    Object value();

    /**
     * Gets the line number in the import file where this value appears.
     * Used for error reporting and tracking duplicate values within
     * the imported content.
     *
     * @return The CSV file line number containing this value
     */
    int lineNumber();

    /**
     * Gets the language ID for the content containing this field value.
     * This is needed because uniqueness constraints may apply differently
     * across languages.
     *
     * @return The language ID for the content
     */
    long languageId();
}
