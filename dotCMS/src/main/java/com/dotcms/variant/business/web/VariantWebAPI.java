package com.dotcms.variant.business.web;

import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;

/**
 * WebAPI for {@link com.dotcms.variant.model.Variant}
 */
public interface VariantWebAPI {

    /**
     * Return the current variant ID
     * @return
     */
    String currentVariantId();

    /**
     * Return the {@link com.dotcms.variant.model.Variant} and {@link com.dotmarketing.portlets.languagesmanager.model.Language}
     * to render a specific {@link com.dotcms.content.model.Contentlet} according to the versions that
     * this has.
     *
     * @param tryingLang Language to try if not exists any version for this lang try with default
     * @param identifier {@link com.dotcms.content.model.Contentlet}'s identifier
     * @param pageMode page mode to render
     * @param user to check {@link com.dotmarketing.beans.Permission}
     * @return
     */
    RenderContext getRenderContext(final long tryingLang, final String identifier,
            final PageMode pageMode, final User user);

    class RenderContext {
        private final String currentVariantKey;
        private final long currentLanguageId;

        public RenderContext(final String currentVariantKey,
                long currentLanguageId) {
            this.currentVariantKey = currentVariantKey;
            this.currentLanguageId = currentLanguageId;
        }

        public String getCurrentVariantKey() {
            return currentVariantKey;
        }

        public long getCurrentLanguageId() {
            return currentLanguageId;
        }
    }
}
