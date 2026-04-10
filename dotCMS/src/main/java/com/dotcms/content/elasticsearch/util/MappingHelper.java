package com.dotcms.content.elasticsearch.util;

import com.dotcms.content.index.IndexTag;
import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.json.JSONException;
import java.io.IOException;
import java.util.List;

public interface MappingHelper {

    /**
     * Returns the singleton instance behind this interface.
     * Delegates to {@link ESMappingUtilHelper#getInstance()} for backwards compatibility.
     */
    static MappingHelper getInstance() {
        return ESMappingUtilHelper.getInstance();
    }

    /**
     * Sets a custom index mapping for all fields in a full reindex (including relationship fields
     * and field variables that define the {@code esCustomMapping} key).
     *
     * @param indexes where mapping will be applied
     */
    void addCustomMapping(String... indexes);

    /**
     * Sets an index mapping for a {@link Field} (does not include field variables) on the
     * specified indexes. Used when a new field is created.
     *
     * @param field   the field whose mapping will be set
     * @param indexes where mapping will be applied
     */
    void addCustomMapping(Field field, String... indexes)
            throws DotSecurityException, DotDataException, IOException, JSONException;

    /**
     * Targeted overload: applies the full reindex mapping exclusively to the provider
     * identified by {@code tag}, regardless of the current migration phase.
     *
     * <p>The no-tag variant {@link #addCustomMapping(String...)} fans out to all write providers.</p>
     *
     * @param indexes plain (untagged) index names where mapping will be applied
     * @param tag     the target vendor ({@link IndexTag#ES} or {@link IndexTag#OS})
     */
    void addCustomMapping(List<String> indexes, IndexTag tag);

    /**
     * Targeted overload: applies the field-level mapping exclusively to the provider
     * identified by {@code tag}, regardless of the current migration phase.
     *
     * @param field   the field whose mapping will be set
     * @param indexes plain (untagged) index names where mapping will be applied
     * @param tag     the target vendor ({@link IndexTag#ES} or {@link IndexTag#OS})
     */
    void addCustomMapping(Field field, List<String> indexes, IndexTag tag)
            throws DotSecurityException, DotDataException, IOException, JSONException;
}
