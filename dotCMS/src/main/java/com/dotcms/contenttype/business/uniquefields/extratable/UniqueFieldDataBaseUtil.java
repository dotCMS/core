package com.dotcms.contenttype.business.uniquefields.extratable;

import com.dotcms.business.CloseDBIfOpened;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.dotcms.util.CollectionsUtils.list;

/**
 * Util class to handle QL statement related with the unique_fiedls table
 */
enum UniqueFieldDataBaseUtil {

    INSTANCE;
    private final static String INSERT_SQL = "INSERT INTO unique_fields (unique_key_val, supporting_values) VALUES (?, ?)";
    private final static String UPDATE_CONTENT_LIST ="UPDATE unique_fields " +
            "SET supporting_values = jsonb_set(supporting_values, '{contentletsId}', ?::jsonb) " +
            "WHERE unique_key_val = ?";

    private final static String GET_UNIQUE_FIELDS_BY_CONTENTLET = "SELECT * FROM unique_fields " +
            "WHERE supporting_values->'contentletsId' @> ?::jsonb AND supporting_values->>'variant' = ?";

    private final String DELETE_UNIQUE_FIELDS = "DELETE FROM unique_fields WHERE unique_key_val = ?";

    /**
     * Insert a new register into the unique_fields table, if already exists another register with the same
     * 'unique_key_val' then a {@link java.sql.SQLException} is thrown.
     *
     * @param key
     * @param supportingValues
     */
    public void insert(final String key, final Map<String, Object> supportingValues) throws DotDataException {
        new DotConnect().setSQL(INSERT_SQL).addParam(key).addJSONParam(supportingValues).loadObjectResults();
    }

    /**
     * Update the contentList attribute in the supportingValues field of the unique_fields table.
     *
     * @param hash
     * @param contentletId
     */
    public void updateContentList(final String hash, final String contentletId) throws DotDataException {
        updateContentLists(hash, list(contentletId));
    }

    public void updateContentLists(final String hash, final List contentletIds) throws DotDataException {
        new DotConnect().setSQL(UPDATE_CONTENT_LIST)
                .addJSONParam(contentletIds)
                .addParam(hash)
                .loadObjectResults();
    }

    public Optional<Map<String, Object>> get(final Contentlet contentlet) throws DotDataException {
        final List<Map<String, Object>> results = new DotConnect().setSQL(GET_UNIQUE_FIELDS_BY_CONTENTLET)
                .addParam("\"" + contentlet.getIdentifier() + "\"")
                .addParam(contentlet.getVariantId())
                .loadObjectResults();

        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public void delete(final String hash) throws DotDataException {
        new DotConnect().setSQL(DELETE_UNIQUE_FIELDS)
                .addParam(hash)
                .loadObjectResults();
    }
}
