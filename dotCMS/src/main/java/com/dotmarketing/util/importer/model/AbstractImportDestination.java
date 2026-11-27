package com.dotmarketing.util.importer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import org.immutables.value.Value;

/**
 * How many rows of an import land in one site/folder, and why they land there (#37896).
 * <p>
 * Computed with the same placement the import itself applies, so a preview tells the user where
 * content will go before anything is written. Rows with errors are not counted.
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonDeserialize(as = ImportDestination.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractImportDestination extends Serializable {

    /**
     * The site and folder, as {@code //hostname/folder/path/}; a folder of {@code /} is the site
     * root.
     *
     * @return the destination path
     */
    String path();

    /**
     * The number of rows placed there.
     *
     * @return the row count
     */
    int rows();

    /**
     * Why the rows land there.
     *
     * @return the source of the location
     */
    Source source();

    /** Why a row lands where it does. */
    enum Source {
        /** The row's own site/folder column. */
        CSV,
        /** New content in a CSV with no site/folder column: the content type's own site/folder. */
        CONTENT_TYPE_DEFAULT,
        /** Existing content matched by key or identifier, in a CSV with no site/folder column. */
        EXISTING
    }
}
