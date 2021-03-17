package com.dotcms.contenttype.util;

import com.dotcms.util.marshal.DotTypeToken;
import com.dotcms.util.marshal.MarshalFactory;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
import io.vavr.control.Try;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility class to handle Key Value field
 *
 * @author Roger
 */
public class KeyValueFieldUtil {

  public static Map<String, Object> JSONValueToHashMap(final String json) {
    LinkedHashMap<String, Object> keyValueMap = new LinkedHashMap<String, Object>();
    if (UtilMethods.isSet(json)) {

      // the following code fixes issue 10529
      try {
        JSONObject object = Try.of(() -> new JSONObject(json)).getOrNull();
        if (object != null)
          return object.getAsMap();

        return MarshalFactory.getInstance().getMarshalUtils().unmarshal(json, new DotTypeToken<LinkedHashMap<String, String>>().getType());
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
            return MarshalFactory.getInstance().getMarshalUtils().unmarshal(replacedJSJson,
                new DotTypeToken<LinkedHashMap<String, String>>().getType());
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
