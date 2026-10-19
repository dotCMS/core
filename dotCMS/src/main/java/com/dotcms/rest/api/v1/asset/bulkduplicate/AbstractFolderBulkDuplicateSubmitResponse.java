package com.dotcms.rest.api.v1.asset.bulkduplicate;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

/**
 * What {@code POST /v1/assets/folders/_bulkduplicate} answers with {@code 202}: the run's handle,
 * before any folder is duplicated (#37062, contract §2).
 *
 * @author dotCMS
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = FolderBulkDuplicateSubmitResponse.class)
@JsonDeserialize(as = FolderBulkDuplicateSubmitResponse.class)
public interface AbstractFolderBulkDuplicateSubmitResponse {

    /**
     * The run's handle. Everything the client can do afterwards is addressed by it.
     *
     * @return the job id
     */
    String jobId();

    /**
     * Where to follow, cancel and read the outcome, ready to use.
     *
     * @return the job's status address
     */
    String statusUrl();

    /**
     * How many distinct folders the server accepted into the run, which the outcome's total later
     * equals.
     *
     * @return the number of distinct folders
     */
    int submitted();
}
