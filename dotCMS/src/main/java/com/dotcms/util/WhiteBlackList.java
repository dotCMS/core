package com.dotcms.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Encapsulates a white/black list of regular expressions.
 * @author jsanca
 */
public class WhiteBlackList {

    private final List<Pattern> includePatterns;
    private final List<Pattern> excludePatterns;

    private WhiteBlackList(final Set<String> includePatterns, final Set<String> excludePatterns) {

        this.includePatterns = includePatterns.stream().map(Pattern::compile).collect(Collectors.toUnmodifiableList());
        this.excludePatterns = excludePatterns.stream().map(Pattern::compile).collect(Collectors.toUnmodifiableList());
    }

    /**
     * Returns true if the input is allowed by the white/black list.
     * @param input
     * @return
     */
    public boolean isAllowed(final String input) {

        if (Objects.isNull(input) || input.isEmpty()) {
            return false;
        }

        boolean anyIncludeMatches = isEmptyPattern(includePatterns) ||
                includePatterns.stream().map(pattern -> pattern.matcher(input)).anyMatch(Matcher::matches);
        boolean anyExcludeMatches = excludePatterns.stream().map(pattern -> pattern.matcher(input)).anyMatch(Matcher::matches);

        return anyIncludeMatches && !anyExcludeMatches;
    }

    private boolean isEmptyPattern (final List<Pattern> collection) {
        return Objects.isNull(collection) || collection.isEmpty() || (collection.size()==1 && collection.get(0).pattern().isEmpty());
    }

    /**
     *
     * @param input
     * @return
     */
    public Stream<String> filter(final Stream<String> input) {
        return input.filter(this::isAllowed);
    }

    public static final class Builder {
        private final Set<String> includePatterns = new LinkedHashSet<>();
        private final Set<String> excludePatterns = new LinkedHashSet<>();

        public Builder addWhitePatterns(final String... includePatterns) {
            this.includePatterns.addAll(Arrays.asList(includePatterns));
            return this;
        }

        public Builder addWhitePatterns(final Collection<String> includePatterns) {
            this.includePatterns.addAll(includePatterns);
            return this;
        }

        public Builder addBlackPatterns(final String... excludePatterns) {
            this.excludePatterns.addAll(Arrays.asList(excludePatterns));
            return this;
        }

        public Builder addBlackPatterns(final Collection<String> excludePatterns) {
            this.excludePatterns.addAll(excludePatterns);
            return this;
        }

        public WhiteBlackList build() {
            return new WhiteBlackList(this.includePatterns, this.excludePatterns);
        }
    }
}
