package com.dotcms.analytics.track.matchers;

/**
 * This is just flag class to identify the user custom defined event matcher
 * @author jsanca
 */
public final class UserCustomDefinedRequestMatcher implements RequestMatcher {

    public static final String USER_CUSTOM_EVENT_MATCHER_ID = "user-custom-event";


    @Override
    public String getId() {
        return USER_CUSTOM_EVENT_MATCHER_ID;
    }
}
