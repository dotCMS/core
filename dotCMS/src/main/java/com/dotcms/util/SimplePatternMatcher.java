package com.dotcms.util;

import com.dotmarketing.util.UtilMethods;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;


public class SimplePatternMatcher {

    private final Collection<Pattern> simplePatterns;


    public SimplePatternMatcher(final Collection<String> simplePatternsIn) {
        this.simplePatterns = cleanSimplePatterns(simplePatternsIn);
    }

    public SimplePatternMatcher(final String simplePatternIn) {
        this(simplePatternIn == null ? List.of() : List.of(simplePatternIn));
    }


    private List<Pattern> cleanSimplePatterns(final Collection<String> simplePatternsIn) {
        if (simplePatternsIn == null) {
            return List.of();
        }
        return simplePatternsIn.stream()
                .filter(UtilMethods::isSet)
                .map(s -> Pattern.compile(s.trim().toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<String> matches(@NotNull final String possibleMatch) {
        return matches((possibleMatch == null ? List.of() : List.of(possibleMatch)));
    }


    public List<String> matches(@NotNull final Collection<String> possibleMatches) {
        if (possibleMatches == null) {
            return List.of();
        }
        return possibleMatches
                .stream()
                .map(String::toLowerCase)
                .filter(this::isMatch)
                .collect(Collectors.toList());
    }


    public boolean isMatch(@NotNull String stringToTestArg) {
        if (stringToTestArg == null) {
            return false;
        }

        return this.simplePatterns
                .stream()
                .anyMatch(s -> s.matcher(stringToTestArg).find());

    }


}
