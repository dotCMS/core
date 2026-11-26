package com.dotcms.rest.api.v1.drive.export;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
import org.immutables.value.Value;

/**
 * The folders and contentlets selected in Content Drive for an export.
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = ExportSelectionForm.class)
@JsonDeserialize(as = ExportSelectionForm.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractExportSelectionForm {

    /**
     * Site-qualified folder paths, {@code //host/path/}, as Drive sends them for bulk duplicate.
     * Each folder brings every contentlet below it.
     *
     * @return the selected folders; empty when none
     */
    @JsonProperty("folders")
    List<String> folders();

    /**
     * The selected contentlets, each in the language that was selected.
     *
     * @return the selected contentlets; empty when none
     */
    @JsonProperty("contentlets")
    List<ExportContentletRefForm> contentlets();
}
