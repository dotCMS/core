package com.dotcms.rest.api.v1.temp;

import java.io.File;

import com.dotcms.util.MimeTypeUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DotTempFile {

  public final String id,mimeType,referenceUrl,thumbnailUrl,fileName,folder;
  public final boolean image;

  @JsonIgnore
  public final File file;
  
  public DotTempFile(final String id, final File file) {
    super();
    this.id = id;
    this.file = file;
    this.mimeType= getMimeType(file);
    this.image = UtilMethods.isImage(file.getName());
    this.referenceUrl = "/dA/" + id + "/tmp/" + file.getName() ;
    this.thumbnailUrl = this.image ? "/contentAsset/image/" + id + "/tmp/filter/Thumbnail/thumbnail_w/250/thumbnail_h/250/" +  file.getName() :null;
    this.fileName = file.getName();
    this.folder = resolveFolder();
  }

  private String getMimeType(final File file) {

    String mimeType = APILocator.getFileAssetAPI().getMimeType(file.getName());
    if (FileAsset.UNKNOWN_MIME_TYPE.equals(mimeType)) {

      mimeType = MimeTypeUtils.getMimeType(file);
    }

    return mimeType;
  }

  @JsonProperty("length")
  public long length() {
    return file.length();
  }
  
  @Override
  public String toString() {
    return "DotTempFile [id=" + id + ", mimeType=" + mimeType + ", referenceUrl=" + referenceUrl + ", thumbnailUrl=" + thumbnailUrl
        + ", image=" + image + ", file=" + file + ", folder=" + folder + "]";
  }
  
  private String resolveFolder() {
    final int begin = file.getPath().indexOf(id)+id.length();
    final int end = file.getPath().lastIndexOf(File.separator);
    final String path = file.getPath().substring(begin,end);
    return path.startsWith("/") ? path.substring(1,path.length()) : path;
    
  }
  
}
