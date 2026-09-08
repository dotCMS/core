package com.dotcms.rest.api.v1.asset.bulkupload;

/**
 * One file the submission placed on the staging layer, and what staging reported about it.
 * <p>
 * {@code sizeBytes} and {@code mimeType} are <b>measured and resolved facts</b>, not values a
 * caller declared (spec FR-013). That is what lets the batch total be accumulated during the read
 * and lets a type rejection later name a media type rather than infer one from a validation
 * exception whose wording differs only by a translated string (research R4).
 *
 * @author dotCMS
 */
public class StagedPart {

    private final String tempFileId;
    private final String fileName;
    private final long sizeBytes;
    private final String mimeType;

    public StagedPart(final String tempFileId, final String fileName,
                      final long sizeBytes, final String mimeType) {
        this.tempFileId = tempFileId;
        this.fileName = fileName;
        this.sizeBytes = sizeBytes;
        this.mimeType = mimeType;
    }

    public String tempFileId() { return tempFileId; }

    public String fileName() { return fileName; }

    public long sizeBytes() { return sizeBytes; }

    public String mimeType() { return mimeType; }
}
