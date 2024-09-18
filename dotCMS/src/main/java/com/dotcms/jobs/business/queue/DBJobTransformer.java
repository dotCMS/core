package com.dotcms.jobs.business.queue;

import com.dotcms.jobs.business.error.ErrorDetail;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobResult;
import com.dotcms.jobs.business.job.JobState;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.UtilMethods;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Utility class for transforming database result sets into Job objects. This class provides static
 * methods to convert raw database data into structured Job objects and their associated
 * components.
 */
public class DBJobTransformer {

    private DBJobTransformer() {
        // Prevent instantiation
    }

    /**
     * Transforms a database result row into a Job object.
     *
     * @param row A map representing a row from the database result set
     * @return A fully constructed Job object, or null if the input is null
     */
    public static Job toJob(final Map<String, Object> row) {
        if (row == null) {
            return null;
        }

        return Job.builder()
                .id(getString(row, "id"))
                .queueName(getString(row, "queue_name"))
                .state(getJobState(row))
                .parameters(getParameters(row))
                .result(getJobResult(row))
                .progress(getFloat(row, "progress"))
                .createdAt(getDateTime(row, "created_at"))
                .updatedAt(getDateTime(row, "updated_at"))
                .startedAt(getOptionalDateTime(row, "started_at"))
                .completedAt(getOptionalDateTime(row, "completed_at"))
                .executionNode(getOptionalString(row, "execution_node"))
                .retryCount(getInt(row, "retry_count"))
                .build();
    }

    /**
     * Retrieves a string value from the row map.
     *
     * @param row    The row map
     * @param column The column name
     * @return The string value, or null if not present
     */
    private static String getString(final Map<String, Object> row, final String column) {
        return (String) row.get(column);
    }

    /**
     * Retrieves an optional string value from the row map.
     *
     * @param row    The row map
     * @param column The column name
     * @return An Optional containing the string value, or empty if not present
     */
    private static Optional<String> getOptionalString(final Map<String, Object> row,
            final String column) {
        return Optional.ofNullable(getString(row, column));
    }

    /**
     * Retrieves the JobState from the row map.
     *
     * @param row The row map
     * @return The JobState, or null if not present or invalid
     */
    private static JobState getJobState(final Map<String, Object> row) {
        String stateStr = getString(row, "state");
        return UtilMethods.isSet(stateStr) ? JobState.valueOf(stateStr) : null;
    }

    /**
     * Retrieves the job parameters from the row map.
     *
     * @param row The row map
     * @return A Map of job parameters, or an empty map if not present or invalid
     */
    private static Map<String, Object> getParameters(final Map<String, Object> row) {

        String paramsJson = getString(row, "parameters");
        if (!UtilMethods.isSet(paramsJson)) {
            return new HashMap<>();
        }

        try {
            JSONObject jsonObject = new JSONObject(paramsJson);
            return jsonObject.toMap();
        } catch (JSONException e) {
            throw new DotRuntimeException("Error parsing job parameters", e);
        }
    }

    /**
     * Retrieves the JobResult from the row map.
     *
     * @param row The row map
     * @return An Optional containing the JobResult, or empty if not present or invalid
     */
    private static Optional<JobResult> getJobResult(final Map<String, Object> row) {

        String resultJson = getString(row, "result");
        if (!UtilMethods.isSet(resultJson)) {
            return Optional.empty();
        }

        try {
            JSONObject resultJsonObject = new JSONObject(resultJson);
            return Optional.of(JobResult.builder()
                    .errorDetail(getErrorDetail(resultJsonObject))
                    .metadata(getMetadata(resultJsonObject))
                    .build());
        } catch (JSONException e) {
            throw new DotRuntimeException("Error parsing job result", e);
        }
    }

    /**
     * Retrieves the ErrorDetail from a JSON object.
     *
     * @param resultJsonObject The JSON object containing error details
     * @return An Optional containing the ErrorDetail, or empty if not present or invalid
     */
    private static Optional<ErrorDetail> getErrorDetail(final JSONObject resultJsonObject) {

        if (!resultJsonObject.has("errorDetail")) {
            return Optional.empty();
        }

        try {
            JSONObject errorDetailJson = resultJsonObject.getJSONObject("errorDetail");
            return Optional.of(ErrorDetail.builder()
                    .message(errorDetailJson.optString("message"))
                    .exceptionClass(errorDetailJson.optString("exceptionClass"))
                    .timestamp(getDateTime(errorDetailJson.opt("timestamp")))
                    .processingStage(errorDetailJson.optString("processingStage"))
                    .build());
        } catch (JSONException e) {
            throw new DotRuntimeException("Error parsing error detail", e);
        }
    }

    /**
     * Retrieves the metadata from a JSON object.
     *
     * @param resultJsonObject The JSON object containing metadata
     * @return An Optional containing the metadata as a Map, or empty if not present or invalid
     */
    private static Optional<Map<String, Object>> getMetadata(final JSONObject resultJsonObject) {

        if (!resultJsonObject.has("metadata")) {
            return Optional.empty();
        }

        try {
            return Optional.of(resultJsonObject.getJSONObject("metadata").toMap());
        } catch (JSONException e) {
            throw new DotRuntimeException("Error parsing metadata", e);
        }
    }

    /**
     * Retrieves a float value from the row map.
     *
     * @param row    The row map
     * @param column The column name
     * @return The float value, or 0 if not present or invalid
     */
    private static float getFloat(final Map<String, Object> row, final String column) {
        Object value = row.get(column);
        return value instanceof Number ? ((Number) value).floatValue() : 0f;
    }

    /**
     * Retrieves an integer value from the row map.
     *
     * @param row    The row map
     * @param column The column name
     * @return The integer value, or 0 if not present or invalid
     */
    private static int getInt(final Map<String, Object> row, final String column) {
        Object value = row.get(column);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    /**
     * Retrieves a LocalDateTime value from the row map.
     *
     * @param row    The row map
     * @param column The column name
     * @return The LocalDateTime value, or null if not present or invalid
     */
    private static LocalDateTime getDateTime(final Map<String, Object> row, final String column) {
        Object value = row.get(column);
        return getDateTime(value);
    }

    /**
     * Converts an Object to a LocalDateTime.
     *
     * @param value The object to convert
     * @return The LocalDateTime value, or null if the input is not a Timestamp
     */
    private static LocalDateTime getDateTime(final Object value) {
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        }
        return null;
    }

    /**
     * Retrieves an optional LocalDateTime value from the row map.
     *
     * @param row    The row map
     * @param column The column name
     * @return An Optional containing the LocalDateTime value, or empty if not present or invalid
     */
    private static Optional<LocalDateTime> getOptionalDateTime(final Map<String, Object> row,
            final String column) {
        return Optional.ofNullable(getDateTime(row, column));
    }

}