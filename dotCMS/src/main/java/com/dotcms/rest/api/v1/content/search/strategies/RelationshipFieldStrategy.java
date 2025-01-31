package com.dotcms.rest.api.v1.content.search.strategies;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rest.api.v1.content.search.handlers.FieldContext;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.liferay.util.StringPool.BLANK;

/**
 * This Strategy Field implementation specifies the correct syntax for querying a Relationship Field
 * via Lucene query in dotCMS. This particular Field Strategy is more complex than others because
 * the provided value represent the Identifier(s) of the child contentlet(s), and the resulting
 * query must contain the Identifier(s) of the parent contentlets that are referencing them.
 *
 * @author Jose Castro
 * @since Jan 29th, 2025
 */
public class RelationshipFieldStrategy implements FieldStrategy {

    /** Number of children related IDs to be added to a lucene query to get children related to a selected parent */
    private static final int RELATIONSHIPS_FILTER_CRITERIA_SIZE = Config.getIntProperty("RELATIONSHIPS_FILTER_CRITERIA_SIZE", 500);

    @Override
    public boolean checkRequiredValues(final FieldContext fieldContext) {
        final Object fieldValue = fieldContext.fieldValue();
        final ContentType contentType = fieldContext.contentType();
        final User currentUser = fieldContext.user();
        return null != fieldValue && !fieldValue.toString().isEmpty() && null != contentType && null != currentUser;
    }

    @Override
    public String generateQuery(final FieldContext fieldContext) {
        final ContentType contentType = fieldContext.contentType();
        final User currentUser = fieldContext.user();
        final int offset = fieldContext.offset();
        final String sortBy = fieldContext.sortBy();
        final String fieldName = fieldContext.fieldName();
        final String fieldValue = fieldContext.fieldValue().toString();
        if (null == contentType) {
            return BLANK;
        }
        final Optional<Relationship> childRelationship = this.getRelationshipFromChildField(contentType, fieldName);
        final StringBuilder relatedQueryByChild = new StringBuilder();
        List<String> relatedContent = new ArrayList<>();
        if (childRelationship.isPresent()) {
            // Getting related identifiers from index when filtering by parent
            relatedContent = this.getRelatedIdentifiers(currentUser, offset, relatedQueryByChild,
                    sortBy, fieldValue, childRelationship);

            relatedQueryByChild.append(fieldName).append(":").append(fieldValue);
        }
        return String.join(",", relatedContent).trim();
    }

    /**
     * Returns a relationship field from the child side
     *
     * @param contentType
     * @param fieldName
     *
     * @return
     */
    private Optional<Relationship> getRelationshipFromChildField(final ContentType contentType,
                                                                 final String fieldName) {
        final String fieldVar = fieldName.split("\\.")[1];
        final Field field = contentType.fieldMap().get(fieldVar);
        Relationship relationship = APILocator.getRelationshipAPI().byTypeValue(field.relationType());
        // Considers Many-to-One relationships where the fieldName might not contain the relation type value
        if (null == relationship && !Objects.isNull(field.relationType()) && !field.relationType().contains(".")) {
            relationship = APILocator.getRelationshipAPI().byTypeValue(contentType.variable() + "." + fieldVar);
        }
        return relationship != null ? Optional.of(relationship) : Optional.empty();
    }

    /**
     *
     * @param currentUser
     * @param offset
     * @param relatedQueryByChild
     * @param finalSort
     * @param fieldValue
     * @param childRelationship
     * @return
     */
    private List<String> getRelatedIdentifiers(final User currentUser, int offset,
                                       final StringBuilder relatedQueryByChild, final String finalSort, final String fieldValue,
                                       final Optional<Relationship> childRelationship) {
        final ContentletAPI conAPI = APILocator.getContentletAPI();
        try {
            final Contentlet relatedParent = conAPI.findContentletByIdentifierAnyLanguage(fieldValue);
            final List<String> relatedContent = conAPI
                    .getRelatedContent(relatedParent, childRelationship.get(), true,
                            currentUser, false, RELATIONSHIPS_FILTER_CRITERIA_SIZE,
                            offset / RELATIONSHIPS_FILTER_CRITERIA_SIZE, finalSort).stream()
                    .map(Contentlet::getIdentifier).collect(Collectors.toList());

            if (relatedQueryByChild.length() > 0) {
                relatedQueryByChild.append(",");
            }
            return relatedContent;
        } catch (final DotDataException e) {
            Logger.warn(this, String.format("An error occurred when retrieving Contentlet ID '%s': %s",
                    fieldValue, e.getMessage()), e);
            return List.of();
        }
    }

}
