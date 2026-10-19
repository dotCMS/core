package com.dotcms.enterprise.achecker.impl;

import com.dotcms.enterprise.achecker.ACheckerAPI;
import com.dotcms.enterprise.achecker.ACheckerRequest;
import com.dotcms.enterprise.achecker.ACheckerRequestValidator;
import com.dotcms.enterprise.achecker.ACheckerResponse;
import com.dotcms.enterprise.achecker.dao.GuidelinesDAO;
import com.dotcms.enterprise.achecker.model.GuideLineBean;
import com.dotcms.enterprise.achecker.tinymce.DaoLocator;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.util.EnterpriseFeature;
import com.dotmarketing.business.DotValidationException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.util.List;
import java.util.Map;

/**
 * Implements the {@link ACheckerAPI} interface.
 *
 * @author Jose Castro
 * @since Sep 5th, 2024
 */
public class ACheckerAPIImpl implements ACheckerAPI {

    /** Message reported when the check fails for a reason other than a rejected request value. */
    static final String CHECK_FAILED_MESSAGE = "The accessibility check could not be completed";

    private List<GuideLineBean> accessibilityGuidelineList = null;

    /**
     * Returns the list of Accessibility Guidelines available in the system.
     *
     * @return The list of Accessibility Guidelines available in the system.
     *
     * @throws Exception If the guidelines can't be retrieved.
     */
    @EnterpriseFeature
    public List<GuideLineBean> getAccessibilityGuidelineList() throws DotDataException {
        try {
            if (UtilMethods.isNotSet(this.accessibilityGuidelineList)) {
                GuidelinesDAO gLines = DaoLocator.getGuidelinesDAO();
                this.accessibilityGuidelineList = gLines.getOpenGuidelines();
            }
            return this.accessibilityGuidelineList;
        } catch (final Exception e) {
            throw new DotDataException(ExceptionUtil.getErrorMessage(e), e);
        }
    }

    /**
     * Validates the given content against the specified Accessibility Guidelines.
     * <p>The {@code guidelines} and {@code lang} values are checked by
     * {@link ACheckerRequestValidator} before anything is looked up. When no guideline
     * abbreviation is given, the default guideline is used. Any other failure during the check is
     * logged and reported with a fixed message that does not repeat the underlying error.</p>
     *
     * @param validationData Map containing the parameters to validate:
     *                       <ul>
     *                           <li>{@code lang}: the optional language of the content to
     *                           validate, as a 2 or 3 letter code (for example {@code en} or
     *                           {@code eng})</li>
     *                           <li>{@code content}: the content to validate</li>
     *                           <li>{@code guidelines}: the guidelines to validate against
     *                           (comma-separated list of guideline abbreviations)</li>
     *                           <li>{@code fragment}: whether the content is a fragment (does
     *                           not contain the required HTML tags)</li>
     *                       </ul>
     *
     * @return The result of the validation, as a JSON object.
     *
     * @throws DotValidationException If the {@code guidelines} or {@code lang} value is not
     *                                accepted, if a requested guideline does not exist, or, with
     *                                the message "The accessibility check could not be completed",
     *                                if the check fails for any other reason.
     */
    @EnterpriseFeature
    public ACheckerResponse validate(final Map<String, String> validationData) throws DotValidationException {
        final List<String> guidelineTokens =
                ACheckerRequestValidator.guidelineTokens(validationData.get(GUIDELINES));
        final String lang = ACheckerRequestValidator.lang(validationData.get(LANG));
        final String content = validationData.get(CONTENT);
        final String fragment = validationData.get(FRAGMENT);
        try {
            final ACheckerRequest request = new ACheckerRequest(lang, content,
                    String.join(",", guidelineTokens), Boolean.parseBoolean(fragment));
            return new ACheckerImpl().validate(request);
        } catch (final DotValidationException e) {
            throw e;
        } catch (final Exception e) {
            Logger.error(this, "Accessibility check failed: " + e.getClass().getName(), e);
            throw new DotValidationException(CHECK_FAILED_MESSAGE);
        }
    }

}
