package com.dotcms.rest.api.v1.drive.export;

import com.dotcms.rest.api.v1.drive.DriveRequestForm;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * Request body of {@code POST /api/v1/drive/export}.
 * <p>
 * A request with a non-empty {@code selection} exports that selection; otherwise a request with a
 * {@code query} exports everything the Drive listing shows for that query, which must name exactly
 * one content type. The selection wins when both are sent.
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = DriveExportRequestForm.class)
@JsonDeserialize(as = DriveExportRequestForm.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractDriveExportRequestForm {

    /** The most folders and contentlets one selection can hold, together. */
    int MAX_SELECTION_ITEMS = 60;

    /**
     * Which of the two exports a request asks for.
     */
    enum Mode {
        /** Everything the listing shows for one content type: one CSV. */
        FILTER,
        /** The selected folders and contentlets: a zip with one CSV per content type. */
        SELECTION
    }

    /**
     * The identifier of the site being browsed. Required in selection mode, ignored in filter mode,
     * where the site comes from the query's asset path.
     *
     * @return the site identifier, or {@code null}
     */
    @Nullable
    @JsonProperty("siteId")
    String siteId();

    /**
     * The selected folders and contentlets.
     *
     * @return the selection, or {@code null} when nothing is selected
     */
    @Nullable
    @JsonProperty("selection")
    ExportSelectionForm selection();

    /**
     * The listing request, exactly as Drive sends it to {@code /api/v1/drive/search}. Paging,
     * sorting, {@code showFolders}, {@code showLinks} and {@code live} are ignored.
     *
     * @return the listing query, or {@code null}
     */
    @Nullable
    @JsonProperty("query")
    DriveRequestForm query();

    /**
     * Checks the request and tells which export it asks for.
     *
     * @return {@link Mode#SELECTION} when the selection has at least one entry, otherwise
     *         {@link Mode#FILTER} when a query is present
     * @throws com.dotcms.rest.exception.BadRequestException when the request asks for neither, the
     *         selection holds more than {@link #MAX_SELECTION_ITEMS} entries, or a contentlet entry
     *         has no identifier or no valid language
     */
    default Mode validate() {
        throw new UnsupportedOperationException("not implemented");
    }
}
