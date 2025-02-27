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
        final Optional<Relationship> childRelationship = this.getRelationshipFromChildField(contentType, fieldName);
        final List<String> relatedContent = childRelationship.isPresent()
                // Getting related identifiers from index when filtering by parent
                ? this.getRelatedIdentifiers(currentUser, offset, sortBy, fieldValue, childRelationship.get())
                : new ArrayList<>();
        return UtilMethods.isSet(relatedContent)
                ? String.join(",", relatedContent)
                : "+" + fieldName + ":" + fieldValue;
    }

    /**
     * Returns a {@link Relationship} object based on the provided field name and Content Type. This
     * method is used to determine the relationship between the parent and child Contentlets.
     *
     * @param contentType The {@link ContentType} containing the Relationships field.
     * @param fieldName  The name of the field.
     *
     * @return An {@link Optional} containing the {@link Relationship} object if found, or an empty
     * {@link Optional} otherwise.
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
     * Returns the list of Contentlets that are referencing the provided child Contentlet. That is,
     * this method returns the potential parents of the specified Contentlet. If no records are
     * returned, it may indicate that the child Contentlet is part of a Relationship between a
     * single Content Type.
     *
     * @param user              The {@link User} performing the search.
     * @param offset            The offset to be used when filtering the related content.
     * @param sort              The sort criteria to be used when filtering the related content.
     * @param fieldValue        The value of the field. In this case, the ID of the child
     *                          Contentlet.
     * @param childRelationship The {@link Relationship} object representing the relationship
     *                          between the parent and child Contentlets.
     *
     * @return The list of Contentlet identifiers that are referencing the provided child
     * Contentlet.
     */
    private List<String> getRelatedIdentifiers(final User user, final int offset,
                                               final String sort, final String fieldValue,
                                               final Relationship childRelationship) {
        final ContentletAPI conAPI = APILocator.getContentletAPI();
        try {
            final int filteringOffset = offset > 0 && RELATIONSHIPS_FILTER_CRITERIA_SIZE > 0
                    ? offset / RELATIONSHIPS_FILTER_CRITERIA_SIZE
                    : 0;
            final Contentlet relatedContent = conAPI.findContentletByIdentifierAnyLanguage(fieldValue);
            return conAPI.getRelatedContent(relatedContent, childRelationship, true, user,
                            DONT_RESPECT_FRONT_END_ROLES, RELATIONSHIPS_FILTER_CRITERIA_SIZE,
                            filteringOffset, sort)
                    .stream().map(Contentlet::getIdentifier).collect(Collectors.toList());
        } catch (final DotDataException e) {
            Logger.warn(this, String.format("An error occurred when retrieving related contents " +
                            "for Contentlet ID '%s': %s",
                    fieldValue, ExceptionUtil.getErrorMessage(e)), e);
            return List.of();
        }
    }

}
