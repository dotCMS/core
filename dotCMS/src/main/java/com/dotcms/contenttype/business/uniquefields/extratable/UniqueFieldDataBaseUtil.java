package com.dotcms.contenttype.business.uniquefields.extratable;

import com.dotcms.api.system.event.Visibility;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.ExternalTransaction;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.util.I18NMessage;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import io.vavr.Lazy;
import io.vavr.control.Try;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.dotcms.contenttype.business.uniquefields.extratable.SqlQueries.DELETE_UNIQUE_FIELDS;
import static com.dotcms.contenttype.business.uniquefields.extratable.SqlQueries.DELETE_UNIQUE_FIELDS_BY_CONTENTLET;
import static com.dotcms.contenttype.business.uniquefields.extratable.SqlQueries.DELETE_UNIQUE_FIELDS_BY_CONTENT_TYPE;
import static com.dotcms.contenttype.business.uniquefields.extratable.SqlQueries.DELETE_UNIQUE_FIELDS_BY_FIELD;
import static com.dotcms.contenttype.business.uniquefields.extratable.SqlQueries.FIX_DUPLICATE_ENTRY;
import static com.dotcms.contenttype.business.uniquefields.extratable.SqlQueries.GET_RECORDS_WITH_SAME_HASH;
import static com.dotcms.contenttype.business.uniquefields.extratable.SqlQueries.GET_UNIQUE_FIELDS_BY_CONTENTLET;
import static com.dotcms.contenttype.business.uniquefields.extratable.SqlQueries.GET_UNIQUE_FIELDS_BY_CONTENTLET_AND_LANGUAGE;
import static com.dotcms.contenttype.business.uniquefields.extratable.SqlQueries.GET_UNIQUE_FIELDS_BY_CONTENTLET_AND_VARIANT;
import static com.dotcms.contenttype.business.uniquefields.extratable.SqlQueries.GET_UNIQUE_FIELDS_BY_HASH;
import static com.dotcms.contenttype.business.uniquefields.extratable.SqlQueries.GET_UNIQUE_FIELDS_BY_UNIQUE_FIELD_CRITERIA;
import static com.dotcms.contenttype.business.uniquefields.extratable.SqlQueries.INSERT_SQL;
import static com.dotcms.contenttype.business.uniquefields.extratable.SqlQueries.POPULATE_UNIQUE_FIELDS_VALUES_QUERY;
import static com.dotcms.contenttype.business.uniquefields.extratable.SqlQueries.RECALCULATE_UNIQUE_KEY_VAL;
import static com.dotcms.contenttype.business.uniquefields.extratable.SqlQueries.SET_LIVE_BY_CONTENTLET;
import static com.dotcms.contenttype.business.uniquefields.extratable.SqlQueries.UPDATE_CONTENT_LIST;
import static com.dotcms.contenttype.business.uniquefields.extratable.SqlQueries.UPDATE_CONTENT_LIST_WITH_HASH;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.CONTENTLET_IDS_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.CONTENT_TYPE_ID_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.FIELD_VALUE_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.FIELD_VARIABLE_NAME_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.LANGUAGE_ID_ATTR;
import static com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldCriteria.LIVE_ATTR;
import static com.dotcms.util.CollectionsUtils.list;
import static com.liferay.util.StringPool.BLANK;

/**
 * This class allows developers to perform CRUD operations on the {@code unique_fields} table. It
 * also exposes methods that enforce uniqueness and data integrity for dotCMS to be able to validate
 * unique values as expected.
 *
 * @author Freddy Rodriguez
 * @since Oct 30th, 2024
 */
@ApplicationScoped
public class UniqueFieldDataBaseUtil {

    private static final String AUDIT_FILE_NAME = "unique_field_data_conflicts.log";

    private static final Lazy<String> AUDIT_FILE_PATH = Lazy.of(() -> Config.getStringProperty(
            "TAIL_LOG_LOG_FOLDER", "./dotsecure/logs") + File.separator + AUDIT_FILE_NAME);

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
     * Returns the unique field record that matches a specific Hash.
     *
     * @param uniqueFieldCriteria The {@link UniqueFieldCriteria} containing the criteria to
     *                            retrieve the record.
     *
     * @return An {@link Optional} containing the unique field record if it exists, or an empty
     * optional otherwise.
     *
     * @throws DotDataException An error occurred when interacting with the database.
     */
    @CloseDBIfOpened
    public Optional<UniqueFieldValue> get(final UniqueFieldCriteria uniqueFieldCriteria) throws DotDataException {
        return new DotConnect().setSQL(GET_UNIQUE_FIELDS_BY_UNIQUE_FIELD_CRITERIA)
                .addParam(uniqueFieldCriteria.criteria())
                .loadObjectResults()
                .stream().findFirst()
                .map(item ->
                        new UniqueFieldValue(item.get("unique_key_val").toString(), getSupportingValues(item) ));
    }

    /**
     * Returns the supporting_values for a specific unique value entry. Also, this method transforms
     * the {@code supporting_values} from a {@link org.postgresql.util.PGobject} into a Map.
     *
     * @param uniqueFieldValues The {@link Map} containing the unique field attributes.
     *
     * @return A {@link Map} containing the supporting values.
     */
    private static Map<String, Object> getSupportingValues(final Map<String, Object> uniqueFieldValues) {
        try {
            return JsonUtil.getJsonFromString(uniqueFieldValues.get("supporting_values").toString());
        } catch (final IOException e) {
            Logger.error(UniqueFieldDataBaseUtil.class, "Error getting supporting values", e);
            throw new DotRuntimeException(e);
        }
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
                : BLANK,
                uniquePerSite);
    }

    /**
     * Returns a list of unique values that match a specific Contentlet ID and Language ID.
     *
     * @param contentId  The Contentlet ID.
     * @param languageId The Language ID.
     *
     * @return A list of unique values that match the specified criteria.
     *
     * @throws DotDataException An error occurred when interacting with the database.
     */
    @CloseDBIfOpened
    public List<Map<String, Object>> get(final String contentId, final long languageId) throws DotDataException {
        return new DotConnect().setSQL(GET_UNIQUE_FIELDS_BY_CONTENTLET_AND_LANGUAGE)
                .addParam("\"" + contentId + "\"")
                .addParam(languageId)
                .loadObjectResults();
    }

    /**
     * Returns a list of unique values that match a specific Contentlet ID and Variant ID.
     *
     * @param contentId The Contentlet ID.
     * @param variantId The Variant ID.
     *
     * @return A list of unique values that match the specified criteria.
     *
     * @throws DotDataException An error occurred when interacting with the database.
     */
    @CloseDBIfOpened
    public List<Map<String, Object>> get(final String contentId, final String variantId) throws DotDataException {
        return new DotConnect().setSQL(GET_UNIQUE_FIELDS_BY_CONTENTLET_AND_VARIANT)
                .addParam("\"" + contentId + "\"")
                .addParam(variantId)
                .loadObjectResults();
    }

    /**
     * Deletes a Unique Field Value by its unique hash.
     *
     * @param hash The unique hash of the Unique Field Value to delete.
     *
     * @throws DotDataException An error occurred when interacting with the database.
     */
    @WrapInTransaction
    public void delete(final String hash) throws DotDataException {
        new DotConnect().setSQL(DELETE_UNIQUE_FIELDS)
                .addParam(hash)
                .loadObjectResults();
    }

    /**
     * Deletes all the unique values for a given Contentlet field.
     *
     * @param field The {@link Field} to delete the unique values from.
     *
     * @throws DotDataException An error occurred when interacting with the database.
     */
    @WrapInTransaction
    public void delete(final Field field) throws DotDataException {
        new DotConnect().setSQL(DELETE_UNIQUE_FIELDS_BY_FIELD)
                .addParam(field.variable())
                .loadObjectResults();
    }

    /**
     * Deletes all the unique values for a given Content Type.
     *
     * @param contentType The {@link ContentType} to delete the unique values from.
     *
     * @throws DotDataException An error occurred when interacting with the database.
     */
    @WrapInTransaction
    public void delete(final ContentType contentType) throws DotDataException {
        new DotConnect().setSQL(DELETE_UNIQUE_FIELDS_BY_CONTENT_TYPE)
                .addParam(contentType.id())
                .loadObjectResults();
    }

    /**
     * Sets the {@code supporting_value->live} attribute to {@code true} to any record with the same
     * Contentlet id, variant and language.
     *
     * @param contentlet The {@link Contentlet} whose attributes will be updated.
     * @param liveValue  The new value of the {@code live} attribute.
     *
     * @throws DotDataException An error occurred when interacting with the database.
     */
    @WrapInTransaction
    public void setLive(final Contentlet contentlet, final boolean liveValue) throws DotDataException {

         new DotConnect().setSQL(SET_LIVE_BY_CONTENTLET)
                 .addParam(String.valueOf(liveValue))
                .addParam("\"" + contentlet.getIdentifier() + "\"")
                .addParam(contentlet.getVariantId())
                .addParam(contentlet.getLanguageId())
                .loadObjectResults();

    }

    /**
     * Removes any record with {@code supporting_value->live} set to {@code true} and the same
     * Contentlet's id, variant and language.
     *
     * @param contentlet The {@link Contentlet} whose record will be removed.
     *
     * @throws DotDataException An error occurred when interacting with the database.
     */
    @WrapInTransaction
    public void removeLive(final Contentlet contentlet) throws DotDataException {

        new DotConnect().setSQL(DELETE_UNIQUE_FIELDS_BY_CONTENTLET)
                .addParam("\"" + contentlet.getIdentifier() + "\"")
                .addParam(contentlet.getVariantId())
                .addParam(contentlet.getLanguageId())
                .addParam(true)
                .loadObjectResults();
    }

    /**
     * Create the {@code unique_fields} table for the new Unique Field Database Validation
     * mechanism. This new table will be used to validate fields that must be unique, and will store
     * what parameters were used to defined such a uniqueness feature.
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
     * The {@code unique_key_val} column will store a hash created from a combination of the
     * following:
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
     *     "live": true|false
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
                    "unique_key_val VARCHAR(64)," +
                    "supporting_values JSONB)").loadObjectResults();
        new DotConnect().setSQL("CREATE INDEX IF NOT EXISTS idx_unique_key_val ON unique_fields(unique_key_val)")
                .loadObjectResults();
    }

    /**
     * Creates and populates the {@code unique_fields} table with unique field values extracted from
     * the {@code contentlet} table.
     *
     * @throws DotDataException An error occurred when interacting with the database.
     */
    public void createTableAndPopulate() throws DotDataException {
        Logger.info(this, "---> Creating the unique_fields table");
        createUniqueFieldsValidationTable();
        Logger.info(this, "---> Populating the unique_fields table");
        populateUniqueFieldsTable();
        Logger.info(this, "---> Fixing and reporting records with the same hash, if any");
        this.handleDuplicateRecords();
        Logger.info(this, "---> Bringing primary key constraints back");
        this.addPrimaryKeyConstraintsBack();
        Logger.info(this, "---> Create table indexes for performance improvement");
        this.addTableIndexes();
    }

    /**
     * Locates records that have the exact same hash, but referenced by different Contentlet IDs,
     * and updates their unique values so that they don't conflict anymore. This involves:
     * <ul>
     *     <li>Locating at least two records with the same hash value.</li>
     *     <li>Setting an updated unique value in the form of:
     *     {@code legacy*support*<RANDOM-NUMBER>{<ORIGINAL-UNIQUE-VALUE>}}.</li>
     *     <li>Re-generating its hash so that it won't match any other entry.</li>
     * </ul>
     * Additionally, a JSON file containing the affected records will be created under the
     * following path: {@link #AUDIT_FILE_PATH} . It will be available for download from the
     * back-end so that users can be notified of the changes and inspect what Contentlets must be
     * manually updated.
     *
     * @throws DotDataException An error occurred when interacting with the database.
     */
    @CloseDBIfOpened
    public void handleDuplicateRecords() throws DotDataException {
        final DotConnect dotConnect = new DotConnect().setSQL(GET_RECORDS_WITH_SAME_HASH);
        final List<Map<String, Object>> duplicateRecords = dotConnect.loadObjectResults();
        Logger.info(this, String.format("A total of %d records with the same hash value were found",
                duplicateRecords.size()));
        if (!duplicateRecords.isEmpty()) {
            final List<UniqueFieldConflict> duplicateEntriesReport = new ArrayList<>();
            duplicateRecords.forEach(record -> {
                final String uniqueKeyVal = Try.of(() -> record.get("unique_key_val").toString()).getOrNull();
                duplicateEntriesReport.add(this.updateDuplicates(uniqueKeyVal));
            });
            this.reportDuplicateRecords(duplicateEntriesReport);
        }
        // Dropping the previous temporary index, which will be replaced by the Unique Index created
        // by the Primary Key
        new DotConnect().setSQL("DROP INDEX IF EXISTS idx_unique_key_val").loadObjectResults();
    }

    /**
     * Takes the list of conflicting entries in the {@code unique_fields} table and writes them to
     * the following path: {@link #AUDIT_FILE_PATH}. Moreover, it notifies the user via the static
     * Notifications System in the back-end so that the audit file is available for download.
     *
     * @param duplicateEntriesReport The list of {@link UniqueFieldConflict} objects containing the
     *                               information of the Contentlets whose specific unique value must
     *                               be fixed.
     */
    private void reportDuplicateRecords(final List<UniqueFieldConflict> duplicateEntriesReport) {
        final String jsonString = JsonUtil.getPrettyJsonStringFromObject(Map.of("duplicates", duplicateEntriesReport));
        try {
            FileUtil.write(new File(AUDIT_FILE_PATH.get()), jsonString);
            final User systemUser = APILocator.getUserAPI().getSystemUser();
            final Role cmsAdminRole = APILocator.getRoleAPI().loadCMSAdminRole();
            APILocator.getNotificationAPI().generateNotification(
                    new I18NMessage("uniquefields.notification.duplicatevalues.title"),
                    new I18NMessage("uniquefields.notification.duplicatevalues.message"),
                    null,
                    NotificationLevel.WARNING, NotificationType.GENERIC, Visibility.ROLE, cmsAdminRole.getId(),
                    systemUser.getUserId(),
                    systemUser.getLocale());
        } catch (final IOException e) {
            Logger.error(this, String.format("An error occurred while trying to write the " +
                    "'%s' audit file: %s", AUDIT_FILE_PATH.get(), ExceptionUtil.getErrorMessage(e)), e);
        } catch (final DotDataException e) {
            Logger.error(this, String.format("An error occurred while trying to generate the notification: " +
                    "%s", ExceptionUtil.getErrorMessage(e)), e);
        }
    }

    /**
     * Once the {@code unique_fields} table has been cleared of any duplicate entries -- i.e.;
     * entries with the same hash -- this method will add the primary key constraint back to the
     * table.
     *
     * @throws DotDataException An error occurred when interacting with the database.
     */
    @WrapInTransaction
    public void addPrimaryKeyConstraintsBack() throws DotDataException {
        try {
            String sqlQuery = "ALTER TABLE unique_fields ALTER COLUMN unique_key_val SET NOT NULL";
            new DotConnect().setSQL(sqlQuery).loadObjectResults();
            sqlQuery = "ALTER TABLE unique_fields ADD PRIMARY KEY (unique_key_val)";
            new DotConnect().setSQL(sqlQuery).loadObjectResults();
        } catch (final DotDataException e) {
            Logger.error(this, "Failed to bring primary key constraints back. There may be an unhandled unique value scenario");
            throw e;
        }
    }

    /**
     * Adds the necessary Indexes to the {@code unique_fields} table in order to improve its
     * performance as much as possible.
     *
     * @throws DotDataException An error occurred when interacting with the database.
     */
    @ExternalTransaction
    public void addTableIndexes() throws DotDataException {
        boolean defaultAutoCommit = false;
        Connection connection = null;
        try {
            connection = DbConnectionFactory.getConnection();
            defaultAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(true);
            Logger.info(this, "(1/6) Adding GIN Index for the supporting_values->'contentletIds' JSONB attribute");
            new DotConnect()
                    .setSQL("CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_unique_fields_contentlet_ids_gin ON unique_fields USING GIN ((supporting_values->'contentletIds'))")
                    .loadResult(connection);

            Logger.info(this, "(2/6) Adding Functional Index for the supporting_values->'languageId' JSONB attribute");
            new DotConnect()
                    .setSQL("CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_unique_fields_language_id ON unique_fields (((supporting_values->>'languageId')::BIGINT))")
                    .loadResult(connection);

            Logger.info(this, "(3/6) Adding Functional Index for the supporting_values->'contentTypeId' JSONB attribute");
            new DotConnect()
                    .setSQL("CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_unique_fields_content_type_id ON unique_fields (((supporting_values->>'contentTypeId')))")
                    .loadResult(connection);

            Logger.info(this, "(4/6) Adding Functional Index for the supporting_values->'fieldVariableName' JSONB attribute");
            new DotConnect()
                    .setSQL("CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_unique_fields_field_variable_name ON unique_fields (((supporting_values->>'fieldVariableName')))")
                    .loadResult(connection);

            Logger.info(this, "(5/6) Adding Functional Index for the supporting_values->'variant' JSONB attribute");
            new DotConnect()
                    .setSQL("CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_unique_fields_variant ON unique_fields (((supporting_values->>'variant')))")
                    .loadResult(connection);

            Logger.info(this, "(6/6) Adding Functional Index for the supporting_values->'live' JSONB attribute");
            new DotConnect()
                    .setSQL("CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_unique_fields_live ON unique_fields (((supporting_values->>'live')::BOOLEAN))")
                    .loadResult(connection);
        } catch (final SQLException e) {
            throw new DotRuntimeException(String.format("An error occurred when creating indexes on the 'unique_fields' table: " +
                    "%s", ExceptionUtil.getErrorMessage(e)), e);
        } finally {
            if (null != connection) {
                try {
                    connection.setAutoCommit(defaultAutoCommit);
                } catch (final SQLException e) {
                    Logger.warn(this, String.format("Failed to set autocommit value back to its original value: " +
                            "%s", ExceptionUtil.getErrorMessage(e)), e);
                }
            }
        }
    }

    /**
     * Updates the unique values of the records that have the same hash, but are referenced by
     * different Contentlet IDs. During this process, the first record found in the list of
     * duplicates will be considered as the source of truth. The remaining ones will have their
     * unique values updated so that they don't conflict anymore.
     *
     * @param uniqueKeyVal The hash value that is present in the table more than once.
     */
    @CloseDBIfOpened
    private UniqueFieldConflict updateDuplicates(final String uniqueKeyVal) {
        final DotConnect dotConnect = new DotConnect().setSQL(GET_UNIQUE_FIELDS_BY_HASH).addParam(uniqueKeyVal);
        try {
            final List<Map<String, Object>> uniqueFieldsData = dotConnect.loadObjectResults();
            final List<String> supportingValuesToUpdate = new ArrayList<>();
            // Always skip the first record, as it will be the source of truth
            for (int i = 1; i < uniqueFieldsData.size(); i++) {
                final String supportingValues = uniqueFieldsData.get(i).get("supporting_values").toString();
                supportingValuesToUpdate.add(supportingValues);
            }
            this.updateUniqueValues(uniqueKeyVal, supportingValuesToUpdate);
            return this.buildConflict(uniqueFieldsData);
        } catch (final DotDataException e) {
            final String errorMsg = String.format("An error occurred while trying to update the unique values for hash " +
                    "'%s': %s'", uniqueKeyVal, ExceptionUtil.getErrorMessage(e));
            Logger.error(this, errorMsg, e);
            throw new DotRuntimeException(errorMsg, e);
        }
    }

    /**
     * Takes the list of records that have the same hash -- a.k.a. unique key value -- and builds
     * the {@link UniqueFieldConflict} object that will be used to keep track of the specific
     * conflicting Contentlets via the audit file.
     *
     * @param conflictingData The list of Maps containing the raw conflicting data from the
     *                        database.
     *
     * @return The {@link UniqueFieldConflict} tracking object.
     */
    private UniqueFieldConflict buildConflict(final List<Map<String, Object>> conflictingData) {
        try {
            UniqueFieldConflict.Builder conflictBuilder = new UniqueFieldConflict.Builder();
            for (final Map<String, Object> rawConflict : conflictingData) {
                if (null == rawConflict.get("supporting_values")) {
                    return null;
                }
                final Map<String, Object> supportingValuesAsMap = JsonUtil
                        .getJsonFromString(rawConflict.get("supporting_values").toString());
                // Set these initial values only once
                if (UtilMethods.isNotSet(conflictBuilder.fieldName())) {
                    conflictBuilder = new UniqueFieldConflict.Builder()
                            .fieldName(supportingValuesAsMap.get(FIELD_VARIABLE_NAME_ATTR).toString())
                            .contentTypeId(supportingValuesAsMap.get(CONTENT_TYPE_ID_ATTR).toString())
                            .originalValue(supportingValuesAsMap.get(FIELD_VALUE_ATTR).toString());
                }
                conflictBuilder = conflictBuilder.conflictingData(Map.of(
                        "contentletId", supportingValuesAsMap.get(CONTENTLET_IDS_ATTR).toString(),
                        "languageId", supportingValuesAsMap.get(LANGUAGE_ID_ATTR)));
            }
            return conflictBuilder.build();
        } catch (final IOException e) {
            final String errorMsg = String.format("Failed to parse supporting values as JSON into Java map: " +
                    "%s", ExceptionUtil.getErrorMessage(e));
            Logger.error(this, errorMsg, e);
            throw new DotRuntimeException(errorMsg, e);
        }
    }

    /**
     * For a given hash -- a.k.a. unique key value -- takes the list of unique field criteria of the
     * conflicting entries in the {@code unique_fields} table and updates the corresponding unique
     * value. This way, a new hash will be generated, and it won't collide with the original one.
     * <p>There are specific properties that are used to make up the unique field criteria. For
     * more details, you can refer to: {@link UniqueFieldCriteria#criteria()} .</p>
     *
     * @param uniqueKeyVal   The hash value that is present in the table more than once.
     * @param valuesToUpdate The list of unique field criteria of the conflicting entries.
     *
     * @throws DotDataException An error occurred when interacting with the database.
     */
    @WrapInTransaction
    private void updateUniqueValues(final String uniqueKeyVal, final List<String> valuesToUpdate) throws DotDataException {
        valuesToUpdate.forEach(supportingValues -> {

            try {
                final Map<String, Object> supportingValuesAsMap = JsonUtil.getJsonFromString(supportingValues);
                Logger.info(this, String.format("Fixing conflict for Content IDs '%s' with duplicate unique value '%s'",
                        supportingValuesAsMap.get(CONTENTLET_IDS_ATTR), supportingValuesAsMap.get(FIELD_VALUE_ATTR)));
                final String updatedFieldValue =
                        this.generateUniqueName(supportingValuesAsMap.getOrDefault(FIELD_VALUE_ATTR, BLANK).toString());
                final boolean isLive = Try.of(() -> Boolean.parseBoolean(supportingValuesAsMap.get(LIVE_ATTR).toString()))
                        .getOrNull();
                supportingValuesAsMap.put(FIELD_VALUE_ATTR, updatedFieldValue);
                final String updatedCriteria = UniqueFieldCriteria.criteria(supportingValuesAsMap);
                final DotConnect dotConnect = new DotConnect().setSQL(FIX_DUPLICATE_ENTRY)
                        .addParam(updatedCriteria)
                        .addJSONParam(supportingValuesAsMap)
                        .addParam(uniqueKeyVal)
                        .addJSONParam(supportingValuesAsMap.get(CONTENTLET_IDS_ATTR))
                        .addParam(isLive);
                dotConnect.loadObjectResults();
            } catch (final IOException e) {
                final String errorMsg = String.format("Failed to transform support values [ %s ] into JSON Map for key " +
                        "'%s': %s", supportingValues, uniqueKeyVal, ExceptionUtil.getErrorMessage(e));
                throw new DotRuntimeException(errorMsg, e);
            } catch (final DotDataException e) {
                final String errorMsg = String.format("Failed to update Unique Field entry with key " +
                        "'%s' and support values [ %s ]: %s", uniqueKeyVal, supportingValues,
                        ExceptionUtil.getErrorMessage(e));
                throw new DotRuntimeException(errorMsg, e);
            }

        });
    }

    /**
     * Generates a new unique value based on the original one, using a specific format that can also
     * be used to look for conflicting entries that were fixed.
     *
     * @param originalUniqueValue The original unique value.
     *
     * @return The new unique value.
     */
    private String generateUniqueName(final String originalUniqueValue) {
        return "legacy*support*{" + originalUniqueValue + "}";
    }

    /**
     * Drops the {@code unique_fields} table.
     *
     * @throws DotDataException An error occurred when interacting with the database.
     * @see UniqueFieldDataBaseUtil#createUniqueFieldsValidationTable()
     */
    @WrapInTransaction
    public void dropUniqueFieldsValidationTable() throws DotDataException {
        new DotConnect().setSQL("DROP TABLE IF EXISTS unique_fields").loadObjectResults();
    }

    /**
     * Populates the {@code unique_fields} table with the unique values extracted from the
     * {@code contentlet} table. This process involves:
     * <ul>
     *     <li>Identifying all {@link com.dotcms.contenttype.model.type.ContentType} objects with
     *     unique fields.</li>
     *     <li>Retrieving all {@link Contentlet} entries and their corresponding values for both
     *     LIVE and Working versions.</li>
     *     <li>Storing these unique field values into the {@code unique_fields} table with all
     *     this data.</li>
     * </ul>
     *
     * @throws DotDataException An error occurred when interacting with the database.
     */
    @WrapInTransaction
    public void populateUniqueFieldsTable() throws DotDataException {
        new DotConnect().setSQL(POPULATE_UNIQUE_FIELDS_VALUES_QUERY).loadObjectResults();
    }

    /**
     * Represents a record in the {@code unique_fields} table. It consists of:
     * <ul>
     *     <li>A unique hash acting as the entry identifier. This is an encoded SHA-256 of the
     *     unique field criteria</li>
     *     <li>The supporting values, a.k.a. the unique criteria, which provides information on the
     *     Contentlet or Contentlets that are using the same unique value. For more details, please
     *     refer to: {@link UniqueFieldCriteria} .</li>
     * </ul>
     */
    public static class UniqueFieldValue {

        private final String uniqueKeyVal;
        private final Map<String, Object> supportingValues;

        public UniqueFieldValue(final String uniqueKeyVal, final Map<String, Object> supportingValues) {
            this.uniqueKeyVal = uniqueKeyVal;
            this.supportingValues = supportingValues;
        }

        public String getUniqueKeyVal() {
            return uniqueKeyVal;
        }

        public Map<String, Object> getSupportingValues() {
            return supportingValues;
        }

    }

}
