package com.dotcms.rest.api.v1.asset.bulkupload;

import java.io.InputStream;

/**
 * One part of the incoming multipart body, reduced to what the reader needs.
 * <p>
 * Keeps {@link BoundedMultipartReader} free of Jersey's {@code FormDataMultiPart}, so the bounding
 * and reclaim rules can be exercised without building an HTTP request.
 *
 * @author dotCMS
 */
public class UploadPart {

    private final String fileName;
    private final InputStream content;

    public UploadPart(final String fileName, final InputStream content) {
        this.fileName = fileName;
        this.content = content;
    }

    public String fileName() { return fileName; }

    public InputStream content() { return content; }
}
