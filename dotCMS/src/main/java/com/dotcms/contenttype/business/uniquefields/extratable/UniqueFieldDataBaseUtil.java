package com.dotcms.contenttype.business.uniquefields.extratable;


import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;

import com.dotcms.contenttype.business.UniqueFieldValueDuplicatedException;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.liferay.util.StringPool;

import javax.enterprise.context.ApplicationScoped;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;


import static com.dotcms.content.elasticsearch.business.ESContentletAPIImpl.UNIQUE_PER_SITE_FIELD_VARIABLE_NAME;

import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.*;
import static com.dotcms.util.CollectionsUtils.list;


/**
 * Util class to handle QL statement related with the unique_fiedls table
 */
@ApplicationScoped
public class UniqueFieldDataBaseUtil {

    private static final String INSERT_SQL = "INSERT INTO unique_fields (unique_key_val, supporting_values) VALUES (encode(sha256(convert_to(?::text, 'UTF8')), 'hex'), ?)";

    private static final String RECALCULATE_UNIQUE_KEY_VAL = "UPDATE unique_fields\n" +
            "SET unique_key_val = encode(sha256(" +
                "convert_to(\n" +
                    "CONCAT(" +
                        "    jsonb_extract_path_text(supporting_values, '" + CONTENT_TYPE_ID_ATTR + "')::text || \n" +
                        "    jsonb_extract_path_text(supporting_values, '" + FIELD_VARIABLE_NAME_ATTR + "')::text || \n" +
                        "    jsonb_extract_path_text(supporting_values, '" + LANGUAGE_ID_ATTR + "')::text || \n" +
                        "    jsonb_extract_path_text(supporting_values, '" + FIELD_VALUE_ATTR + "')::text\n" +
                        "    %s \n" +
                    "),'UTF8'\n" +
                ")\n" +
            "), 'hex'), \n" +
            "supporting_values = jsonb_set(supporting_values, '{" + UNIQUE_PER_SITE_ATTR + "}', '%s') \n" +
            "WHERE supporting_values->>'" + CONTENT_TYPE_ID_ATTR + "' = ?\n" +
            "AND supporting_values->>'" + FIELD_VARIABLE_NAME_ATTR + "' = ?";

    private static final String UPDATE_CONTENT_LIST ="UPDATE unique_fields " +
            "SET supporting_values = jsonb_set(supporting_values, '{" + CONTENTLET_IDS_ATTR + "}', ?::jsonb) " +
            "WHERE unique_key_val = encode(sha256(convert_to(?::text, 'UTF8')), 'hex')";

    private static final String UPDATE_CONTENT_LIST_WITH_HASH ="UPDATE unique_fields " +
            "SET supporting_values = jsonb_set(supporting_values, '{" + CONTENTLET_IDS_ATTR + "}', ?::jsonb) " +
            "WHERE unique_key_val = ?";

    private static final String DELETE_UNIQUE_FIELD = "DELETE FROM unique_fields WHERE unique_key_val = ? " +
            "AND supporting_values->>'" + FIELD_VARIABLE_NAME_ATTR + "' = ?";

    private final static String GET_UNIQUE_FIELDS_BY_CONTENTLET = "SELECT * FROM unique_fields " +
            "WHERE supporting_values->'" + CONTENTLET_IDS_ATTR + "' @> ?::jsonb " +
            "AND supporting_values->>'" + VARIANT_ATTR + "' = ? " +
            "AND (supporting_values->>'"+ LANGUAGE_ID_ATTR + "')::BIGINT = ? " +
            "AND supporting_values->>'" + FIELD_VARIABLE_NAME_ATTR + "' = ?";

    private final static String DELETE_UNIQUE_FIELDS_BY_CONTENTLET = "DELETE FROM unique_fields " +
            "WHERE supporting_values->'" + CONTENTLET_IDS_ATTR + "' @> ?::jsonb AND supporting_values->>'" + VARIANT_ATTR + "' = ? " +
            "AND (supporting_values->>'"+ LANGUAGE_ID_ATTR + "')::BIGINT = ? " +
            "AND (supporting_values->>'" + LIVE_ATTR + "')::BOOLEAN = ?";

    private final static String SET_LIVE_BY_CONTENTLET = "UPDATE unique_fields " +
            "SET supporting_values = jsonb_set(supporting_values, '{" + LIVE_ATTR +  "}', ?::jsonb) " +
            "WHERE supporting_values->'" + CONTENTLET_IDS_ATTR + "' @> ?::jsonb " +
                "AND supporting_values->>'" + VARIANT_ATTR + "' = ? " +
                "AND (supporting_values->>'"+ LANGUAGE_ID_ATTR + "')::BIGINT = ? " +
                "AND (supporting_values->>'" + LIVE_ATTR + "')::BOOLEAN = false";


    private final static String GET_UNIQUE_FIELDS_BY_CONTENTLET_AND_LANGUAGE = "SELECT * FROM unique_fields " +
            "WHERE supporting_values->'" + CONTENTLET_IDS_ATTR + "' @> ?::jsonb AND (supporting_values->>'" + LANGUAGE_ID_ATTR +"')::BIGINT = ?";

    private final static String GET_UNIQUE_FIELDS_BY_CONTENTLET_AND_VARIANT= "SELECT * FROM unique_fields " +
            "WHERE supporting_values->'" + CONTENTLET_IDS_ATTR + "' @> ?::jsonb AND supporting_values->>'" + VARIANT_ATTR + "' = ?";

    private final String DELETE_UNIQUE_FIELDS = "DELETE FROM unique_fields WHERE unique_key_val = ?";

    private final static String DELETE_UNIQUE_FIELDS_BY_FIELD = "DELETE FROM unique_fields " +
            "WHERE supporting_values->>'" + FIELD_VARIABLE_NAME_ATTR + "' = ?";

    private final static String DELETE_UNIQUE_FIELDS_BY_CONTENT_TYPE = "DELETE FROM unique_fields " +
            "WHERE supporting_values->>'" + CONTENT_TYPE_ID_ATTR + "' = ?";


    private final static String POPULATE_UNIQUE_FIELDS_VALUES_QUERY = "INSERT INTO unique_fields (unique_key_val, supporting_values) " +
            "SELECT  encode(" +
            "            sha256(" +
            "                    convert_to(" +
            "                            CONCAT(" +
            "                                    content_type_id::text," +
            "                                    field_var_name::text," +
            "                                    language_id::text," +
            "                                    field_value::text," +
            "                                    CASE WHEN uniquePerSite = 'true' THEN COALESCE(host_id::text, '') ELSE '' END" +
            "                            )," +
            "                            'UTF8'" +
            "                    )" +
            "            )," +
            "            'hex'" +
            "       ) AS unique_key_val, " +
            "       json_build_object('" + CONTENT_TYPE_ID_ATTR + "', content_type_id, " +
                                    "'" + FIELD_VARIABLE_NAME_ATTR + "', field_var_name, " +
                                    "'" + LANGUAGE_ID_ATTR + "', language_id, " +
                                    "'" + FIELD_VALUE_ATTR +"', field_value, " +
                                    "'" + SITE_ID_ATTR + "', host_id, " +
                                    "'" + VARIANT_ATTR + "', variant_id, " +
                                    "'" + UNIQUE_PER_SITE_ATTR + "', " + "uniquePerSite, " +
                                    "'" + LIVE_ATTR + "', live, " +
                                    "'" + CONTENTLET_IDS_ATTR + "', contentlet_identifier) AS supporting_values " +
            "FROM (" +
            "        SELECT structure.inode                                       AS content_type_id," +
            "               field.velocity_var_name                               AS field_var_name," +
            "               contentlet.language_id                                AS language_id," +
            "               identifier.host_inode                                 AS host_id," +
            "               jsonb_extract_path_text(contentlet_as_json -> 'fields', field.velocity_var_name)::jsonb ->>'value' AS field_value," +
            "               ARRAY_AGG(DISTINCT contentlet.identifier)                      AS contentlet_identifier," +
            "               (CASE WHEN COUNT(DISTINCT contentlet_version_info.variant_id) > 1 THEN 'DEFAULT' ELSE MAX(contentlet_version_info.variant_id) END) AS variant_id, " +
            "               ((CASE WHEN COUNT(*) > 1 AND COUNT(DISTINCT contentlet_version_info.live_inode = contentlet.inode) > 1 THEN 0 " +
            "                   ELSE MAX((CASE WHEN contentlet_version_info.live_inode = contentlet.inode THEN 1 ELSE 0 END)::int) " +
            "                   END) = 1) AS live," +
            "               (MAX(CASE WHEN field_variable.variable_value = 'true' THEN 1 ELSE 0 END)) = 1 AS uniquePerSite" +
            "        FROM contentlet" +
            "                 INNER JOIN structure ON structure.inode = contentlet.structure_inode" +
            "                 INNER JOIN field ON structure.inode = field.structure_inode" +
            "                 INNER JOIN identifier ON contentlet.identifier = identifier.id" +
            "                 INNER JOIN contentlet_version_info ON contentlet_version_info.live_inode = contentlet.inode OR" +
            "                                                       contentlet_version_info.working_inode = contentlet.inode" +
            "                 LEFT JOIN field_variable ON field_variable.field_id = field.inode AND field_variable.variable_key = '" + UNIQUE_PER_SITE_FIELD_VARIABLE_NAME + "'" +
            "        WHERE jsonb_extract_path_text(contentlet_as_json -> 'fields', field.velocity_var_name) IS NOT NULL" +
            "          AND field.unique_ = true" +
            "        GROUP BY structure.inode," +
            "                 field.velocity_var_name," +
            "                 contentlet.language_id," +
            "                 identifier.host_inode," +
            "                 jsonb_extract_path_text(contentlet_as_json -> 'fields', field.velocity_var_name)::jsonb ->>'value') as data_to_populate";


    @WrapInTransaction
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
    @WrapInTransaction
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
    @WrapInTransaction
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
    @WrapInTransaction
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
    @CloseDBIfOpened
    public List<Map<String, Object>> get(final Contentlet contentlet, final Field field) throws DotDataException {
        return new DotConnect().setSQL(GET_UNIQUE_FIELDS_BY_CONTENTLET)
                .addParam("\"" + contentlet.getIdentifier() + "\"")
                .addParam(contentlet.getVariantId())
                .addParam(contentlet.getLanguageId())
                .addParam(field.variable())
                .loadObjectResults();
    }

    /**
     * Deletes a unique field from the database.
     *
     * @param hash          The hash of the unique field.
     * @param fieldVariable The Velocity Var Name of the unique field being deleted.
     *
     * @throws DotDataException If an error occurs when interacting with the database.
     */
    @WrapInTransaction
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
    @WrapInTransaction
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

    @CloseDBIfOpened
    public List<Map<String, Object>> get(final String contentId, final long languageId) throws DotDataException {
        return new DotConnect().setSQL(GET_UNIQUE_FIELDS_BY_CONTENTLET_AND_LANGUAGE)
                .addParam("\"" + contentId + "\"")
                .addParam(languageId)
                .loadObjectResults();
    }

    /**
     * Find Unique Field Values by {@link Contentlet} and {@link  com.dotcms.variant.model.Variant}
     *
     * @param contentId
     * @param variantId
     * @return
     * @throws DotDataException
     */
    @CloseDBIfOpened
    public List<Map<String, Object>> get(final String contentId, final String variantId) throws DotDataException {
        return new DotConnect().setSQL(GET_UNIQUE_FIELDS_BY_CONTENTLET_AND_VARIANT)
                .addParam("\"" + contentId + "\"")
                .addParam(variantId)
                .loadObjectResults();
    }

    /**
     * Delete a Unique Field Value by hash
     *
     * @param hash
     * @throws DotDataException
     */
    @WrapInTransaction
    public void delete(final String hash) throws DotDataException {
        new DotConnect().setSQL(DELETE_UNIQUE_FIELDS)
                .addParam(hash)
                .loadObjectResults();
    }

    /**
     * Delete all the unique values for a Field
     *
     * @param field
     * @throws DotDataException
     */
    @WrapInTransaction
    public void delete(final Field field) throws DotDataException {
        new DotConnect().setSQL(DELETE_UNIQUE_FIELDS_BY_FIELD)
                .addParam(field.variable())
                .loadObjectResults();
    }

    /**
     * Delete all the unique values for a {@link ContentType}
     *
     * @param contentType
     * @throws DotDataException
     */
    @WrapInTransaction
    public void delete(final ContentType contentType) throws DotDataException {
        new DotConnect().setSQL(DELETE_UNIQUE_FIELDS_BY_CONTENT_TYPE)
                .addParam(contentType.id())
                .loadObjectResults();
    }

    /**
     * Set the supporting_value->live attribute to true to any register with the same Content's id, variant and language
     *
     * @param contentlet
     * @param liveValue
     * @throws DotDataException
     */
    @WrapInTransaction
    public void setLive(Contentlet contentlet, final boolean liveValue) throws DotDataException {

         new DotConnect().setSQL(SET_LIVE_BY_CONTENTLET)
                 .addParam(String.valueOf(liveValue))
                .addParam("\"" + contentlet.getIdentifier() + "\"")
                .addParam(contentlet.getVariantId())
                .addParam(contentlet.getLanguageId())
                .loadObjectResults();

    }

    /**
     * Remove any register with supporting_value->live set to true and the same Content's id, variant and language
     *
     * @param contentlet
     *
     * @throws DotDataException
     */
    @WrapInTransaction
    public void removeLive(Contentlet contentlet) throws DotDataException {

        new DotConnect().setSQL(DELETE_UNIQUE_FIELDS_BY_CONTENTLET)
                .addParam("\"" + contentlet.getIdentifier() + "\"")
                .addParam(contentlet.getVariantId())
                .addParam(contentlet.getLanguageId())
                .addParam(true)
                .loadObjectResults();
    }

    /**
     * Create the {@code unique_fields} table for the new Unique Field Data base
     * Validation mechanism. The new {@code unique_fields} table will be used to validate fields that must be
     * unique, and what parameters were used to defined such a uniqueness feature.
     *
     * <h4>Table Definition:</h4>
     * <pre>
     *     {@code
     * CREATE TABLE unique_fields (
     *     unique_key_val VARCHAR(64) PRIMARY KEY,
     *     supporting_values JSONB
     * );
     * }
     * </pre>
     * <h4>Columns:</h4>
     * The {@code unique_key_val} column will store a hash created from a combination of the following:
     * <ul>
     *     <li>Content type ID.</li>
     *     <li>Field variable name.</li>
     *     <li>Field value.</li>
     *     <li>Language.</li>
     *     <li>Site ID (if the {@code uniquePerSite} option is enabled).</li>
     * </ul>
     * <p>
     * The {@code supporting_values} column contains a JSON object with the following format:
     * <pre>
     *     {@code
     * {
     *     "contentTypeID": "",
     *     "fieldVariableName": "",
     *     "fieldValue": "",
     *     "languageId": "",
     *     "hostId": "",
     *     "uniquePerSite": true|false,
     *     "contentletsId": [...],
     *     "variant": "",
     *     "live": true|fsle
     * }
     * }
     * </pre>
     * <p>The {@code contentletsId} array holds the IDs of contentlets with the same field value that
     * existed before the database was upgraded. After the upgrade, no more contentlets with
     * duplicate values will be allowed.</p>
     *
     * <h4>Additional Details:</h4>
     * <ul>
     *     <li>The Host ID is included in the hash calculation only if the {@code uniquePerSite}
     *     field variable is enabled.</li>
     *     <li>The {@code unique_key_val} field ensures that only truly unique values can be inserted
     *     moving forward.</li>
     *     <li>This upgrade task also populates the {@code unique_fields} table with the existing
     *     unique field values from the current database.</li>
     * </ul>
     */
    @WrapInTransaction
    public void createUniqueFieldsValidationTable() throws DotDataException {
        new DotConnect().setSQL("CREATE TABLE IF NOT EXISTS unique_fields (" +
                    "unique_key_val VARCHAR(64) PRIMARY KEY," +
                    "supporting_values JSONB" +
                " )").loadObjectResults();
    }

    @WrapInTransaction
    public void createTableAnsPopulate() throws DotDataException {
            createUniqueFieldsValidationTable();
            populateUniqueFieldsTable();
    }

    /**
     * Drop the {@code unique_fields} table for the new Unique Field Data base validation mechanism.
     *
     * @see UniqueFieldDataBaseUtil#createUniqueFieldsValidationTable()
     *
     * @throws DotDataException
     */
    @WrapInTransaction
    public void dropUniqueFieldsValidationTable() throws DotDataException {
        try {
            new DotConnect().setSQL("DROP TABLE unique_fields").loadObjectResults();
        } catch (DotDataException e) {
            final Throwable cause = e.getCause();

            if (!SQLException.class.isInstance(cause) ||
                    !"ERROR: table \"unique_fields\" does not exist".equals(cause.getMessage())) {
                throw e;
            }
        }
    }

    /**
     * Populates the {@code unique_fields} table with unique field values extracted from the {@code contentlet} table.
     *
     * The process involves:
     * - Identifying all {@link com.dotcms.contenttype.model.type.ContentType} objects with unique fields.
     * - Retrieving all {@link Contentlet} entries and their corresponding values for both LIVE and Working versions.
     * - Storing these unique field values into the {@code unique_fields} table with all this data.
     *
     * @throws DotDataException
     */
    @WrapInTransaction
    public void populateUniqueFieldsTable() throws DotDataException {
        new DotConnect().setSQL(POPULATE_UNIQUE_FIELDS_VALUES_QUERY).loadObjectResults();
    }
}
