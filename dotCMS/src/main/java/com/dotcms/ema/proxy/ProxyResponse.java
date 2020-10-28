package com.dotcms.ema.proxy;

import org.apache.http.Header;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;

public class ProxyResponse {

  /**
   * The Response.
   */
  final private byte[] response;
  final private StatusLine status;
  final private int responseCode;
  final private Header[] headers;

  public ProxyResponse(StatusLine status, byte[] out, Header[] headers) {
    this.response = out;
    this.status = status;
    this.responseCode = status.getStatusCode();
    this.headers = headers;
  }

  public ProxyResponse(final int responseCode, byte[] out, Header[] headers) {
    this(new StatusLine() {
      @Override
      public int getStatusCode() {
        return responseCode;
      }

      @Override
      public String getReasonPhrase() {
        return "unknown error";
      }

      @Override
      public ProtocolVersion getProtocolVersion() {
        return null;
      }
    }, out, headers);

  }

  public byte[] getResponse() {
    return this.response;
  }

  public int getResponseCode() {
    return (status != null) ? status.getStatusCode() : responseCode;
  }

  public StatusLine getStatus() {
    return this.status;
  }

  public Header[] getHeaders() {
    return (headers != null) ? headers : new Header[0];
  }
}
