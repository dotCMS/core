package com.dotcms.regex;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.util.StringPool;
import io.vavr.control.Try;

/**
 * A factory class used to generate Matcher instances that will throw if in use after their timeout.
 * MIT Licensed and Taken from
 * https://www.exratione.com/2017/06/preventing-unbounded-regular-expression-operations-in-java/
 */
public abstract class MatcherTimeoutFactory {


    /**
     * This pattern is for TESTING only and not intended to match anything in the real world.
     */
    @VisibleForTesting
    public static final String MATCHER_TIMEOUT_PATTERN="MATCHER_TIMEOUT_TESTING_PATTERNe8f124f01427";




    private MatcherTimeoutFactory() {
        
    }

    // Global regex timeout from config
    @VisibleForTesting
    static final long VANITY_URL_REGEX_TIMEOUT = Config.getLongProperty("VANITY_URL_REGEX_TIMEOUT", 2000L);

    // regex quarantine time
    @VisibleForTesting
    static final long VANITY_URL_QUARANTINE_MS = Config.getLongProperty("VANITY_URL_QUARANTINE_MS", 30000L);

    // timed cache for bad regexes
    @VisibleForTesting
    static final Cache<String, Boolean> SLOW_REGEX_CACHE = Caffeine.newBuilder().maximumSize(5000)
                    .expireAfterWrite(VANITY_URL_QUARANTINE_MS, TimeUnit.MILLISECONDS).build();
    
    @VisibleForTesting
    // No match regex that is returned if a regex is in the quarantine box
    static final Pattern NO_MATCH_PATTERN = Pattern.compile("^\\b$");


    
    public static boolean isUrlQuaratined(String url) {
        

        return null != SLOW_REGEX_CACHE.getIfPresent(url);
        
    }
    
    

    /**
     * Generate a Matcher instance that will throw if used or still in use more than
     * timeoutInMilliseconds after its instantiation.
     *
     * Use the instance immediately and then discard it.
     *
     * @param pattern The Pattern instance.
     * @param charSequence The CharSequence to operate on.
     * @param timeoutInMilliseconds Throw after this timeout is reached.
     */
    public static Matcher matcher(Pattern pattern, CharSequence charSequence, long timeoutInMilliseconds) {
        if (null != SLOW_REGEX_CACHE.getIfPresent(pattern.toString())) {
            return NO_MATCH_PATTERN.matcher(StringPool.BLANK);
        }
        if (null != SLOW_REGEX_CACHE.getIfPresent(charSequence)) {
            return NO_MATCH_PATTERN.matcher(StringPool.BLANK);
        }
        // Substitute in our exploding CharSequence implementation.
        if (!(charSequence instanceof TimeLimitedCharSequence)) {
            charSequence = new TimeLimitedCharSequence(charSequence, timeoutInMilliseconds, pattern, charSequence);
        }

        return pattern.matcher(charSequence);
    }

    /**
     * Takes a String and includes the pattern compile time in the timeout calculation
     * 
     * @param patternStr
     * @param charSequence
     * @return
     */
    public static Matcher matcher(String patternStr, CharSequence charSequence) {
        long timeoutAfterTimestamp = System.currentTimeMillis() + VANITY_URL_REGEX_TIMEOUT;
        Pattern pattern = Pattern.compile(patternStr);
        return matcher(pattern, charSequence, timeoutAfterTimestamp - System.currentTimeMillis());
    }



    /**
     * Generate a Matcher instance that will throw if used or still in use more than 2 seconds after its
     * instantiation.
     *
     * Use the instance immediately and then discard it.
     *
     * @param pattern The Pattern instance.
     * @param charSequence The CharSequence to operate on.
     */
    public static Matcher matcher(Pattern pattern, CharSequence charSequence) {
        return matcher(pattern, charSequence, VANITY_URL_REGEX_TIMEOUT);
    }

    /**
     * An exception to indicate that a regular expression operation timed out.
     */
    @SuppressWarnings("serial")
    public static class RegExpTimeoutException extends RuntimeException {
        public RegExpTimeoutException(String message) {
            super(message);
        }
    }

    /**
     * A CharSequence implementation that throws when charAt() is called after a given timeout.
     *
     * Since charAt() is invoked frequently in regular expression operations on a string, this gives a
     * way to abort long-running regular expression operations.
     */
    static class TimeLimitedCharSequence implements CharSequence {
        private final CharSequence inner;

        private final long timeoutAfterTimestamp;
        private final Pattern pattern;
        private final CharSequence originalCharSequence;

        @Override
        public IntStream chars() {
            checkTimeout();
            return inner.chars();
        }

        @Override
        public IntStream codePoints() {
            checkTimeout();
            return inner.codePoints();
        }

        /**
         * Default constructor.
         *
         * @param inner The CharSequence to wrap. This may be a subsequence of the original.
         * @param timeoutInMilliseconds How long before calls to charAt() throw.
         * @param pattern The Pattern instance; only used for logging purposes.
         * @param originalCharSequence originalCharSequence The original sequence, used for logging
         *        purposes.
         */
        public TimeLimitedCharSequence(CharSequence inner, long timeoutInMilliseconds, Pattern pattern,
                        CharSequence originalCharSequence) {
            super();
            this.inner = inner;
            // Carry out this calculation here, once, rather than every time
            // charAt() is invoked. Little optimizations make the world turn.
            timeoutAfterTimestamp = System.currentTimeMillis() + timeoutInMilliseconds;
            this.pattern = pattern;
            this.originalCharSequence = originalCharSequence;
            if(pattern.toString().equals(MATCHER_TIMEOUT_PATTERN)){
                Try.run(()->Thread.sleep(timeoutInMilliseconds));
            }
        }
        
        @Override
        public char charAt(int index) {

            // This is an unavoidable slowdown, but what can you do?
            checkTimeout();
            return inner.charAt(index);
        }

        private void checkTimeout() {
            if (System.currentTimeMillis() >= timeoutAfterTimestamp) {
                SLOW_REGEX_CACHE.put(this.pattern.toString(), Boolean.TRUE);
                SLOW_REGEX_CACHE.put(originalCharSequence.toString(), Boolean.TRUE);
                // Note that we add the original charsequence to the exception
                // message. This condition can be met on a subsequence of the
                // original sequence, and the subsequence string is rarely
                // anywhere near as helpful.
                final String message = "RegEx timeout for [ " + pattern.pattern() + " ] on [ " + originalCharSequence
                                + " ].  Skipping Regex Evaluation for " + (VANITY_URL_QUARANTINE_MS / 1000) + " sec.";
                Logger.warn(MatcherTimeoutFactory.class, message);
                throw new RegExpTimeoutException(message);
            }

        }

        @Override
        public int length() {
            checkTimeout();
            return inner.length();
        }
        
        @Override
        public CharSequence subSequence(int start, int end) {
            // Ensure that any subsequence generated during a regular expression
            // operation is still going to explode on time.
            return new TimeLimitedCharSequence(inner.subSequence(start, end), timeoutAfterTimestamp - System.currentTimeMillis(),
                            pattern, originalCharSequence);
        }

        @Override
        public String toString() {
            checkTimeout();
            return inner.toString();
        }
    }
}
