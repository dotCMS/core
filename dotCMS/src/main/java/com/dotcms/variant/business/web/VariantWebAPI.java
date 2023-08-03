package com.dotcms.variant.business.web;

import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import org.apache.velocity.exception.ResourceNotFoundException;

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
    RenderContext getRenderContextForceLangFallback(final long tryingLang, final String identifier,
            final PageMode pageMode, final User user);

    /**
     * Return the {@link ContentletVersionInfo} according to the follow algorithm:
     *
     * - Try to get a {@link ContentletVersionInfo} using <code>tryingLang</code> and the current
     * {@link com.dotcms.variant.model.Variant}.
     * - If it does not exist it try to get a {@link ContentletVersionInfo} using <code>tryingLang</code>
     * and the DEFAULT {@link com.dotcms.variant.model.Variant}.
     *
     * If any of the follow condition are TRUE
     *
     * - If the <code>Identifier</code> is from a FORM
     * - or If the <code>Identifier</code> is from a WIDGET and the DEFAULT_WIDGET_TO_DEFAULT_LANGUAGE
     * property is TRUE (Default value).
     * - or If the <code>Identifier</code> is ot from a WIDGET or a FORM and the
     * DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE property is true.
     *
     * then we try the follow:
     *
     * - If it does not exist it try to get a {@link ContentletVersionInfo} using the DEFAULT
     * {@link com.dotmarketing.portlets.languagesmanager.model.Language} and the current
     * {@link com.dotcms.variant.model.Variant}.
     * - If it does not exist it try to get a {@link ContentletVersionInfo} using the DEFAULT
     * {@link com.dotmarketing.portlets.languagesmanager.model.Language} and the DEFAULT
     * {@link com.dotcms.variant.model.Variant}.
     * - If it does not exist it throw a {@link ResourceNotFoundException}
     *
     * To get the Current {@link com.dotcms.variant.model.Variant} we are using
     * {@link VariantWebAPI#currentVariantId()} method.
     *
     * @param tryingLang {@link com.dotmarketing.portlets.languagesmanager.model.Language}'s id to try
     * @param identifier {@link com.dotmarketing.portlets.contentlet.model.Contentlet}'s id
     * @param pageMode
     * @param user
     * @return
     */
    ContentletVersionInfo getContentletVersionInfoByFallback(final long tryingLang, final String identifier,
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
