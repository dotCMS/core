package com.dotmarketing.portlets.templates.business;

import com.dotmarketing.util.UtilMethods;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tells which Containers an advanced (code) Template names, as cheaply as the Template body allows.
 *
 * <p>The reliable way to read the {@code #parseContainer} calls of a body is to run it through
 * Velocity (see {@link TemplateFactory#getContainerUUIDFromHTML(String)}), which is slow and
 * executes the Template's own code. This class avoids that run for the usual body, where every
 * call is written with literal arguments and nothing is pulled in from another file, by reading the
 * calls straight from the text. Only the bodies the text cannot answer for sure go to Velocity, and
 * the answers Velocity gives are kept for a short time, so a few lookups in a row (the check before
 * a delete and the delete itself) run it once.</p>
 *
 * <p>Instances are thread safe. The cache lives in the memory of the node and is not shared across a
 * cluster: it only holds answers that cost time to compute and are safe to compute again.</p>
 */
final class TemplateContainerReferences {

    /**
     * Directives that bring text in from somewhere else, and so may bring {@code #parseContainer}
     * calls the body does not show: {@code #dotParse}, {@code #parse}, {@code #include},
     * {@code #evaluate}, and the {@code #{name}} spelling of any directive.
     */
    private static final Pattern BRINGS_IN_OTHER_TEXT =
            Pattern.compile("#(?:dotParse|include|evaluate)\\b|#parse\\s*\\(|#\\{");

    /** Every use of the {@code #parseContainer} directive, whatever its arguments. */
    private static final Pattern ANY_PARSE_CONTAINER = Pattern.compile("#parseContainer\\b");

    /**
     * A {@code #parseContainer} call whose Container, and instance uuid if any, are plain quoted
     * text: no variable, no concatenation. Group 2 is the Container identifier or path.
     */
    private static final Pattern LITERAL_PARSE_CONTAINER = Pattern.compile(
            "#parseContainer\\s*\\(\\s*(['\"])([^'\"$#]+)\\1\\s*(?:,\\s*(['\"])[^'\"$#]*\\3\\s*)?\\)");

    private final Function<String, Collection<String>> velocityHarvest;
    private final Cache<String, Set<String>> cache;

    /**
     * Creates the resolver with the system clock.
     *
     * @param velocityHarvest gives the Container identifiers a body names by running it in Velocity
     * @param ttlSeconds      how long, in seconds, an answer from Velocity is kept; zero or less
     *                        turns the cache off
     * @param maxEntries      the most answers kept at once; zero or less turns the cache off
     */
    TemplateContainerReferences(final Function<String, Collection<String>> velocityHarvest,
            final long ttlSeconds, final long maxEntries) {
        this(velocityHarvest, ttlSeconds, maxEntries, Ticker.systemTicker());
    }

    /**
     * Same as {@link #TemplateContainerReferences(Function, long, long)} with the clock given, so a
     * test can make time pass.
     *
     * @param velocityHarvest gives the Container identifiers a body names by running it in Velocity
     * @param ttlSeconds      how long, in seconds, an answer from Velocity is kept; zero or less
     *                        turns the cache off
     * @param maxEntries      the most answers kept at once; zero or less turns the cache off
     * @param ticker          the clock that measures the time to live
     */
    TemplateContainerReferences(final Function<String, Collection<String>> velocityHarvest,
            final long ttlSeconds, final long maxEntries, final Ticker ticker) {
        this.velocityHarvest = velocityHarvest;
        this.cache = ttlSeconds > 0 && maxEntries > 0
                ? Caffeine.newBuilder()
                        .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
                        .maximumSize(maxEntries)
                        .ticker(ticker)
                        .build()
                : null;
    }

    /**
     * Reads the Containers a body names from its text, when the text is enough to be sure.
     *
     * <p>The text is enough when the body pulls in no other file (see
     * {@link #BRINGS_IN_OTHER_TEXT}) and every {@code #parseContainer} call has a plain quoted
     * Container, and instance uuid if any. A call that sits in an {@code #if} that never runs, or in
     * a comment, still counts; Velocity would skip it. Counting a Container the Template does not
     * really use only makes a check before deleting it stricter, never looser.</p>
     *
     * @param body the body of an advanced Template, possibly {@code null}
     * @return the Container identifiers or paths named by the body, none when it has no calls; empty
     * when the text cannot answer and the body has to be run in Velocity
     */
    static Optional<Set<String>> fromLiteralCalls(final String body) {

        if (!UtilMethods.isSet(body)) {
            return Optional.of(Collections.emptySet());
        }
        if (BRINGS_IN_OTHER_TEXT.matcher(body).find()) {
            return Optional.empty();
        }

        int calls = 0;
        final Matcher anyCall = ANY_PARSE_CONTAINER.matcher(body);
        while (anyCall.find()) {
            calls++;
        }

        final Set<String> references = new LinkedHashSet<>();
        int literalCalls = 0;
        final Matcher literalCall = LITERAL_PARSE_CONTAINER.matcher(body);
        while (literalCall.find()) {
            literalCalls++;
            references.add(literalCall.group(2).trim());
        }

        return literalCalls == calls ? Optional.of(references) : Optional.empty();
    }

    /**
     * Gives the Containers an advanced Template names: read from its text when that is enough,
     * otherwise by running its body in Velocity, remembering the answer for a short time.
     *
     * <p>An empty answer from Velocity is not remembered: Velocity also answers empty when the body
     * fails to run, and a failure must not be taken for "uses no Containers" on the next lookup.</p>
     *
     * @param inode the inode of the Template version, which changes with every save and so keys the
     *              cache; a {@code null} inode is never cached
     * @param body  the body of the advanced Template
     * @return the identifiers or paths of the Containers the body names
     */
    Set<String> forAdvancedTemplate(final String inode, final String body) {

        final Optional<Set<String>> fromText = fromLiteralCalls(body);
        if (fromText.isPresent()) {
            return fromText.get();
        }
        if (null == this.cache || null == inode) {
            return this.harvest(body);
        }
        final Set<String> references = this.cache.get(inode, key -> {
            final Set<String> harvested = this.harvest(body);
            return harvested.isEmpty() ? null : harvested;
        });
        return null == references ? Collections.emptySet() : references;
    }

    private Set<String> harvest(final String body) {

        final Set<String> references = new LinkedHashSet<>();
        for (final String identifier : this.velocityHarvest.apply(body)) {
            if (UtilMethods.isSet(identifier)) {
                references.add(identifier);
            }
        }
        return Collections.unmodifiableSet(references);
    }
}
