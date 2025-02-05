package com.dotcms.rest.api.v1.content.search.strategies;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.rest.api.v1.content.search.handlers.FieldContext;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.dotmarketing.util.Constants.DONT_RESPECT_FRONT_END_ROLES;
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
        List<String> relatedContent = new ArrayList<>();
        if (childRelationship.isPresent()) {
            // Getting related identifiers from index when filtering by parent
            relatedContent = this.getRelatedIdentifiers(currentUser, offset, sortBy, fieldValue, childRelationship.get());
        }
        return UtilMethods.isSet(relatedContent)
                ? String.join(",", relatedContent).trim()
                : "+" + fieldName + ":" + fieldValue;
    }

    /**
     * Returns a relationship field from the child side
     *
     * @param contentType The {@link ContentType} containing the Relationships field.
     * @param fieldName  The name of the field.
     *
     * @return
     */
    private Optional<Relationship> getRelationshipFromChildField(final ContentType contentType,
                                                                 final String fieldName) {
        final String fieldVar = fieldName.split("\\.")[1];
        final Field field = contentType.fieldMap().get(fieldVar);
        Relationship relationship = APILocator.getRelationshipAPI().byTypeValue(field.relationType());
        // Considers Many-to-One relationships where the fieldName might not contain the relation type value
        if (null == relationship && Objects.nonNull(field.relationType()) && !field.relationType().contains(".")) {
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
    private List<String> getRelatedIdentifiers(final User currentUser, final int offset,
                                               final String finalSort, final String fieldValue,
                                               final Relationship childRelationship) {
        final ContentletAPI conAPI = APILocator.getContentletAPI();
        try {
            final int filteringOffset = offset > 0 && RELATIONSHIPS_FILTER_CRITERIA_SIZE > 0
                    ? offset / RELATIONSHIPS_FILTER_CRITERIA_SIZE
                    : 0;
            final Contentlet relatedContent = conAPI.findContentletByIdentifierAnyLanguage(fieldValue);
            return conAPI.getRelatedContent(relatedContent, childRelationship, true, currentUser,
                            DONT_RESPECT_FRONT_END_ROLES, RELATIONSHIPS_FILTER_CRITERIA_SIZE,
                            filteringOffset, finalSort)
                    .stream().map(Contentlet::getIdentifier).collect(Collectors.toList());
        } catch (final DotDataException e) {
            Logger.warn(this, String.format("An error occurred when retrieving related contents " +
                            "for Contentlet ID '%s': %s",
                    fieldValue, ExceptionUtil.getErrorMessage(e)), e);
            return List.of();
        }
    }

}
