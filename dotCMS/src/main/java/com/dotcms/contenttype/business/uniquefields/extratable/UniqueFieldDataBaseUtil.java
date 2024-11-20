package com.dotcms.contenttype.business.uniquefields.extratable;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.util.StringPool;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.CONTENTLET_IDS_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.CONTENT_TYPE_ID_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.FIELD_VALUE_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.FIELD_VARIABLE_NAME_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.LANGUAGE_ID_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.UNIQUE_PER_SITE_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.VARIANT_ATTR;
import static com.dotcms.util.CollectionsUtils.list;

/**
 * Util class to handle QL statement related with the unique_fiedls table
 */
@ApplicationScoped
public class UniqueFieldDataBaseUtil {

    private static final String INSERT_SQL = "INSERT INTO unique_fields (unique_key_val, supporting_values) VALUES (encode(sha256(?::bytea), 'hex'), ?)";

    private static final String RECALCULATE_UNIQUE_KEY_VAL = "UPDATE unique_fields\n" +
            "SET unique_key_val = encode(sha256(\n" +
            "    jsonb_extract_path_text(supporting_values, '" + CONTENT_TYPE_ID_ATTR + "')::bytea || \n" +
            "    jsonb_extract_path_text(supporting_values, '" + FIELD_VARIABLE_NAME_ATTR + "')::bytea || \n" +
            "    jsonb_extract_path_text(supporting_values, '" + LANGUAGE_ID_ATTR + "')::bytea || \n" +
            "    jsonb_extract_path_text(supporting_values, '" + FIELD_VALUE_ATTR + "')::bytea\n" +
            "    %s \n" +
            "), 'hex'), \n" +
            "supporting_values = jsonb_set(supporting_values, '{" + UNIQUE_PER_SITE_ATTR + "}', '%s') \n" +
            "WHERE supporting_values->>'" + CONTENT_TYPE_ID_ATTR + "' = ?\n" +
            "AND supporting_values->>'" + FIELD_VARIABLE_NAME_ATTR + "' = ?";

    private static final String INSERT_SQL_WIT_HASH = "INSERT INTO unique_fields (unique_key_val, supporting_values) VALUES (?, ?)";

    private static final String UPDATE_CONTENT_LIST ="UPDATE unique_fields " +
            "SET supporting_values = jsonb_set(supporting_values, '{" + CONTENTLET_IDS_ATTR + "}', ?::jsonb) " +
            "WHERE unique_key_val = ?";

    private static final String GET_UNIQUE_FIELDS_BY_CONTENTLET = "SELECT * FROM unique_fields " +
            "WHERE supporting_values->'" + CONTENTLET_IDS_ATTR + "' @> ?::jsonb AND supporting_values->>'" + VARIANT_ATTR + "' = ?";

    private static final String DELETE_UNIQUE_FIELD = "DELETE FROM unique_fields WHERE unique_key_val = ? " +
            "AND supporting_values->>'" + FIELD_VARIABLE_NAME_ATTR + "' = ?";

    /**
     * Insert a new register into the unique_fields table, if already exists another register with the same
     * 'unique_key_val' then a {@link java.sql.SQLException} is thrown.
     *
     * @param key
     * @param supportingValues
     */
    public void insertWithHash(final String key, final Map<String, Object> supportingValues) throws DotDataException {
        new DotConnect().setSQL(INSERT_SQL_WIT_HASH).addParam(key).addJSONParam(supportingValues).loadObjectResults();
    }

    public void insert(final String key, final Map<String, Object> supportingValues) throws DotDataException {
        new DotConnect()
                .setSQL(INSERT_SQL)
                .addParam(key)
                .addJSONParam(supportingValues)
                .loadObjectResults();
    }

    /**
     * Update the contentList attribute in the supportingValues field of the unique_fields table.
     *
     * @param hash
     * @param contentletId
     */
    public void updateContentList(final UniqueFieldCriteria uniqueFieldCriteria, final String contentletId) throws DotDataException {
        updateContentLists(uniqueFieldCriteria.criteria(), list(contentletId));
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

    public void delete(final String hash, String fieldVariable) throws DotDataException {
        new DotConnect().setSQL(DELETE_UNIQUE_FIELD)
                .addParam(hash)
                .addParam(fieldVariable)
                .loadObjectResults();
    }

    /**
     * Recalculates the values of Unique Fields for all contents of a given type. Additionally, it
     * will take into consideration whether the Site ID must be included in the new hash or not
     * based on the value of the 'uniquePerSite' parameter.
     *
     * @param contentTypeId The Content Type ID of the contents whose hash must be recalculated.
     * @param fieldVarName  The Velocity Var Name of the unique field.
     * @param uniquePerSite If {@code true}, the Site ID will be included in the new hash.
     *
     * @throws DotDataException If an error occurs when interacting with the database.
     */
    public void recalculate(final String contentTypeId, final String fieldVarName, final boolean uniquePerSite) throws DotDataException {
        new DotConnect().setSQL(getUniqueRecalculationQuery(uniquePerSite))
                .addParam(contentTypeId)
                .addParam(fieldVarName)
                .loadObjectResults();
    }

    /**
     * Returns the SQL query to recalculate the unique key value of a unique field taking into
     * consideration the value of the 'uniquePerSite' field variable.
     *
     * @param uniquePerSite If {@code true}, the Site ID will be included in the new hash.
     *
     * @return The SQL query to recalculate the unique key value.
     */
    private static String getUniqueRecalculationQuery(final boolean uniquePerSite) {
        return String.format(RECALCULATE_UNIQUE_KEY_VAL, uniquePerSite
                ? " || jsonb_extract_path_text(supporting_values, '" + UniqueFieldCriteria.SITE_ID_ATTR + "')::bytea"
                : StringPool.BLANK,
                uniquePerSite);
    }

}
