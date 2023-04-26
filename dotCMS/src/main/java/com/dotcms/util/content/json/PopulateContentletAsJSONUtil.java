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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.primitives.Ints;
import org.apache.commons.lang3.mutable.MutableInt;

import javax.annotation.Nullable;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
            "   WHERE i.asset_subtype = '%s' AND c.contentlet_as_json IS NULL;";

    // Query to find all the contentlets that have a null contentlet_as_json
    private final String CONTENTS_WITH_NO_JSON = "select c.* " +
            "from contentlet c" +
            "         JOIN identifier i ON i.id = c.identifier" +
            "         JOIN contentlet_version_info cv ON i.id = cv.identifier" +
            "    AND (c.inode = cv.working_inode OR c.inode = cv.live_inode)" +
            "   WHERE i.asset_type = 'contentlet' AND c.contentlet_as_json IS NULL;";

    // Query to find all the contentlets that are NOT of a given asset_subtype and have a null contentlet_as_json
    private final String CONTENTS_WITH_NO_JSON_AND_EXCLUDE = "select c.* " +
            "from contentlet c" +
            "         JOIN identifier i ON i.id = c.identifier" +
            "         JOIN contentlet_version_info cv ON i.id = cv.identifier" +
            "    AND (c.inode = cv.working_inode OR c.inode = cv.live_inode)" +
            "   WHERE i.asset_subtype <> '%s' AND i.asset_type = 'contentlet' AND c.contentlet_as_json IS NULL;";

    // Query to update the contentlet_as_json column of the contentlet table
    private final String UPDATE_CONTENTLET_AS_JSON = "UPDATE contentlet SET contentlet_as_json = ? " +
            "WHERE inode = ? AND contentlet_as_json IS NULL";

    // Cursor related queries
    private final String DECLARE_CURSOR = "DECLARE missingContentletAsJSONCursor CURSOR FOR %s";
    private final String FETCH_CURSOR_POSTGRES = "FETCH FORWARD %s FROM missingContentletAsJSONCursor";
    private final String FETCH_CURSOR_MSSQL = "FETCH NEXT FROM missingContentletAsJSONCursor";
    private final String OPEN_CURSOR_MSSQL = "OPEN missingContentletAsJSONCursor";
    private final String CLOSE_CURSOR = "CLOSE missingContentletAsJSONCursor";
    private final String DEALLOCATE_CURSOR_MSSQL = "DEALLOCATE missingContentletAsJSONCursor";

    private static final int MAX_UPDATE_BATCH_SIZE = Config.getIntProperty("task.230320.maxupdatebatchsize", 100);
    private static final int MAX_CURSOR_FETCH_SIZE = Config.getIntProperty("task.230320.maxcursorfetchsize", 100);

    public PopulateContentletAsJSONUtil() {
        this.contentletJsonAPI = APILocator.getContentletJsonAPI();
    }

    /**
     * Finds all the contentlets that need to be updated with the contentlet_as_json column for a given
     * assetSubtype (Content Type).
     *
     * @param assetSubtype Asset subtype (Content Type) to filter the contentlets to process, if null then all
     *                     the contentlets will be processed.
     * @throws SQLException
     * @throws DotDataException
     * @throws IOException
     */
    public void populateForAssetSubType(final String assetSubtype) throws SQLException, DotDataException, IOException {
        Logger.info(this, String.format("Populate Contentlet as JSON task started for asset subtype [%s]", assetSubtype));
        populate(assetSubtype, null);
    }

    /**
     * Finds all the contentlets that need to be updated with the contentlet_as_json column excluding the contentles
     * of a given assetSubtype (Content Type).
     *
     * @param assetSubtype Asset subtype (Content Type) use to exclude contentlets of that given type from the query.
     * @throws SQLException
     * @throws DotDataException
     * @throws IOException
     */
    public void populateExcludingAssetSubType(final String assetSubtype) throws SQLException, DotDataException, IOException {
        Logger.info(this, String.format("Populate Contentlet as JSON task started excluding asset subtype [%s]", assetSubtype));
        populate(null, assetSubtype);
    }

    /**
     * Finds all the contentlets that need to be updated with the contentlet_as_json column for the given
     * assetSubtype and excludingAssetSubtype.
     *
     * @param assetSubtype          Optional asset subtype (Content Type) to filter the contentlets to process, if null then all
     *                              the contentlets will be processed unless the excludingAssetSubtype is provided.
     * @param excludingAssetSubtype Optional asset subtype (Content Type) use to exclude contentlets from the query
     * @throws IOException
     */
    @LogTime(loggingLevel = "INFO")
    private void populate(@Nullable String assetSubtype, @Nullable String excludingAssetSubtype) throws IOException {

        final File populateJSONTaskDataFile = File.createTempFile("rows-task-230320", "tmp");

        Logger.debug(this, "File created: " + populateJSONTaskDataFile.getAbsolutePath());

        Runnable findAndStore = () -> {
            try {
                // First we need to find all the contentlets to process and write them into a file
                findAndStoreToDisk(assetSubtype, excludingAssetSubtype, populateJSONTaskDataFile);
            } catch (SQLException | DotDataException | IOException e) {
                throw new DotRuntimeException("Error finding, generating JSON representation of Contentlets " +
                        "and storing them in file.", e);
            }
        };

        Runnable processFile = () -> {
            try {
                // Now we need to process the file and each record on it
                processFile(populateJSONTaskDataFile);
            } catch (IOException e) {
                throw new DotRuntimeException("Error processing file with the JSON representation of Contentlets to " +
                        "update.", e);
            }
        };

        CompletableFuture.
                runAsync(findAndStore).
                thenRunAsync(processFile).
                thenAccept(unused -> Logger.info(this, String.format("Contentlet as JSON migration task " +
                                "DONE for assetSubtype: [%s] / excludingAssetSubtype [%s].",
                        assetSubtype, excludingAssetSubtype))).
                join();// Block the current thread and wait for the CompletableFuture to complete
    }

    /**
     * Searches for all the contentlets of a given asset subtype (Content Type) that have a null contentlet_as_json. This
     * method uses a cursor to avoid loading all the contentlets into memory.
     * Each found contentlet is written into a file for a later processing.
     *
     * @param assetSubtype             The asset subtype (Content Type) to search for, if null all the contentlets will be searched.
     * @param excludingAssetSubtype    The asset subtype (Content Type) to exclude in the search, if null no Content Type will be excluded.
     * @param populateJSONTaskDataFile The file where the contentlets will be written.
     * @throws SQLException
     * @throws DotDataException
     * @throws IOException
     */
    @WrapInTransaction
    private void findAndStoreToDisk(@Nullable final String assetSubtype,
                                    @Nullable final String excludingAssetSubtype,
                                    final File populateJSONTaskDataFile) throws
            SQLException, DotDataException, IOException {

        int recordsProcessed = 0;

        final Connection conn = DbConnectionFactory.getConnection();

        try (var fileWriter = new BufferedWriter(new FileWriter(populateJSONTaskDataFile));
             var stmt = conn.createStatement()) {

            // Declaring the cursor
            declareCursor(stmt, assetSubtype, excludingAssetSubtype);

            boolean hasRows;

            do {

                if (DbConnectionFactory.isMsSql()) {
                    stmt.execute(FETCH_CURSOR_MSSQL);
                } else {
                    // Fetching batches of 100 records
                    stmt.execute(String.format(FETCH_CURSOR_POSTGRES, MAX_CURSOR_FETCH_SIZE));
                }

                try (ResultSet rs = stmt.getResultSet()) {

                    // Now we want to write the found Contentlets into a file for a later processing
                    var dotConnect = new DotConnect();
                    dotConnect.fromResultSet(rs);

                    var loadedResults = dotConnect.loadObjectResults();
                    recordsProcessed += loadedResults.size();

                    if (!loadedResults.isEmpty()) {

                        hasRows = true;

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
                            // Write the json representation of the contentlet into the file
                            fileWriter.write(jsonData);
                            fileWriter.newLine();
                        }

                        Logger.debug(this, String.format("Added [%s] records for update to temp file", jsonDataArray.size()));

                    } else {
                        hasRows = false;
                    }
                }

            } while (hasRows);

            // Flush the writer to the file
            fileWriter.flush();

            // Close the cursor
            stmt.execute(CLOSE_CURSOR);
            if (DbConnectionFactory.isMsSql()) {
                stmt.execute(DEALLOCATE_CURSOR_MSSQL);
            }
        }

        Logger.info(this, "-- Records found to process: " + recordsProcessed);
    }

    /**
     * This method processes a file that contains all the contentlets that need to be updated with the contentlet_as_json
     *
     * @param taskDataFile
     * @throws IOException
     */
    @WrapInTransaction
    private void processFile(final File taskDataFile) throws IOException {

        Logger.info(this, "Updating records with missing Contentlet as JSON");

        final Collection<Params> paramsUpdate = new ArrayList<>();
        final MutableInt totalUpdateAffected = new MutableInt(0);

        try (final Stream<String> streamLines = Files.lines(taskDataFile.toPath())) {

            streamLines.forEachOrdered(line -> this.processLine(paramsUpdate, line, totalUpdateAffected));

            if (!paramsUpdate.isEmpty()) {
                this.doUpdateBatch(paramsUpdate, totalUpdateAffected);
            }
        } finally {
            Logger.info(this, "-- total updates: " + totalUpdateAffected.intValue());
        }

        Logger.info(this, "Updated records with missing Contentlet as JSON");
    }

    /**
     * Declares the cursor to be used to find the contentlets that need to be updated with the contentlet_as_json
     *
     * @param stmt
     * @param assetSubtype          The asset subtype (Content Type) to search for, if null all the contentlets will be searched.
     * @param excludingAssetSubtype The asset subtype (Content Type) to exclude in the search, if null no Content Type will be excluded.
     * @throws SQLException
     */
    private void declareCursor(final Statement stmt, @Nullable final String assetSubtype,
                               @Nullable final String excludingAssetSubtype) throws SQLException {

        // Declaring the cursor
        if (Strings.isNullOrEmpty(assetSubtype)) {
            if (Strings.isNullOrEmpty(excludingAssetSubtype)) {
                stmt.execute(String.format(DECLARE_CURSOR, CONTENTS_WITH_NO_JSON));
            } else {
                var selectQuery = String.format(CONTENTS_WITH_NO_JSON_AND_EXCLUDE, excludingAssetSubtype);
                stmt.execute(String.format(DECLARE_CURSOR, selectQuery));
            }
        } else {
            var selectQuery = String.format(SUBTYPE_WITH_NO_JSON, assetSubtype);
            stmt.execute(String.format(DECLARE_CURSOR, selectQuery));
        }

        if (DbConnectionFactory.isMsSql()) {
            stmt.execute(OPEN_CURSOR_MSSQL);
        }
    }

    /**
     * Processes the given line preparing the params for the batch update.
     *
     * @param line Line with the json representation of the contentlet.
     * @throws JsonProcessingException
     */
    private void processLine(final Collection<Params> paramsUpdate, final String line,
                             final MutableInt totalInsertAffected) {

        try {
            var contentlet = ContentletJsonHelper.INSTANCE.get().immutableFromJson(line);

            final Object contentletAsJSON;
            if (DbConnectionFactory.isPostgres()) {
                contentletAsJSON = new DotPGobject.Builder()
                        .jsonValue(line)
                        .build();
            } else {
                contentletAsJSON = line;
            }

            paramsUpdate.add(new Params(contentletAsJSON, contentlet.inode()));

            // Execute the batch for the updates if we have reached the max batch size
            if (paramsUpdate.size() >= MAX_UPDATE_BATCH_SIZE) {
                this.doUpdateBatch(paramsUpdate, totalInsertAffected);
            }
        } catch (JsonProcessingException e) {
            throw new DotRuntimeException("Error processing line", e);
        }
    }

    /**
     * Converts the contentlet to an immutable contentlet and then builds a json representation of it.
     *
     * @param contentlet
     * @return The Contentlet with the json representation attached to it.
     */
    private String toJSON(Contentlet contentlet) {

        try {
            // Converts the given contentlet to an immutable contentlet and then builds a json representation of it.
            var contentletAsJSON = ContentletJsonHelper.INSTANCE.get().writeAsString(this.contentletJsonAPI.toImmutable(contentlet));

            // I need to have the JSON in a single line, so I can write it into a file
            return contentletAsJSON.replaceAll("[\\t\\n\\r]", "");
        } catch (JsonProcessingException e) {
            throw new DotRuntimeException(String.format("Error creating the JSON representation of Contentlet - " +
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
            Logger.info(this, "Batch rows to populate contentlet_as_json column, updated: " + rowsAffected + " rows");
            totalUpdateAffected.add(rowsAffected);
        } catch (DotDataException e) {
            Logger.error(this, "Couldn't update these rows: " + paramsUpdate);
            Logger.error(this, e.getMessage(), e);
        } finally {
            paramsUpdate.clear();
        }
    }

    /**
     * This basically tells Weather or not we support saving content as json and if we have not turned it off.
     */
    public static boolean canPersistContentAsJson() {
        return APILocator.getContentletJsonAPI().isPersistContentAsJson();
    }

}
