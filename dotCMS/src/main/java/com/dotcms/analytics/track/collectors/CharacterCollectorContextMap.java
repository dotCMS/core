package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.track.matchers.RequestMatcher;
import com.dotcms.visitor.filter.characteristics.Character;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * This Context Map has the character map
 * @author jsanca
 */
public class CharacterCollectorContextMap implements CollectorContextMap {

    private final Map<String, Object> contextMap = new HashMap<>();
    private final RequestMatcher requestMatcher;
    private final Map<String, Serializable> characterMap;

    public CharacterCollectorContextMap(final Character character,
                                        final RequestMatcher requestMatcher,
                                        final Map<String, Object> contextMap) {

        this.characterMap = character.getMap();
        this.requestMatcher = requestMatcher;
        this.contextMap.putAll(contextMap);
    }



    @Override
    public Object get(final String key) {

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
