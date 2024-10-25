package com.dotcms.contenttype.business.uniquefields.extratable;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.content.elasticsearch.business.ESContentletAPIImpl;
import com.dotcms.contenttype.business.*;
import com.dotcms.contenttype.business.uniquefields.UniqueFieldValidationStrategy;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import org.postgresql.util.PGobject;

import static com.dotcms.util.CollectionsUtils.list;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * {@link UniqueFieldValidationStrategy} that check the unique values using a SQL Query and a Extra table.
 * This is the same extra table created here {@link com.dotmarketing.startup.runonce.Task241007CreateUniqueFieldsTable}
 */
public class ExtraTableUniqueFieldValidationStrategy extends UniqueFieldValidationStrategy {


    /**
     *
     * @param contentlet
     * @param field
     * @param fieldValue
     * @param contentType
     *
     * @throws UniqueFieldValueDuplicatedException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Override
    @WrapInTransaction
    @CloseDBIfOpened
    public void innerValidate(final Contentlet contentlet, final  Field field, final  Object fieldValue,
                       final ContentType contentType) throws UniqueFieldValueDuplicatedException, DotDataException, DotSecurityException {

        if (UtilMethods.isSet(contentlet.getIdentifier())) {
            cleanUniqueFieldsUp(contentlet, field);
       }

        final User systemUser = APILocator.systemUser();
        final Host host = APILocator.getHostAPI().find(contentlet.getHost(), systemUser, false);
        final Language language = APILocator.getLanguageAPI().getLanguage(contentlet.getLanguageId());

        UniqueFieldCriteria uniqueFieldCriteria = new UniqueFieldCriteria.Builder()
                .setSite(host)
                .setLanguage(language)
                .setField(field)
                .setContentType(contentType)
                .setValue(fieldValue)
                .setVariantName(contentlet.getVariantId())
                .build();

        checkUnique(uniqueFieldCriteria, contentlet.getIdentifier());
    }

    private static void cleanUniqueFieldsUp(final Contentlet contentlet, final  Field field) throws DotDataException {
        Optional<Map<String, Object>> uniqueFieldOptional = UniqueFieldDataBaseUtil.INSTANCE.get(contentlet);

        try {
            if (uniqueFieldOptional.isPresent()) {
                final Map<String, Object> uniqueFields = uniqueFieldOptional.get();

                final String hash = uniqueFields.get("unique_key_val").toString();
                final PGobject supportingValues = (PGobject) uniqueFields.get("supporting_values");
                final Map<String, Object> supportingValuesMap = JsonUtil.getJsonFromString(supportingValues.getValue());
                final List<String> contentletsId = (List<String>) supportingValuesMap.get("contentletsId");

                if (contentletsId.size() == 1) {
                    UniqueFieldDataBaseUtil.INSTANCE.delete(hash, field.variable());
                } else {
                    contentletsId.remove(contentlet.getIdentifier());
                    UniqueFieldDataBaseUtil.INSTANCE.updateContentLists(hash, contentletsId);
                }
            }
        } catch (IOException e){
            throw new DotDataException(e);
        }
    }

    @Override
    public void afterSaved(final Contentlet contentlet, final boolean isNew) throws DotDataException, DotSecurityException {
        if (isNew) {
            final ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                    .find(contentlet.getContentTypeId());

            final User systemUser = APILocator.systemUser();
            final Host host = APILocator.getHostAPI().find(contentlet.getHost(), systemUser, false);
            final Language language = APILocator.getLanguageAPI().getLanguage(contentlet.getLanguageId());

            final List<Field> uniqueFields = contentType.fields().stream()
                    .filter(Field::unique)
                    .collect(Collectors.toList());

            if (uniqueFields.isEmpty()) {
                throw new IllegalArgumentException("The ContentType must contains at least one Unique Field");
            }

            for (final Field uniqueField : uniqueFields) {
                final Object fieldValue = contentlet.get(uniqueField.variable());

                UniqueFieldCriteria uniqueFieldCriteria = new UniqueFieldCriteria.Builder()
                        .setSite(host)
                        .setLanguage(language)
                        .setField(uniqueField)
                        .setContentType(contentType)
                        .setValue(fieldValue)
                        .build();

                UniqueFieldDataBaseUtil.INSTANCE.updateContentList(uniqueFieldCriteria.hash(), contentlet.getIdentifier());
            }
        }
    }

    /**
     * Insert a new unique field value, if the value is duplicated then a {@link java.sql.SQLException} is thrown.
     *
     * @param uniqueFieldCriteria
     * @param contentletId
     *
     * @throws UniqueFieldValueDuplicatedException when the Value is duplicated
     * @throws DotDataException when a DotDataException is throws
     */
    private static void checkUnique(UniqueFieldCriteria uniqueFieldCriteria, String contentletId) throws UniqueFieldValueDuplicatedException {
        final boolean uniqueForSite = uniqueFieldCriteria.field().fieldVariableValue(ESContentletAPIImpl.UNIQUE_PER_SITE_FIELD_VARIABLE_NAME)
                .map(Boolean::valueOf).orElse(false);

        final Map<String, Object> supportingValues =  new HashMap<>(uniqueFieldCriteria.toMap());
        supportingValues.put("contentletsId", CollectionsUtils.list(contentletId));
        supportingValues.put("uniquePerSite", uniqueForSite);

        try {
            Logger.debug(ExtraTableUniqueFieldValidationStrategy.class, "Including value in the unique_fields table");
            UniqueFieldDataBaseUtil.INSTANCE.insert(uniqueFieldCriteria.hash(), supportingValues);
        } catch (DotDataException e) {

            if (isDuplicatedKeyError(e)) {
                final String duplicatedValueMessage = String.format("The value %s for the field %s in the Content type %s is duplicated",
                        uniqueFieldCriteria.value(), uniqueFieldCriteria.field().variable(),
                        uniqueFieldCriteria.contentType().variable());

                Logger.error(ExtraTableUniqueFieldValidationStrategy.class, duplicatedValueMessage);
                throw new UniqueFieldValueDuplicatedException(duplicatedValueMessage);
            }
        }
    }


    private static boolean isDuplicatedKeyError(final Exception exception) {
        final String originalMessage = exception.getMessage();

        return originalMessage != null && originalMessage.startsWith(
                "ERROR: duplicate key value violates unique constraint \"unique_fields_pkey\"");
    }
}
