package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.track.matchers.RequestMatcher;
import com.dotcms.visitor.filter.characteristics.Character;

import com.dotmarketing.business.web.WebAPILocator;

import javax.servlet.http.HttpServletRequest;

/**
 * This Context Map has the request + character map
 * @author jsanca
 */
public class RequestCharacterCollectorContextMap implements CollectorContextMap {

    private final RequestMatcher requestMatcher;
    private final Character character;
    final HttpServletRequest request;

    public RequestCharacterCollectorContextMap(final HttpServletRequest request,
                                               final Character character,
                                               final RequestMatcher requestMatcher) {
        this.request = request;
        this.character = character;
        this.requestMatcher = requestMatcher;
    }



    @Override
    public Object get(final String key) {

        if (request.getParameter(key) != null) {
            return request.getParameter(key);
        }

        if(request.getAttribute(key) != null) {
            return request.getAttribute(key);
        }

        if (this.character.getMap().containsKey(key)) {
            return this.character.getMap().get(key);
        }

        if("request".equals(key)) {
            return request;
        }

        if (key.equals("currentHost")) {
            return WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        }

        return null;
    }


    @Override
    public RequestMatcher getRequestMatcher() {
        return this.requestMatcher;
    }
}
