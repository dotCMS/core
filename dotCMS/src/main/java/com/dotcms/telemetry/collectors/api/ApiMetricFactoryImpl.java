package com.dotcms.telemetry.collectors.api;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Try;
import org.postgresql.util.PGobject;

import javax.enterprise.context.ApplicationScoped;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Map;

@ApplicationScoped
public class ApiMetricFactoryImpl implements ApiMetricFactory {

    final ObjectMapper jsonMapper = new ObjectMapper();

    /**
     * Save request on the metrics_temp
     *
     * @param apiMetricRequest request
     */
    @Override
    public void save(final ApiMetricRequest apiMetricRequest) {
        try {
            final String jsonStr = jsonMapper.writeValueAsString(apiMetricRequest.getMetric());
            final PGobject jsonObject = new PGobject();
            jsonObject.setType("json");
            Try.run(() -> jsonObject.setValue(jsonStr)).getOrElseThrow(
                    () -> new IllegalArgumentException("Invalid JSON"));

            new DotConnect()
                    .setSQL("INSERT INTO metrics_temp (timestamp, metric_type, hash) VALUES (?, ?, ?)")
                    .addParam(OffsetDateTime.now(ZoneOffset.UTC))
                    .addParam(jsonObject)
                    .addParam(apiMetricRequest.getHash())
                    .loadResults();
        } catch (final JsonProcessingException | DotDataException e) {
            Logger.debug(this, String.format("Error saving metric: %s", apiMetricRequest), e);
            throw new DotRuntimeException(e);
        }
    }


    /**
     * Save a register with just the current time as timestamp, it is used to mark when we start
     * collecting the data.
     */
    @Override
    public void saveStartEvent() {
        try {
            new DotConnect()
                    .setSQL("INSERT INTO metrics_temp (timestamp) VALUES (?)")
                    .addParam(OffsetDateTime.now(ZoneOffset.UTC))
                    .loadResults();
        } catch (final DotDataException e) {
            Logger.debug(this, "Error saving start event", e);
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Drop all the registers on the table
     */
    @Override
    public void flushTemporaryTable() {
        try {
            new DotConnect()
                    .setSQL("DELETE from  metrics_temp")
                    .loadResults();
        } catch (final DotDataException e) {
            Logger.debug(this, "Error flushing the temporary table", e);
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Create the metrics_temp table
     *
     * @throws DotDataException if something wrong happened
     */
    @Override
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
    @Override
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
        final DotConnect dotConnect = new DotConnect();
        return Double.parseDouble(((Map<String, Object>) dotConnect.setSQL(OVERALL_QUERY)
                .loadResults()
                .get(0))
                .get("overall")
                .toString()
        );
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
    @Override
    public Collection<Map<String, Object>> getMetricTemporaryTableData() {
        try {
            final DotConnect dotConnect = new DotConnect();
            final double overall = getOverall();
            final String sql = String.format(GET_DATA_FROM_TEMPORARY_METRIC_TABLE, overall);
            return dotConnect.setSQL(sql).loadResults();
        } catch (final Exception e) {
            Logger.debug(this, "Error getting the temporary table data", e);
            throw new DotRuntimeException(e);
        }
    }

}
