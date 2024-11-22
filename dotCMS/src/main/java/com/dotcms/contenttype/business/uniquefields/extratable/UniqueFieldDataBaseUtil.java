package com.dotcms.contenttype.business.uniquefields.extratable;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.util.StringPool;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.*;
import static com.dotcms.util.CollectionsUtils.list;
import static org.apache.lucene.queries.function.valuesource.LiteralValueSource.hash;

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
            "WHERE unique_key_val = encode(sha256(?::bytea), 'hex')";

    private static final String UPDATE_CONTENT_LIST_WITH_HASH ="UPDATE unique_fields " +
            "SET supporting_values = jsonb_set(supporting_values, '{" + CONTENTLET_IDS_ATTR + "}', ?::jsonb) " +
            "WHERE unique_key_val = ?";

    private static final String DELETE_UNIQUE_FIELD = "DELETE FROM unique_fields WHERE unique_key_val = ? " +
            "AND supporting_values->>'" + FIELD_VARIABLE_NAME_ATTR + "' = ?";

    private final static String GET_UNIQUE_FIELDS_BY_CONTENTLET = "SELECT * FROM unique_fields " +
            "WHERE supporting_values->'" + CONTENTLET_IDS_ATTR + "' @> ?::jsonb AND supporting_values->>'" + VARIANT_ATTR + "' = ? " +
            "AND (supporting_values->>'"+ LANGUAGE_ID_ATTR + "')::INTEGER = ? " +
            "AND (supporting_values->>'" + LIVE_ATTR + "')::BOOLEAN = ?";


    private final static String GET_UNIQUE_FIELDS_BY_CONTENTLET_AND_LANGUAGE = "SELECT * FROM unique_fields " +
            "WHERE supporting_values->'" + CONTENTLET_IDS_ATTR + "' @> ?::jsonb AND (supporting_values->>'" + LANGUAGE_ID_ATTR +"')::INTEGER = ?";

    private final static String GET_UNIQUE_FIELDS_BY_CONTENTLET_AND_VARIANT= "SELECT * FROM unique_fields " +
            "WHERE supporting_values->'" + CONTENTLET_IDS_ATTR + "' @> ?::jsonb AND supporting_values->>'variant' = ?";

    private final String DELETE_UNIQUE_FIELDS = "DELETE FROM unique_fields WHERE unique_key_val = ?";


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
     * Updates the list of Contentlets associated to a specific unique field. This method is
     * critical to track Contentlets whose unique value is the same, which is what happened with the
     * Elasticsearch Strategy.
     *
     * @param uniqueFieldCriteria The {@link UniqueFieldCriteria} to used to generate the hash
     *                            representing the unique field.
     * @param contentletId        The Contentlet ID to be added to the list.
     */
    public void updateContentList(final UniqueFieldCriteria uniqueFieldCriteria, final String contentletId) throws DotDataException {
        updateContentList(uniqueFieldCriteria.criteria(), list(contentletId));
    }

    /**
     * Updates the list of Contentlets associated to a specific unique field. In case of corrupted
     * records, the {@code contentletIds} parameter will contain more than one Contentlet ID.
     *
     * @param criteria      The String with the concatenated values of the unique field.
     * @param contentletIds The list of Contentlet IDs that match the same unique field.
     *
     * @throws DotDataException An error occurred when interacting with the database.
     */
    public void updateContentList(final String criteria, final List<String> contentletIds) throws DotDataException {
        new DotConnect().setSQL(UPDATE_CONTENT_LIST)
                .addJSONParam(contentletIds)
                .addParam(criteria)
                .loadObjectResults();
    }

    /**
     * Updates the list of Contentlets associated to a specific unique field. In case of corrupted
     * records, the {@code contentletIds} parameter will contain more than one Contentlet ID.
     *
     * @param hash          The hashed String with the concatenated values of the unique field.
     * @param contentletIds The list of Contentlet IDs that match the same unique field.
     *
     * @throws DotDataException An error occurred when interacting with the database.
     */
    public void updateContentListWithHash(final String hash, final List<String> contentletIds) throws DotDataException {
        new DotConnect().setSQL(UPDATE_CONTENT_LIST_WITH_HASH)
                .addJSONParam(contentletIds)
                .addParam(hash)
                .loadObjectResults();
    }

    /**
     * Retrieves the unique field values of a given Contentlet.
     *
     * @param contentlet The {@link Contentlet} to retrieve the unique field values from.
     *
     * @return An {@link Optional} containing the unique field values if they exist, or an empty
     * optional otherwise.
     *
     * @throws DotDataException If an error occurs when interacting with the database.
     */
    public Optional<Map<String, Object>> get(final Contentlet contentlet) throws DotDataException {
        try {
            final List<Map<String, Object>> results = new DotConnect().setSQL(GET_UNIQUE_FIELDS_BY_CONTENTLET)
                    .addParam("\"" + contentlet.getIdentifier() + "\"")
                    .addParam(contentlet.getVariantId())
                    .addParam(contentlet.getLanguageId())
                    .addParam(contentlet.isLive())
                    .loadObjectResults();

            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (DotSecurityException e) {
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Deletes a unique field from the database.
     *
     * @param hash          The hash of the unique field.
     * @param fieldVariable The Velocity Var Name of the unique field being deleted.
     *
     * @throws DotDataException If an error occurs when interacting with the database.
     */
    public void delete(final String hash, final String fieldVariable) throws DotDataException {
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

    public List<Map<String, Object>> get(final String contentId, final long languegeId) throws DotDataException {
        return new DotConnect().setSQL(GET_UNIQUE_FIELDS_BY_CONTENTLET_AND_LANGUAGE)
                .addParam("\"" + contentId + "\"")
                .addParam(languegeId)
                .loadObjectResults();
    }

    public List<Map<String, Object>> get(final String contentId, final String variantId) throws DotDataException {
        return new DotConnect().setSQL(GET_UNIQUE_FIELDS_BY_CONTENTLET_AND_VARIANT)
                .addParam("\"" + contentId + "\"")
                .addParam(variantId)
                .loadObjectResults();
    }

    public void delete(final String hash) throws DotDataException {
        new DotConnect().setSQL(DELETE_UNIQUE_FIELDS)
                .addParam(hash)
                .loadObjectResults();
    }

}
