package com.dotcms.experience.collectors.api;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Try;
import org.postgresql.util.PGobject;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Map;

import static com.dotcms.experience.business.MetricFactory.closeDbIfOpened;

/**
 * A utility class to interact with the metrics_temp table, providing the more basic methods to
 * create/drop the table or save/flush data.
 * <p>
 * This class encapsulates the queries for saving/flushing data into the metrics_temp and
 * creating/dropping the whole table.
 * <p>
 * The metrics_temp is a special table designed to store the request to the endpoints we wish to
 * track. Later, the data in this table is summarized and stored as part of the MetricSnapshot.
 */
public enum ApiMetricFactory {

    INSTANCE;

    private static final String GET_DATA_FROM_TEMPORARY_METRIC_TABLE =
            "SELECT " +
                    "metric_type->>'feature' as feature, " +
                    "metric_type->>'category' as category, " +
                    "metric_type->>'name' as name, " +
                    "COUNT(*) / %1$.2f AS average_per_hour, " +
                    "COUNT(distinct hash) / %1$.2f AS unique_average_per_hour " +
                    "FROM metrics_temp " +
                    "GROUP BY metric_type->>'feature', " +
                    "metric_type->>'category', " +
                    "metric_type->>'name' " +
                    "HAVING metric_type->>'name' IS NOT null";
    private static final String OVERALL_QUERY = "SELECT (EXTRACT(epoch FROM now()) - EXTRACT" +
            "(epoch FROM MIN(timestamp)))/3600 " +
            "as overall FROM metrics_temp";

    final ObjectMapper jsonMapper = new ObjectMapper();

    ApiMetricFactory() {

    }

    /**
     * Save request on the metrics_temp
     *
     * @param apiMetricRequest request
     */
    public void save(final ApiMetricRequest apiMetricRequest) {
        try {
            final String jsonStr = jsonMapper.writeValueAsString(apiMetricRequest.getMetric());

            final PGobject jsonObject = new PGobject();
            jsonObject.setType("json");
            Try.run(() -> jsonObject.setValue(jsonStr)).getOrElseThrow(
                    () -> new IllegalArgumentException("Invalid JSON"));

            new DotConnect()
                    .setSQL("INSERT INTO metrics_temp (timestamp, metric_type, hash) VALUES (?, " +
                            "?, ?)")
                    .addParam(OffsetDateTime.now(ZoneOffset.UTC))
                    .addParam(jsonObject)
                    .addParam(apiMetricRequest.getHash())
                    .loadResults();

        } catch (JsonProcessingException | DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }


    /**
     * Save a register with just the current time as timestamp, it is used to mark when we start
     * collecting the data.
     */
    public void saveStartEvent() {
        try {
            new DotConnect()
                    .setSQL("INSERT INTO metrics_temp (timestamp) VALUES (?)")
                    .addParam(OffsetDateTime.now(ZoneOffset.UTC))
                    .loadResults();

        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Drop all the registers on the table
     */
    public void flushTemporaryTable() {
        try {
            new DotConnect()
                    .setSQL("DELETE from  metrics_temp")
                    .loadResults();

        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Create the metrics_temp table
     *
     * @throws DotDataException if something wrong happened
     */
    public void createTemporaryTable() throws DotDataException {
        new DotConnect().setSQL("CREATE TABLE metrics_temp (\n" +
                        "    timestamp TIMESTAMPTZ,\n" +
                        "    metric_type JSON,\n" +
                        "    hash VARCHAR(255)\n" +
                        ")")
                .loadResults();

        saveStartEvent();
    }

    /**
     * Drop the metrics_temp table
     *
     * @throws DotDataException if something wrong happened
     */
    public void dropTemporaryTable() throws DotDataException {
        new DotConnect().setSQL("DROP TABLE IF EXISTS metrics_temp").loadResults();
    }

    /**
     * return the amount of hours between we start collecting the data until we are generating the
     * summary
     *
     * @return the amount of hours
     *
     * @throws DotDataException An error occurred when accessing the database.
     */
    @SuppressWarnings("unchecked")
    private double getOverall() throws DotDataException {
        return closeDbIfOpened(() -> {
            final DotConnect dotConnect = new DotConnect();

            return Double.parseDouble(((Map<String, Object>) dotConnect.setSQL(OVERALL_QUERY)
                    .loadResults()
                    .get(0))
                    .get("overall")
                    .toString()
            );
        });
    }

    /**
     * Return all the summary from the temporal table
     *
     * @return a collection of maps with the summary data.
     *
     * @see ApiMetricFactory
     * @see ApiMetricAPI
     */
    @SuppressWarnings("unchecked")
    public Collection<Map<String, Object>> getMetricTemporaryTableData() {
        try {
            return closeDbIfOpened(() -> {
                try {
                    final DotConnect dotConnect = new DotConnect();

                    final double overall = getOverall();
                    final String sql = String.format(GET_DATA_FROM_TEMPORARY_METRIC_TABLE, overall);

                    return dotConnect.setSQL(sql).loadResults();
                } catch (Exception e) {
                    throw new DotRuntimeException(e);
                }
            });
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

}
