package com.dotcms.rest.api.v1.system.i18n;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.collect.Maps;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.InternalServerException;
import com.dotcms.rest.exception.NotFoundException;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.util.Logger;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.struts.MultiMessageResources;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.apache.commons.lang.StringUtils;

/**
 * @author Geoff M. Granum
 */
@Path("/v1/system/i18n")
@Tag(name = "Internationalization")
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
        // 1) Forces the loading of the language that was provided.
        // 2) Let's us check for 'invalid' (according to ReST) Resource Key use.
        Optional<String> singleResult = Optional.ofNullable(messages.getMessage(Locale.forLanguageTag(lang), lookup.key));
        Map<String, String> subTreeResult = getMessagesForLocale(messages, lookup.locale, lookup);

        checkHasResult(lookup, singleResult, subTreeResult);

        boolean isSubtree = refIsSubTree(lookup, singleResult, subTreeResult);

        if(isSubtree) {
            Map<String, String> responseMap = Maps.newHashMap();

            for (Map.Entry<String, String> entry : subTreeResult.entrySet()) {
                String key = entry.getKey();
                key = key.substring(lookup.key.length() + 1); // trim the base reference and trailing '.'
                responseMap.put(key, entry.getValue());
            }

            JSONObject root = new JSONObject();
            for (Map.Entry<String, String> entry : responseMap.entrySet()) {
                messageToJson(root, entry.getKey().split("\\."), entry.getValue());
            }

            response = Response.ok(root.toString()).build();
        } else {
            response = Response.ok("\"" + singleResult.get() + "\"").build();
        }
        return response;
    }

    @VisibleForTesting
    void checkHasResult(RestResourceLookup lookup, Optional<String> singleResult, Map<String, String> subTreeResult) {
        if(singleResult.isEmpty() && subTreeResult.isEmpty()) {
            throw new NotFoundException("No resource messages found for %s", lookup.ref);
        }
    }

    private boolean refIsSubTree(RestResourceLookup lookup, Optional<String> singleResult, Map<String, String> subTreeResult) {
        boolean isTree = subTreeResult.size() > 1 || (subTreeResult.size() == 1 && singleResult.isEmpty());
        if(isTree && singleResult.isPresent()) {
            // the case where both `foo.bar=x` and `foo.bar.baz=y` exist.
            logInvalidPropertyDefinition(singleResult.get(), lookup.key);
        }
        return isTree;
    }

    @VisibleForTesting
    void messageToJson(JSONObject root, String[] pathKeys, String value) {
        root = DotPreconditions.checkNotNull(root, InternalServerException.class, "Root cannot be null.");
        pathKeys = DotPreconditions.checkNotEmpty(pathKeys, InternalServerException.class, "PathKeys cannot be null.");

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
        Logger.warn(this.getClass(), String.format("Resource message key is defined as both a key and a value: %s", currentPath));
        Logger.warn(this.getClass(), String.format("Ignoring value: %s=%s", currentPath, value));
    }

    private Map<String, String> getMessagesForLocale(MultiMessageResources messages, Locale locale, RestResourceLookup lookup) {
        @SuppressWarnings("unchecked")
        Map<String, String> map = messages.getMessages();
        Map<String, String> allMessages = Maps.newHashMap();
        TreeMap<String, String> treeMap = new TreeMap<>(map);

        char dotPlusOne = ((char)('.' + 1)); // this is a forward slash, at least on in US on mac. Use math to be certain.
        String startKeyToken = '.' + lookup.key + '.';
        String endKeyToken = '.' + lookup.key + dotPlusOne;

        String variant = ""; // default.
        replaceLessSpecificVariantMessages(startKeyToken, endKeyToken, treeMap, variant, allMessages);

        variant = locale.getLanguage(); // zh
        replaceLessSpecificVariantMessages(startKeyToken, endKeyToken, treeMap, variant, allMessages);

        variant = variant + '_' + locale.getCountry(); // zh_CN
        replaceLessSpecificVariantMessages(startKeyToken, endKeyToken, treeMap, variant, allMessages);

        variant = variant + '_' + locale.getVariant(); // zh_CN_BOBSYOURUNCLE
        replaceLessSpecificVariantMessages(startKeyToken, endKeyToken, treeMap, variant, allMessages);

        return allMessages;
    }

    private void replaceLessSpecificVariantMessages(String startKeyToken,
                                                    String endKeyToken,
                                                    TreeMap<String, String> treeMap,
                                                    String variant,
                                                    Map<String, String> allMessages) {
        int trimKeyLength = variant.length() + 1;
        startKeyToken = variant + startKeyToken;
        endKeyToken = variant + endKeyToken;

        Map<String, String> moreSpecificMessages = getSubTree(startKeyToken, endKeyToken, treeMap);

        for (Map.Entry<String, String> entry : moreSpecificMessages.entrySet()) {
            allMessages.put(entry.getKey().substring(trimKeyLength), entry.getValue());
        }
    }

    @VisibleForTesting
    Map<String, String> getSubTree(String startKeyToken, String endKeyToken, TreeMap<String, String> treeMap) {
        String fromKey = treeMap.lowerKey(startKeyToken);

        // increment last character in token so that when 'foo.bar' is requested as end token,
        // we also get 'foo.bar.baz' etc.
        StringBuilder incrementedToken = new StringBuilder(endKeyToken);
        char endTokenLastChar = endKeyToken.charAt(endKeyToken.length() - 1);
        incrementedToken.setCharAt(endKeyToken.length() - 1, (char)(endTokenLastChar + 1));

        String toKey = treeMap.ceilingKey(incrementedToken.toString());
        Map<String, String> result;
        if(fromKey == null && toKey == null) {
            result = Collections.emptyMap();
        } else if(fromKey == null) {
            result = treeMap.headMap(toKey, false); // take first 'half' of the tree
        } else if(toKey == null) {
            result = treeMap.tailMap(fromKey, false); // take last 'half' of the tree
        } else {
            result = treeMap.subMap(fromKey, false, toKey, false); // take a chunk out of the middle.
        }
        return result;
    }

    static class RestResourceLookup {

        private final Locale locale;
        private final String ref;
        private final String key;

        public RestResourceLookup(String lang, String childRef) {
            DotPreconditions.checkNotEmpty(lang, BadRequestException.class, "Language is required.");
            DotPreconditions.checkNotNull(childRef, BadRequestException.class, "Resource path is required.");

            this.ref = lang + '/' + childRef;
            String key = childRef.replace('/', '.');
            if(key.endsWith(".")) {
                key = key.substring(0, key.length() - 1);
            }
            this.key = key;
            locale = Locale.forLanguageTag(lang);

            if(locale == null) {
                throw new BadRequestException("Could not process requested language '%s'", lang);
            }
        }
    }
}
 
