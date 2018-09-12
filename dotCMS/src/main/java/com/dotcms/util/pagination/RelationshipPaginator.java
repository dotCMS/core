package com.dotcms.util.pagination;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author nollymar
 */
public class RelationshipPaginator implements Paginator<Map<String, Object>> {

    private final RelationshipAPI relationshipAPI;
    private final ContentTypeAPI contentTypeAPI;
    public static final String CONTENT_TYPE_PARAM = "content_type";

    @VisibleForTesting
    public RelationshipPaginator(final RelationshipAPI relationshipAPI,
            final ContentTypeAPI contentTypeAPI) {
        this.relationshipAPI = relationshipAPI;
        this.contentTypeAPI = contentTypeAPI;
    }

    public RelationshipPaginator(User user) {
        this(APILocator.getRelationshipAPI(), APILocator.getContentTypeAPI(user));
    }

    @Override
    public PaginatedArrayList<Map<String, Object>> getItems(final User user, final int limit,
            final int offset, final Map<String, Object> params) throws PaginationException {
        try {

            final PaginatedArrayList<Map<String, Object>> result = new PaginatedArrayList();

            final List<ContentType> contentTypes = contentTypeAPI.search(null, "name asc", limit, offset);

            final List<Relationship> relationships = relationshipAPI
                    .getOneSidedRelationships((ContentType) params.get(CONTENT_TYPE_PARAM), limit, offset);

            //Adding content types
            result.add(contentTypes.stream().collect(Collectors.toMap(ContentType::id, c -> c)));

            //Adding one sided relationships
            result.add(relationships.stream()
                    .collect(Collectors.toMap(Relationship::getInode, r -> r)));

            return result;
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }
}