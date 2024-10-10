package com.dotmarketing.startup.runonce;

import com.dotcms.content.elasticsearch.business.ESContentletAPIImpl;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.notifications.bean.NotificationType;
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
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import org.jetbrains.annotations.NotNull;
import org.postgresql.util.PGobject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Array;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static com.dotcms.util.CollectionsUtils.list;

/**
 * Task Upgrade to Create/populate the unique_fields Table for Unique Field Validation
 *
 * This introduces a new table, unique_fields, which will be used to validate fields that must be unique.
 *
 * <b>Table Structure</b>
 *
 * <code>
 * CREATE TABLE unique_fields (
 *     unique_key_val VARCHAR(64) PRIMARY KEY,
 *     supporting_values JSONB
 * );
 * </code>
 *
 * - unique_key_val: This field will store a hash created from a combination of the following:
 *
 * Content type ID
 * Field variable name
 * Field value
 * Language
 * Host ID (if the uniquePerSite option is enabled)
 *
 * - supporting_values: This field contains a JSON object with the following format:
 *
 * <code>
 * {
 *     "contentTypeID": "",
 *     "fieldVariableName": "",
 *     "fieldValue": "",
 *     "languageId": "",
 *     "hostId": "",
 *     "uniquePerSite": true|false,
 *     "contentletsId": [...]
 * }
 * </code>
 *
 * The contentletsId array holds the IDs of contentlets with the same field value that existed before the database was upgraded.
 * After the upgrade, no more contentlets with duplicate values will be allowed.
 *
 * <b>Additional Details</b>
 *
 *-  The Host ID is included in the hash calculation only if the uniquePerSite field variable is enabled.
 *- The unique_key_val field ensures that only truly unique values can be inserted moving forward.
 *- This upgrade task also populates the unique_fields table with the existing unique field values from the current database.
 */
public class Task241007CreateUniqueFieldsTable implements StartupTask {

    private final static String CREATE_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS unique_fields (" +
            "unique_key_val VARCHAR(64) PRIMARY KEY," +
            "supporting_values JSONB" +
            " )";

    private static final String RETRIVE_UNIQUE_FIELD_VALUES_QUERY = "SELECT structure.inode AS content_type_id," +
                " field.velocity_var_name AS field_var_name," +
                " contentlet.language_id AS language_id," +
                " identifier.host_inode AS host_id," +
                " jsonb_extract_path_text(contentlet_as_json -> 'fields', field.velocity_var_name)::jsonb ->> 'value' AS field_value," +
                " ARRAY_AGG(contentlet.identifier) AS contentlet_identifier" +
            " FROM contentlet" +
                " INNER JOIN structure ON structure.inode = contentlet.structure_inode" +
                " INNER JOIN field ON structure.inode = field.structure_inode" +
                " INNER JOIN identifier ON contentlet.identifier = identifier.id" +
            " WHERE jsonb_extract_path_text(contentlet_as_json->'fields', field.velocity_var_name) IS NOT NULL" +
            " GROUP BY structure.inode," +
                    " field.velocity_var_name ," +
                    " contentlet.language_id," +
                    " identifier.host_inode," +
                    " jsonb_extract_path_text(contentlet_as_json -> 'fields', field.velocity_var_name)::jsonb ->> 'value'";

    private static final String TESTING_QUERY = "SELECT structure.inode AS content_type_id," +
            " field.velocity_var_name AS field_var_name," +
            " contentlet.language_id AS language_id," +
            " identifier.host_inode AS host_id," +
            " jsonb_extract_path_text(contentlet_as_json -> 'fields', field.velocity_var_name)::jsonb ->> 'value' AS field_value," +
            " ARRAY_AGG(contentlet.identifier) AS contentlet_identifier" +
            " FROM contentlet" +
            " INNER JOIN structure ON structure.inode = contentlet.structure_inode" +
            " INNER JOIN field ON structure.inode = field.structure_inode" +
            " INNER JOIN identifier ON contentlet.identifier = identifier.id" +
            " WHERE jsonb_extract_path_text(contentlet_as_json->'fields', field.velocity_var_name) IS NOT NULL AND" +
            " jsonb_extract_path_text(contentlet_as_json -> 'fields', field.velocity_var_name)::jsonb ->> 'value' = 'test.jpg'" +
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

            final Map<String, Object> supportingValues = Map.of(
                 "contentTypeID", uniqueFieldsValue.get("content_type_id"),
                "fieldVariableName", uniqueFieldsValue.get("field_var_name"),
                "fieldValue", uniqueFieldsValue.get("field_value"),
                "languageId", Long.parseLong(uniqueFieldsValue.get("language_id").toString()),
                "hostId", uniqueFieldsValue.get("host_id"),
                "uniquePerSite", false,
                "contentletsId", contentlets
            );

            Params notificationParams = new Params.Builder().add(hash, getJSONObject(supportingValues)).build();
            params.add(notificationParams);
        }

        try {

            insertUniqueFieldsRegister(params);
        } catch (DotDataException e) {
            throw new DotRuntimeException(new DotConnect().setSQL(TESTING_QUERY).loadObjectResults().toString());
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

        final Field uniqueField = APILocator.getContentTypeFieldAPI().byContentTypeIdAndVar(contentTypeId, fieldVariableName);

        final boolean uniqueForSite = uniqueField.fieldVariableValue(ESContentletAPIImpl.UNIQUE_PER_SITE_FIELD_VARIABLE_NAME)
                .map(Boolean::valueOf).orElse(false);

        final String valueToHash_1 = contentTypeId + fieldVariableName +
                uniqueFieldsValue.get("language_id").toString() +
                uniqueFieldsValue.get("field_value").toString() +
                (uniqueForSite ? uniqueFieldsValue.get("host_id").toString() : StringPool.BLANK);

        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hashBytes = digest.digest(valueToHash_1.getBytes());

            final StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
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
        return new DotConnect().setSQL(RETRIVE_UNIQUE_FIELD_VALUES_QUERY).loadObjectResults();
    }
}
