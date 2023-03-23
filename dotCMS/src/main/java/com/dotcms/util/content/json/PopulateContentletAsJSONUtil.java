package com.dotcms.util.content.json;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.content.business.json.ContentletJsonAPI;
import com.dotcms.content.business.json.ContentletJsonHelper;
import com.dotcms.repackage.com.google.common.base.Strings;
import com.dotcms.util.CloseUtils;
import com.dotcms.util.marshal.MarshalFactory;
import com.dotcms.util.marshal.MarshalUtils;
import com.dotcms.util.transform.TransformerLocator;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.fasterxml.jackson.core.JsonProcessingException;

import javax.annotation.Nullable;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.dotcms.content.business.json.ContentletJsonAPI.SAVE_CONTENTLET_AS_JSON;

public class PopulateContentletAsJSONUtil {

    private final ContentletJsonAPI contentletJsonAPI;

    // Query to find all the contentlets that are Hosts and have a null contentlet_as_json
    private final String SUBTYPE_WITH_NO_JSON = "select c.*\n" +
            "from contentlet c\n" +
            "         JOIN identifier i ON i.id = c.identifier\n" +
            "         JOIN contentlet_version_info cv ON i.id = cv.identifier\n" +
            "    AND (c.inode = cv.working_inode OR c.inode = cv.live_inode)\n" +
            "WHERE i.asset_subtype = '%s' AND c.contentlet_as_json IS NULL;";

    // Query to find all the contentlets that are NOT Hosts and have a null contentlet_as_json
    private final String CONTENTS_WITH_NO_JSON = "select c.*\n" +
            "from contentlet c\n" +
            "         JOIN identifier i ON i.id = c.identifier\n" +
            "         JOIN contentlet_version_info cv ON i.id = cv.identifier\n" +
            "    AND (c.inode = cv.working_inode OR c.inode = cv.live_inode)\n" +
            "WHERE i.asset_type <> 'contentlet' AND c.contentlet_as_json IS NULL;";

    // Query to update the contentlet_as_json column of the contentlet table
    private final String UPDATE_CONTENTLET_AS_JSON = "UPDATE contentlet SET contentlet_as_json = ? WHERE inode = ?";

    // Cursor related queries
    private final String DECLARE_CURSOR = "DECLARE missingContentletAsJSONCursor CURSOR FOR %s";
    private final String FETCH_CURSOR = "FETCH FORWARD 100 FROM missingContentletAsJSONCursor";
    private final String CLOSE_CURSOR = "CLOSE missingContentletAsJSONCursor";

    private static class SingletonHolder {
        private static final PopulateContentletAsJSONUtil INSTANCE = new PopulateContentletAsJSONUtil();
    }

    public static PopulateContentletAsJSONUtil getInstance() {
        return PopulateContentletAsJSONUtil.SingletonHolder.INSTANCE;
    } // getInstance.

    public PopulateContentletAsJSONUtil() {
        this.contentletJsonAPI = APILocator.getContentletJsonAPI();
    }

    @FunctionalInterface
    private interface CheckedFunction<T, R, E extends Exception> {
        R apply(T t) throws E;
    }

    @FunctionalInterface
    private interface CheckedConsumer<T, E extends Exception> {
        void accept(T t) throws E;
    }

//    @WrapInTransaction
//    public void populateSites() throws DotDataException, DotRuntimeException {
//
//        // Finding the contentlets that are Hosts and have a null contentlet_as_json
//        final List<Contentlet> sites = findSiteContentlets();
//
//        // Building a json representation of each contentlet to update the contentlet_as_json column of the
//        // contentlet table.
//        sites.stream()
//                .map(wrapCheckedFunction(this::toJSON))
//                .forEach(wrapCheckedConsumer(this::updateContentlet));
//    }

    /**
     * Search for all the contentlets that are Hosts and have a null contentlet_as_json using a db query and transform
     * the results into a list of contentlets.
     *
     * @return List of contentlets
     * @throws DotDataException
     * @throws DotStateException
     */
//    private List<Contentlet> findSiteContentlets() throws DotDataException, DotStateException {
//
//        final var dotConnect = new DotConnect();
//        dotConnect.setSQL(SITES_WITH_NO_JSON);
//
//        return Optional.ofNullable(dotConnect.loadObjectResults())
//                .map(results -> TransformerLocator.createContentletTransformer(results).asList())
//                .orElse(Collections.emptyList());
//    }
    @WrapInTransaction
    public void run(@Nullable String assetSubtype) throws SQLException, DotDataException, IOException {

        final File populateJSONTaskDataFile = File.createTempFile("rows-task-230320", "tmp");
        final MarshalUtils marshalUtils = MarshalFactory.getInstance().getMarshalUtils();

        // First we need to find all the contentlets to process and write them into a file
        findAndStoreToDisk(assetSubtype, populateJSONTaskDataFile, marshalUtils);

        // Now we need to process the file and each record on it
        processFile(populateJSONTaskDataFile, marshalUtils);
    }

    private void processFile(final File taskDataFile,
                             final MarshalUtils marshalUtils) throws IOException {

        try (final Stream<String> streamLines = Files.lines(taskDataFile.toPath())) {

            streamLines.map(line -> lineToContentlet(line, marshalUtils))// Map each line to a new contentlet
                    .map(wrapCheckedFunction(this::toJSON))// Generate populate the contentlet_as_json attribute in the contentlet
                    .forEach(wrapCheckedConsumer(this::updateContentlet));// Update each contentlet in the DB
        }
    }

    private void findAndStoreToDisk(@Nullable String assetSubtype,
                                    final File populateJSONTaskDataFile, final MarshalUtils marshalUtils) throws
            SQLException, DotDataException, IOException {

        var fileWriter = new BufferedWriter(new FileWriter(populateJSONTaskDataFile));

        try (final Connection conn = DbConnectionFactory.getConnection();
             var stmt = conn.createStatement()) {

            if (Strings.isNullOrEmpty(assetSubtype)) {
                stmt.execute(String.format(DECLARE_CURSOR, CONTENTS_WITH_NO_JSON));
            } else {
                var selectQuery = String.format(SUBTYPE_WITH_NO_JSON, assetSubtype);
                stmt.execute(String.format(DECLARE_CURSOR, selectQuery));
            }

            boolean hasMoreRows = true;

            do {

                stmt.execute(FETCH_CURSOR);// Fetching batches of 100 records

                try (ResultSet rs = stmt.getResultSet()) {

                    // Process the batch of rows
                    while (rs.next()) {

                        // Now we want to write the found Contentlets into a file for a later processing
                        var dotConnect = new DotConnect();
                        dotConnect.fromResultSet(rs);

                        var contentlets = Optional.ofNullable(dotConnect.loadObjectResults())
                                .orElse(Collections.emptyList());

                        for (var contentlet : contentlets) {
                            fileWriter.write(marshalUtils.marshal(contentlet));
                            fileWriter.newLine();
                        }
                    }

                    // Check if there are more rows to fetch
                    hasMoreRows = rs.getRow() > 0;
                }

                // Flush the writer to the file
                fileWriter.flush();

            } while (hasMoreRows);

            // Close the cursor
            stmt.execute(CLOSE_CURSOR);
        } finally {
            CloseUtils.closeQuietly(fileWriter);
        }
    }

    private Contentlet lineToContentlet(final String line, final MarshalUtils marshalUtils) {

        final var contentletMap = marshalUtils.unmarshal(line, Map.class);

        return Optional.ofNullable(contentletMap)
                .map(results -> TransformerLocator.createContentletTransformer(Collections.singletonList(contentletMap))
                        .findFirst())
                .orElse(null);
    }

    /**
     * Converts the contentlet to an immutable contentlet and then builds a json representation of it.
     *
     * @param contentlet
     * @return The Contentlet with the json representation attached to it.
     */
    private Contentlet toJSON(Contentlet contentlet) throws JsonProcessingException {

        // Converts the given contentlet to an immutable contentlet and then builds a json representation of it.
        var asJSON = ContentletJsonHelper.INSTANCE.get().writeAsString(this.contentletJsonAPI.toImmutable(contentlet));

        //Attach the json, so it can be grabbed by the upsert downstream
        contentlet.setProperty(Contentlet.CONTENTLET_AS_JSON, asJSON);

        return contentlet;
    }

    /**
     * Updates the contentlet_as_json column of the contentlet table with the json representation of the contentlet.
     *
     * @param contentlet
     * @throws DotDataException
     */
    private void updateContentlet(final Contentlet contentlet) throws DotDataException {

        final var dotConnect = new DotConnect();
        dotConnect.setSQL(UPDATE_CONTENTLET_AS_JSON);
        dotConnect.addParam(contentlet.getStringProperty(Contentlet.CONTENTLET_AS_JSON));
        dotConnect.addObject(contentlet.getInode());
        dotConnect.loadResult();
    }

    private static <T, R, E extends Exception> Function<T, R> wrapCheckedFunction(PopulateContentletAsJSONUtil.CheckedFunction<T, R, E> function) {
        return t -> {
            try {
                return function.apply(t);
            } catch (Exception e) {
                throw new DotRuntimeException(e.getMessage(), e);
            }
        };
    }

    private static <T, E extends Exception> Consumer<T> wrapCheckedConsumer(PopulateContentletAsJSONUtil.CheckedConsumer<T, E> consumer) {
        return t -> {
            try {
                consumer.accept(t);
            } catch (Exception e) {
                throw new DotRuntimeException(e.getMessage(), e);
            }
        };
    }

    /**
     * This basically tells Weather or not we support saving content as json and if we have not turned it off.
     */
    public static boolean canPersistContentAsJson() {
        return isJsonSupportedDatabase()
                && Config.getBooleanProperty(SAVE_CONTENTLET_AS_JSON, true);
    }

    /**
     * This tells us if we're running on a db that supports json
     */
    private static boolean isJsonSupportedDatabase() {
        return DbConnectionFactory.isPostgres() || DbConnectionFactory.isMsSql();
    }

}
