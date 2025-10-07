package com.dotcms.contenttype.business.uniquefields;

import com.dotcms.content.elasticsearch.util.ESUtils;
import com.dotcms.contenttype.business.UniqueFieldValueDuplicatedException;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import  com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

import static com.dotcms.content.elasticsearch.business.ESContentletAPIImpl.UNIQUE_PER_SITE_FIELD_VARIABLE_NAME;

/**
 * {@link UniqueFieldValidationStrategy} that check the unique values using ElasticSearch queries.
 *
 * The query that is run looks like the follow:
 *
 * +structureInode:[typeInode] -identifier: [contentletId]  +languageId:[contentletLang]  +conHost:[hostId] [typeVariable].[uniqueFieldVariable]_sha256 = sha256(fieldValue)
 *
 * Where:
 * - typeInode: Inode of the Content Type
 * - contentletId: {@link Contentlet}'s Identifier, this filter is just add if the {@link Contentlet} is not new,
 * if it is a new {@link Contentlet} then this filter is removed because the {@link Contentlet} does not have any Id after be saved.
 * - contentletLang: {@link Contentlet}'s language
 * - [typeVariable].[uniqueFieldVariable]_sha256 = sha256(fieldValue): For each unique field an extra attribute is saved
 * in ElasticSearch it is named concatenating _sha256 to the name of the unique field, so here the filter is looking for the value.
 * 
 * If this query return any register then it means that the value was already taken so a UniqueFieldValidationStrategy is thrown.
 * 
 * This approach has a race condition bug because if another {@link Contentlet} is saved before that the change is mirror
 * in ES then the duplicate value is going to be allowed, remember that the {@link Contentlet} sare storage in ES in an async way.
 */
@ApplicationScoped
@Default
public class ESUniqueFieldValidationStrategy implements UniqueFieldValidationStrategy {

    /**
     * ES implementation for {@link UniqueFieldValidationStrategy#innerValidate(Contentlet, Field, Object, ContentType)}
     *
     * @param contentlet
     * @param uniqueField
     * @param fieldValue
     * @param contentType
     * @throws UniqueFieldValueDuplicatedException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Override
    public void innerValidate(final Contentlet contentlet, final Field uniqueField, final Object fieldValue,
                       final  ContentType contentType)
            throws UniqueFieldValueDuplicatedException, DotDataException, DotSecurityException {

        final boolean isDataTypeNumber =
                uniqueField.dataType().equals(DataTypes.INTEGER)
                        || uniqueField.dataType().equals(DataTypes.FLOAT);

        final List<ContentletSearch> contentlets = getContentletFromES(contentlet, uniqueField, fieldValue);
        int size = contentlets.size();

        if (size > 0) {
            boolean unique = true;

            for (final ContentletSearch contentletSearch : contentlets) {
                final com.dotmarketing.portlets.contentlet.model.Contentlet uniqueContent = APILocator.getContentletAPI()
                        .find(contentletSearch.getInode(), APILocator.systemUser(), false);

                if (null == uniqueContent) {
                    final String errorMsg = String.format(
                            "Unique field [%s] could not be validated, as " +
                                    "unique content Inode '%s' was not found. ES Index might need to be reindexed.",
                            uniqueField.variable(), contentletSearch.getInode());
                    Logger.warn(this, errorMsg);
                    throw DotContentletValidationException.builder(errorMsg)
                            .addUniqueField(new LegacyFieldTransformer(uniqueField).asOldField(),
                                    null != fieldValue ? fieldValue.toString() : "N/A").build();
                }

                final Map<String, Object> uniqueContentMap = uniqueContent.getMap();
                final Object obj = uniqueContentMap.get(uniqueField.variable());

                if ((isDataTypeNumber && Objects.equals(fieldValue, obj)) ||
                        (!isDataTypeNumber && ((String) obj).equalsIgnoreCase(
                                ((String) fieldValue)))) {
                    unique = false;
                    break;
                }

            }

            if (!unique) {
                if (UtilMethods.isSet(contentlet.getIdentifier())) {
                    Iterator<ContentletSearch> contentletsIter = contentlets.iterator();
                    while (contentletsIter.hasNext()) {
                        ContentletSearch cont = contentletsIter.next();
                        if (!contentlet.getIdentifier()
                                .equalsIgnoreCase(cont.getIdentifier())) {

                            final String duplicatedValueMessage = String.format("The value %s for the field %s in the Content type %s is duplicated",
                                    fieldValue, uniqueField.variable(), contentType.variable());

                            throw UniqueFieldValueDuplicatedException.builder(
                                    duplicatedValueMessage,
                                    uniqueField.variable(), 
                                    fieldValue, 
                                    contentType.variable())
                                    .fieldType(uniqueField.dataType().toString())
                                    .contentletIds(contentlets.stream().map(ContentletSearch::getIdentifier).collect(Collectors.toList()))
                                    .build();
                        }
                    }
                } else {
                    final String duplicatedValueMessage = String.format("The value %s for the field %s in the Content type %s is duplicated",
                            fieldValue, uniqueField.variable(), contentType.variable());

                    throw UniqueFieldValueDuplicatedException.builder(
                            duplicatedValueMessage,
                            uniqueField.variable(), 
                            fieldValue, 
                            contentType.variable())
                            .fieldType(uniqueField.dataType().toString())
                            .contentletIds(contentlets.stream().map(ContentletSearch::getIdentifier).collect(Collectors.toList()))
                            .build();
                }
            }
        }
    }

    /**
     * Build and execute the Lucene Query to check unique fields validation in ES.
     *
     * @param contentlet
     * @param uniqueField
     * @param fieldValue
     * @return
     */
    private List<ContentletSearch> getContentletFromES(Contentlet contentlet, Field uniqueField, Object fieldValue) {
        final StringBuilder buffy = new StringBuilder(UUIDGenerator.generateUuid());
        buffy.append(" +structureInode:" + contentlet.getContentTypeId());
        if (UtilMethods.isSet(contentlet.getIdentifier())) {
            buffy.append(" -(identifier:" + contentlet.getIdentifier() + ")");
        }

        buffy.append(" +languageId:" + contentlet.getLanguageId());

        if (getUniquePerSiteConfig(uniqueField)) {

            buffy.append(" +conHost:" + contentlet.getHost());
        }

        buffy.append(" +").append(contentlet.getContentType().variable())
                .append(StringPool.PERIOD)
                .append(uniqueField.variable()).append(ESUtils.SHA_256)
                .append(StringPool.COLON)
                .append(ESUtils.sha256(contentlet.getContentType().variable()
                                + StringPool.PERIOD + uniqueField.variable(), fieldValue,
                        contentlet.getLanguageId()));
        final List<ContentletSearch> contentlets = new ArrayList<>();
        try {
            contentlets.addAll(
                    APILocator.getContentletAPI().searchIndex(buffy.toString() + " +working:true", -1, 0, "inode",
                            APILocator.getUserAPI().getSystemUser(), false));
            contentlets.addAll(
                    APILocator.getContentletAPI().searchIndex(buffy.toString() + " +live:true", -1, 0, "inode",
                            APILocator.getUserAPI().getSystemUser(), false));
        } catch (final Exception e) {
            final String errorMsg =
                    "Unique field [" + uniqueField.variable() + "] with value '" +
                            fieldValue + "' could not be validated: " + e.getMessage();
            Logger.warn(this, errorMsg, e);
            throw new DotContentletValidationException(errorMsg, e);
        }
        return contentlets;
    }

    private boolean getUniquePerSiteConfig(final com.dotcms.contenttype.model.field.Field field) {
        return field.fieldVariableValue(UNIQUE_PER_SITE_FIELD_VARIABLE_NAME)
                .map(Boolean::valueOf).orElse(false);
    }
}
