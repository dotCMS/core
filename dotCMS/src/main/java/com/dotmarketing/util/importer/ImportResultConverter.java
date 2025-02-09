package com.dotmarketing.util.importer;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.ImportUtil.Counters;
import com.dotmarketing.util.importer.model.AbstractSpecialHeaderInfo.SpecialHeaderType;
import com.dotmarketing.util.importer.model.AbstractValidationMessage.ValidationMessageType;
import com.dotmarketing.util.importer.model.HeaderInfo;
import com.dotmarketing.util.importer.model.HeaderValidationResult;
import com.dotmarketing.util.importer.model.ImportResult;
import com.dotmarketing.util.importer.model.LineImportResult;
import com.dotmarketing.util.importer.model.ResultData;
import com.dotmarketing.util.importer.model.SpecialHeaderInfo;
import com.dotmarketing.util.importer.model.ValidationMessage;
import com.liferay.util.StringPool;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Converter utility to maintain backward compatibility during the migration from legacy
 * HashMap-based import results to the new structured ImportResult format. This class handles
 * converting the new structured format back to the legacy format that existing code expects.
 * <p>
 * The legacy format uses a HashMap with standard keys:
 * <ul>
 *   <li>"warnings" - List of warning messages</li>
 *   <li>"errors" - List of error messages</li>
 *   <li>"messages" - List of informational messages</li>
 *   <li>"counters" - List of counter values in format "key=value"</li>
 *   <li>"results" - List of summary messages about the operation</li>
 *   <li>"validHeaders" - List of valid header names</li>
 *   <li>"invalidHeaders" - List of invalid header names</li>
 *   <li>"missingHeaders" - List of required headers that were not found</li>
 *   <li>"headerValidation" - List of header validation details</li>
 * </ul>
 */
public class ImportResultConverter {

    /**
     * Converts from the new structured ImportResult format to the legacy HashMap format. This
     * method ensures backward compatibility by formatting the structured data into the string-based
     * format expected by legacy code.
     *
     * @param result The structured ImportResult to convert
     * @return A HashMap containing the legacy format results with standard keys for warnings,
     * errors, messages, counters, and other import-related data
     * @throws IllegalArgumentException if the result parameter is null
     */
    public static Map<String, List<String>> toLegacyFormat(ImportResult result) {
        Map<String, List<String>> legacyResults = new HashMap<>();

        // Convert messages to legacy format by type
        Map<ValidationMessageType, List<String>> messagesByType =
                convertMessagesToLegacyFormat(result.messages());

        legacyResults.put("warnings", messagesByType.get(ValidationMessageType.WARNING));
        legacyResults.put("errors", messagesByType.get(ValidationMessageType.ERROR));
        legacyResults.put("messages", messagesByType.get(ValidationMessageType.INFO));

        // Add counters
        List<String> counters = new ArrayList<>();
        ResultData data = result.data();
        counters.add("linesread=" + result.fileInfo().totalRows());
        counters.add("errors=" + messagesByType.get(ValidationMessageType.ERROR).size());
        counters.add("newContent=" + data.summary().created());
        counters.add("contentToUpdate=" + data.summary().updated());
        legacyResults.put("counters", counters);

        // Add results summary
        List<String> results = new ArrayList<>();
        results.add(data.summary().created() + " new \"" + data.summary().contentType()
                + "\" were created");
        if (data.summary().updated() > 0) {
            results.add(data.summary().updated() + " \"" + data.summary().contentType() +
                    "\" contentlets updated");
        }
        legacyResults.put("results", results);

        // Add header validation info if present
        addHeaderValidationToLegacy(result.fileInfo().headerInfo(), legacyResults);

        return legacyResults;
    }

    /**
     * Converts the structured ImportHeaderValidationResult to the legacy HashMap format. This
     * method ensures backward compatibility by formatting the structured data into the string-based
     * format expected by legacy code.
     *
     * @param validationResult The structured ImportHeaderValidationResult to convert
     * @param legacyResults    The legacy format results map to update
     * @return The number of errors found during header validation
     */
    public static int headerValidationResultsToLegacyMap(
            final HeaderValidationResult validationResult,
            final Map<String, List<String>> legacyResults) {

        // Convert validation messages
        for (ValidationMessage message : validationResult.messages()) {
            String messageText = message.message();
            switch (message.type()) {
                case ERROR:
                    legacyResults.get("errors").add(messageText);
                    break;
                case WARNING:
                    legacyResults.get("warnings").add(messageText);
                    break;
                case INFO:
                    legacyResults.get("messages").add(messageText);
                    break;
            }
        }

        // Convert special headers info
        for (SpecialHeaderInfo specialHeader : validationResult.headerInfo()
                .specialHeaders()) {
            if (specialHeader.type() == SpecialHeaderType.IDENTIFIER) {
                legacyResults.get("identifiers").add(
                        StringPool.BLANK + specialHeader.columnIndex()
                );
            } else if (specialHeader.type() == SpecialHeaderType.WORKFLOW_ACTION) {
                legacyResults.get(Contentlet.WORKFLOW_ACTION_KEY).add(
                        StringPool.BLANK + specialHeader.columnIndex()
                );
            }
        }

        return (int) validationResult.messages().stream()
                .filter(m -> m.type() == ValidationMessageType.ERROR)
                .count();
    }

    /**
     * Converts the structured LineImportResult to the legacy HashMap format. This method ensures
     * backward compatibility by formatting the structured data into the string-based format
     * expected by legacy code.
     *
     * @param importResults The structured LineImportResult to convert
     * @param legacyResults The legacy format results map to update
     * @param counters      The import counters to update
     * @return The number of errors found during line import
     */
    public static int lineImportResultToLegacyMap(
            final LineImportResult importResults,
            final Map<String, List<String>> legacyResults,
            final Counters counters) {

        // Convert messages
        for (ValidationMessage msg : importResults.messages()) {
            switch (msg.type()) {
                case ERROR:
                    legacyResults.get("errors").add(formatMessage(msg));
                    break;
                case WARNING:
                    legacyResults.get("warnings").add(formatMessage(msg));
                    break;
                case INFO:
                    legacyResults.get("messages").add(formatMessage(msg));
                    break;
            }
        }

        counters.setNewContentCounter(
                counters.getNewContentCounter() + importResults.contentToCreate());
        counters.setContentCreated(
                counters.getContentCreated() + importResults.createdContent());
        counters.setContentToUpdateCounter(
                counters.getContentToUpdateCounter() + importResults.contentToUpdate());
        counters.setContentUpdated(
                counters.getContentUpdated() + importResults.updatedContent());
        counters.setContentUpdatedDuplicated(
                counters.getContentUpdatedDuplicated() + importResults.duplicateContent());

        legacyResults.get("lastInode").clear();
        List<String> l = legacyResults.get("lastInode");
        l.add(importResults.lastInode());
        legacyResults.put("lastInode", l);

        return (int) importResults.messages().stream()
                .filter(m -> m.type() == ValidationMessageType.ERROR)
                .count();
    }

    /**
     * Converts validation messages from the structured format to legacy format, organizing them by
     * message type. Each message is formatted to include line numbers, field names, and invalid
     * values where applicable.
     *
     * @param messages The list of structured validation messages to convert
     * @return A map where keys are validation message types and values are lists of formatted
     * message strings
     */
    private static Map<ValidationMessageType, List<String>> convertMessagesToLegacyFormat(
            List<ValidationMessage> messages) {

        Map<ValidationMessageType, List<String>> result = Arrays.stream(
                        ValidationMessageType.values())
                .collect(Collectors.toMap(
                        type -> type,
                        type -> new ArrayList<>()
                ));

        if (messages != null) {
            messages.forEach(message ->
                    result.computeIfAbsent(message.type(), k -> new ArrayList<>())
                            .add(formatMessage(message)));
        }

        return result;
    }

    /**
     * Formats a single validation message into a human-readable string format. The resulting string
     * includes:
     * <ul>
     *   <li>Line number (if present): "Line X: "</li>
     *   <li>Field name (if present): "Field 'X': "</li>
     *   <li>The main message</li>
     *   <li>Invalid value (if present): " (value: 'X')"</li>
     * </ul>
     *
     * @param message The validation message to format
     * @return A formatted string representation of the message
     */
    private static String formatMessage(ValidationMessage message) {
        StringBuilder sb = new StringBuilder();

        // Add line number if present
        message.lineNumber().ifPresent(line ->
                sb.append("Line #").append(line).append(": "));

        // Add main message
        sb.append(message.message());

        return sb.toString();
    }

    /**
     * Adds header validation information to the legacy results map. This includes arrays of valid,
     * invalid, and missing headers, as well as any additional validation details stored in the
     * headerInfo.
     *
     * @param headerInfo    The structured header validation information
     * @param legacyResults The legacy format results map to update
     */
    private static void addHeaderValidationToLegacy(HeaderInfo headerInfo,
            Map<String, List<String>> legacyResults) {

        if (headerInfo == null || legacyResults == null) {
            return;
        }

        // Add header arrays
        legacyResults.put("validHeaders",
                Arrays.asList(headerInfo.validHeaders() != null ? headerInfo.validHeaders()
                        : new String[0]));
        legacyResults.put("invalidHeaders",
                Arrays.asList(headerInfo.invalidHeaders() != null ? headerInfo.invalidHeaders()
                        : new String[0]));
        legacyResults.put("missingHeaders",
                Arrays.asList(headerInfo.missingHeaders() != null ? headerInfo.missingHeaders()
                        : new String[0]));

        // Add validation details
        List<String> headerValidation = new ArrayList<>();
        Map<String, String> details = headerInfo.validationDetails();
        if (details != null) {
            details.forEach((key, value) ->
                    headerValidation.add(key + "=" + value));
        }
        legacyResults.put("headerValidation", headerValidation);
    }

}