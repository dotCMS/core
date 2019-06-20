package com.dotcms.rest.api.v1.temp;

import java.io.File;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DotTempFile {

  public final String id,mimeType,referenceUrl,thumbnailUrl,fileName;
  public final boolean image;

  @JsonIgnore
  public final File file;
  
  public DotTempFile(final String id, final File file) {
    super();
    this.id = id;
    this.file = file;
    this.mimeType=APILocator.getFileAssetAPI().getMimeType(file.getName());
    this.image = UtilMethods.isImage(file.getName());
    this.referenceUrl = this.image ? "/contentAsset/image/" + id + "/tmp/" + file.getName() : "/contentAsset/data/" + id + "/fileAsset/" + file.getName() ;
    this.thumbnailUrl = this.image ? "/contentAsset/image/" + id + "/tmp/filter/Thumbnail/thumbnail_w/250/thumbnail_h/250/" +  file.getName() :null;
    this.fileName = file.getName();
  }
  @JsonProperty("length")
  public long length() {
    return file.length();
  }
  
  @Override
  public String toString() {
    return "DotTempFile [id=" + id + ", mimeType=" + mimeType + ", referenceUrl=" + referenceUrl + ", thumbnailUrl=" + thumbnailUrl
        + ", image=" + image + ", file=" + file + "]";
  }
  
}
