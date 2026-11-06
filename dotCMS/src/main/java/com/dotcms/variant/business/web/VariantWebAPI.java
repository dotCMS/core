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
     * Whether the given user decides which variant their request resolves on.
     *
     * @param user the {@link User} the request is served for, or null when there is none
     * @return true when the request may name its own variant
     */
    boolean selectsVariant(final User user);

    /**
     * Returns the variant the current request resolves on for the given user: the one the request
     * names when {@link #selectsVariant(User)} holds, the default variant otherwise.
     *
     * @param user the {@link User} the request is served for, or null when there is none
     * @return the variant name to resolve the request on
     */
    String currentVariantId(final User user);

    /**
     * Whether a version living in the given variant may be served to the given user.
     *
     * <p>A caller that names its own variant may read any of them; every other caller may only be
     * served the default one. This is the check for a lookup that resolves nothing of its own -
     * one by inode, where the caller already named the exact version it wants - so where
     * {@link #currentVariantId(User)} chooses a variant, this one refuses the wrong choice.
     *
     * @param variantId the variant the resolved version lives in
     * @param user the {@link User} the request is served for, or null when there is none
     * @return true when the version may be served to that user
     */
    boolean canReadVariant(final String variantId, final User user);

    /**
     * Returns the Lucene query to run for the given user, scoped to the variant that user is
     * allowed to read.
     *
     * <p>A caller that may name its own variant gets its query back untouched. For every other
     * caller, any {@code variant:} term it wrote is dropped and a single {@code +variant:default}
     * is put in front. Dropping the caller's term rather than only adding ours is what keeps the
     * answer useful: a query that named a variant comes back with the default version of that
     * content instead of coming back empty.
     *
     * <p>This rewrite is a convenience, not the guarantee. Every search path has the same clause
     * imposed further down, beside the live-only clause, so a term spelled in a way this method
     * does not recognise costs a less helpful answer and never access.
     *
     * @param query the Lucene query as the caller wrote it
     * @param user the {@link User} the search runs for, or null when there is none
     * @return the query to run
     */
    String scopeQueryToDefaultVariant(final String query, final User user);

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
