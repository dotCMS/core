package com.dotcms.contenttype.transform;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.io.Serializable;

public class SerialWrapper<T> implements Serializable {
  private static final long serialVersionUID = 1L;
  @JsonUnwrapped private final T inner;
  private final Class implClass;

  public SerialWrapper(T inner, Class clazz) {
    this.inner = inner;
    this.implClass = clazz;
  }

  public T getInner() {
    return inner;
  }

  public Class getImplClass() {
    return implClass;
  }
}
