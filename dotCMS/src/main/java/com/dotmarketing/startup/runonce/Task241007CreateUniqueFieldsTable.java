package com.dotmarketing.startup.runonce;

import com.dotcms.content.elasticsearch.business.ESContentletAPIImpl;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.common.db.Params;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import org.jetbrains.annotations.NotNull;
import org.postgresql.util.PGobject;

import java.sql.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.CONTENTLET_IDS_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.CONTENT_TYPE_ID_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.FIELD_VALUE_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.FIELD_VARIABLE_NAME_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.LANGUAGE_ID_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.SITE_ID_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.UNIQUE_PER_SITE_ATTR;

/**
 * This Upgrade Task creates and populates the {@code unique_fields} table for the new Unique Field
 * Validation mechanism. The new {@code unique_fields} will be used to validate fields that must be
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
 *     "contentletsId": [...]
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
 *
 * @author Freddy Rodriguez
 * @since Oct 30th, 2024
 */
public class Task241007CreateUniqueFieldsTable implements StartupTask {

    private final static String CREATE_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS unique_fields (" +
            "unique_key_val VARCHAR(64) PRIMARY KEY," +
            "supporting_values JSONB" +
            " )";

    private static final String RETRIEVE_UNIQUE_FIELD_VALUES_QUERY = "SELECT structure.inode AS content_type_id," +
                " field.velocity_var_name AS field_var_name," +
                " contentlet.language_id AS language_id," +
                " identifier.host_inode AS host_id," +
                " jsonb_extract_path_text(contentlet_as_json -> 'fields', field.velocity_var_name)::jsonb ->> 'value' AS field_value," +
                " ARRAY_AGG(contentlet.identifier) AS contentlet_identifier" +
            " FROM contentlet" +
                " INNER JOIN structure ON structure.inode = contentlet.structure_inode" +
                " INNER JOIN field ON structure.inode = field.structure_inode" +
                " INNER JOIN identifier ON contentlet.identifier = identifier.id" +
            " WHERE jsonb_extract_path_text(contentlet_as_json->'fields', field.velocity_var_name) IS NOT NULL AND " +
            " field.unique_ = true " +
            " GROUP BY structure.inode," +
                    " field.velocity_var_name ," +
                    " contentlet.language_id," +
                    " identifier.host_inode," +
                    " jsonb_extract_path_text(contentlet_as_json -> 'fields', field.velocity_var_name)::jsonb ->> 'value'";

    private static final String INSERT_UNIQUE_FIELDS_QUERY = "INSERT INTO unique_fields(unique_key_val, supporting_values) VALUES(?, ?)";

    @Override
    public boolean forceRun() {
        try {
            final DotDatabaseMetaData databaseMetaData = new DotDatabaseMetaData();
            return !databaseMetaData.tableExists(DbConnectionFactory.getConnection(), "unique_fields");
        } catch (SQLException e) {
            Logger.error(this, e.getMessage(),e);
            return false;
        }
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {

        if (forceRun()) {
            createUniqueFieldTable();

            try {
                populate();
            } catch (SQLException e) {
                throw new DotDataException(e);
            }
        }
    }

    /**
     * Populate the unique_fields table with the Unique Fields values
     *
     * @throws DotDataException
     * @throws SQLException
     */
    private void populate() throws DotDataException, SQLException {
        final List<Map<String, Object>> uniqueFieldsValues = retrieveUniqueFieldsValues();

        final List<Params> params = new ArrayList<>();

        for (final Map<String, Object> uniqueFieldsValue : uniqueFieldsValues) {

            final String hash = calculateHash(uniqueFieldsValue);
            final List<String> contentlets = Arrays.stream(((String[]) ((Array) uniqueFieldsValue.get("contentlet_identifier"))
                    .getArray())).collect(Collectors.toList());

            final boolean uniqueForSite = isUniqueForSite(uniqueFieldsValue.get("content_type_id").toString(),
                    uniqueFieldsValue.get("field_var_name").toString());

            final Map<String, Object> supportingValues = Map.of(
                    CONTENT_TYPE_ID_ATTR, uniqueFieldsValue.get("content_type_id"),
                    FIELD_VARIABLE_NAME_ATTR, uniqueFieldsValue.get("field_var_name"),
                    FIELD_VALUE_ATTR, uniqueFieldsValue.get("field_value"),
                    LANGUAGE_ID_ATTR, Long.parseLong(uniqueFieldsValue.get("language_id").toString()),
                    SITE_ID_ATTR, uniqueFieldsValue.get("host_id"),
                    UNIQUE_PER_SITE_ATTR, uniqueForSite,
                    CONTENTLET_IDS_ATTR, contentlets
            );

            Params notificationParams = new Params.Builder().add(hash, getJSONObject(supportingValues)).build();
            params.add(notificationParams);
        }

        try {
            insertUniqueFieldsRegister(params);
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    @NotNull
    private static PGobject getJSONObject(Map<String, Object> supportingValues) {
        final PGobject supportingValuesParam = new PGobject();
        supportingValuesParam.setType("json");
        Try.run(() -> supportingValuesParam.setValue(JsonUtil.getJsonAsString(supportingValues))).getOrElseThrow(
                () -> new IllegalArgumentException("Invalid JSON"));
        return supportingValuesParam;
    }

    /**
     * Inset a new register in the unique_field table.
     *
     * @param listOfParams
     * @throws DotDataException
     */
    private void insertUniqueFieldsRegister(final Collection<Params> listOfParams) throws DotDataException {

        new DotConnect().executeBatch(INSERT_UNIQUE_FIELDS_QUERY, listOfParams);
    }

    /**
     * Calculate hash use as value for the 'unique_key_val' unique_fields table field.
     * @param uniqueFieldsValue
     * @return
     * @throws DotDataException
     */
    private static String calculateHash(final Map<String, Object> uniqueFieldsValue) throws DotDataException {
        final String contentTypeId = uniqueFieldsValue.get("content_type_id").toString();
        final String fieldVariableName = uniqueFieldsValue.get("field_var_name").toString();

        final boolean uniqueForSite = isUniqueForSite(contentTypeId, fieldVariableName);

        final String valueToHash_1 = contentTypeId + fieldVariableName +
                uniqueFieldsValue.get("language_id").toString() +
                uniqueFieldsValue.get("field_value").toString() +
                (uniqueForSite ? uniqueFieldsValue.get("host_id").toString() : StringPool.BLANK);

        return StringUtils.hashText(valueToHash_1);
    }

    private static boolean isUniqueForSite(String contentTypeId, String fieldVariableName) throws DotDataException {
        final Field uniqueField = APILocator.getContentTypeFieldAPI().byContentTypeIdAndVar(contentTypeId, fieldVariableName);
        return uniqueField.fieldVariableValue(ESContentletAPIImpl.UNIQUE_PER_SITE_FIELD_VARIABLE_NAME)
                .map(Boolean::valueOf).orElse(false);
    }

    /**
     * Create the unique_fields table
     * @throws DotDataException
     */
    private static void createUniqueFieldTable() throws DotDataException {
        new DotConnect().setSQL(CREATE_TABLE_QUERY).loadObjectResults();
    }

    /**
     * retrive the Unique Field value this data is later used to populate the unique_fields table
     *
     * @return
     * @throws DotDataException
     */
    private static List<Map<String, Object>> retrieveUniqueFieldsValues() throws DotDataException {
        return new DotConnect().setSQL(RETRIEVE_UNIQUE_FIELD_VALUES_QUERY).loadObjectResults();
    }

}
