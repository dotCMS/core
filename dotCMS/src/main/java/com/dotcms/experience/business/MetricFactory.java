package com.dotcms.experience.business;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Utility class tha provide methods to run SQL Query into dotCMS DataBase
 */
public enum MetricFactory {

    INSTANCE;

    public static <T> T closeDbIfOpened(final InnerSupplier<T> supplier) throws DotDataException {

        final boolean isNewConnection = !DbConnectionFactory.connectionExists();
        final T result = supplier.get();

        if (isNewConnection) {
            DbConnectionFactory.closeSilently();
        }

        return result;
    }

    public Optional<Object> getValue(final String sqlQuery) throws DotDataException {
        return closeDbIfOpened(() -> {
            final DotConnect dotConnect = new DotConnect();
            final List<Map<String, Object>> loadObjectResults = dotConnect.setSQL(sqlQuery)
                    .loadObjectResults();

            if (loadObjectResults.isEmpty()) {
                return Optional.empty();
            }

            return Optional.ofNullable(loadObjectResults.get(0).get("value"));
        });
    }

    /**
     * Execute a query that returns a list of String objects. The query should retrieve a field
     * called value, as shown in the example below:
     * <pre>
     * {@code
     * SELECT identifier as value FROM template
     * }
     * </pre>
     * <p>
     * This method will iterate through the results returned by the query and use the values from
     * the value field to create a Collection of Strings.
     *
     * @param sqlQuery the query to be executed.
     *
     * @return a Collection of Strings with the values returned by the query.
     *
     * @throws DotDataException if an error occurs while executing the query.
     */
    public Optional<List<String>> getList(final String sqlQuery) throws DotDataException {
        return closeDbIfOpened(() -> {
            final DotConnect dotConnect = new DotConnect();
            final List<Map<String, Object>> loadObjectResults = dotConnect.setSQL(sqlQuery)
                    .loadObjectResults();

            if (loadObjectResults.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(
                    loadObjectResults.stream().map(item -> item.get("value").toString()).collect(Collectors.toList())
            );
        });
    }

    @SuppressWarnings("unchecked")
    public int getSchemaDBVersion() throws DotDataException {
        return closeDbIfOpened(() -> {
            final ArrayList<Map<String, Object>> results = new DotConnect()
                    .setSQL("SELECT max(db_version) AS version FROM db_version")
                    .loadResults();

            return Integer.parseInt(results.get(0).get("version").toString());
        });

    }

    @FunctionalInterface
    public interface InnerSupplier<T> {
        T get() throws DotDataException;
    }

}
