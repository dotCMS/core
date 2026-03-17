package com.dotmarketing.util;

import com.google.common.base.Preconditions;
import com.liferay.portal.util.PropsUtil;
import java.nio.Buffer;
import java.nio.CharBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * Small Password generator that can be configured to include custom-charsets and a minimum number of them
 */
public class PasswordGenerator {

    private final Random random = new SecureRandom();

    private final List<Charset> charsets;

    private final int length;

    /**
     * Private constructor
     * @param charsets
     * @param length
     */
    private PasswordGenerator(final List<Charset> charsets, final int length) {
        this.charsets = charsets;
        this.length = length;
    }

    /**
     * from the given charset this returns one char randomly picked
     * @param chars
     * @return a char randomly picked
     */
    char nextChar(final String chars) {
        return chars.charAt(random.nextInt(chars.length()));
    }

    /**
     * The only public method you should cre about when consuming this class
     * @return random password
     */
    public String nextPassword() {
        //Allocate buffer
        CharBuffer buffer = CharBuffer.allocate(length);
        //collect from the charsets the minimum required number of chars from each set
        buffer = collectRequiredMinimums(buffer, charsets);
        //At this point we have guaranteed that we have the min number of chars from every charset
        int index = buffer.position();
        //Now we need to combine all the chars into one single string to even the odds for picking chars from all charsets
        final String combined = combineCharsets(charsets);
        //now while we still need to fill the char buffer
        while (index < length) {
            //Pick from the combined charsets
            final char chr = nextChar(combined);
            buffer.append(chr);
            index++;
        }

        ((Buffer) buffer).flip();
        //And now randomize the buffer to ensure we shuffle the result
        randomize(buffer);
        return buffer.toString();
    }

    /**
     * This will take charset and get from it the required minimum number of chars
     * @param buffer
     * @param charsets
     * @return
     */
    CharBuffer collectRequiredMinimums(final CharBuffer buffer, final List<Charset> charsets) {
        for (Charset charset : charsets) {
            for (int i = 0; i < charset.min; i++) {
                buffer.append(nextChar(charset.chars));
            }
        }
        return buffer;
    }

    /**
     * This is done to even the odds of picking chars
     * @param charsets
     * @return
     */
    String combineCharsets(final List<Charset> charsets) {
        return String.join("",
                charsets.stream().map(charset -> charset.chars).collect(Collectors.toSet()));
    }

    /**
     * Once the buffer has been filled we randomize the result
     * @param buffer
     */
    void randomize(final CharBuffer buffer) {
        char c;
        int n;
        for (int i = buffer.position(); i < buffer.limit(); i++) {
            n = random.nextInt(buffer.length());
            c = buffer.get(n);
            buffer.put(n, buffer.get(i));
            buffer.put(i, c);
        }
    }

    /**
     * Builder to facilitate configuring the class
     */
    public static class Builder {

        private final List<Charset> charsets = new ArrayList<>();
        private int length = 16;

        /**
         * Builder instance maker
         * @return
         */
        public PasswordGenerator build() {
            final int minSize = charsets.stream().mapToInt(value -> value.min).sum();
            if (length < minSize) {
                throw new IllegalArgumentException(
                        String.format("The desired length %d is under the minimum %d", length,
                                minSize));
            }
            return new PasswordGenerator(this.charsets, this.length);
        }

        /**
         * Add a new charset and set a min number of chars required from it
         * @param charset
         * @param min
         * @return
         */
        public Builder charset(final String charset, final int min) {
            Preconditions.checkNotNull(charset, "Charset param must not be null");
            Preconditions.checkArgument(min < charset.length(),
                    "min is expected to be less than " + charset.length());
            charsets.add(Charset.of(charset, min));
            return this;
        }

        /**
         * Specify the password length
         * @param length
         * @return
         */
        public Builder charset(final int length) {
            Preconditions.checkArgument(length <= 16, "The minimum length allowed is 16");
            this.length = length;
            return this;
        }

        /**
         * convenience method that basically passes what we really want to see used building this
         * @return
         */
        public Builder withDefaultValues() {
            return charset(SPECIAL_CHARS, 2).charset(UPPER_CASE_LETTERS_CHARS, 2)
                    .charset(LOWER_CASE_LETTERS_CHARS, 2).charset(NUMBER_CHARS, 2);
        }

        /**
         * Builds the generator using the default charsets (special, upper, lower, digits) each
         * filtered to only contain characters accepted by {@code passwords.regexptoolkit.pattern},
         * but only when {@code RegExpToolkit} is the active password toolkit.
         *
         * <p>Each default charset group retains its "at least one" minimum requirement — only
         * individual characters that the backend validator rejects are removed.  Any
         * pattern-allowed characters not covered by the four default groups are added as an
         * optional (min=0) supplementary charset, ensuring the generator never produces a
         * character the validator rejects while still generating the strongest possible
         * passwords.</p>
         *
         * <p>Falls back to {@link #withDefaultValues()} when:</p>
         * <ul>
         *   <li>The active toolkit is not {@code RegExpToolkit}.</li>
         *   <li>The pattern uses complex lookaheads with no extractable character class.</li>
         * </ul>
         *
         * @return this builder
         */
        public Builder withRegExpToolkitValues() {
            final String configuredToolkit = PropsUtil.get(PropsUtil.PASSWORDS_TOOLKIT);
            if ("com.liferay.portal.pwd.RegExpToolkit".equals(configuredToolkit)) {
                final String rawPattern = PropsUtil.get(PropsUtil.PASSWORDS_REGEXPTOOLKIT_PATTERN);
                final String allowedChars = extractCharset(rawPattern);
                if (allowedChars != null) {
                    // Honour the minimum length from the pattern's {n,} or {n,m} quantifier.
                    this.length = Math.max(this.length, extractMinLength(rawPattern));
                    return withFilteredValues(allowedChars);
                }
                Logger.warn(Builder.class,
                        "Could not extract a character class from passwords.regexptoolkit.pattern"
                        + " (value: \"" + rawPattern + "\"). "
                        + "Pattern must follow the /^[CharClass]{n,}$/ or /^[CharClass]{n,m}$/ shape. "
                        + "Complex lookahead patterns are not supported. "
                        + "Falling back to default password generator charsets.");
            }
            return withDefaultValues();
        }

        /**
         * Like {@link #withDefaultValues()} but each default charset is filtered so that only
         * characters present in {@code allowedChars} are kept.  Any characters in
         * {@code allowedChars} that fall outside the four default groups are appended as an
         * optional supplementary charset (min=0).
         *
         * <p>Package-private to allow direct unit-testing without a live PropsUtil context.</p>
         *
         * @param allowedChars the complete set of characters accepted by the backend validator
         * @return this builder
         */
        Builder withFilteredValues(final String allowedChars) {
            addFiltered(SPECIAL_CHARS, allowedChars, 1);
            addFiltered(UPPER_CASE_LETTERS_CHARS, allowedChars, 1);
            addFiltered(LOWER_CASE_LETTERS_CHARS, allowedChars, 1);
            addFiltered(NUMBER_CHARS, allowedChars, 1);

            // Include any pattern-allowed chars not covered by the four default groups.
            final String allDefaults =
                    SPECIAL_CHARS + UPPER_CASE_LETTERS_CHARS + LOWER_CASE_LETTERS_CHARS + NUMBER_CHARS;
            final String remainder = allowedChars.chars()
                    .filter(c -> allDefaults.indexOf(c) < 0)
                    .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                    .toString();
            if (!remainder.isEmpty()) {
                charset(remainder, 0);
            }
            return this;
        }

        /**
         * Filters {@code defaults} to the characters present in {@code allowed} and, if the
         * result is non-empty, registers it as a charset.  The minimum is capped at
         * {@code filtered.length() - 1} to satisfy the builder precondition.
         */
        private Builder addFiltered(final String defaults, final String allowed, final int desiredMin) {
            final String filtered = defaults.chars()
                    .filter(c -> allowed.indexOf(c) >= 0)
                    .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                    .toString();
            if (!filtered.isEmpty()) {
                charset(filtered, Math.min(desiredMin, filtered.length() - 1));
            }
            return this;
        }

        /**
         * Returns the JS regex literal for per-character filtering, suitable for direct
         * embedding in a JSP/JavaScript context.
         *
         * <p>When {@code RegExpToolkit} is the active toolkit and the configured pattern follows
         * the simple {@code /^[CharClass]{n,}$/} shape, returns {@code "/[CharClass]/"}.
         * Otherwise returns the string {@code "null"} so that the caller's JavaScript can fall
         * back to its own default {@code _pattern}.</p>
         *
         * @return a JS regex literal like {@code "/[A-Za-z0-9!@...]/"}, or {@code "null"}
         */
        public static String buildJsCharPattern() {
            final String configuredToolkit = PropsUtil.get(PropsUtil.PASSWORDS_TOOLKIT);
            if ("com.liferay.portal.pwd.RegExpToolkit".equals(configuredToolkit)) {
                final String rawPattern = PropsUtil.get(PropsUtil.PASSWORDS_REGEXPTOOLKIT_PATTERN);
                final String charClass = extractCharClass(rawPattern);
                if (charClass != null) {
                    return "/" + charClass + "/";
                }
            }
            return "null";
        }

        /**
         * Extracts the set of allowed characters from a simple {@code /^[CharClass]{n,}$/}
         * pattern by testing every printable ASCII character (32–126) against the extracted
         * character class.
         *
         * <p>Returns {@code null} when the pattern is {@code null}, empty, or does not match
         * the expected simple character-class shape (e.g. lookahead-based patterns).</p>
         *
         * @param rawPattern the raw Perl5 pattern string as stored in {@code portal.properties}
         * @return a string of all allowed characters, or {@code null} if extraction fails
         */
        static String extractCharset(final String rawPattern) {
            final String charClass = extractCharClass(rawPattern);
            if (charClass == null) {
                return null;
            }
            final Pattern charFilter;
            try {
                charFilter = Pattern.compile(charClass);
            } catch (PatternSyntaxException e) {
                return null;
            }
            // Collect every printable ASCII character accepted by the character class
            final StringBuilder allowed = new StringBuilder();
            for (char c = 32; c < 127; c++) {
                if (charFilter.matcher(String.valueOf(c)).matches()) {
                    allowed.append(c);
                }
            }
            return allowed.length() > 0 ? allowed.toString() : null;
        }

        /**
         * Extracts just the {@code [CharClass]} token from a {@code /^[CharClass]{n,}$/}
         * pattern.  Returns {@code null} when the pattern is {@code null}, empty, or does not
         * follow that simple shape.
         *
         * <p>The inner {@code (?:[^\]\\]|\\.)*} handles escaped characters such as {@code \[}
         * and {@code \]} inside the class.</p>
         *
         * @param rawPattern the raw Perl5 pattern string as stored in {@code portal.properties}
         * @return the {@code [...]} character-class string, or {@code null} if not extractable
         */
        static String extractCharClass(final String rawPattern) {
            if (rawPattern == null || rawPattern.trim().isEmpty()) {
                return null;
            }
            // \\{\\d+,\\d*\\} matches both {n,} (open) and {n,m} (bounded) quantifiers
            final java.util.regex.Matcher m = Pattern.compile(
                    "^/\\^(\\[(?:[^\\]\\\\]|\\\\.)*\\])\\{\\d+,\\d*\\}\\$/$"
            ).matcher(rawPattern.trim());
            return m.matches() ? m.group(1) : null;
        }

        /**
         * Extracts the minimum password length from a pattern's {@code {n,}} or {@code {n,m}}
         * quantifier (e.g. returns {@code 8} for {@code /^[...]{8,}$/}).
         *
         * <p>Returns {@code 0} when the pattern is {@code null}, empty, or contains no
         * recognisable quantifier, leaving the caller's current length unchanged.</p>
         *
         * @param rawPattern the raw Perl5 pattern string as stored in {@code portal.properties}
         * @return the minimum length declared by the quantifier, or {@code 0} if not found
         */
        static int extractMinLength(final String rawPattern) {
            if (rawPattern == null || rawPattern.trim().isEmpty()) {
                return 0;
            }
            // Matches {n,} or {n,m} — group 1 is the minimum
            final java.util.regex.Matcher m = Pattern.compile("\\{(\\d+),\\d*}")
                    .matcher(rawPattern.trim());
            return m.find() ? Integer.parseInt(m.group(1)) : 0;
        }

        static final String SPECIAL_CHARS = "!#%+:=?@";
        static final String UPPER_CASE_LETTERS_CHARS = "ABCDEFGHJKLMNPRSTUVWXYZ";
        static final String LOWER_CASE_LETTERS_CHARS = "abcdefghijklmnopqrstuvwxyz";
        static final String NUMBER_CHARS = "0123456789";

    }

    /**
     * Small Wrapper to group chars and indicate the minimum number of chars we need from it
     */
    static class Charset {

        final String chars;
        final int min;

        /**
         * private constructor
         * @param chars
         * @param min
         */
        private Charset(final String chars, final int min) {
            this.chars = chars;
            this.min = min;
        }

        /**
         * factory method
         * @param charSet
         * @param min
         * @return
         */
        static Charset of(final String charSet, final int min) {
            return new Charset(charSet, min);
        }
    }

}
