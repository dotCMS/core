package com.dotcms.util;

import static com.dotmarketing.util.LuceneQueryUtils.isLuceneQuery;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotValidationException;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility class used for operations over relationships
 * @author nollymar
 */
public class RelationshipUtil {

    private static final ContentletAPI contentletAPI = APILocator.getContentletAPI();

    private static final IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();

    private static final RelationshipAPI relationshipAPI = APILocator.getRelationshipAPI();


    /**
     * Returns a list of related contentlet given a comma separated list of lucene queries and/or contentlet identifiers
     * Additionally, validates the contentlets returned by the query, actually belongs to the specified relationship in the
     * given content type
     * @param relationship
     * @param contentType
     * @param language Language ID of the contentlets to be returned when filtering by identifiers
     * @param query Comma separated list of lucene queries and/or identifiers
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public static List<Contentlet> getRelatedContentFromQuery(
            final Relationship relationship, final ContentType contentType, final long language,
            final String query, final User user) throws DotDataException, DotSecurityException {
        List<Contentlet> relatedContentlets = List.of();

        if (UtilMethods.isSet(query)) {
            relatedContentlets = filterContentlet(language, query, user, true);
            validateRelatedContent(relationship, contentType, relatedContentlets);
        }

        return relatedContentlets;
    }

    /**
     * Returns a list of contentlets filtering by lucene query and/or identifiers
     * @param language
     * @param filter Comma-separated list of filtering criteria, which can be lucene queries and contentlets identifier
     * @param sortBy
     * @param user
     * @param respectFrontendRoles
     * @param isCheckout
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public static List<Contentlet> filterContentlet(final long language, final String filter,
            final String sortBy,
            final User user, final boolean respectFrontendRoles, final boolean isCheckout)
            throws DotDataException, DotSecurityException {
        //LinkedHashMap to preserve the order of the contentlets
        final Map<String, Contentlet> relatedContentlets = new LinkedHashMap<>();

        final boolean isLuceneQuery = isLuceneQuery(filter);

        //Filter can be an identifier or a lucene query (comma separated)
        for (final String elem : filter.split(StringPool.COMMA)) {
            if (!filter.isEmpty()) {
                final boolean isUUID = UUIDUtil.isUUID(elem.trim());
                if (!isUUID && !isLuceneQuery) {
                    throw DotValidationException.relationshipInvalidFilterValue(filter);
                }
                if (isUUID && !relatedContentlets.containsKey(elem.trim())) {
                    final Identifier identifier = identifierAPI.find(elem.trim());
                    final Contentlet relatedContentlet = contentletAPI
                            .findContentletForLanguage(language, identifier);
                    relatedContentlets.put(relatedContentlet.getIdentifier(), isCheckout ? contentletAPI
                            .checkout(relatedContentlet.getInode(), user, respectFrontendRoles)
                            : relatedContentlet);
                } else {
                    relatedContentlets
                            .putAll((isCheckout ? contentletAPI.checkoutWithQuery(elem, user, false)
                                    : contentletAPI.search(elem, 0, 0, sortBy, user, false)).stream()
                                    .collect(Collectors
                                            .toMap(Contentlet::getIdentifier, Function.identity(), (oldValue, newValue) -> oldValue)));
                }
            }
        }

        return relatedContentlets.values().stream().collect(CollectionsUtils.toImmutableList());
    }

    /**
     * Returns a list of contentlets filtering by lucene query and/or identifiers
     * @param language Comma-separated list of filtering criteria, which can be lucene queries and contentlets identifier
     * @param filter
     * @param user
     * @param isCheckout
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public static List<Contentlet> filterContentlet(final long language, final String filter,
            final User user, final boolean isCheckout)
            throws DotDataException, DotSecurityException {
        return filterContentlet(language, filter, null, user, false, isCheckout);
    }

    /**
     * Validates related contentlets according to the specified relationship and content type
     * @param relationship
     * @param contentType
     * @param relatedContentlets
     * @throws DotValidationException
     */
    private static void validateRelatedContent(final Relationship relationship,
            final ContentType contentType, final List<Contentlet> relatedContentlets)
            throws DotValidationException {

        //validates if the contentlet retrieved is using the correct type
        if (relationshipAPI.isParent(relationship, contentType)) {
            for (final Contentlet relatedContentlet : relatedContentlets) {
                final Structure relatedStructure = relatedContentlet.getStructure();
                if (!(relationshipAPI.isChild(relationship, relatedStructure))) {
                    throw DotValidationException.nonMatchingRelationship(
                             relationship, true, contentType, relatedContentlet.getContentType());
                }
            }
        }
        if (relationshipAPI.isChild(relationship, contentType)) {
            for (final Contentlet relatedContentlet : relatedContentlets) {
                final Structure relatedStructure = relatedContentlet.getStructure();
                if (!(relationshipAPI.isParent(relationship, relatedStructure))) {
                    throw DotValidationException.nonMatchingRelationship(
                            relationship, false, contentType, relatedContentlet.getContentType());
                }
            }
        }
    }

}
