package com.dotcms.rest.api.v1.drive.export;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * One contentlet of an export selection: an identifier in one language. It always exports the
 * working version of that identifier in that language at export time.
 */
@Value.Style(typeImmutable = "*", typeAbstract = "Abstract*")
@Value.Immutable
@JsonSerialize(as = ExportContentletRefForm.class)
@JsonDeserialize(as = ExportContentletRefForm.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface AbstractExportContentletRefForm {

    /**
     * The contentlet identifier.
     *
     * @return the identifier; blank or {@code null} makes the request invalid
     */
    @Nullable
    @JsonProperty("identifier")
    String identifier();

    /**
     * The language of the selected version.
     *
     * @return the language id; {@code 0} or less makes the request invalid
     */
    @JsonProperty("languageId")
    @Value.Default
    default long languageId() {
        return 0;
    }
}
