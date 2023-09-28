package com.dotcms.rendering.js;

import io.vavr.control.Try;
import org.apache.commons.io.IOUtils;
import org.graalvm.polyglot.HostAccess;

import javax.servlet.http.Part;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

/**
 * Encapsulates a Blob in a Js context, which is basically a file sent by multipart/form-data
 * @author jsanca
 */
public class JsBlob implements Serializable {

    private final Part part;

    public JsBlob(final Part part) {
        this.part = part;
    }

    @HostAccess.Export
    public InputStream getArrayBuffer() {
        return Try.of(() -> this.part.getInputStream()).getOrNull();
    }

    @HostAccess.Export
    public String getText() {
        return Try.of(() -> IOUtils.toString(this.getArrayBuffer(), StandardCharsets.UTF_8)).getOrNull();
    }

    @HostAccess.Export
    public String getType() {
        return this.part.getContentType();
    }

    @HostAccess.Export
    public String getName() {
        return this.part.getName();
    }

    @HostAccess.Export
    public String getSubmittedFileName() {
        return this.part.getSubmittedFileName();
    }

    @HostAccess.Export
    public long getSize() {
        return this.part.getSize();
    }

}
