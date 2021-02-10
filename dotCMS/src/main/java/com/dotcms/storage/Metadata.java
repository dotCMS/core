package com.dotcms.storage;

import java.io.Serializable;
import java.util.Map;

public class FieldMetadata implements Serializable{

      final String fieldName;

      final Map<String, Serializable> fieldsMeta;

      final Map<String, Serializable> custom;

      public FieldMetadata(String fieldName,
              Map<String, Serializable> fieldsMeta,
              Map<String, Serializable> custom) {
            this.fieldName = fieldName;
            this.fieldsMeta = fieldsMeta;
            this.custom = custom;
      }
}
