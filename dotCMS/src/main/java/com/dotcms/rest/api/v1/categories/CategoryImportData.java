package com.dotcms.rest.api.v1.categories;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import java.io.InputStream;

/**
 * Runtime binding bean for multipart form parts.
 */
@Schema(name = "CategoryImportFormSchema", description = "Category import form data with CSV file and processing parameters")
public class CategoryImportData {

    private InputStream fileInputStream;
    private FormDataContentDisposition fileDetail;
    private String filter;
    private String exportType;
    private String contextInode;

    @FormDataParam("file")
    @JsonProperty("file")
    @Schema(name = "file", description = "CSV file containing categories to import", type = "string", format = "binary")
    public void setFileInputStream(final InputStream inputStream) { this.fileInputStream = inputStream; }

    @FormDataParam("file")
    @Schema(hidden = true)
    public void setFileDetail(final FormDataContentDisposition detail) { this.fileDetail = detail; }

    @FormDataParam("filter")
    @Schema(description = "Filter pattern for categories")
    public void setFilter(final String filter) { this.filter = filter; }

    @FormDataParam("exportType")
    @Schema(description = "Import behavior", allowableValues = {"replace", "merge"})
    public void setExportType(final String exportType) { this.exportType = exportType; }

    @FormDataParam("contextInode")
    @Schema(description = "Context category inode to import into")
    public void setContextInode(final String contextInode) { this.contextInode = contextInode; }

    public InputStream getFileInputStream() { return fileInputStream; }
    public FormDataContentDisposition getFileDetail() { return fileDetail; }
    public String getFilter() { return filter; }
    public String getExportType() { return exportType; }
    public String getContextInode() { return contextInode; }
}

