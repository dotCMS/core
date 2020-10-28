package com.dotcms.rest.api.v1.temp;

import com.dotcms.util.ConversionUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RemoteUrlForm {

  public final String remoteUrl;
  public final String fileName;
  public final String accessKey;
  public final Integer urlTimeoutSeconds;
  public final Long maxFileLength;

  public RemoteUrlForm(@JsonProperty("remoteUrl") final String remoteUrl, @JsonProperty("fileName") final String fileName,
      @JsonProperty("accessKey") final String accessKey) {
    this(remoteUrl, fileName, accessKey, null,"-1");
  }

  @JsonCreator
  public RemoteUrlForm(@JsonProperty("remoteUrl") final String remoteUrl, @JsonProperty("fileName") final String fileName,
      @JsonProperty("accessKey") final String accessKey, @JsonProperty("urlTimeoutSeconds") final Integer urlTimeout,
                       @JsonProperty(TempFileResource.MAX_FILE_LENGTH_PARAM)final String maxFileLength) {
    super();
    this.remoteUrl = remoteUrl;
    this.fileName = fileName;
    this.accessKey = accessKey;
    this.urlTimeoutSeconds = urlTimeout != null && urlTimeout < 600 ? urlTimeout : 30;
    this.maxFileLength=(maxFileLength==null)?-1L : ConversionUtils.toLongFromByteCountHumanDisplaySize(maxFileLength, -1l );
  }

}
