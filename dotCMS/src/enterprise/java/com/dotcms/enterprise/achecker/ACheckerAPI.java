package com.dotcms.enterprise.achecker;

import com.dotcms.enterprise.achecker.model.GuideLineBean;
import com.dotmarketing.business.DotValidationException;
import com.dotmarketing.exception.DotDataException;

import java.util.List;
import java.util.Map;

/**
 * This API provides the methods to interact with the Accessibility Checker service in dotCMS. Users
 * of this API can access the different Accessibility Guidelines available in the system, and
 * validate content against them.
 *
 * @author Jose Castro
 * @since Sep 5th, 2024
 */
public interface ACheckerAPI {

    String LANG = "lang";
    String CONTENT = "content";
    String GUIDELINES = "guidelines";
    String FRAGMENT = "fragment";

    /**
     * Returns the list of Accessibility Guidelines available in the system.
     *
     * @return The list of Accessibility Guidelines available in the system.
     *
     * @throws DotDataException If the guidelines can't be retrieved.
     */
    List<GuideLineBean> getAccessibilityGuidelineList() throws DotDataException;

    /**
     * Validates the given content against the specified Accessibility Guidelines.
     *
     * @param validationData Map containing the parameters to validate:
     *                       <ul>
     *                           <li>{@code lang}: the language of the content to validate (ISO
     *                           639-1, 2 letters)</li>
     *                           <li>{@code content}: the content to validate</li>
     *                           <li>{@code guidelines}: the guidelines to validate against
     *                           (comma-separated list of guideline abbreviations)</li>
     *                           <li>{@code fragment}: whether the content is a fragment (does
     *                           not contain the required HTML tags)</li>
     *                       </ul>
     *
     * @return The result of the validation, as a JSON object.
     *
     * @throws DotValidationException If the validation fails.
     */
    ACheckerResponse validate(final Map<String, String> validationData) throws DotValidationException;

}
