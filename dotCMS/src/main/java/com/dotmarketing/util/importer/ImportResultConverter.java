package com.dotmarketing.util.importer;

import static com.dotmarketing.util.ImportUtil.KEY_COUNTERS;
import static com.dotmarketing.util.ImportUtil.KEY_ERRORS;
import static com.dotmarketing.util.ImportUtil.KEY_IDENTIFIERS;
import static com.dotmarketing.util.ImportUtil.KEY_LAST_INODE;
import static com.dotmarketing.util.ImportUtil.KEY_MESSAGES;
import static com.dotmarketing.util.ImportUtil.KEY_RESULTS;
import static com.dotmarketing.util.ImportUtil.KEY_UPDATED_INODES;
import static com.dotmarketing.util.ImportUtil.KEY_WARNINGS;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.ImportUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.importer.model.AbstractImportResult.OperationType;
import com.dotmarketing.util.importer.model.HeaderInfo;
import com.dotmarketing.util.importer.model.ImportResult;
import com.dotmarketing.util.importer.model.SpecialHeaderInfo;
import com.dotmarketing.util.importer.model.ValidationMessage;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The ImportResultConverter class provides utility methods for converting
 * import results into a legacy format. This class is not intended to be instantiated
 * and only contains static methods.
 */
public class ImportResultConverter {

    private ImportResultConverter() {
        // Utility class, do not instantiate
    }

    /**
     * Converts the provided import result into a legacy format representation. This method
     * processes the import results and formats them into a structure compatible with legacy systems
     * by categorizing messages, errors, warnings, counters, and results.
     *
     * @param result the ImportResult object containing processed data, messages, and statistics. It
     *               must not be null.
     * @param user   the User object required for language-specific message formatting. It is used
     *               to retrieve localized messages and translations.
     * @return a HashMap containing the legacy format representation of the import result. Keys
     * include warnings, errors, messages, counters, results, identifiers, and other categorized
     * data.
     * @throws IllegalArgumentException if the provided result is null.
     */
    public static HashMap<String, List<String>> toLegacyFormat(final ImportResult result,
            final User user) {

        if (result == null) {
            throw new IllegalArgumentException("Import result cannot be null");
        }

        try {
            final var preview = result.type().equals(OperationType.PREVIEW);

            final HashMap<String, List<String>> legacyResults = new HashMap<>();
            legacyResults.put(KEY_WARNINGS, new ArrayList<>());
            legacyResults.put(KEY_ERRORS, new ArrayList<>());
            legacyResults.put(KEY_MESSAGES, new ArrayList<>());
            legacyResults.put(KEY_RESULTS, new ArrayList<>());
            legacyResults.put(KEY_COUNTERS, new ArrayList<>());
            legacyResults.put(KEY_IDENTIFIERS, new ArrayList<>());
            legacyResults.put(KEY_UPDATED_INODES, new ArrayList<>());
            legacyResults.put(KEY_LAST_INODE, new ArrayList<>());
            legacyResults.put(Contentlet.WORKFLOW_ACTION_KEY, new ArrayList<>());

            List<String> lastInode = legacyResults.get(KEY_LAST_INODE);
            if (result.lastInode().isPresent()) {
                lastInode.add(result.lastInode().get());
            }

            // Convert messages to legacy format by type
            for (ValidationMessage message : result.info()) {
                if (message.message().contains("repeated content based on the key provided")) {
                    continue;
                }
                String formattedMessage = formatMessage(message);
                legacyResults.get(KEY_MESSAGES).add(formattedMessage);
            }
            for (ValidationMessage message : result.warning()) {
                String formattedMessage = formatMessage(message);
                legacyResults.get(KEY_WARNINGS).add(formattedMessage);
            }
            for (ValidationMessage message : result.error()) {
                String formattedMessage = formatMessage(message);
                legacyResults.get(KEY_ERRORS).add(formattedMessage);
            }

            final var fileInfo = result.fileInfo().orElseThrow(() ->
                    new IllegalArgumentException("File info cannot be null"));

            final var data = result.data().orElseThrow(() ->
                    new IllegalArgumentException("Result data cannot be null"));

            // Process header info
            final HeaderInfo headerInfo = fileInfo.headerInfo();
            if (headerInfo != null) {
                // Add special headers (Identifier, Workflow Action)
                for (SpecialHeaderInfo specialHeader : headerInfo.specialHeaders()) {
                    if (specialHeader.header().equalsIgnoreCase(ImportUtil.identifierHeader)) {
                        legacyResults.get(KEY_IDENTIFIERS)
                                .add(String.valueOf(specialHeader.columnIndex()));
                    } else if (specialHeader.header()
                            .equalsIgnoreCase(Contentlet.WORKFLOW_ACTION_KEY)) {
                        legacyResults.get(Contentlet.WORKFLOW_ACTION_KEY)
                                .add(String.valueOf(specialHeader.columnIndex()));
                    }
                }
            }

            // Add import statistics
            if (!preview) {
                List<String> counters = legacyResults.get(KEY_COUNTERS);
                counters.add("linesread=" + data.processed().parsedRows());
                counters.add("errors=" + data.processed().failedRows());
                counters.add("newContent=" + data.summary().toCreateContent());
                counters.add("contentToUpdate=" + data.summary().updatedContent());
            }

            // Add result messages
            List<String> results = legacyResults.get(KEY_RESULTS);
            results.add(data.summary().createdContent() + " " + LanguageUtil.get(user, "new")
                    + " " + "\"" + result.contentTypeName() + "\" " + LanguageUtil.get(
                    user, "were-created"));
            results.add(data.summary().duplicateContent() + " \""
                    + result.contentTypeName() + "\" " + LanguageUtil.get(user,
                    "contentlets-updated-corresponding-to") + " "
                    + data.summary().updatedContent() + " " + LanguageUtil.get(user,
                    "repeated-contents-based-on-the-key-provided"));

            // Add error messages
            List<String> errorMessages = legacyResults.get(KEY_ERRORS);
            int errorCount = data.processed().failedRows();
            if (errorCount > 0) {
                errorMessages.add(errorCount + " " + LanguageUtil.get(user,
                        "input-lines-had-errors"));
            }

            // Add the count summaries
            List<String> messages = legacyResults.get(KEY_MESSAGES);

            if (!fileInfo.headerInfo().specialHeaders().isEmpty()) {
                for (SpecialHeaderInfo specialHeader : fileInfo.headerInfo()
                        .specialHeaders()) {
                    if (specialHeader.header().equalsIgnoreCase(ImportUtil.identifierHeader)) {
                        messages.add(LanguageUtil.get(user,
                                "identifier-field-found-in-import-contentlet-csv-file"));
                    }
                }
            }

            if (fileInfo.headerInfo().context().containsKey("importableFields")) {
                final int headersSize = fileInfo.headerInfo().validHeaders().length;
                final int importableFields = (int) fileInfo.headerInfo().context()
                        .get("importableFields");
                if (importableFields == headersSize) {
                    messages.add(
                            LanguageUtil.get(user, headersSize + " " + LanguageUtil.get(user,
                                    "headers-match-these-will-be-imported")));
                }
            }

            messages.add(data.processed().parsedRows() + " " +
                    LanguageUtil.get(user, "lines-of-data-were-read"));

            if (preview && !result.keyFields().isEmpty()) {
                messages.add(
                        LanguageUtil.get(user,
                                "Fields-selected-as-key") + ": " +
                                String.join(", ", result.keyFields()) + ".");
            }

            if (data.summary().toCreateContent() > 0) {
                messages.add(LanguageUtil.get(user, "Attempting-to-create") + " "
                        + (data.summary().toCreateContent()) + " contentlets - "
                        + LanguageUtil.get(user, "check-below-for-errors"));
            }

            if (data.summary().toUpdateContent() > 0) {
                messages.add(LanguageUtil.get(user, "Approximately") + " "
                        + (data.summary().toUpdateContent()) + " " + LanguageUtil.get(
                        user, "old-content-will-be-updated"));
            }

            return legacyResults;
        } catch (LanguageException e) {
            Logger.error(ImportResultConverter.class, String.format("Error converting import " +
                    "result to legacy format: %s", e.getMessage()), e);
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Formats a ValidationMessage into a human-readable string representation.
     *
     * @param message the ValidationMessage object containing details such as line number, message
     *                text, field, and invalid value to be formatted.
     * @return a formatted string representation of the ValidationMessage including line number,
     * message, field, and invalid value if present.
     */
    private static String formatMessage(ValidationMessage message) {

        StringBuilder sb = new StringBuilder();

        if (message.type().equals(ValidationMessage.ValidationMessageType.ERROR) ||
                message.type().equals(ValidationMessage.ValidationMessageType.WARNING)) {
            message.lineNumber().ifPresent(line ->
                    sb.append("Line #").append(line).append(": "));
        }

        sb.append(message.message());

        return sb.toString();
    }

}