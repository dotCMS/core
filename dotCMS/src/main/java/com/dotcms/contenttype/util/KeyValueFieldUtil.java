package com.dotcms.contenttype.util;

import com.dotcms.util.marshal.MarshalFactory;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
import com.fasterxml.jackson.core.type.TypeReference;
import io.vavr.control.Try;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility class to handle Key Value field
 *
 * @author Roger
 */
public class KeyValueFieldUtil {

  /**
   * Parses a JSON string into a {@link LinkedHashMap} to preserve the insertion order of keys
   * as they appear in the source JSON.
   *
   * <p>This is the entry point for the <em>write path</em> when a KeyValue field value arrives
   * as a JSON string (from the REST API, legacy import, etc.). The returned {@link LinkedHashMap}
   * is subsequently converted to a {@code List<Entry<?>>} by
   * {@link com.dotcms.contenttype.model.field.KeyValueField#fieldValue} and stored as a JSON
   * <em>array</em> in {@code contentlet_as_json} — the only format that guarantees key order
   * across the DB round-trip.</p>
   *
   * @param json JSON object string, e.g. {@code {"D":"d","B":"b"}}
   * @return insertion-order-preserving map of the parsed key/value pairs, or an empty map if
   *         {@code json} is blank
   */
  public static Map<String, Object> JSONValueToHashMap(final String json) {
    LinkedHashMap<String, Object> keyValueMap = new LinkedHashMap<>();
    if (UtilMethods.isSet(json)) {

      // the following code fixes issue 10529
      try {
        JSONObject object = Try.of(() -> new JSONObject(json)).getOrNull();
        if (object != null)
          return object.getAsMap();

        return (Map) MarshalFactory.getInstance()
                .getMarshalUtils()
                .unmarshal(json, new TypeReference<LinkedHashMap<String, String>>() {
                });
      } catch (Exception ex) {
        Logger.warn(KeyValueFieldUtil.class,
            String.format("Error parsing json: %s. Trying to parse with the JS replacement due to container or key/value data...", json));

        boolean tryEvaluate = false;
        String replacedJSJson;
        if (json.contains("\\")) {
          replacedJSJson = UtilMethods.replace(json, "\\", "&#92;");
          tryEvaluate = true;
        } else {
          replacedJSJson = json;
        }

        if (tryEvaluate) {
          try {
            return (Map) MarshalFactory.getInstance()
                    .getMarshalUtils()
                    .unmarshal(replacedJSJson, new TypeReference<LinkedHashMap<String, String>>() {
                    });

          } catch (Exception ex2) {
            Logger.error(KeyValueFieldUtil.class,
                String.format("Unable to parse JSON with backslash replacement: %s. Returning Exception.", replacedJSJson), ex2);
            throw ex2;
          }
        } else {
          Logger.error(KeyValueFieldUtil.class,
              String.format(
                  "Unable to parse JSON: %s. Please review the content and check for potential JS replacements. Returning exception",
                  replacedJSJson),
              ex);
          throw ex;
        }
      }

    }
    return keyValueMap;
  }

}
