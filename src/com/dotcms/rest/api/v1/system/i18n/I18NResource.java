package com.dotcms.rest.api.v1.system.i18n;

import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.repackage.javax.ws.rs.GET;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotcms.repackage.org.json.JSONException;
import com.dotcms.repackage.org.json.JSONObject;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.InternalServerException;
import com.dotcms.rest.exception.NotFoundException;
import com.dotcms.rest.validation.Preconditions;
import com.dotmarketing.util.Logger;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.struts.MultiMessageResources;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

/**
 * @author Geoff M. Granum
 */
@Path("/v1/system/i18n")
public class I18NResource {


    public I18NResource() {
    }


    @GET
    @Path("/{lang:[\\w]{2,3}(?:-?[\\w]{2})?}/{rsrc:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(@PathParam("lang") String lang, @PathParam("rsrc") String rsrc) {
        RestResourceLookup lookup = new RestResourceLookup(lang, rsrc == null ? "" : rsrc);
        Response response;

        MultiMessageResources messages = LanguageUtil.getMessagesForDefaultCompany(lookup.locale, lookup.key);

        // Looking up the key accomplishes two things:
        // 1) If present, it's MUCH faster than looping
        // 2) Forces the loading of the language that was provided.
        Optional<String> result = Optional.ofNullable(messages.getMessage(Locale.forLanguageTag(lang), lookup.key));

        if(!lookup.ref.endsWith("/")) {
            if(result.isPresent()) {
                response = Response.ok("{ \"" + lookup.leafName + "\": \"" + result + "\"}").build();
            } else {
                throw new NotFoundException("Could not find matching resource string for the reference '%s'",
                                            lookup.ref);
            }
        } else {
            if(result.isPresent()) {
                logInvalidPropertyDefinition(result.get(), lookup.key);
            }
            Map<String, String> localizedMessages = getMessagesForLocale(messages, lookup.locale, lookup);
            Map<String, String> responseMap = Maps.newHashMap();

            for (Map.Entry<String, String> entry : localizedMessages.entrySet()) {
                String key = entry.getKey();
                key = key.substring(lookup.key.length() + 1); // trim the base reference and trailing '.'
                responseMap.put(key, entry.getValue());
            }
            if(responseMap.isEmpty()) {
                throw new NotFoundException("Message not found for resource %s", lookup.ref);
            }

            JSONObject root = new JSONObject();
            for (Map.Entry<String, String> entry : responseMap.entrySet()) {
                messageToJson(root, entry.getKey().split("\\."), entry.getValue());
            }

            response = Response.ok(root.toString()).build();
        }
        return response;
    }

    private void messageToJson(JSONObject root, String[] pathKeys, String value) {
        StringBuilder currentPath = new StringBuilder("");
        try {
            JSONObject parent = root;
            int lastIdx = pathKeys.length - 1;

            for (int i = 0; i < lastIdx; i++) {
                String pathKey = pathKeys[i];
                currentPath.append(pathKey);

                JSONObject child;
                if(parent.has(pathKey) && parent.get(pathKey) instanceof JSONObject) {
                    child = parent.getJSONObject(pathKey);
                } else {
                    if(parent.has(pathKey)) {
                        logInvalidPropertyDefinition(value, currentPath.toString());
                    }
                    child = new JSONObject();
                    parent.put(pathKey, child);
                }
                parent = child;
                currentPath.append('.');
            }
            String leafName = pathKeys[lastIdx];
            currentPath.append(leafName);
            if(parent.has(leafName)) {
                if(parent.get(leafName) instanceof JSONObject) {
                    logInvalidPropertyDefinition(value, currentPath.toString());
                } else {
                    Logger.warn(this.getClass(),
                                String.format("Resource message key has duplicate definitions: %s",
                                              StringUtils.join(pathKeys, '.')));
                }
            } else {
                parent.put(pathKeys[lastIdx], value);
            }
        } catch (JSONException e) {
            throw new InternalServerException(e,
                                              "Unexpected error while reading resources strings at: %s",
                                              currentPath.toString());
        }
    }

    private void logInvalidPropertyDefinition(String value, String currentPath) {
        Logger.warn(this.getClass(),
                    String.format("Resource message key is defined as both a key and a value: %s",
                                  currentPath));
        Logger.warn(this.getClass(), String.format("Ignoring value: %s=%s", currentPath, value));
    }

    private Map<String, String> getMessagesForLocale(
        MultiMessageResources messages,
        Locale locale,
        RestResourceLookup lookup) {
        @SuppressWarnings("unchecked")
        Map<String, String> map = messages.getMessages();
        TreeMap<String, String> treeMap = new TreeMap<>(map);
        String lang = locale.getLanguage();
        String startKeyToken = '.' + lookup.key + '.';
        String endKeyToken = '.' + lookup.key + '/';

        NavigableMap<String, String> defaultMessages =
            treeMap.subMap(treeMap.lowerKey(startKeyToken), false, treeMap.lowerKey(endKeyToken), true);

        startKeyToken = lang + startKeyToken;
        endKeyToken = lang + endKeyToken;
        NavigableMap<String, String> subTree =
            treeMap.subMap(treeMap.lowerKey(startKeyToken), false, treeMap.lowerKey(endKeyToken), true);
        Map<String, String> allMessages = Maps.newHashMap();

        for (Map.Entry<String, String> entry : defaultMessages.entrySet()) {
            allMessages.put(entry.getKey().substring(1), entry.getValue());
        }
        int trimLanguageLength = lang.length() + 1;
        for (Map.Entry<String, String> entry : subTree.entrySet()) {
            allMessages.put(entry.getKey().substring(trimLanguageLength), entry.getValue());
        }
        return allMessages;
    }

    private static class RestResourceLookup {

        private final Locale locale;
        private final String ref;
        private final String key;
        private final String leafName;

        public RestResourceLookup(String lang, String childRef) {
            Preconditions.checkNotEmpty(lang, BadRequestException.class, "Language is required.");
            Preconditions.checkNotNull(childRef, BadRequestException.class, "Resource path is required.");

            this.ref = lang + '/' + childRef;
            String key = childRef.replace('/', '.');
            if(key.endsWith(".")) {
                key = key.substring(0, key.length() - 1);
            }
            this.key = key;
            int idx = key.lastIndexOf('.') + 1;
            this.leafName = (idx != 0 && idx < key.length()) ? key.substring(idx) : key;
            locale = Locale.forLanguageTag(lang);

            if(locale == null) {
                throw new BadRequestException("Could not process requested language '%s'", lang);
            }
        }

    }
}
 
