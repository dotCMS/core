package com.dotcms.rest.api.v1.accessibility;

import com.dotcms.enterprise.achecker.ACheckerRequest;
import com.dotcms.enterprise.achecker.ACheckerResponse;
import com.dotcms.enterprise.achecker.dao.GuidelinesDAO;
import com.dotcms.enterprise.achecker.impl.ACheckerImpl;
import com.dotcms.enterprise.achecker.model.GuideLineBean;
import com.dotcms.enterprise.achecker.tinymce.DaoLocator;
import com.dotcms.util.EnterpriseFeature;
import com.dotmarketing.util.UtilMethods;

import java.util.List;
import java.util.Map;

/**
 *
 *
 * @author Jose Castro
 * @since Sep 4th, 2024
 */
public class ACheckerHelper {

    private List<GuideLineBean> accessibilityGuidelineList = null;

    public static final String LANG = "lang";
    public static final String CONTENT = "content";
    public static final String GUIDELINES = "guidelines";
    public static final String FRAGMENT = "fragment";

    /**
     * Private constructor
     */
    private ACheckerHelper() {
        // Empty constructor
    }

    /**
     * Provides a singleton instance of the {@link ACheckerHelper}
     */
    private static class SingletonHolder {
        private static final ACheckerHelper INSTANCE = new ACheckerHelper();
    }

    /**
     * Returns a singleton instance of this class.
     *
     * @return A single instance of this class.
     */
    public static ACheckerHelper getInstance() {
        return ACheckerHelper.SingletonHolder.INSTANCE;
    }

    /**
     * Returns the list of Accessibility Guidelines available in the system.
     *
     * @return The list of Accessibility Guidelines available in the system.
     *
     * @throws Exception If the guidelines can't be retrieved.
     */
    @EnterpriseFeature
    public List<GuideLineBean> getAccessibilityGuidelineList() throws Exception {
        if (UtilMethods.isNotSet(this.accessibilityGuidelineList)){
            GuidelinesDAO gLines = DaoLocator.getGuidelinesDAO();
            this.accessibilityGuidelineList = gLines.getOpenGuidelines();
        }
        return this.accessibilityGuidelineList;
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
     * @throws Exception If the validation fails.
     */
    @EnterpriseFeature
    public ACheckerResponse validate(final Map<String, String> validationData) throws Exception {
        final String lang = validationData.get(LANG);
        final String content = validationData.get(CONTENT);
        final String guidelines = validationData.get(GUIDELINES);
        final String fragment = validationData.get(FRAGMENT);
        if (UtilMethods.isSet(lang) && lang.trim().length() == 2) {
            DaoLocator.getLangCodesDAO().getLangCodeBy3LetterCode(lang);
        }
        final ACheckerRequest request = new ACheckerRequest(lang, content, guidelines, Boolean.parseBoolean(fragment));
        return new ACheckerImpl().validate(request);
    }

}
