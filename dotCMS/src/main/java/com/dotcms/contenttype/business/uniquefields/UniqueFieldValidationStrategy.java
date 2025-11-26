package com.dotcms.contenttype.business.uniquefields;

import com.dotcms.contenttype.business.UniqueFieldValueDuplicatedException;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

import java.util.Objects;

/**
 * Allows you to define a strategy to validate unique fields in a {@link Contentlet} in dotCMS.
 *
 * @author Freddy Rodriguez
 * @since Oct 30th, 2024
 */
public interface  UniqueFieldValidationStrategy {

    /**
     * Verifies if a Contentlet with a given Unique Field can be saved without violating unique
     * field constraints. If such a constraint is violated, a
     * {@link UniqueFieldValueDuplicatedException} will be thrown. For content types with multiple
     * unique fields, this method must be called for each unique field individually. This method
     * performs the following checks:
     * <ul>
     *     <li>Ensures the {@link Contentlet}, the {@link Field}, and the value of the
     *     {@link Field} in the {@link Contentlet} are not null.</li>
     *     <li>Verifies that the {@link Field} is indeed a unique field. If not, an
     *     {@link IllegalArgumentException} is thrown.</li>
     *     <li>Ensures the {@link Field} is part of the {@link Contentlet}'s {@link ContentType}.
     *     If not, an {@link IllegalArgumentException} is thrown.</li>
     *     <li>Calls the {@link UniqueFieldValidationStrategy#innerValidate(Contentlet, Field,
     *     Object, ContentType)} method, which must be overridden by subclasses. This method is
     *     responsible for the actual unique value validation.</li>
     * </ul>
     *
     * @param contentlet  The Contentlet that is going to be saved
     * @param uniqueField The Unique field to check
     *
     * @throws UniqueFieldValueDuplicatedException If the unique field constraints is violated.
     * @throws DotDataException                    An error occurred when interacting with the
     *                                             database.
     * @throws DotSecurityException                A User permission error has occurred.
     */
    default void validate(final Contentlet contentlet, final Field uniqueField)
            throws UniqueFieldValueDuplicatedException, DotDataException, DotSecurityException {

        if (!uniqueField.unique()) {
            throw new IllegalArgumentException(String.format("Field '%s' is not marked as 'unique'", uniqueField.variable()));
        }

        final Object value = contentlet.get(uniqueField.variable());
        Objects.requireNonNull(contentlet);
        Objects.requireNonNull(uniqueField);
        Objects.requireNonNull(value);

        final ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(contentlet.getContentTypeId());

        DotPreconditions.isTrue(contentType.fields().stream()
                        .anyMatch(contentTypeField -> Objects.equals(uniqueField.variable(), contentTypeField.variable())),
                String.format("Field %s does not belong to the '%s' Content Type", uniqueField.variable(), contentType.variable()));

        innerValidate(contentlet, uniqueField, value, contentType);
    }

    /**
     * Validate Unique fiedla inside Preview mode, by default this method just called
     * {@link UniqueFieldValidationStrategy#innerValidate(Contentlet, Field, Object, ContentType)} but this behavior
     * can be overriding
     *
     * @param contentlet Contentlet to validate
     * @param uniqueField Unique field to validate
     *
     * @throws UniqueFieldValueDuplicatedException throw if we have any duplicated value
     * @throws DotDataException
     * @throws DotSecurityException
     */
    default void validateInPreview(final Contentlet contentlet, final Field uniqueField)
            throws UniqueFieldValueDuplicatedException, DotDataException, DotSecurityException {

        if (!uniqueField.unique()) {
            throw new IllegalArgumentException(String.format("Field '%s' is not marked as 'unique'", uniqueField.variable()));
        }

        final Object value = contentlet.get(uniqueField.variable());
        Objects.requireNonNull(contentlet);
        Objects.requireNonNull(uniqueField);
        Objects.requireNonNull(value);

        final ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(contentlet.getContentTypeId());

        DotPreconditions.isTrue(contentType.fields().stream()
                        .anyMatch(contentTypeField -> Objects.equals(uniqueField.variable(), contentTypeField.variable())),
                String.format("Field %s does not belong to the '%s' Content Type", uniqueField.variable(), contentType.variable()));

        innerValidateInPreview(contentlet, uniqueField, value, contentType);
    }

    /**
     * This method must be overridden for each {@link UniqueFieldValidationStrategy} implementation.
     * It runs the validation mechanism for the specific strategy. This method must be called by the
     * {@link UniqueFieldValidationStrategy#validate(Contentlet, Field)} method only.
     *
     * @param contentlet  The {@link Contentlet} to be saved.
     * @param field       The {@link Field} to be validated.
     * @param fieldValue  The value to be set.
     * @param contentType The {@link Contentlet}'s {@link ContentType}.
     *
     * @throws UniqueFieldValueDuplicatedException If the unique field constraints is violate
     * @throws DotDataException                    An error occurred when interacting with the
     *                                             database.
     * @throws DotSecurityException                A User permission error has occurred.
     */
     void innerValidate(final Contentlet contentlet, final Field field, final Object fieldValue,
                                final ContentType contentType)
            throws UniqueFieldValueDuplicatedException, DotDataException, DotSecurityException;

    /**
     * Method to be overriding if you want to override the default behavior of the preview validation
     * by default the preview validation in the same that the normal validation.
     *
     * @param contentlet Contentlet to be validated
     * @param field Field to be validated
     * @param fieldValue Value to be validated
     * @param contentType
     *
     * @throws UniqueFieldValueDuplicatedException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    default void innerValidateInPreview(final Contentlet contentlet, final Field field, final Object fieldValue,
                       final ContentType contentType)
            throws UniqueFieldValueDuplicatedException, DotDataException, DotSecurityException {
        innerValidate(contentlet, field, fieldValue, contentType);
    }

    /**
     * This method is called after a {@link Contentlet} is saved. It allows the Strategy to perform
     * any necessary actions to ensure it functions correctly the next time it's used. If the
     * {@link Contentlet} is new when the validate method is called, its ID might not be set yet, so
     * it may be necessary to assign the ID to save information for future use by the Strategy.
     *
     * @param contentlet The {@link Contentlet} that was saved.
     * @param isNew      If {@code true}, then the {@link Contentlet} is new, otherwise the
     *                   {@link Contentlet} was updated.
     *
     * @throws DotDataException     An error occurred when interacting with the database.
     * @throws DotSecurityException A User permission error has occurred.
     */
    default void afterSaved(final Contentlet contentlet, final boolean isNew) throws DotDataException, DotSecurityException {
        // Default implementation does nothing
    }
    default void recalculate(final Field field, final boolean uniquePerSite) throws UniqueFieldValueDuplicatedException {
        // Default implementation does nothing
    }

    /**
     * Validates that the specified {@link Field} is a proper unique field.
     *
     * @param field The field to validate.
     */
    default void validateField(final Field field) {
        Objects.requireNonNull(field);
        Objects.requireNonNull(field.variable());
        if (!field.unique()) {
            throw new IllegalArgumentException(String.format("Field '%s' is not marked as 'unique'", field.variable()));
        }
    }

    /**
     * Clean the Extra unique validation field table after a {@link Contentlet} have been removed.
     * We need to remove all the unique values of this {@link Contentlet} and {@link com.dotmarketing.portlets.languagesmanager.model.Language}
     * from the extra table.
     *
     * @param contentlet
     */
    default void cleanUp(final Contentlet contentlet, final boolean deleteAllVariant) throws DotDataException {
        //Default implementation do nothing
    }

    /**
     * Method call after publish a {@link Contentlet} it allow the {@link UniqueFieldValidationStrategy} do any extra
     * work that it need it.
     *
     * @param inode Published {@link Contentlet}'s inode
     */
    default void afterPublish(final String inode) {
        //Default implementation do nothing
    }

    /**
     * Method call after unpublished a {@link Contentlet} it allow thw {@link UniqueFieldValidationStrategy} do any extra
     * work that it need it.
     *
     * @param versionInfo {@link Contentlet}'s {@link VersionInfo} before un publish
     */
    default void afterUnpublish(final VersionInfo versionInfo){
        //Default implementation do nothing
    }

    /**
     * Method called after delete a Unique {@link Field}, to allow the {@link UniqueFieldValidationStrategy} do any extra
     * work that it need it.
     *
     * @param field deleted field
     * @throws DotDataException
     */
    default void cleanUp(final Field field) throws DotDataException {
        //Default implementation do nothing
    }

    /**
     * Method called after delete a {@link ContentType}, to allow the {@link UniqueFieldValidationStrategy} do any extra
     * work that it need it.
     *
     * @param contentType deleted ContentType
     * @throws DotDataException
     */
    default void cleanUp(final ContentType contentType) throws DotDataException {
        //Default implementation do nothing
    }

}
