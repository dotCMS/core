package com.dotcms.content.opensearch.business;

import com.dotcms.content.opensearch.util.OpenSearchDefaultClientProvider;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.reindex.ReindexEntry;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.Time;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.opensearch.client.opensearch.core.bulk.DeleteOperation;
import org.opensearch.client.opensearch.core.bulk.IndexOperation;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helper class for handling OpenSearch bulk operations using ReindexEntry objects.
 * This class provides a convenient interface to register documents for bulk processing
 * and execute batch operations for both indexing and deletion.
 *
 * Usage pattern:
 * 1. Create instance
 * 2. Register documents using addToQueue()
 * 3. Execute bulk operation using executeBulk()
 * 4. Handle results and cleanup using clear()
 *
 * @author fabrizio
 */
@ApplicationScoped
@Default
public class OpenSearchBulkHelper {

    private static final int DEFAULT_BULK_TIMEOUT_MS = Config.getIntProperty("OPENSEARCH_BULK_TIMEOUT", 30000);
    private static final int DEFAULT_BATCH_SIZE = Config.getIntProperty("OPENSEARCH_BULK_BATCH_SIZE", 100);
    private static final String DEFAULT_INDEX_SUFFIX = Config.getStringProperty("ES_INDEX_NAME", "content");

    @Inject
    private OpenSearchDefaultClientProvider clientProvider;

    // Thread-safe collections for bulk operations
    private final List<BulkOperation> operations = new ArrayList<>();
    private final Map<String, ReindexEntry> entryMap = new ConcurrentHashMap<>();
    private final AtomicInteger operationCount = new AtomicInteger(0);

    /**
     * Registers a ReindexEntry for bulk processing.
     * The operation type (index/delete) is determined by the ReindexEntry.isDelete() flag.
     *
     * @param entry The ReindexEntry to process
     * @throws DotDataException if there's an error preparing the operation
     */
    public void addToQueue(ReindexEntry entry) throws DotDataException {
        if (entry == null || !UtilMethods.isSet(entry.getIdentToIndex())) {
            Logger.warn(this, "Invalid ReindexEntry provided - skipping");
            return;
        }

        try {
            BulkOperation operation = createBulkOperation(entry);
            synchronized (operations) {
                operations.add(operation);
                entryMap.put(entry.getIdentToIndex(), entry);
                operationCount.incrementAndGet();
            }

            Logger.debug(this, () -> String.format("Added %s operation for identifier: %s",
                entry.isDelete() ? "DELETE" : "INDEX", entry.getIdentToIndex()));

        } catch (Exception e) {
            Logger.error(this, "Error adding ReindexEntry to bulk queue: " + e.getMessage(), e);
            throw new DotDataException("Failed to add entry to bulk queue", e);
        }
    }

    /**
     * Registers multiple ReindexEntries for bulk processing.
     *
     * @param entries List of ReindexEntry objects to process
     * @throws DotDataException if there's an error preparing any operation
     */
    public void addToQueue(List<ReindexEntry> entries) throws DotDataException {
        if (entries == null || entries.isEmpty()) {
            Logger.debug(this, "No entries provided to add to bulk queue");
            return;
        }

        for (ReindexEntry entry : entries) {
            addToQueue(entry);
        }
    }

    /**
     * Executes the bulk operation with all registered documents.
     *
     * @return BulkOperationResult containing success/failure information
     * @throws DotDataException if the bulk operation fails
     */
    public BulkOperationResult executeBulk() throws DotDataException {
        if (operations.isEmpty()) {
            Logger.debug(this, "No operations to execute");
            return new BulkOperationResult(0, 0, new ArrayList<>(), new ArrayList<>());
        }

        try {
            Logger.info(this, String.format("Executing bulk operation with %d operations", operationCount.get()));

            BulkRequest.Builder requestBuilder = new BulkRequest.Builder()
                .operations(new ArrayList<>(operations))
                .timeout(Time.of(t -> t.time(String.valueOf(DEFAULT_BULK_TIMEOUT_MS) + "ms")));

            OpenSearchClient client = clientProvider.getClient();
            BulkResponse response = client.bulk(requestBuilder.build());

            return processBulkResponse(response);

        } catch (IOException e) {
            Logger.error(this, "OpenSearch bulk operation failed: " + e.getMessage(), e);
            throw new DotDataException("Bulk operation execution failed", e);
        } catch (Exception e) {
            Logger.error(this, "Unexpected error during bulk operation: " + e.getMessage(), e);
            throw new DotRuntimeException("Unexpected bulk operation error", e);
        }
    }

    /**
     * Returns the current number of operations in the queue.
     *
     * @return number of pending operations
     */
    public int getQueueSize() {
        return operationCount.get();
    }

    /**
     * Checks if the queue has reached the configured batch size.
     *
     * @return true if queue size >= batch size
     */
    public boolean isBatchReady() {
        return getQueueSize() >= DEFAULT_BATCH_SIZE;
    }

    /**
     * Clears all registered operations and resets internal state.
     * Should be called after successful bulk execution or for cleanup.
     */
    public void clear() {
        synchronized (operations) {
            operations.clear();
            entryMap.clear();
            operationCount.set(0);
        }
        Logger.debug(this, "Bulk operation queue cleared");
    }

    /**
     * Creates a BulkOperation from a ReindexEntry.
     * Determines operation type based on the entry's delete flag.
     */
    private BulkOperation createBulkOperation(ReindexEntry entry) throws DotDataException {
        String identifier = entry.getIdentToIndex();

        if (entry.isDelete()) {
            return createDeleteOperation(identifier);
        } else {
            return createIndexOperation(entry);
        }
    }

    /**
     * Creates a delete operation for the given identifier.
     */
    private BulkOperation createDeleteOperation(String identifier) {
        return BulkOperation.of(op -> op.delete(DeleteOperation.of(del -> del
            .index(getIndexName())
            .id(identifier)
        )));
    }

    /**
     * Creates an index operation for the given ReindexEntry.
     * Retrieves the contentlet and converts it to the document format.
     */
    private BulkOperation createIndexOperation(ReindexEntry entry) throws DotDataException {
        String identifier = entry.getIdentToIndex();

        try {
            // Retrieve the contentlet for indexing
            Contentlet contentlet = APILocator.getContentletAPI()
                .findContentletByIdentifier(identifier, false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                    APILocator.systemUser(), false);

            if (contentlet == null || !UtilMethods.isSet(contentlet.getIdentifier())) {
                throw new DotDataException("Contentlet not found for identifier: " + identifier);
            }

            // Convert contentlet to indexable document
            Map<String, Object> document = createDocumentFromContentlet(contentlet);

            return BulkOperation.of(op -> op.index(IndexOperation.of(idx -> idx
                .index(getIndexName())
                .id(identifier)
                .document(document)
            )));

        } catch (Exception e) {
            Logger.error(this, "Error creating index operation for identifier: " + identifier, e);
            throw new DotDataException("Failed to create index operation", e);
        }
    }

    /**
     * Converts a Contentlet to a Map for OpenSearch indexing.
     * Uses the existing ContentletIndexAPI transformation logic.
     */
    private Map<String, Object> createDocumentFromContentlet(Contentlet contentlet) throws DotDataException {
        try {
            return Map.of();
            /*
                    APILocator.getContentletIndexAPI()
                .toMap(contentlet, IndexPolicy.WAIT_FOR, false);
             */
        } catch (Exception e) {
            Logger.error(this, "Error converting contentlet to document: " + e.getMessage(), e);
            throw new DotDataException("Failed to convert contentlet to document", e);
        }
    }

    /**
     * Processes the BulkResponse and creates a result object with success/failure details.
     */
    private BulkOperationResult processBulkResponse(BulkResponse response) {
        List<String> successful = new ArrayList<>();
        List<BulkOperationError> errors = new ArrayList<>();

        int successCount = 0;
        int errorCount = 0;

        for (BulkResponseItem item : response.items()) {
            if (item.error() != null) {
                errorCount++;
                String identifier = item.id();
                ReindexEntry entry = entryMap.get(identifier);

                errors.add(new BulkOperationError(
                    identifier,
                    item.error().type(),
                    item.error().reason(),
                    entry
                ));

                Logger.error(this, String.format("Bulk operation failed for %s: %s - %s",
                    identifier, item.error().type(), item.error().reason()));
            } else {
                successCount++;
                successful.add(item.id());
                Logger.debug(this, () -> "Bulk operation successful for: " + item.id());
            }
        }

        Logger.info(this, String.format("Bulk operation completed - Success: %d, Errors: %d",
            successCount, errorCount));

        return new BulkOperationResult(successCount, errorCount, successful, errors);
    }

    /**
     * Gets the current index name.
     * This should be enhanced to use the proper index resolution logic.
     */
    private String getIndexName() {
        // TODO: Use proper index resolution logic from ContentletIndexAPI
        return Try.of(() -> APILocator.getContentletIndexAPI().getActiveIndexName(DEFAULT_INDEX_SUFFIX))
            .getOrElse(() -> {
                Logger.warn(this, "Could not resolve active index name, using default");
                return "dotcms_" + DEFAULT_INDEX_SUFFIX;
            });
    }

    /**
     * Result class containing bulk operation execution details.
     */
    public static class BulkOperationResult {
        private final int successCount;
        private final int errorCount;
        private final List<String> successful;
        private final List<BulkOperationError> errors;

        public BulkOperationResult(int successCount, int errorCount,
                                 List<String> successful, List<BulkOperationError> errors) {
            this.successCount = successCount;
            this.errorCount = errorCount;
            this.successful = successful;
            this.errors = errors;
        }

        public int getSuccessCount() { return successCount; }
        public int getErrorCount() { return errorCount; }
        public List<String> getSuccessful() { return successful; }
        public List<BulkOperationError> getErrors() { return errors; }
        public boolean hasErrors() { return errorCount > 0; }
        public int getTotalOperations() { return successCount + errorCount; }
    }

    /**
     * Error information for failed bulk operations.
     */
    public static class BulkOperationError {
        private final String identifier;
        private final String errorType;
        private final String errorReason;
        private final ReindexEntry entry;

        public BulkOperationError(String identifier, String errorType, String errorReason, ReindexEntry entry) {
            this.identifier = identifier;
            this.errorType = errorType;
            this.errorReason = errorReason;
            this.entry = entry;
        }

        public String getIdentifier() { return identifier; }
        public String getErrorType() { return errorType; }
        public String getErrorReason() { return errorReason; }
        public ReindexEntry getEntry() { return entry; }
    }
}