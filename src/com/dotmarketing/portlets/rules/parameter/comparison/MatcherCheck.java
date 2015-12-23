package com.dotmarketing.portlets.rules.parameter.comparison;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;

public class MatcherCheck {


    public static <T> void checkThat(T actual, Matcher<? super T> matcher) {
        if(!verifyThat(actual, matcher)){
            Description description = new StringDescription();
            description.appendText("Expected: ")
                .appendDescriptionOf(matcher)
                .appendText("\n     but: ");
            matcher.describeMismatch(actual, description);

            throw new IllegalStateException(description.toString() );
        }
    }
    
    public static <T> boolean verifyThat(T actual, Matcher<? super T> matcher) {
        return matcher.matches(actual);
    }

}
