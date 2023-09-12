package com.dotcms.util.content.json;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.content.business.json.ContentletJsonAPI;
import com.dotcms.content.business.json.ContentletJsonHelper;
import com.dotcms.repackage.com.google.common.base.Strings;
import com.dotcms.util.LogTime;
import com.dotcms.util.transform.TransformerLocator;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotPGobject;
import com.dotmarketing.common.db.Params;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.primitives.Ints;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.commons.lang3.mutable.MutableInt;

import javax.annotation.Nullable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Utility class to populate the contentlet_as_json column in the contentlet table.
 */
public class PopulateContentletAsJSONUtil {

    private final ContentletJsonAPI contentletJsonAPI;

    // Query to find all the contentlets for a given asset_subtype and have a null contentlet_as_json
    private final String SUBTYPE_WITH_NO_JSON = "select c.* " +
            "from contentlet c" +
            "         JOIN identifier i ON i.id = c.identifier" +
            "         JOIN contentlet_version_info cv ON i.id = cv.identifier" +
            "    AND (c.inode = cv.working_inode OR c.inode = cv.live_inode)" +
            "   WHERE i.asset_subtype = '%s' AND c.contentlet_as_json IS NULL " +
            "   LIMIT %d;";

    // Query to find all the contentlets that have a null contentlet_as_json
    private final String CONTENTS_WITH_NO_JSON = "select c.* " +
            "from contentlet c" +
            "         JOIN identifier i ON i.id = c.identifier" +
            "         JOIN contentlet_version_info cv ON i.id = cv.identifier" +
            "    AND (c.inode = cv.working_inode OR c.inode = cv.live_inode)" +
            "   WHERE i.asset_type = 'contentlet' AND c.contentlet_as_json IS NULL " +
            "   LIMIT %d;";

    // Query to find all the contentlets that have a null contentlet_as_json for all the versions
    private final String CONTENTS_WITH_NO_JSON_ALL_VERSIONS = "SELECT c.* " +
            "FROM contentlet c " +
            "WHERE c.contentlet_as_json IS NULL " +
            "LIMIT %d;";

    // Query to find all the contentlets that are NOT of a given asset_subtype and have a null contentlet_as_json
    private final String CONTENTS_WITH_NO_JSON_AND_EXCLUDE = "select c.* " +
            "from contentlet c" +
            "         JOIN identifier i ON i.id = c.identifier" +
            "         JOIN contentlet_version_info cv ON i.id = cv.identifier" +
            "    AND (c.inode = cv.working_inode OR c.inode = cv.live_inode)" +
            "   WHERE i.asset_subtype <> '%s' AND i.asset_type = 'contentlet' AND c.contentlet_as_json IS NULL " +
            "   LIMIT %d;";

    // Query to update the contentlet_as_json column of the contentlet table
    private final String UPDATE_CONTENTLET_AS_JSON =
            "UPDATE contentlet SET contentlet_as_json = ? " +
                    "WHERE inode = ? AND contentlet_as_json IS NULL";

    // Temporal table related queries
    private final String CREATE_TEMP_TABLE = "CREATE TEMP TABLE tmp_contentlet_json ("
            + "     inode varchar(36) not null,"
            + "     json text not null"
            + ");";

    private final String DROP_TEMP_TABLE = "DROP TABLE IF EXISTS tmp_contentlet_json;";

    private final String INSERT_INTO_TEMP_TABLE = "INSERT INTO tmp_contentlet_json (inode, json) "
            + "VALUES (?,?)";

    // Cursor related queries
    private final String DECLARE_CURSOR = "DECLARE missingContentletAsJSONCursor CURSOR FOR %s";
    private final String DECLARE_CURSOR_FOR_TEMPORAL_TABLE =
            "DECLARE tmpContentletJSONCursor CURSOR "
                    + "FOR SELECT inode, json FROM tmp_contentlet_json;";
    private final String FETCH_CURSOR = "FETCH FORWARD %s FROM missingContentletAsJSONCursor";
    private final String FETCH_CURSOR_FOR_TEMPORAL_TABLE = "FETCH FORWARD %s FROM tmpContentletJSONCursor";
    private final String CLOSE_CURSOR = "CLOSE missingContentletAsJSONCursor";
    private final String CLOSE_CURSOR_FOR_TEMPORAL_TABLE = "CLOSE tmpContentletJSONCursor";

    private static final int MAX_BATCH_SIZE = Config.getIntProperty(
            "task.populateContentletAsJSON.maxbatchsize", 200);
    private static final int MAX_CURSOR_FETCH_SIZE = Config.getIntProperty(
            "task.populateContentletAsJSON.maxcursorfetchsize", 200);
    private static final int LIMIT_SIZE_FOR_SELECTS = Config.getIntProperty(
            "task.populateContentletAsJSON.selectslimitsize", 5000);

    public PopulateContentletAsJSONUtil() {
        this.contentletJsonAPI = APILocator.getContentletJsonAPI();
    }

    /**
     * Finds all the contentlets that need to be updated with the contentlet_as_json column.
     * <p>
     * <strong>All versions will be processed.</strong>
     */
    public void populateEverything() {
        Logger.info(this, "Populate Contentlet as JSON task started for all versions");
        populate(null, null, true);
    }

    /**
     * Finds all the contentlets that need to be updated with the contentlet_as_json column for a
     * given assetSubtype (Content Type).
     * <p>
     * <strong>Only working and live versions of the contentlets will be processed.</strong>
     *
     * @param assetSubtype Asset subtype (Content Type) to filter the contentlets to process, if
     *                     null then all the contentlets will be processed.
     * @throws SQLException
     * @throws IOException
     */
    public void populateForAssetSubType(final String assetSubtype) {
        Logger.info(this,
                String.format("Populate Contentlet as JSON task started for asset subtype [%s]",
                        assetSubtype));
        populate(assetSubtype, null, false);
    }

    /**
     * Finds all the contentlets that need to be updated with the contentlet_as_json column excluding the contentles
     * of a given assetSubtype (Content Type).
     * <p>
     * <strong>Only working and live versions of the contentlets will be processed.</strong>
     *
     * @param assetSubtype Asset subtype (Content Type) use to exclude contentlets of that given type from the query.
     * @throws SQLException
     * @throws DotDataException
     * @throws IOException
     */
    public void populateExcludingAssetSubType(final String assetSubtype) {
        Logger.info(this, String.format("Populate Contentlet as JSON task started excluding asset subtype [%s]", assetSubtype));
        populate(null, assetSubtype, false);
    }

    /**
     * Finds all the contentlets that need to be updated with the contentlet_as_json for the
     * given assetSubtype and excludingAssetSubtype.
     *
     * @param assetSubtype          Optional asset subtype (Content Type) to filter the contentlets
     *                              to process. If null, all the contentlets will be processed
     *                              unless the excludingAssetSubtype is provided.
     *                              Applies only for working and live versions.
     * @param excludingAssetSubtype Optional asset subtype (Content Type) used to exclude contentlets
     *                              from the query.
     * @param allVersions           Boolean indicating whether to process all versions of contentlets.
     */
    @LogTime(loggingLevel = "INFO")
    private void populate(@Nullable String assetSubtype,
                          @Nullable String excludingAssetSubtype,
                          final Boolean allVersions) {

        final MutableInt totalRecordsAffected = new MutableInt(0);

        while (true) {

            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() ->
                    populateWrapper(assetSubtype, excludingAssetSubtype, allVersions, totalRecordsAffected));

            try {
                Boolean foundRecords = future.get();
                if (!foundRecords) {
                    break; // We don't need to continue processing
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new DotRuntimeException("Error populating contentlets with missing contentlet as JSON", e);
            }
        }

        // Log task completion status
        Logger.info(this, "---- Records processed: " + totalRecordsAffected.intValue());
        if (allVersions) {

            Logger.info(this, "Contentlet as JSON migration task DONE for all versions");
        } else if (!Strings.isNullOrEmpty(assetSubtype)) {

            Logger.info(this, String.format("Contentlet as JSON migration task " +
                    "DONE for assetSubtype: [%s].", assetSubtype));
        } else if (!Strings.isNullOrEmpty(excludingAssetSubtype)) {

            Logger.info(this, String.format("Contentlet as JSON migration task " +
                    "DONE for excludingAssetSubtype [%s].", excludingAssetSubtype));
        } else {

            Logger.info(this, "Contentlet as JSON migration task DONE");
        }
    }

    /**
     * Internal method for populating the contentlet_as_json in contentlets.
     * Executes the population process for the given assetSubtype and excludingAssetSubtype.
     *
     * @param assetSubtype          Optional asset subtype (Content Type) to filter the contentlets
     *                              to process. If null, all the contentlets will be processed
     *                              unless the excludingAssetSubtype is provided.
     *                              Applies only for working and live versions.
     * @param excludingAssetSubtype Optional asset subtype (Content Type) used to exclude contentlets
     *                              from the query.
     * @param allVersions           Boolean indicating whether to process all versions of contentlets.
     * @param totalRecords          A MutableInt object to keep track of the total number of affected records.
     * @return True if contentlets were found and processed, false otherwise.
     */
    @WrapInTransaction
    private boolean populateWrapper(@Nullable String assetSubtype,
                                    @Nullable String excludingAssetSubtype,
                                    final Boolean allVersions,
                                    final MutableInt totalRecords) {

        var foundRecords = false;

        try {
            // First we need to find all the contentlets to process and write them into a file
            foundRecords = findAndStore(assetSubtype, excludingAssetSubtype, allVersions, totalRecords);
        } catch (SQLException | DotDataException e) {
            throw new DotRuntimeException("Error finding, generating JSON representation of "
                    + "Contentlets and storing them into a temporal table.", e);
        }

        if (foundRecords) {

            try {
                // Now we need to process the file and each record on it
                processRecords();
            } catch (SQLException | DotDataException e) {
                throw new DotRuntimeException(
                        "Error processing records with the JSON representation "
                                + "of Contentlets to update.", e);
            }
        }

        return foundRecords;
    }

    /**
     * Searches for all the contentlets of a given asset subtype (Content Type) that have a null
     * contentlet_as_json. This method uses a cursor to avoid loading all the contentlets into
     * memory. Each found contentlet is written into a temporal table for a later processing.
     *
     * @param assetSubtype          The asset subtype (Content Type) to search for, if null all the
     *                              contentlets will be searched.
     * @param excludingAssetSubtype The asset subtype (Content Type) to exclude in the search, if
     *                              null no Content Type will be excluded.
     * @param allVersions           Boolean indicating whether to process all versions of contentlets.
     * @param totalRecords          A MutableInt object to keep track of the total number of affected records.
     */
    @WrapInTransaction
    private boolean findAndStore(@Nullable final String assetSubtype,
                                 @Nullable final String excludingAssetSubtype,
                                 final Boolean allVersions,
                                 final MutableInt totalRecords
    ) throws SQLException, DotDataException {

        final Collection<Params> paramsInsert = new ArrayList<>();
        final MutableInt totalInsertAffected = new MutableInt(0);

        var foundData = false;
        final var type = UtilMethods.isSet(assetSubtype) ? assetSubtype : "[ any ]";
        Logger.info(this, "Finding records with missing Contentlet as JSON of sub-type '" + type + "'");

        final Connection conn = DbConnectionFactory.getConnection();

        // Creating the temporal table to hold the contentlets and json to process
        createTempTable(conn);

        try (var stmt = conn.createStatement()) {

            // Declaring the cursor
            declareCursor(stmt, assetSubtype, excludingAssetSubtype, allVersions);

            boolean hasRows;

            do {

                // Fetching batches of 100 records
                stmt.execute(String.format(FETCH_CURSOR, MAX_CURSOR_FETCH_SIZE));

                try (ResultSet rs = stmt.getResultSet()) {

                    // Now we want to write the found Contentlets into a file for a later processing
                    var dotConnect = new DotConnect();
                    dotConnect.fromResultSet(rs);

                    var loadedResults = dotConnect.loadObjectResults();

                    if (!loadedResults.isEmpty()) {

                        hasRows = true;
                        foundData = true;

                        var jsonDataArray = Optional.ofNullable(loadedResults)
                                .map(results ->
                                        TransformerLocator.createContentletTransformer(results).asList()
                                )// Transform the results into a list of contentlets
                                .map(contentletList ->
                                        contentletList.stream()
                                                .map(this::toJSON)
                                                .collect(Collectors.toList())
                                )// Transform the contentlets into a list of json strings
                                .orElse(Collections.emptyList());

                        for (var jsonData : jsonDataArray) {
                            // Insert the json representation of the contentlet into the temp table
                            this.processInsertRecord(jsonData._1(), jsonData._2(), paramsInsert, totalInsertAffected);
                        }

                        if (!paramsInsert.isEmpty()) {
                            this.doInsertBatch(paramsInsert, totalInsertAffected);
                        }

                    } else {
                        hasRows = false;
                    }
                }

            } while (hasRows);
            Logger.info(this, "-- Batch rows to populate temporary table. Inserted: " + totalInsertAffected.intValue() + " rows");
            // Close the cursor
            stmt.execute(CLOSE_CURSOR);

            if (foundData) {
                totalRecords.add(totalInsertAffected);
            }
        }

        return foundData;
    }

    /**
     * This method processes a temporal table that contains all the contentlets that need to be
     * updated with the contentlet_as_json
     *
     * @throws SQLException     If there is an error in the SQL execution.
     * @throws DotDataException If there is an error related to data handling.
     */
    @WrapInTransaction
    private void processRecords() throws SQLException, DotDataException {

        final Collection<Params> paramsUpdate = new ArrayList<>();
        final MutableInt totalUpdateAffected = new MutableInt(0);

        Logger.info(this, "Updating records with missing Contentlet as JSON of any sub-type");

        final Connection conn = DbConnectionFactory.getConnection();

        try (var stmt = conn.createStatement()) {

            // Declaring the cursor
            stmt.execute(DECLARE_CURSOR_FOR_TEMPORAL_TABLE);

            boolean hasRows;

            do {

                // Fetching batches of 100 records
                stmt.execute(String.format(FETCH_CURSOR_FOR_TEMPORAL_TABLE, MAX_CURSOR_FETCH_SIZE));

                try (ResultSet rs = stmt.getResultSet()) {

                    // Now we want to write the found Contentlets into a file for a later processing
                    var dotConnect = new DotConnect();
                    dotConnect.fromResultSet(rs);

                    var loadedResults = dotConnect.loadObjectResults();

                    if (!loadedResults.isEmpty()) {

                        hasRows = true;

                        loadedResults.forEach(
                                record -> this.processUpdateRecord(
                                        (String) record.get("inode"),
                                        (String) record.get("json"),
                                        paramsUpdate,
                                        totalUpdateAffected)
                        );

                        if (!paramsUpdate.isEmpty()) {
                            this.doUpdateBatch(paramsUpdate, totalUpdateAffected);
                        }

                    } else {
                        hasRows = false;
                    }
                }

            } while (hasRows);
            Logger.info(this, "-- Batch rows to populate temporary table. Updated: " + totalUpdateAffected.intValue() + " rows");
            // Close the cursor
            stmt.execute(CLOSE_CURSOR_FOR_TEMPORAL_TABLE);
        }
    }

    /**
     * Declares the cursor to be used to find the contentlets that need to be updated with the contentlet_as_json
     *
     * @param stmt
     * @param assetSubtype          The asset subtype (Content Type) to search for, if null all the contentlets will be searched.
     * @param excludingAssetSubtype The asset subtype (Content Type) to exclude in the search, if null no Content Type will be excluded.
     * @throws SQLException
     */
    private void declareCursor(final Statement stmt,
                               @Nullable final String assetSubtype,
                               @Nullable final String excludingAssetSubtype,
                               final Boolean allVersions
    ) throws SQLException {

        // Declaring the cursor
        if (allVersions) {
            var selectQuery = String.format(CONTENTS_WITH_NO_JSON_ALL_VERSIONS, LIMIT_SIZE_FOR_SELECTS);
            stmt.execute(String.format(DECLARE_CURSOR, selectQuery));
        } else if (!Strings.isNullOrEmpty(assetSubtype)) {
            var selectQuery = String.format(SUBTYPE_WITH_NO_JSON, assetSubtype, LIMIT_SIZE_FOR_SELECTS);
            stmt.execute(String.format(DECLARE_CURSOR, selectQuery));
        } else if (!Strings.isNullOrEmpty(excludingAssetSubtype)) {
            var selectQuery = String.format(CONTENTS_WITH_NO_JSON_AND_EXCLUDE, excludingAssetSubtype, LIMIT_SIZE_FOR_SELECTS);
            stmt.execute(String.format(DECLARE_CURSOR, selectQuery));
        } else {
            var selectQuery = String.format(CONTENTS_WITH_NO_JSON, LIMIT_SIZE_FOR_SELECTS);
            stmt.execute(String.format(DECLARE_CURSOR, selectQuery));
        }
    }

    /**
     * Processes a record by preparing the parameters for a batch insert into the temporal tables.
     *
     * @param inode               The inode of the contentlet.
     * @param json                The JSON representation of the contentlet.
     * @param paramsInsert        A collection of Params objects used for batch inserts. The Params
     *                            object contains the contentlet inode and JSON.
     * @param totalInsertAffected A MutableInt object to keep track of the total number of affected
     *                            rows in batch inserts.
     */
    private void processInsertRecord(
            final String inode,
            final String json,
            final Collection<Params> paramsInsert,
            final MutableInt totalInsertAffected
    ) {

        paramsInsert.add(new Params(inode, json));

        // Execute the batch for the inserts if we have reached the max batch size
        if (paramsInsert.size() >= MAX_BATCH_SIZE) {
            this.doInsertBatch(paramsInsert, totalInsertAffected);
        }
    }

    /**
     * Processes a record by preparing the parameters for a batch update.
     *
     * @param inode               The inode of the contentlet.
     * @param json                The JSON representation of the contentlet.
     * @param paramsUpdate        A collection of Params objects used for batch updates. The Params
     *                            object contains the contentlet JSON and inode.
     * @param totalUpdateAffected A MutableInt object to keep track of the total number of affected
     *                            rows in batch updates.
     * @throws JsonProcessingException If there is an error while processing the JSON.
     */
    private void processUpdateRecord(
            final String inode,
            final String json,
            final Collection<Params> paramsUpdate,
            final MutableInt totalUpdateAffected
    ) {

        final Object contentletAsJSON;
        if (DbConnectionFactory.isPostgres()) {
            contentletAsJSON = new DotPGobject.Builder()
                    .jsonValue(json)
                    .build();
        } else {
            contentletAsJSON = json;
        }

        paramsUpdate.add(new Params(contentletAsJSON, inode));

        // Execute the batch for the updates if we have reached the max batch size
        if (paramsUpdate.size() >= MAX_BATCH_SIZE) {
            this.doUpdateBatch(paramsUpdate, totalUpdateAffected);
        }
    }

    /**
     * Creates a temporary table in the database.
     *
     * @throws DotDataException If there is an error related to data handling.
     * @throws SQLException     If there is an error in the SQL execution.
     */
    private void createTempTable(final Connection conn) throws DotDataException, SQLException {

        new DotConnect().setSQL(DROP_TEMP_TABLE).loadResult(conn);
        new DotConnect().setSQL(CREATE_TEMP_TABLE).loadResult(conn);
    }

    /**
     * Converts the given {@link Contentlet} to an immutable contentlet and then builds a json
     * representation of it.
     *
     * @param contentlet The contentlet to convert.
     * @return A tuple containing the inode of the contentlet and its JSON representation.
     */
    private Tuple2<String, String> toJSON(Contentlet contentlet) {

        try {
            // Converts the given contentlet to an immutable contentlet and then builds a json representation of it.
            var contentletAsJSON = ContentletJsonHelper.INSTANCE.get()
                    .writeAsString(this.contentletJsonAPI.toImmutable(contentlet));

            return Tuple.of(contentlet.getInode(), contentletAsJSON);
        } catch (JsonProcessingException e) {
            throw new DotRuntimeException(
                    String.format("Error creating the JSON representation of Contentlet - " +
                            "inode [%s]", contentlet.getInode()), e);
        }
    }

    /**
     * Executes the batch of updates to fill the contentlet_as_json column.
     */
    private void doUpdateBatch(final Collection<Params> paramsUpdate, final MutableInt totalUpdateAffected) {

        try {
            final List<Integer> batchResult =
                    Ints.asList(new DotConnect().executeBatch(
                            UPDATE_CONTENTLET_AS_JSON,
                            paramsUpdate));

            final int rowsAffected = batchResult.stream().reduce(0, Integer::sum);
            totalUpdateAffected.add(rowsAffected);
        } catch (DotDataException e) {
            Logger.error(this, "Couldn't update these rows: " + paramsUpdate);
            Logger.error(this, e.getMessage(), e);
        } finally {
            paramsUpdate.clear();
        }
    }

    /**
     * Executes the batch of inserts to populate the temporal tmp_contentlet_json table.
     */
    private void doInsertBatch(final Collection<Params> paramsInsert, final MutableInt totalInsertAffected) {

        try {
            final List<Integer> batchResult =
                    Ints.asList(new DotConnect().executeBatch(
                            INSERT_INTO_TEMP_TABLE,
                            paramsInsert));

            final int rowsAffected = batchResult.stream().reduce(0, Integer::sum);
            totalInsertAffected.add(rowsAffected);
        } catch (DotDataException e) {
            Logger.error(this, "Couldn't insert these rows: " + paramsInsert);
            Logger.error(this, e.getMessage(), e);
        } finally {
            paramsInsert.clear();
        }
    }

    /**
     * This basically tells Weather or not we support saving content as json and if we have not turned it off.
     */
    public static boolean canPersistContentAsJson() {
        return APILocator.getContentletJsonAPI().isPersistContentAsJson();
    }

}
