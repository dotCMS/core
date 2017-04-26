package com.dotcms.contenttype.transform.field;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotmarketing.business.DotStateException;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonFieldVariableTransformer {

  private static final ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(
	Include.NON_NULL
  ).configure(
	DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false
  );

  final List<FieldVariable> list;


  public JsonFieldVariableTransformer(FieldVariable fieldVariable) {
    this.list = ImmutableList.of(fieldVariable);
  }

  public JsonFieldVariableTransformer(List<FieldVariable> list) {
    this.list = ImmutableList.copyOf(list);
  }

  public JsonFieldVariableTransformer(String json) {
	List<FieldVariable> l = new ArrayList<>();
	l.add(fromJsonStr(json));
	this.list = ImmutableList.copyOf(l);
  }


  private FieldVariable fromJsonStr(String input) throws DotStateException {
	try {
	  return (FieldVariable) mapper.readValue(input, FieldVariable.class);
	} catch (Exception e) {
	  throw new DotStateException(e);
	}
  }


  public FieldVariable from() throws DotStateException {
    return this.list.get(0);
  }

  public List<FieldVariable> asList() throws DotStateException {
    return this.list;
  }


  public List<Map<String, Object>> mapList() {
	  List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
	  for (FieldVariable fieldVariable : asList()) {
		  list.add(new JsonFieldVariableTransformer(fieldVariable).mapObject());
	  }
	  return list;
  }

  public Map<String, Object> mapObject() {
	  try {
		  Map<String, Object> map = mapper.readValue(
			mapper.writeValueAsString(from()),
			new TypeReference<Map<String, String>>(){}
		  );

		  map.remove("modDate");
		  map.remove("name");
		  map.remove("userId");

		  return map;
      } catch (IOException e) {
        throw new DotStateException(e);
      }
  }
}

