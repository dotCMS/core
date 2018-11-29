package com.dotcms.util;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class used for operations over relationships
 * @author nollymar
 */
public class RelationshipUtil {

    private final static ContentletAPI contentletAPI = APILocator.getContentletAPI();

    private static final IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();

    private static final LanguageAPI languageAPI     = APILocator.getLanguageAPI();

    private final static RelationshipAPI relationshipAPI = APILocator.getRelationshipAPI();

    public static Relationship getRelationshipFromField(final Field field, final Contentlet contentlet){
        final String fieldRelationType = field.getFieldRelationType();
        return APILocator.getRelationshipAPI().byTypeValue(
                fieldRelationType.contains(StringPool.PERIOD) ? fieldRelationType
                        : contentlet.getContentType().variable() + StringPool.PERIOD + field
                                .getVelocityVarName());


    }

    public static Map<Relationship, List<Contentlet>> getRelatedContentFromQuery(
            final Relationship relationship, final Contentlet contentlet, final String value,
            final User user) throws DotDataException, DotSecurityException {
        List<Contentlet> relatedContentlets = Collections.EMPTY_LIST;
        if (relationship != null && InodeUtils.isSet(relationship.getInode())) {

            if (UtilMethods.isSet(value)) {
                final long language = contentlet != null ? contentlet.getLanguageId() : languageAPI.getDefaultLanguage().getId();
                relatedContentlets = filterContentlet(language, value, user);

                if (contentlet != null){
                    validateRelatedContent(relationship, contentlet, relatedContentlets);
                }
            }
        } else {
            throw new DotDataException("Error processing related content");
        }

        return CollectionsUtils.map(relationship, relatedContentlets);
    }

    /**
     * Returns a list of contentlets filtering by lucene query and/or identifiers
     * @param language
     * @param filter Comma-separated list of filtering criteria, which can be lucene queries and contentlets identifier
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public static List<Contentlet> filterContentlet(final long language, final String filter,
            final User user) throws DotDataException, DotSecurityException {

        final Map<String, Contentlet> relatedContentlets = new HashMap<>();

        //Filter can be an identifier or a lucene query (comma separated)
        for (final String elem : filter.split(StringPool.COMMA)) {
            if (UUIDUtil.isUUID(elem.trim()) && !relatedContentlets.containsKey(elem.trim())) {
                final Identifier identifier = identifierAPI.find(elem.trim());
                final Contentlet relatedContentlet = contentletAPI
                        .findContentletForLanguage(language, identifier);
                relatedContentlets.put(relatedContentlet.getIdentifier(), relatedContentlet);
            } else {
                relatedContentlets
                        .putAll(contentletAPI.search(elem, 0, 0, null, user, false).stream()
                                .filter(contentlet -> !relatedContentlets
                                        .containsKey(contentlet.getIdentifier())).collect(Collectors
                                        .toMap(contentlet -> contentlet.getIdentifier(),
                                                contentlet -> contentlet)));
            }
        }

        return relatedContentlets.values().stream().collect(CollectionsUtils.toImmutableList());
    }

    private static void validateRelatedContent(final Relationship relationship,
            final Contentlet contentlet, final List<Contentlet> relatedContentlets)
            throws DotDataException {

        //validates if the contentlet retrieved is using the correct type
        if (relationshipAPI.isParent(relationship, contentlet.getContentType())) {
            for (final Contentlet relatedContentlet : relatedContentlets) {
                final Structure relatedStructure = relatedContentlet.getStructure();
                if (!(relationshipAPI.isChild(relationship, relatedStructure))) {
                    throw new DotDataException(
                            "The structure does not match the relationship" + relationship
                                    .getRelationTypeValue());
                }
            }
        }
        if (relationshipAPI.isChild(relationship, contentlet.getContentType())) {
            for (final Contentlet relatedContentlet : relatedContentlets) {
                final Structure relatedStructure = relatedContentlet.getStructure();
                if (!(relationshipAPI.isParent(relationship, relatedStructure))) {
                    throw new DotDataException(
                            "The structure does not match the relationship " + relationship
                                    .getRelationTypeValue());
                }
            }
        }
    }

}
