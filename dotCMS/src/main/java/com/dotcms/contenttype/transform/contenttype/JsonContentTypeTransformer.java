package com.dotcms.contenttype.transform.contenttype;

import java.util.ArrayList;
import java.util.List;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.transform.JsonHelper;
import com.dotcms.contenttype.transform.JsonTransformer;
import com.dotcms.contenttype.transform.SerialWrapper;
import com.dotcms.contenttype.transform.field.JsonFieldTransformer;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;

public class JsonContentTypeTransformer implements ContentTypeTransformer, JsonTransformer {
  final List<ContentType> list;

  public JsonContentTypeTransformer(ContentType type) {
    this.list = ImmutableList.of(type);
  }


  public JsonContentTypeTransformer(String json) {
    List<ContentType> types = new ArrayList<>();
    // are we an array?
    try {
      if (json != null && json.trim().startsWith("[")) {
        types = ImmutableList.copyOf(fromJsonArrayStr(json));
      } else {
        types = ImmutableList.of(fromJsonStr(json));
      }
    } catch (JSONException arrEx) {
      throw new DotStateException(arrEx);

    }
    this.list = ImmutableList.copyOf(types);
  }


  public JsonContentTypeTransformer(List<ContentType> list) {
    this.list = ImmutableList.copyOf(list);
  }

  @Override
  public JSONObject jsonObject() {
    ContentType type = from();
    JSONObject jsonObject;
    try {
      jsonObject =
          new JSONObject(mapper.writeValueAsString(type));


      jsonObject.remove("permissionType");
      jsonObject.remove("permissionId");
      jsonObject.remove("versionable");
      jsonObject.remove("multilingualable");

      jsonObject.put("fields", new JsonFieldTransformer(type.fields()).jsonArray());

      return jsonObject;
    } catch (JSONException | JsonProcessingException e) {
      throw new DotStateException(e);
    }
  }


  private ContentType fromJsonStr(String input) {
    try {
      return (ContentType) mapper.readValue(input, ContentType.class);
    } catch (Exception e) {
      throw new DotStateException(e);
    }
  }


  private List<ContentType> fromJsonArrayStr(String input) throws JSONException {
    List<ContentType> types = new ArrayList<>();
    JSONArray jarr = new JSONArray(input);
    for (int i = 0; i < jarr.length(); i++) {
      JSONObject jo = jarr.getJSONObject(i);
      if (jo.has("inode") && !jo.has("id")) {
        jo.put("id", jo.get("inode"));
      }
      HostAPI hapi = APILocator.getHostAPI();
      ContentType type = fromJsonStr(jo.toString());
      try {
        Host host = UUIDUtil.isUUID(type.host()) || "SYSTEM_HOST".equalsIgnoreCase(type.host())
            ? hapi.find(type.host(), APILocator.systemUser(), true)
            : hapi.resolveHostName(type.host(), APILocator.systemUser(), true);
        type = ContentTypeBuilder.builder(type).host(host.getIdentifier()).build();
      } catch (DotDataException | DotSecurityException e) {
        throw new DotStateException("unable to resolve host:" + type.host(), e);
      }

      if (jo.has("fields")) {
        List<Field> fields = new JsonFieldTransformer(jo.getJSONArray("fields").toString()).asList();
        type.constructWithFields(fields);
      }
      types.add(type);
    }
    return types;
  }


  @Override
  public ContentType from() throws DotStateException {
    return asList().get(0);
  }

  @Override
  public List<ContentType> asList() throws DotStateException {
    for (ContentType type : this.list) {
      for (Field f : type.fields()) {
        f.fieldVariables();
      }

    }
    return this.list;
  }



  @Override
  public JSONArray jsonArray() {
    JSONArray jarr = new JSONArray();
    for (ContentType type : asList()) {
      jarr.add(new JsonContentTypeTransformer(type).jsonObject());
    }
    return jarr;
  }



}

