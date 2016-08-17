package com.dotmarketing.exception;

/**
 * @todo remove the setMessage method, replace uses with 'throw new WhateverException(message,
 *       oldException)'
 */
public class DotDataException extends Exception {

  /**
	 * 
	 */
  private static final long serialVersionUID = -7641394178731435069L;

  private final String i18nKey;

  public DotDataException() {
    this(DotDataException.class.getSimpleName());
  }
  
  
  public DotDataException(Throwable t) {
    this(t.getMessage(),t.getMessage(),t);
  }

  public DotDataException(String message) {
    this(message, message, null);
  }

  public DotDataException(String message, Throwable t) {
    this(message,message,t);
  }
  public DotDataException(String message,String i18nKey, Throwable t) {
    super(message, t);

    this.i18nKey = i18nKey;
  }

  public String getI18nKey() {
    return i18nKey;
  }
}
