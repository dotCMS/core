package com.dotcms.rest.api.v1.asset.bulkupload;

import com.dotcms.rest.api.Validated;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * The JSON {@code form} part of a bulk-upload submission — everything about the batch that is not
 * a file (#37166, contracts §1).
 * <p>
 * Shaped as {@code files} parts plus one JSON {@code form} part because that is how content import
 * already shapes its own multipart submission ({@code @FormDataParam("file")} plus
 * {@code @FormDataParam("form")}), and following the shipped precedent is the whole argument for
 * this endpoint's existence.
 *
 * @author dotCMS
 */
public class BulkUploadForm extends Validated {

    /** The only two base types that carry a binary. Anything else has nowhere to put the file. */
    private static final String SUPPORTED_BASE_TYPES = "DOTASSET|FILEASSET";

    @NotNull(message = "baseType is required and must be DOTASSET or FILEASSET")
    @Pattern(regexp = SUPPORTED_BASE_TYPES,
            message = "baseType must be DOTASSET or FILEASSET")
    private final String baseType;
    private final String folderId;
    private final String siteId;
    @Min(value = 0, message = "totalSizeBytes cannot be negative")
    private final Long totalSizeBytes;

    @JsonCreator
    public BulkUploadForm(
            @JsonProperty("baseType") final String baseType,
            @JsonProperty("folderId") final String folderId,
            @JsonProperty("siteId") final String siteId,
            @JsonProperty("totalSizeBytes") final Long totalSizeBytes) {
        super();
        this.baseType = baseType;
        this.folderId = folderId;
        this.siteId = siteId;
        this.totalSizeBytes = totalSizeBytes;
        this.checkValid();
    }

    /**
     * Exactly one target, never neither and never both.
     * <p>
     * <b>Both is refused rather than resolved by precedence.</b> A caller that sends a folder and a
     * site has said two contradictory things, and picking one would silently put an author's files
     * somewhere they did not choose. This is also why the contract carries two explicit fields
     * instead of one overloaded string: the single {@code hostFolder} the workflow API exposes
     * takes a folder id or, at the root, a site id, and that overloading — which ADR-0020 moves
     * away from — is precisely what makes the ambiguity expressible.
     */
    @AssertTrue(message = "Exactly one of folderId or siteId is required")
    @JsonIgnore
    public boolean isExactlyOneTargetGiven() {
        return UtilMethods.isSet(folderId) ^ UtilMethods.isSet(siteId);
    }

    public String getBaseType() { return baseType; }

    /** Target folder. Exactly one of this and {@link #getSiteId()} is set. */
    public String getFolderId() { return folderId; }

    /** Target site, for an upload at the site root. */
    public String getSiteId() { return siteId; }

    /**
     * A total the caller declared, used only for a fast refusal before the body is read — a
     * courtesy so an author is not made to upload gigabytes before being told no. <b>Never the
     * enforcement point</b> (spec FR-013c.1): a caller can under-declare or omit it, and the
     * authoritative total is accumulated while reading.
     */
    public Long getTotalSizeBytes() { return totalSizeBytes; }
}
