package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.track.matchers.RequestMatcher;
import com.dotcms.visitor.filter.characteristics.Character;

import javax.servlet.http.HttpServletRequest;

/**
 * This Context Map has the character map
 * @author jsanca
 */
public class CharacterCollectorContextMap implements CollectorContextMap {

    private final RequestMatcher requestMatcher;
    private final Character character;

    public CharacterCollectorContextMap(final Character character,
                                        final RequestMatcher requestMatcher) {
        this.character = character;
        this.requestMatcher = requestMatcher;
    }



    @Override
    public Object get(final String key) {


        if (this.character.getMap().containsKey(key)) {
            return this.character.getMap().get(key);
        }

        return null;
    }


    @Override
    public RequestMatcher getRequestMatcher() {
        return this.requestMatcher;
    }
}
