package com.dotcms.contenttype.business.uniquefields;

import com.dotcms.contenttype.business.UniqueFieldValueDuplicatedException;
import  com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;

import java.util.Objects;

/**
 * Represent a Strategy to check if a value may violate unique field constraints.
 */
public interface  UniqueFieldValidationStrategy {

    /**
     * This method checks if a contentlet can be saved without violating unique field constraints.
     * If a constraint is violated, a {@link UniqueFieldValueDuplicatedException} will be thrown.
     * For content types with multiple unique fields, this method must be called for each unique field individually.
     *
     * This method performs the following checks:
     *
     * - Ensures the {@link Contentlet}, the {@link Field}, and the value of the {@link Field} in the {@link Contentlet}
     * are not null.
     * - Verifies that the {@link Field} is indeed a unique field. If not, an {@link IllegalArgumentException} is thrown.
     * - Ensures the {@link Field} is part of the {@link Contentlet}'s {@link ContentType}.
     * If not, an {@link IllegalArgumentException} is thrown.
     * - Calls the {@link UniqueFieldValidationStrategy#innerValidate(Contentlet, Field, Object, ContentType)} method,
     * which must be overridden by subclasses. This method is responsible for the actual unique value validation.
     *
     * @param contentlet that is going to be saved
     * @param uniqueField Unique field to check
     * @throws UniqueFieldValueDuplicatedException If the unique field contraints is violate
     * @throws DotDataException If it is thrown in the process
     * @throws DotSecurityException If it is thrown in the process
     */
    default void validate(final Contentlet contentlet, final Field uniqueField)
            throws UniqueFieldValueDuplicatedException, DotDataException, DotSecurityException {

        if (!uniqueField.unique()) {
            throw new IllegalArgumentException("The Field " + uniqueField.variable() + " is not unique");
        }

        Object value = contentlet.get(uniqueField.variable());

        Objects.requireNonNull(contentlet);
        Objects.requireNonNull(uniqueField);
        Objects.requireNonNull(value);

        final ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(contentlet.getContentTypeId());

        DotPreconditions.isTrue(contentType.fields().stream()
                        .anyMatch(contentTypeField -> uniqueField.variable().equals(contentTypeField.variable())),
                "Field %s must be one of the field of the ContentType");

        innerValidate(contentlet, uniqueField, value, contentType);
    }

    /**
     * Inner validation this method must be Override for each {@link UniqueFieldValidationStrategy} to implements
     * the real validation approach for the specific strategy.
     *
     * This method must be called just by the {@link UniqueFieldValidationStrategy#validate(Contentlet, Field)} method.
     *
     * @param contentlet {@link Contentlet} to be saved
     * @param field Field to be validated
     * @param fieldValue Value to be set
     * @param contentType {@link Contentlet}'s {@link ContentType}
     *
     * @throws UniqueFieldValueDuplicatedException If the unique field contraints is violate
     * @throws DotDataException If it is thrown in the process
     * @throws DotSecurityException If it is thrown in the process
     */
     void innerValidate(final Contentlet contentlet, final Field field, final Object fieldValue,
                                ContentType contentType)
            throws UniqueFieldValueDuplicatedException, DotDataException, DotSecurityException;

    /**
     * This method is called after a {@link Contentlet} is saved. It allows the Strategy to perform any necessary
     * actions to ensure it functions correctly the next time it's used. If the {@link Contentlet} is new when the validate
     * method is called, its ID might not be set yet, so it may be necessary to assign the ID to save information
     * for future use by the Strategy.
     *
//     * @param contentlet {@link Contentlet} saved
     * @param isNew if it is true then the  {@link Contentlet} is new, otherwise the {@link Contentlet} was updated
     */
    default void afterSaved(final Contentlet contentlet, final boolean isNew) throws DotDataException, DotSecurityException, UniqueFieldValueDuplicatedException {

    }
}
