package com.dotcms.contenttype.transform;

import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;


public interface JsonTransformer {
  
  static final ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(Include.NON_NULL)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  JSONObject jsonObject();

  JSONArray jsonArray();
}
