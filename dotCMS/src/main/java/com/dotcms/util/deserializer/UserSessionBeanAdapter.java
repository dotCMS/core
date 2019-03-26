package com.dotcms.util.deserializer;

import com.dotcms.api.system.event.UserSessionBean;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;

/**
 * This is an adapter for the visibility value UserSession
 *
 * @author jsanca
 */
public class UserSessionBeanAdapter
    implements JsonDeserializer<UserSessionBean>, JsonSerializer<UserSessionBean> {

  public static final String USER = "user";
  public static final String SESSION_ID = "sessionId";

  @Override
  public UserSessionBean deserialize(
      final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
      throws JsonParseException {
    JsonObject jsonObject = null;
    String user = null;
    String sessionId = null;

    if (null != json) {

      jsonObject = json.getAsJsonObject();

      if (jsonObject.has(USER)) {
        user = jsonObject.getAsJsonPrimitive(USER).getAsString();
      }

      if (jsonObject.has(SESSION_ID)) {
        sessionId = jsonObject.getAsJsonPrimitive(SESSION_ID).getAsString();
      }
    }

    return new UserSessionBean(user, sessionId);
  }

  @Override
  public JsonElement serialize(
      final UserSessionBean src, final Type typeOfSrc, final JsonSerializationContext context) {

    final JsonObject jsonElement = new JsonObject();

    jsonElement.addProperty(USER, src.getUser());
    jsonElement.addProperty(SESSION_ID, src.getSessionId());

    return jsonElement;
  }
} // E:O:F:UserSessionBeanAdapter.
