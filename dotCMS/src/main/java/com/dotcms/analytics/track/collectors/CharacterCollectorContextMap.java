package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.track.matchers.RequestMatcher;
import com.dotcms.visitor.filter.characteristics.Character;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This Context Map has the character map
 * @author jsanca
 */
public class CharacterCollectorContextMap implements CollectorContextMap {

    private final Map<String, Object> contextMap = new HashMap<>();
    private final RequestMatcher requestMatcher;
    private final Map<String, Serializable> characterMap;
    private final Map<String, Object> customValuesMap;

    public CharacterCollectorContextMap(final Character character,
                                        final RequestMatcher requestMatcher,
                                        final Map<String, Object> contextMap) {

        this.characterMap = character.getMap();
        this.requestMatcher = requestMatcher;
        this.contextMap.putAll(contextMap);
        this.customValuesMap = Map.of();
    }

    public CharacterCollectorContextMap(final Character character,
                                        final RequestMatcher requestMatcher,
                                        final Map<String, Object> contextMap,
                                        Map<String, Object> customValuesMap) {

        this.characterMap = character.getMap();
        this.requestMatcher = requestMatcher;
        this.contextMap.putAll(contextMap);
        this.customValuesMap = Objects.nonNull(customValuesMap)?customValuesMap:Map.of();
    }



    @Override
    public Object get(final String key) {

        if (this.customValuesMap.containsKey(key)) {
            return this.customValuesMap.get(key);
        }

        if (this.characterMap.containsKey(key)) {
            return this.characterMap.get(key);
        }

        if (this.contextMap.containsKey(key)) {
            return this.contextMap.get(key);
        }

        return null;
    }


    @Override
    public RequestMatcher getRequestMatcher() {
        return this.requestMatcher;
    }
}
