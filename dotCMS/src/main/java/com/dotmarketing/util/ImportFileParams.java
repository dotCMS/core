package com.dotmarketing.util;

import com.dotcms.repackage.com.csvreader.CsvReader;
import com.liferay.portal.model.User;
import java.util.function.LongConsumer;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import org.immutables.value.Value;

/**
 * Abstract base class for file import parameters.
 * This class will be processed by the @Value.Immutable annotation to generate
 * an immutable implementation with a Builder pattern.
 */
@Value.Immutable
@Value.Style(
        // Style configuration for code generation
        builderVisibility = Value.Style.BuilderVisibility.PUBLIC, // Public builder
        allParameters = true, // Constructor with all parameters
        defaults = @Value.Immutable(copy = false) // Don't generate copy methods
)
public abstract class ImportFileParams {

    /**
     * @return Import ID
     */
    @Value.Parameter
    public abstract Long importId();

    /**
     * @return Current site ID
     */
    @Value.Parameter
    public abstract String siteId();

    /**
     * @return Content type inode
     */
    @Value.Parameter
    public abstract String contentTypeInode();

    /**
     * @return array of key fields
     */
    @Value.Parameter
    public abstract String[] keyFields();

    /**
     * @return true if preview mode, false otherwise
     */
    @Value.Parameter
    @Value.Default
    public boolean preview() {
        return false;
    }

    /**
     * @return true if multilingual, false otherwise
     */
    @Value.Parameter
    @Value.Default
    public boolean isMultilingual() {
        return false;
    }

    /**
     * @return User performing the import
     */
    @Value.Parameter
    public abstract User user();

    /**
     * @return Language ID
     */
    @Value.Parameter
    @Value.Default
    public long language() {
        return 1; // Default value
    }

    /**
     * @return array of CSV headers
     */
    @Nullable
    @Value.Parameter
    public abstract String[] csvHeaders();

    /**
     * @return CSV reader
     */
    @Value.Parameter
    public abstract CsvReader csvReader();

    /**
     * @return Index of the header column for language code
     */
    @Value.Parameter
    @Value.Default
    public int languageCodeHeaderColumn() {
        return -1;
    }

    /**
     * @return Index of the header column for country code
     */
    @Value.Parameter
    @Value.Default
    public int countryCodeHeaderColumn() {
        return -1;
    }

    /**
     * @return Workflow action ID
     */
    @Nullable
    @Value.Parameter
    public abstract String workflowActionId();

    /**
     * @return Total number of lines in the file
     */
    @Value.Parameter
    @Value.Default
    public long fileTotalLines() {
        return 0; // Default value
    }

    /**
     * @return HTTP request
     */
    @Value.Parameter
    public abstract HttpServletRequest request();

    /**
     * @return Callback to report progress
     */
    @Value.Parameter
    @Value.Default
    public LongConsumer progressCallback() {
        return progress -> {}; // Empty callback by default
    }

    /**
     * @return true if should stop on error, false otherwise
     */
    @Value.Parameter
    @Value.Default
    public boolean stopOnError() {
        return false;
    }

    /**
     * Custom commit granularity override.
     * @return Commit granularity override
     */
    @Value.Parameter
    @Value.Default
    public int commitGranularityOverride() {
        return ImportUtil.COMMIT_GRANULARITY;
    }

    @Value.Check
    protected void validate() {
        final int granularityOverride = commitGranularityOverride();
        if (granularityOverride <= 0) {
            throw new IllegalStateException("Commit Granularity must be positive, got: " + granularityOverride);
        }
    }
}
