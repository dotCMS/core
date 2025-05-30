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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ApiMetricFactoryImpl implements ApiMetricFactory {

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private static final int MAX_BATCH_SIZE = 100;

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
            Logger.debug(this, "Error saving metric: " + apiMetricRequest, e);
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Save multiple metrics in a batch operation for improved performance
     *
     * @param apiMetricRequests collection of requests to save
     */
    @Override
    public void saveBatch(final Collection<ApiMetricRequest> apiMetricRequests) {
        if (apiMetricRequests == null || apiMetricRequests.isEmpty()) {
            return;
        }

        try {
            final DotConnect dc = new DotConnect();
            final StringBuilder sql = new StringBuilder("INSERT INTO metrics_temp (timestamp, metric_type, hash) VALUES ");
            final List<Object> params = new ArrayList<>();
            final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            
            int count = 0;
            int batchCount = 0;
            
            for (ApiMetricRequest request : apiMetricRequests) {
                try {
                    // Add comma if not the first set of values
                    if (count > 0) {
                        sql.append(", ");
                    }
                    
                    // Add value placeholders
                    sql.append("(?, ?, ?)");
                    
                    // Add parameters
                    params.add(now);
                    
                    // Create JSON object for metric
                    final String jsonStr = jsonMapper.writeValueAsString(request.getMetric());
                    final PGobject jsonObject = new PGobject();
                    jsonObject.setType("json");
                    Try.run(() -> jsonObject.setValue(jsonStr)).getOrElseThrow(
                            () -> new IllegalArgumentException("Invalid JSON"));
                    params.add(jsonObject);
                    
                    params.add(request.getHash());
                    count++;
                    
                    // Execute batch if we've reached the maximum batch size
                    if (count >= MAX_BATCH_SIZE) {
                        dc.setSQL(sql.toString());
                        for (Object param : params) {
                            dc.addParam(param);
                        }
                        dc.loadResults();
                        
                        // Reset for next batch
                        sql.setLength(0);
                        sql.append("INSERT INTO metrics_temp (timestamp, metric_type, hash) VALUES ");
                        params.clear();
                        count = 0;
                        batchCount++;
                    }
                } catch (JsonProcessingException e) {
                    Logger.debug(this, "Error serializing metric to JSON, skipping: " + e.getMessage(), e);
                }
            }
            
            // Execute any remaining items
            if (count > 0) {
                dc.setSQL(sql.toString());
                for (Object param : params) {
                    dc.addParam(param);
                }
                dc.loadResults();
                batchCount++;
            }
            
            Logger.debug(this, "Saved " + apiMetricRequests.size() + " metrics in " + batchCount + " batches");
            
        } catch (DotDataException e) {
            Logger.warn(this, "Error saving metrics batch: " + e.getMessage(), e);
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
                    .setSQL("DELETE from metrics_temp")
                    .loadResults();
            saveStartEvent();
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
        try {
            // Check if table exists before creating
            boolean tableExists = false;
            try {
                new DotConnect()
                    .setSQL("SELECT count(*) as count FROM metrics_temp LIMIT 1")
                    .loadResults();
                tableExists = true;
            } catch (Exception e) {
                // Table doesn't exist, which is expected
            }
            
            if (!tableExists) {
                new DotConnect().setSQL("CREATE TABLE metrics_temp (\n" +
                            "    timestamp TIMESTAMPTZ,\n" +
                            "    metric_type JSON,\n" +
                            "    hash VARCHAR(255)\n" +
                            ")")
                        .loadResults();
                
                // Add index to improve performance
                new DotConnect().setSQL("CREATE INDEX IF NOT EXISTS idx_metrics_temp_hash ON metrics_temp (hash)")
                        .loadResults();
                
                saveStartEvent();
                Logger.info(this, "Created metrics_temp table");
            }
        } catch (Exception e) {
            Logger.error(this, "Error creating metrics_temp table: " + e.getMessage(), e);
            throw new DotDataException(e);
        }
    }

    /**
     * Drop the metrics_temp table
     *
     * @throws DotDataException if something wrong happened
     */
    @Override
    public void dropTemporaryTable() throws DotDataException {
        new DotConnect().setSQL("DROP TABLE IF EXISTS metrics_temp").loadResults();
        Logger.info(this, "Dropped metrics_temp table");
    }

    /**
     * Return the amount of hours between we start collecting the data until we are generating the
     * summary
     *
     * @return the amount of hours
     *
     * @throws DotDataException An error occurred when accessing the database.
     */
    @Override
    public double getOverallHours() throws DotDataException {
        final String query = OVERALL_QUERY;
        final List<Map<String, Object>> results = new DotConnect()
                .setSQL(query).loadObjectResults();

        if (results.isEmpty()) {
            return 0;
        }

        final Object value = results.get(0).get("overall");
        return value == null ? 0 : Double.parseDouble(value.toString());
    }

    /**
     * Return all the data from the metrics_temp table.
     *
     * @return Collection of Maps with the summary data.
     */
    @Override
    public Collection<Map<String, Object>> getMetricTemporaryTableData() {
        try {
            final double overallHours = getOverallHours();

            if (overallHours < 0.01f) {
                return new ArrayList<>();
            }

            final String query = String.format(GET_DATA_FROM_TEMPORARY_METRIC_TABLE, overallHours);
            return new DotConnect().setSQL(query).loadObjectResults();
        } catch (final DotDataException e) {
            Logger.debug(this, "Error getting metrics", e);
            throw new DotRuntimeException(e);
        }
    }
}
