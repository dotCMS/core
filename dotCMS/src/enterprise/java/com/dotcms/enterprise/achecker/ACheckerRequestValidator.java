/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.achecker;

import com.dotmarketing.business.DotValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Checks the {@code guidelines} and {@code lang} values of an Accessibility Checker request before
 * they are used to look anything up. Guideline abbreviations may only contain letters, digits,
 * dots, underscores and hyphens, and the language must be a 2 or 3 letter code, optionally followed
 * by region or script subtags such as {@code en-US}. Values that do not follow these rules are
 * rejected with a {@link DotValidationException}, which the REST layer reports as a Bad Request.
 * Rejection messages only name the parameter, never the submitted value.
 */
public final class ACheckerRequestValidator {

    /** Message used when a {@code guidelines} value contains an abbreviation that is not accepted. */
    public static final String UNSUPPORTED_GUIDELINES_MESSAGE =
            "'" + ACheckerAPI.GUIDELINES + "' parameter contains an unsupported value";

    /** Message used when the {@code lang} value is not a 2 or 3 letter language code. */
    public static final String UNSUPPORTED_LANG_MESSAGE =
            "'" + ACheckerAPI.LANG + "' parameter must be a 2 or 3 letter language code";

    /** Largest number of guideline abbreviations accepted in one {@code guidelines} value. */
    public static final int MAX_GUIDELINE_TOKENS = 20;

    /** Message used when a {@code guidelines} value holds more abbreviations than allowed. */
    public static final String TOO_MANY_GUIDELINES_MESSAGE =
            "'" + ACheckerAPI.GUIDELINES + "' parameter accepts at most " + MAX_GUIDELINE_TOKENS + " values";

    private static final String GUIDELINE_SEPARATOR = ",";
    private static final Pattern GUIDELINE_TOKEN = Pattern.compile("[A-Za-z0-9._-]{1,100}");
    private static final Pattern LANGUAGE_TAG = Pattern.compile("[A-Za-z]{2,3}([-_][A-Za-z0-9]{2,8})*");
    private static final Pattern SUBTAG_SEPARATOR = Pattern.compile("[-_]");

    private ACheckerRequestValidator() {
        // Static utility class
    }

    /**
     * Splits a comma-separated list of guideline abbreviations into its individual abbreviations.
     * Each abbreviation is trimmed and blank entries are skipped; letter case and order are kept as
     * submitted. A {@code null} value, or one made only of commas and blanks, returns an empty
     * list so the caller can fall back to the default guideline.
     *
     * @param guidelines the raw {@code guidelines} value, e.g. {@code "WCAG2-AA,508"}
     * @return the abbreviations in submitted order, as a list that cannot be modified
     * @throws DotValidationException with {@link #TOO_MANY_GUIDELINES_MESSAGE} if there are more
     *                                than {@value #MAX_GUIDELINE_TOKENS} abbreviations, or with
     *                                {@link #UNSUPPORTED_GUIDELINES_MESSAGE} if any abbreviation is
     *                                longer than 100 characters or contains anything other than
     *                                letters, digits, '.', '_' or '-'
     */
    public static List<String> guidelineTokens(final String guidelines) {
        if (guidelines == null) {
            return Collections.emptyList();
        }
        final List<String> tokens = new ArrayList<>();
        for (final String rawToken : guidelines.split(GUIDELINE_SEPARATOR, -1)) {
            final String token = rawToken.trim();
            if (token.isEmpty()) {
                continue;
            }
            if (!GUIDELINE_TOKEN.matcher(token).matches()) {
                throw new DotValidationException(UNSUPPORTED_GUIDELINES_MESSAGE);
            }
            if (tokens.size() == MAX_GUIDELINE_TOKENS) {
                throw new DotValidationException(TOO_MANY_GUIDELINES_MESSAGE);
            }
            tokens.add(token);
        }
        return Collections.unmodifiableList(tokens);
    }

    /**
     * Normalizes the optional language of the content being checked. A missing or blank value
     * means no language was requested and returns {@code null}. Otherwise the value is trimmed and
     * may be a plain 2 or 3 letter code ({@code en}, {@code eng}) or a code followed by region or
     * script subtags separated by '-' or '_' ({@code en-US}, {@code pt_BR}). Only the leading
     * language code is returned, in lower case, since language codes are stored in lower case.
     *
     * @param lang the raw {@code lang} value, e.g. {@code "en"}, {@code "eng"} or {@code "en-US"}
     * @return the lower-case 2 or 3 letter language code, or {@code null} when none was given
     * @throws DotValidationException if the value does not start with a 2 or 3 letter code, or a
     *                                subtag is not made of 2 to 8 letters or digits
     */
    public static String lang(final String lang) {
        if (lang == null || lang.trim().isEmpty()) {
            return null;
        }
        final String trimmedLang = lang.trim();
        if (!LANGUAGE_TAG.matcher(trimmedLang).matches()) {
            throw new DotValidationException(UNSUPPORTED_LANG_MESSAGE);
        }
        final String primaryLanguage = SUBTAG_SEPARATOR.split(trimmedLang, 2)[0];
        return primaryLanguage.trim().toLowerCase(Locale.ROOT);
    }

}
