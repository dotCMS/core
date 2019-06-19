package com.dotcms.rest.api.v1.temp;

import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RemoteUrlForm {

  public final String remoteUrl;
  public final String fileName;
  public final String accessKey;
  public final Integer urlTimeout;

  @JsonCreator
  protected RemoteUrlForm(@JsonProperty("remoteUrl") String remoteUrl, @JsonProperty("fileName") String fileName,
      @JsonProperty("accessKey") String accessKey, @JsonProperty("urlTimeout") Integer urlTimeout) {
    super();
    this.remoteUrl = remoteUrl;
    this.fileName = fileName;
    this.accessKey = accessKey;
    this.urlTimeout = urlTimeout!=null ? urlTimeout : 30;
  }

}
