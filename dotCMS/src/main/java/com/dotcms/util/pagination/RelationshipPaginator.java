package com.dotcms.util.pagination;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Paginator for relationships results
 * @author nollymar
 */
public class RelationshipPaginator implements Paginator<Relationship> {

    private final RelationshipAPI relationshipAPI;
    public static final String CONTENT_TYPE_PARAM = "content_type";

    @VisibleForTesting
    public RelationshipPaginator(final RelationshipAPI relationshipAPI) {
        this.relationshipAPI = relationshipAPI;
    }

    public RelationshipPaginator() {
        this(APILocator.getRelationshipAPI());
    }

    @Override
    public PaginatedArrayList<Relationship> getItems(final User user, final int limit,
            final int offset, final Map<String, Object> params) throws PaginationException {
        try {

            final PaginatedArrayList<Relationship> result = new PaginatedArrayList();
            final ContentType contentType = (ContentType) params.get(CONTENT_TYPE_PARAM);

            final List<Relationship> relationships = relationshipAPI
                    .getOneSidedRelationships(contentType, limit, offset);

            //Adding one sided relationships
            result.addAll(relationships);
            result.setTotalResults(relationshipAPI.getOneSidedRelationshipsCount(contentType));
            return result;
        } catch (DotDataException e) {
            Logger.error(this, "Error getting relationships", e);
            throw new DotRuntimeException(e);
        }
    }
}