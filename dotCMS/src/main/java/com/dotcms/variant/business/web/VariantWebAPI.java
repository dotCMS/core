package com.dotcms.variant.business.web;

import com.dotcms.variant.model.Variant;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import java.util.Optional;

public interface VariantWebAPI {

    String currentVariantId();

    RenderContext getRenderContext(final long tryingLang, final String identifier,
            final PageMode pageMode, final User user);

    class RenderContext {
        private String currentVariantKey;
        private long currentLanguageId;

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
