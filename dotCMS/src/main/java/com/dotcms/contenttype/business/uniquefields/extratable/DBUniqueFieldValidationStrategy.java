package com.dotcms.contenttype.business.uniquefields.extratable;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.business.UniqueFieldValueDuplicatedException;
import com.dotcms.contenttype.business.uniquefields.UniqueFieldValidationStrategy;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import org.postgresql.util.PGobject;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.dotcms.content.elasticsearch.business.ESContentletAPIImpl.UNIQUE_PER_SITE_FIELD_VARIABLE_NAME;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.CONTENTLET_IDS_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.UNIQUE_PER_SITE_ATTR;
import static com.dotmarketing.util.Constants.DONT_RESPECT_FRONT_END_ROLES;

/**
 * This implementation of the {@link UniqueFieldValidationStrategy} checks the uniqueness of a given
 * value using a SQL Query supported by additional database table called {@code unique_fields}.
 * <p>The value of a given field is considered <i>unique</i> when a combination of different
 * attributes is met. Such a combination is exposed via the {@link UniqueFieldCriteria} class.</p>
 *
 * @author Freddy Rodriguez
 * @since Oct 30th, 2024
 */
@ApplicationScoped
public class DBUniqueFieldValidationStrategy implements UniqueFieldValidationStrategy {

    @Inject
    private UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil;

    public DBUniqueFieldValidationStrategy(){
    }

    @VisibleForTesting
    public DBUniqueFieldValidationStrategy(final  UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil){
        this.uniqueFieldDataBaseUtil = uniqueFieldDataBaseUtil;
    }

    @Override
    @WrapInTransaction
    @CloseDBIfOpened
    public void innerValidate(final Contentlet contentlet, final Field field, final Object fieldValue,
                       final ContentType contentType) throws UniqueFieldValueDuplicatedException, DotDataException, DotSecurityException {

        if (isContentletBeingUpdated(contentlet)) {
            cleanUniqueFieldsUp(contentlet, field);
        }

        final User systemUser = APILocator.systemUser();
        final Host site = APILocator.getHostAPI().find(contentlet.getHost(), systemUser, DONT_RESPECT_FRONT_END_ROLES);
        final Language language = APILocator.getLanguageAPI().getLanguage(contentlet.getLanguageId());
        final UniqueFieldCriteria uniqueFieldCriteria = new UniqueFieldCriteria.Builder()
                .setSite(site)
                .setLanguage(language)
                .setField(field)
                .setContentType(contentType)
                .setValue(fieldValue)
                .setVariantName(contentlet.getVariantId())
                .build();

        insertUniqueValue(uniqueFieldCriteria, contentlet.getIdentifier());
    }

    /**
     * Determines whether the {@link Contentlet} going through the unique field validation process
     * already exists or not, i.e.; is being updated.
     *
     * @param contentlet The {@link Contentlet} to check.
     *
     * @return If the {@link Contentlet} is being updated, returns {@code true}.
     */
    private static boolean isContentletBeingUpdated(final Contentlet contentlet) {
        return UtilMethods.isSet(contentlet.getIdentifier());
    }

    /**
     * When the {@link Contentlet} is being updated, this method removes its entry from the Unique
     * Field table so that it can be safely re-generated later on.
     * <p>If its associated hash belongs to more than one Contentlet, instead of removing the
     * record, it will be updated in order to NOT include the ID of the updated Contentlet. The hash
     * is not re-generated as the Contentlet ID is not used in it.</p>
     *
     * @param contentlet The {@link Contentlet} being updated.
     * @param field      The {@link Field} representing the Unique Field.
     *
     * @throws DotDataException An error occurred when interacting with the database.
     */
    @SuppressWarnings("unchecked")
    private void cleanUniqueFieldsUp(final Contentlet contentlet, final Field field) throws DotDataException {
        final Optional<Map<String, Object>> uniqueFieldOptional = uniqueFieldDataBaseUtil.get(contentlet);

        try {
            if (uniqueFieldOptional.isPresent()) {
                final Map<String, Object> uniqueFields = uniqueFieldOptional.get();

                final String hash = uniqueFields.get("unique_key_val").toString();
                final PGobject supportingValues = (PGobject) uniqueFields.get("supporting_values");
                final Map<String, Object> supportingValuesMap = JsonUtil.getJsonFromString(supportingValues.getValue());
                final List<String> contentletIds = (List<String>) supportingValuesMap.get(CONTENTLET_IDS_ATTR);

                if (contentletIds.size() == 1) {
                    uniqueFieldDataBaseUtil.delete(hash, field.variable());
                } else {
                    contentletIds.remove(contentlet.getIdentifier());
                    uniqueFieldDataBaseUtil.updateContentLists(hash, contentletIds);
                }
            }
        } catch (final IOException e){
            throw new DotDataException(e);
        }
    }

    @Override
    public void afterSaved(final Contentlet contentlet, final boolean isNew) throws DotDataException, DotSecurityException {
        if (isNew) {
            final ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                    .find(contentlet.getContentTypeId());

            final User systemUser = APILocator.systemUser();
            final Host host = APILocator.getHostAPI().find(contentlet.getHost(), systemUser, DONT_RESPECT_FRONT_END_ROLES);
            final Language language = APILocator.getLanguageAPI().getLanguage(contentlet.getLanguageId());

            final List<Field> uniqueFields = contentType.fields().stream()
                    .filter(Field::unique)
                    .collect(Collectors.toList());

            if (uniqueFields.isEmpty()) {
                throw new IllegalArgumentException(String.format("The Content Type '%s' must contain at least one Unique Field", contentType.variable()));
            }

            for (final Field uniqueField : uniqueFields) {
                final Object fieldValue = contentlet.get(uniqueField.variable());

                final UniqueFieldCriteria uniqueFieldCriteria = new UniqueFieldCriteria.Builder()
                        .setSite(host)
                        .setLanguage(language)
                        .setField(uniqueField)
                        .setContentType(contentType)
                        .setValue(fieldValue)
                        .build();

                uniqueFieldDataBaseUtil.updateContentList(uniqueFieldCriteria, contentlet.getIdentifier());
            }
        }
    }

    /**
     * Inserts a new unique field value in the database.
     *
     * @param uniqueFieldCriteria The {@link UniqueFieldCriteria} used to insert the value.
     * @param contentletId        The {@link Contentlet}'s identifier.
     *
     * @throws UniqueFieldValueDuplicatedException The unique value already exists.
     */
    private void insertUniqueValue(final UniqueFieldCriteria uniqueFieldCriteria, final String contentletId) throws UniqueFieldValueDuplicatedException {
        final boolean uniquePerSite = uniqueFieldCriteria.field().fieldVariableValue(UNIQUE_PER_SITE_FIELD_VARIABLE_NAME)
                .map(Boolean::valueOf).orElse(false);

        final Map<String, Object> supportingValues =  new HashMap<>(uniqueFieldCriteria.toMap());
        supportingValues.put(CONTENTLET_IDS_ATTR, List.of(contentletId));
        supportingValues.put(UNIQUE_PER_SITE_ATTR, uniquePerSite);

        try {
            Logger.debug(DBUniqueFieldValidationStrategy.class, String.format("Including value of field '%s' in Contentlet " +
                    "'%s' in the unique_fields table", uniqueFieldCriteria.field().variable(), contentletId));
            uniqueFieldDataBaseUtil.insert(uniqueFieldCriteria.criteria(), supportingValues);
        } catch (final DotDataException e) {
            if (isDuplicatedKeyError(e)) {
                final String duplicatedValueMessage = String.format("The unique value '%s' for the field '%s'" +
                                " in the Content Type '%s' already exists",
                        uniqueFieldCriteria.value(), uniqueFieldCriteria.field().variable(),
                        uniqueFieldCriteria.contentType().variable());

                Logger.error(DBUniqueFieldValidationStrategy.class, duplicatedValueMessage);
                throw new UniqueFieldValueDuplicatedException(duplicatedValueMessage);
            } else {
                final String errorMsg = String.format("Failed to insert unique value for Field '%s' in Contentlet " +
                        "'%s': %s", uniqueFieldCriteria.field().variable(), contentletId, ExceptionUtil.getErrorMessage(e));
                Logger.error(this, errorMsg, e);
                throw new DotRuntimeException(errorMsg);
            }
        }
    }

    @WrapInTransaction
    @Override
    public void recalculate(final Field field, final boolean uniquePerSite) throws UniqueFieldValueDuplicatedException {
        validateField(field);
        try {
            this.uniqueFieldDataBaseUtil.recalculate(field.contentTypeId(), field.variable(), uniquePerSite);
        } catch (final DotDataException e) {
            String errorMsg;
            if (isDuplicatedKeyError(e)) {
                errorMsg = String.format("Re-calculating unique value hash for field '%s' in Content Type '%s' " +
                        "failed because some values would be duplicated.", field.variable(), field.contentTypeId());
                Logger.error(this, errorMsg + ": " + ExceptionUtil.getErrorMessage(e), e);
            } else {
                errorMsg = String.format("Failed to recalculate unique value has for field '%s' in Content Type " +
                        "'%s': %s", field.variable(), field.contentTypeId(), ExceptionUtil.getErrorMessage(e));
                Logger.error(this, errorMsg, e);
            }
            throw new UniqueFieldValueDuplicatedException(errorMsg);
        }
    }

    /**
     * Utility method to check if the exception's message belongs to a situation in which the
     * unique key has been violated. In this case, it means that a unique value already exists.
     *
     * @param exception The exception to check.
     *
     * @return If the exception is related to a duplicated key error, returns {@code true}.
     */
    private static boolean isDuplicatedKeyError(final Exception exception) {
        final String originalMessage = exception.getMessage();
        return originalMessage != null && originalMessage.startsWith(
                "ERROR: duplicate key value violates unique constraint \"unique_fields_pkey\"");
    }

}
