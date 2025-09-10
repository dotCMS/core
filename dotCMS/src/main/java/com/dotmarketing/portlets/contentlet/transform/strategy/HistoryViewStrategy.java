package com.dotmarketing.portlets.contentlet.transform.strategy;

import com.dotcms.api.APIProvider;
import com.dotcms.experiments.model.AbstractExperimentVariant;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.util.Map;
import java.util.Set;

import static com.dotmarketing.portlets.contentlet.model.Contentlet.ARCHIVED_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.INODE_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.LIVE_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.MOD_DATE_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.MOD_USER_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.TITTLE_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.WORKING_KEY;
import static com.dotmarketing.portlets.contentlet.transform.strategy.LanguageViewStrategy.mapLanguage;
import static com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions.CLEAR_EXISTING_DATA;

/**
 * This Transformation Strategy exposes a view of a Contentlet with minimum specific History
 * properties. They represent basically the information users can see in the History tab of the
 * Content Editor page.
 *
 * @author Jose Castro
 * @since Jul 28th, 2025
 */
class HistoryViewStrategy extends AbstractTransformStrategy<Contentlet> {

    public static final String EXPERIMENT_VARIANT = "experimentVariant";

    HistoryViewStrategy(final APIProvider toolBox) {
        super(toolBox);
    }

    @Override
    protected Map<String, Object> transform(final Contentlet contentlet, final Map<String, Object> map,
                                            final Set<TransformOptions> options, final User user)
            throws DotDataException, DotSecurityException {
        if (options.contains(CLEAR_EXISTING_DATA)) {
            map.clear();
        }
        map.put(INODE_KEY, contentlet.getInode());
        map.put(TITTLE_KEY, contentlet.getTitle());
        map.put(WORKING_KEY, contentlet.isWorking());
        map.put(LIVE_KEY, contentlet.isLive());
        map.put(ARCHIVED_KEY, contentlet.isArchived());
        map.put(MOD_USER_KEY, contentlet.getModUser());
        map.put(MOD_DATE_KEY, contentlet.getModDate());
        this.addLanguageAttrs(contentlet, map);
        final boolean isExperimentVariant = UtilMethods.isSet(contentlet.getVariantId())
                && contentlet.getVariantId().startsWith(AbstractExperimentVariant.EXPERIMENT_VARIANT_NAME_PREFIX);
        map.put(EXPERIMENT_VARIANT, isExperimentVariant);
        return map;
    }

    /**
     * Retrieves the language of the specific Contentlet and exposes specific attributes of it.
     *
     * @param contentlet The {@link Contentlet} object whose language will be retrieved.
     * @param map        The {@link Map} where the language attributes will be added.
     */
    private void addLanguageAttrs(final Contentlet contentlet, final Map<String, Object> map) {
        final Language language = toolBox.languageAPI.getLanguage(contentlet.getLanguageId());
        map.putAll(mapLanguage(language, false));
    }

}
