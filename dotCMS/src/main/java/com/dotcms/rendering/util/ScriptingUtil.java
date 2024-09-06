package com.dotcms.rendering.util;

import com.dotcms.rest.api.v1.HTTPMethod;
import com.dotcms.uuid.shorty.ShortType;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.util.UtilMethods;

import java.io.StringWriter;
import java.util.Map;
import java.util.Optional;

/**
 * Util method for the scripting api
 * @author jsanca
 */
public class ScriptingUtil {

    public static final String IDENTIFIER = "identifier";
    public static final String ESCAPE_ME_VALUE= " THIS_ESCAPES_LINE_BREAKS ";

    private static class SingletonHolder {
        private static final ScriptingUtil INSTANCE = new ScriptingUtil();
    }
    /**
     * Get the instance.
     * @return ScriptingUtil
     */
    public static ScriptingUtil getInstance() {

        return ScriptingUtil.SingletonHolder.INSTANCE;
    } // getInstance.

    /**
     * Validates the body map
     * @param bodyMap
     * @param httpMethod
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void validateBodyMap(final Map bodyMap, final HTTPMethod httpMethod) {

        // if it is an update method (not get) and the
        if (UtilMethods.isSet(bodyMap) && bodyMap.containsKey(IDENTIFIER) &&
                UtilMethods.isSet(bodyMap.get(IDENTIFIER))
                // an non-existing identifier could be just on get or post, put, patch or delete needs an existing id.
                && httpMethod != HTTPMethod.GET && httpMethod != HTTPMethod.POST) {

            final Optional<ShortyId> shortyId = APILocator.getShortyAPI().getShorty(bodyMap.get(IDENTIFIER).toString());
            final boolean isIdentifier = shortyId.isPresent() && shortyId.get().type == ShortType.IDENTIFIER;

            if (!isIdentifier) {

                throw new DoesNotExistException("The identifier: " + bodyMap.get(IDENTIFIER) + " does not exists");
            }
        }
    }

    /**
     * Escape the json values
     * @param jsonStr
     * @param escapeFrom
     * @return String
     */
    public String escapeJsonValues(final String jsonStr, final char escapeFrom) {

        final StringWriter writer  = new StringWriter();
        final char[] charArray     = jsonStr.toCharArray();
        boolean inQuotes           = false;

        for(int i=0; i <charArray.length; i++) {

            final char c = charArray[i];
            if(c=='"' && charArray[i-1] != '\\') {

                inQuotes = !inQuotes;
            }

            if(inQuotes && c==escapeFrom) {

                writer.append(ESCAPE_ME_VALUE);
            } else {
                writer.append(c);
            }
        }

        return writer.toString();
    }

    /**
     * Unescape the json values
     * @param escapedValue
     * @param escapeFrom
     * @return String
     */
    public String unescapeValue(final String escapedValue, final String escapeFrom) {

        return escapedValue.replace(ESCAPE_ME_VALUE, escapeFrom);
    }
}
