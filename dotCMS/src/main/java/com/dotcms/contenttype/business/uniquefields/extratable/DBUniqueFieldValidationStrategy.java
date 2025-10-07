package com.dotcms.contenttype.business.uniquefields.extratable;

import static com.dotcms.content.elasticsearch.business.ESContentletAPIImpl.UNIQUE_PER_SITE_FIELD_VARIABLE_NAME;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.CONTENTLET_IDS_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.FIELD_VALUE_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.UNIQUE_PER_SITE_ATTR;
import static com.dotmarketing.util.Constants.DONT_RESPECT_FRONT_END_ROLES;
import static com.liferay.util.StringPool.BLANK;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.business.UniqueFieldValueDuplicatedException;
import com.dotcms.contenttype.business.uniquefields.UniqueFieldValidationStrategy;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.postgresql.util.PGobject;

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

        final UniqueFieldCriteria uniqueFieldCriteria = this.buildUniqueFieldCriteria(contentlet, field, fieldValue, contentType);
        this.insertUniqueValue(uniqueFieldCriteria, contentlet.getIdentifier());
    }

    @Override
    @WrapInTransaction
    @CloseDBIfOpened
    public void innerValidateInPreview(final Contentlet contentlet, final Field field, final Object fieldValue,
                                        final ContentType contentType)
            throws UniqueFieldValueDuplicatedException, DotDataException, DotSecurityException {
        final UniqueFieldCriteria uniqueFieldCriteria = this.buildUniqueFieldCriteria(contentlet, field, fieldValue, contentType);
        final Optional<UniqueFieldDataBaseUtil.UniqueFieldValue> uniqueFieldValueOptional =
                uniqueFieldDataBaseUtil.get(uniqueFieldCriteria);
        if (uniqueFieldValueOptional.isPresent()) {
            this.checkUniqueFieldDuplication(contentlet, uniqueFieldCriteria, uniqueFieldValueOptional.get());
        }
    }

    /**
     * Creates a {@link UniqueFieldCriteria} object based on the provided {@link Contentlet},
     * {@link Field}, field value, and {@link ContentType}. These criteria are used to check the
     * uniqueness of the field value.
     *
     * @param contentlet  The {@link Contentlet} being validated.
     * @param field       The {@link Field} that is being validated for uniqueness.
     * @param fieldValue  The value of the field to be checked for uniqueness.
     * @param contentType The {@link ContentType} of the Contentlet.
     *
     * @return A new instance of {@link UniqueFieldCriteria}.
     *
     * @throws DotDataException     If there is an error accessing the database.
     * @throws DotSecurityException If there is a security issue accessing the host or language.
     */
    private UniqueFieldCriteria buildUniqueFieldCriteria(final Contentlet contentlet,
                                                         final Field field, final Object fieldValue,
                                                         final ContentType contentType) throws DotDataException, DotSecurityException {
        final User systemUser = APILocator.systemUser();
        final Host site = APILocator.getHostAPI().find(contentlet.getHost(), systemUser, DONT_RESPECT_FRONT_END_ROLES);
        final Language language = APILocator.getLanguageAPI().getLanguage(contentlet.getLanguageId());
        return new UniqueFieldCriteria.Builder()
                .setSite(site)
                .setLanguage(language)
                .setField(field)
                .setContentType(contentType)
                .setValue(fieldValue)
                .setVariantName(contentlet.getVariantId())
                .setLive(isLive(contentlet))
                .build();
    }

    /**
     * Verifies that the Unique Field value of the Contentlet that is being validated in
     * {@code Preview} via the Content Import Tool is NOT used by another Contentlet. This happens
     * when inserting or updating an existing Contentlet that has a unique field value that matches
     * one or more records in the CSV file.
     * <p>If the Contentlet being validated is NOT in the list of Contentlets using this Unique
     * Value set in the {@link UniqueFieldCriteria#CONTENTLET_IDS_ATTR} list, it means that there is
     * a duplication, and an error will be thrown.</p>
     *
     * @param contentlet          The {@link Contentlet} to check.
     * @param uniqueFieldCriteria The {@link UniqueFieldCriteria} used to check the value.
     * @param uniqueFieldValue    The {@link UniqueFieldDataBaseUtil.UniqueFieldValue} that matches
     *                            the unique field value.
     *
     * @throws UniqueFieldValueDuplicatedException The unique value already exists and belongs to
     *                                             another Contentlet.
     */
    @SuppressWarnings("unchecked")
    private void checkUniqueFieldDuplication(final Contentlet contentlet,
                                             final UniqueFieldCriteria uniqueFieldCriteria,
                                             final UniqueFieldDataBaseUtil.UniqueFieldValue uniqueFieldValue) throws UniqueFieldValueDuplicatedException {
        if (null != uniqueFieldValue.getSupportingValues()) {
            final Map<String, Object> supportingValuesMap = uniqueFieldValue.getSupportingValues();
            final List<String> contentletIds = (List<String>) supportingValuesMap.getOrDefault(CONTENTLET_IDS_ATTR, List.of());
            if (!contentletIds.contains(contentlet.getIdentifier())) {
                throwsDuplicatedException(uniqueFieldCriteria);
            }
        }
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
     * When the {@link Contentlet} is being updated, this method removes its existing entry from the
     * Unique Fields table so that it can be safely re-generated after the update.
     * <p>If its associated hash belongs to more than one Contentlet, instead of removing the
     * record, it will be updated in order to NOT include the ID of the updated Contentlet. Keep in
     * mind that the hash is <b>NOT</b> re-generated at all, as the Contentlet ID is not used to
     * create it.</p>
     * <p>A unique value can be used by a LIVE or WORKING version of a Contentlet. If the value for
     * the LIVE and WORKING versions are different, dotCMS will keep a record for each of them. But,
     * if the value is the same, then it'll have just 1 record for both with the LIVE attribute set
     * to {@code true}. When the table is cleaned up after the updating the unique value, we need to
     * check if the records that already exists are LIVE or WORKING.</p>
     *
     * @param contentlet The {@link Contentlet} whose unique field value is being updated.
     *
     * @throws DotDataException An error occurred when interacting with the database.
     */
    private  void cleanUniqueFieldsUp(final Contentlet contentlet, final Field field) throws DotDataException {
        final List<Map<String, Object>> uniqueFields = uniqueFieldDataBaseUtil.get(contentlet, field);
        if (UtilMethods.isSet(uniqueFields)) {
            final List<Map<String, Object>> workingUniqueFields = uniqueFields.stream()
                    .filter(uniqueValue -> Boolean.FALSE.equals(getSupportingValues(uniqueValue).get("live")))
                    .collect(Collectors.toList());

            if (!workingUniqueFields.isEmpty()) {
                workingUniqueFields.forEach(uniqueField -> cleanUniqueFieldUp(contentlet.getIdentifier(), uniqueField));
            } else {
                uniqueFields.stream()
                        .filter(uniqueValue -> Boolean.TRUE.equals(getSupportingValues(uniqueValue).get("live")))
                        .limit(1)
                        .findFirst()
                        .ifPresent(uniqueFieldValue -> {
                            final Map<String, Object> supportingValues = getSupportingValues(uniqueFieldValue);
                            final String oldUniqueValue = supportingValues.get(FIELD_VALUE_ATTR).toString();
                            // All Unique Field values are stored in lower case. So, the incoming
                            // value must be lower-cased to potentially match the existing one
                            final String newUniqueValue = UtilMethods.isSet(contentlet.getStringProperty(field.variable()))
                                    ? contentlet.getStringProperty(field.variable()).toLowerCase()
                                    : BLANK;
                            if (oldUniqueValue.equals(newUniqueValue)) {
                                // The unique value is the same. It must be removed so that it can
                                // be added again later
                                cleanUniqueFieldUp(contentlet.getIdentifier(), uniqueFieldValue);
                            }
                        });
            }
        }
    }

    /**
     * Transforms the JSONB value of the {@code supporting_values} column into a Java Map that can
     * be easily traversed.
     *
     * @param uniqueField The raw record from the {@code unique_fields} table that belongs to a
     *                    unique field value.
     *
     * @return The supporting values as a Java Map.
     */
    private static Map<String, Object> getSupportingValues(Map<String, Object> uniqueField) {
        try {
            return JsonUtil.getJsonFromString(uniqueField.get("supporting_values").toString());
        } catch (final IOException e) {
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Searches for all records that belong to the given Contentlet ID. For every record, if it
     * matches only one Contentlet, this method deletes it from the table. Otherwise, it updates
     * the record to remove the ID of the updated Contentlet, as such an attribute is an array of
     * Contentlet IDs. This way, when the updated Contentlet is saved, it won't have a conflict with
     * the hash.
     *
     * @param contentId The ID of the Contentlet whose unique field value must be temporarily removed.
     * @param uniqueFields The records from the {@code unique_fields} table that belong to a unique
     * field value.
     */
    @SuppressWarnings("unchecked")
    private void cleanUniqueFieldUp(final String contentId,
                                    final Map<String, Object> uniqueFields)  {
        try {
            final String hash = uniqueFields.get("unique_key_val").toString();
            final PGobject supportingValues = (PGobject) uniqueFields.get("supporting_values");
            final Map<String, Object> supportingValuesMap = JsonUtil.getJsonFromString(supportingValues.getValue());
            final List<String> contentletIds = (List<String>) supportingValuesMap.get(CONTENTLET_IDS_ATTR);

            if (contentletIds.size() == 1) {
                uniqueFieldDataBaseUtil.delete(hash);
            } else {
                contentletIds.remove(contentId);
                uniqueFieldDataBaseUtil.updateContentListWithHash(hash, contentletIds);
            }
        } catch (final IOException | DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    @Override
    public void afterSaved(final Contentlet contentlet, final boolean isNew) throws DotDataException, DotSecurityException {
        if (hasUniqueField(contentlet.getContentType()) && isNew) {
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
                        .setLive(isLive(contentlet))
                        .build();

                uniqueFieldDataBaseUtil.updateContentList(uniqueFieldCriteria, contentlet.getIdentifier());
            }
        }
    }

    private static boolean isLive(Contentlet contentlet)  {
        try {
            return contentlet.isLive();
        } catch (DotDataException | DotSecurityException | DotStateException e) {
            return false;
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
                throwsDuplicatedException(uniqueFieldCriteria);
            } else {
                final String errorMsg = String.format("Failed to insert unique value for Field '%s' in Contentlet " +
                        "'%s': %s", uniqueFieldCriteria.field().variable(), contentletId, ExceptionUtil.getErrorMessage(e));
                Logger.error(this, errorMsg, e);
                throw new DotRuntimeException(errorMsg);
            }
        }
    }

    /**
     * Throws a duplicated exception with the right message
     *
     * @param uniqueFieldCriteria
     * @throws UniqueFieldValueDuplicatedException
     */
    private static void throwsDuplicatedException(final UniqueFieldCriteria uniqueFieldCriteria)
            throws UniqueFieldValueDuplicatedException {
        final String duplicatedValueMessage = String.format("The unique value '%s' for the field '%s'" +
                        " in the Content Type '%s' already exists",
                uniqueFieldCriteria.value(), uniqueFieldCriteria.field().variable(),
                uniqueFieldCriteria.contentType().variable());

        Logger.error(DBUniqueFieldValidationStrategy.class, duplicatedValueMessage);
        
        throw UniqueFieldValueDuplicatedException.builder(
                duplicatedValueMessage,
                uniqueFieldCriteria.field().variable(), 
                uniqueFieldCriteria.value(), 
                uniqueFieldCriteria.contentType().variable())
                .fieldType(uniqueFieldCriteria.field().typeName())
                .build();
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
            throw UniqueFieldValueDuplicatedException.builder(
                    errorMsg,
                    field.variable(), 
                    "N/A",
                    field.contentTypeId())
                    .fieldType(field.typeName())
                    .build();
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

    @Override
    public void cleanUp(final Contentlet contentlet, final boolean deleteAllVariant) throws DotDataException {
        if (deleteAllVariant) {
            uniqueFieldDataBaseUtil.get(contentlet.getIdentifier(), contentlet.getVariantId()).stream()
                    .forEach(uniqueFieldValue -> cleanUniqueFieldUp(contentlet.getIdentifier(), uniqueFieldValue));
        } else {
            uniqueFieldDataBaseUtil.get(contentlet.getIdentifier(), contentlet.getLanguageId()).stream()
                    .forEach(uniqueFieldValue -> cleanUniqueFieldUp(contentlet.getIdentifier(), uniqueFieldValue));
        }

    }

    @Override
    public void cleanUp(final Field field) throws DotDataException {
        uniqueFieldDataBaseUtil.delete(field);
    }

    @Override
    public void cleanUp(final ContentType contentType) throws DotDataException {
        uniqueFieldDataBaseUtil.delete(contentType);
    }

    @Override
    public void afterPublish(final String inode) {
        try {
            final Contentlet contentlet = APILocator.getContentletAPI().find(inode, APILocator.systemUser(), false);

            if (hasUniqueField(contentlet.getContentType())) {
                uniqueFieldDataBaseUtil.removeLive(contentlet);
                uniqueFieldDataBaseUtil.setLive(contentlet, true);
            }
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void afterUnpublish(final VersionInfo versionInfo){
        try {
            final Contentlet liveContentlet = APILocator.getContentletAPI().find(versionInfo.getLiveInode(),
                    APILocator.systemUser(), false);

            if (hasUniqueField(liveContentlet.getContentType())) {
                if (versionInfo.getWorkingInode().equals(versionInfo.getLiveInode())) {
                    uniqueFieldDataBaseUtil.setLive(liveContentlet, false);
                } else {
                    uniqueFieldDataBaseUtil.removeLive(liveContentlet);
                }
            }
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException(e);
        }
    }


    private static boolean hasUniqueField(ContentType contentType) {
        return contentType.fields().stream().anyMatch(field -> field.unique());
    }
}
