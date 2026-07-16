package com.dotcms.jobs.business.job;

import com.dotcms.jobs.business.error.ErrorDetail;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Custom serializer for JobResult that transforms the structure to the desired format.
 * This serializer flattens the metadata and includes error information from errorDetail
 * in a consistent format within the metadata's "error" array.
 */
public class OptionalJobResultSerializer extends JsonSerializer<Optional<JobResult>> {

    // Constants for field names
    private static final String ERROR_ARRAY_KEY = "error";
    private static final String ERROR_CODE_KEY = "code";
    private static final String ERROR_MESSAGE_KEY = "message";
    private static final String ERROR_CONTEXT_KEY = "context";

    @Override
    public void serialize(Optional<JobResult> value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {

        if (value.isEmpty()) {
            gen.writeNull();
            return;
        }

        final JobResult jobResult = value.get();

        gen.writeStartObject();

        // Get metadata or create empty map if null, using TreeMap for alphabetical ordering of keys
        Map<String, Object> metadata = jobResult.metadata()
                .map(TreeMap::new)  // Convert to TreeMap to order keys alphabetically
                .orElseGet(TreeMap::new);

        // Process errorDetail if present and add to error array in metadata
        if (jobResult.errorDetail().isPresent()) {
            final ErrorDetail errorDetail = jobResult.errorDetail().get();

            // Create or get the error list from metadata
            final List<Map<String, Object>> errorList;
            if (metadata.containsKey(ERROR_ARRAY_KEY) && metadata.get(ERROR_ARRAY_KEY) instanceof List) {
                errorList = (List<Map<String, Object>>) metadata.get(ERROR_ARRAY_KEY);
            } else {
                errorList = new ArrayList<>();
                metadata.put(ERROR_ARRAY_KEY, errorList);
            }

            // Create error entry from errorDetail (using TreeMap for alphabetical ordering)
            final Map<String, Object> errorEntry = new TreeMap<>();

            // Determine error code from exception class if available
            // Extract just the simple class name from the fully qualified name
            String errorCode = determineErrorCode(errorDetail.exceptionClass());
            errorEntry.put(ERROR_CODE_KEY, errorCode);
            errorEntry.put(ERROR_MESSAGE_KEY, errorDetail.message());

            // Create context with additional error information
            Map<String, Object> context = new TreeMap<>();
            context.put("exceptionClass", errorDetail.exceptionClass());
            context.put("processingStage", errorDetail.processingStage());
            context.put("stackTrace", errorDetail.stackTrace());
            context.put("timestamp", errorDetail.timestamp().toString());

            // Add context to error entry
            errorEntry.put(ERROR_CONTEXT_KEY, context);

            // Add error entry to error list
            errorList.add(errorEntry);
        }

        // Write all metadata fields directly to result object
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            gen.writeObjectField(entry.getKey(), entry.getValue());
        }

        gen.writeEndObject();
    }

    /**
     * Determines an appropriate error code from the exception class name.
     * This method extracts the simple class name and converts it to a standardized error code format.
     *
     * @param exceptionClass The fully qualified exception class name
     * @return A standardized error code derived from the exception class name
     */
    private String determineErrorCode(final String exceptionClass) {
        if (exceptionClass == null || exceptionClass.isEmpty()) {
            return "UNKNOWN_ERROR";
        }

        // Extract simple class name from fully qualified name
        String simpleName = exceptionClass;
        int lastDot = exceptionClass.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < exceptionClass.length() - 1) {
            simpleName = exceptionClass.substring(lastDot + 1);
        }

        // Remove "Exception" suffix if present
        if (simpleName.endsWith("Exception")) {
            simpleName = simpleName.substring(0, simpleName.length() - 9);
        }

        // Convert to uppercase with underscores
        final StringBuilder result = new StringBuilder();
        for (int i = 0; i < simpleName.length(); i++) {
            char c = simpleName.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                result.append('_');
            }
            result.append(Character.toUpperCase(c));
        }

        return result.toString();
    }
}