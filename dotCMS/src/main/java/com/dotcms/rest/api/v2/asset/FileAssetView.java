package com.dotcms.rest.api.v2.asset;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Lightweight view returned by write operations (save/publish) in the v2 file-asset endpoint.
 * Contains the persisted asset's key metadata so callers can verify what was stored.
 */
@Schema(description = "File asset view returned after a save or publish operation")
public class FileAssetView {

    @Schema(description = "Asset identifier", example = "48190c8c-42c4-46af-8d1a-0cd5db894797")
    private final String identifier;

    @Schema(description = "Asset inode", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private final String inode;

    @Schema(description = "File name", example = "banner.vtl")
    private final String name;

    @Schema(description = "Host-qualified path to the asset", example = "//demo.dotcms.com/application/containers/default/banner.vtl")
    private final String path;

    @Schema(description = "Language tag for the asset version", example = "en-US")
    private final String lang;

    @Schema(description = "Whether this is the live (published) version", example = "false")
    private final boolean live;

    @Schema(description = "Whether this is the working version", example = "true")
    private final boolean working;

    @Schema(description = "File size in bytes as stored in the content repository", example = "4096")
    private final long fileSize;

    @JsonCreator
    public FileAssetView(
            @JsonProperty("identifier") final String identifier,
            @JsonProperty("inode")       final String inode,
            @JsonProperty("name")        final String name,
            @JsonProperty("path")        final String path,
            @JsonProperty("lang")        final String lang,
            @JsonProperty("live")        final boolean live,
            @JsonProperty("working")     final boolean working,
            @JsonProperty("fileSize")    final long fileSize) {
        this.identifier = identifier;
        this.inode      = inode;
        this.name       = name;
        this.path       = path;
        this.lang       = lang;
        this.live       = live;
        this.working    = working;
        this.fileSize   = fileSize;
    }

    public String getIdentifier() { return identifier; }
    public String getInode()      { return inode; }
    public String getName()       { return name; }
    public String getPath()       { return path; }
    public String getLang()       { return lang; }
    public boolean isLive()       { return live; }
    public boolean isWorking()    { return working; }
    public long getFileSize()     { return fileSize; }
}
