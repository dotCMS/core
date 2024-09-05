package com.dotcms.enterprise.achecker.impl;

import com.dotcms.enterprise.achecker.ACheckerAPI;
import com.dotcms.enterprise.achecker.ACheckerRequest;
import com.dotcms.enterprise.achecker.ACheckerResponse;
import com.dotcms.enterprise.achecker.dao.GuidelinesDAO;
import com.dotcms.enterprise.achecker.model.GuideLineBean;
import com.dotcms.enterprise.achecker.tinymce.DaoLocator;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.util.EnterpriseFeature;
import com.dotmarketing.business.DotValidationException;
import com.dotmarketing.exception.DotDataException;
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
    @EnterpriseFeature
    public ACheckerResponse validate(final Map<String, String> validationData) throws DotValidationException {
        final String lang = validationData.get(LANG);
        final String content = validationData.get(CONTENT);
        final String guidelines = validationData.get(GUIDELINES);
        final String fragment = validationData.get(FRAGMENT);
        try {
            if (UtilMethods.isSet(lang) && lang.trim().length() == 2) {
                DaoLocator.getLangCodesDAO().getLangCodeBy3LetterCode(lang);
            }
            final ACheckerRequest request = new ACheckerRequest(lang, content, guidelines, Boolean.parseBoolean(fragment));
            return new ACheckerImpl().validate(request);
        } catch (final Exception e) {
            throw new DotValidationException(ExceptionUtil.getErrorMessage(e), e);
        }
    }

}
