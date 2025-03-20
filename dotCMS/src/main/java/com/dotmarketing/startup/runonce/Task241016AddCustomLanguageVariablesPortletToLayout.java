package com.dotmarketing.startup.runonce;

import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.Portlet;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adds the custom 'Language Variables' portlet to all layouts which have 'Locales' portlet too, if
 * it does not already exist. If there is already a Language Variables portlet, that one will be
 * used instead.
 *
 * @author Jose Castro
 * @since Oct 15th, 2024
 */
public class Task241016AddCustomLanguageVariablesPortletToLayout implements StartupTask {

    public static final String LANGUAGE_VARIABLES_PORTLET_ID = "c_Language-Variables";
    public static final String LANGUAGE_VARIABLES_PORTLET_NAME = "Language Variables";
    public static final String LANGUAGE_VARIABLES_CT_VAR_NAME = "Languagevariable";

    private static final String LOCALES_PORTLET_ID = "locales";

    /**
     * Verifies if the custom {@code Language Variables} portlet must be added or not. It performs
     * the following data checks:
     * <ul>
     *     <li>The {@code Language Variables} portlet must be added in the same layout as the
     *     {@code Locales} portlet.</li>
     *     <li>If the {@code Locales} portlet is not part of any menu, the
     *     {@code Language Variables} portlet must be added manually then.</li>
     *     <li>If the {@code Language Variables} portlet is already present, the UT can be skipped.
     *     </li>
     * </ul>
     *
     * @return If the UT must run, returns {@code true}.
     */
    @Override
    public boolean forceRun() {
        try {
            final List<Object> layoutIDs = this.getLayoutIDsContainingLocalesPortletID();
            if (UtilMethods.isNotSet(layoutIDs)) {
                Logger.warn(this, "The 'Locales' portlet has not been added to any Layout yet. " +
                        "The custom 'Language Variables' portlet cannot be added automatically. Please add it manually");
                return false;
            }
            final Set<Integer> layoutsContainingLangVarPortlet =
                    layoutIDs.stream().map(layoutId -> new DotConnect()
                            .setSQL("SELECT COUNT(portlet_id) AS count FROM cms_layouts_portlets WHERE layout_id = ? AND portlet_id = ?")
                            .addParam(layoutId)
                            .addParam(LANGUAGE_VARIABLES_PORTLET_ID)
                            .getInt("count"))
                    .collect(Collectors.toSet());
            return layoutsContainingLangVarPortlet.stream().anyMatch(count -> count == 0);
        } catch (final DotDataException e) {
            Logger.error(this, String.format("An error occurred when adding the custom 'Language Variables' portlet. " +
                    "Please add it manually: %s", ExceptionUtil.getErrorMessage(e)), e);
        }
        return false;
    }

    /**
     * Adds the custom {@code Language Variables} portlet to the appropriate Layouts by either
     * creating it from scratch, or using the exising one.
     *
     * @throws DotDataException An error occurred when adding the custom 'Language Variables'
     *                          portlet.
     */
    @Override
    public void executeUpgrade() throws DotDataException {
        Logger.info(this, "Adding the custom 'Language Variables' portlet to existing layouts");
        String languageVariablesPortletId = this.getExistingLanguageVariablesPortletID();
        if (UtilMethods.isNotSet(languageVariablesPortletId)) {
            final Map<String, String> newMap = Map.of(
                    "name", LANGUAGE_VARIABLES_PORTLET_NAME,
                    "baseTypes", "",
                    "dataViewMode", "list",
                    "view-action", "/ext/contentlet/view_contentlets",
                    "portletSource", "db",
                    "contentTypes", LANGUAGE_VARIABLES_CT_VAR_NAME
            );
            FactoryLocator.getPortletFactory().insertPortlet(
                    new Portlet(LANGUAGE_VARIABLES_PORTLET_ID, "SHARED_KEY",
                            "com.liferay.portlet.StrutsPortlet", newMap));
            languageVariablesPortletId = LANGUAGE_VARIABLES_PORTLET_ID;
            Logger.info(this, "The 'Language Variables' portlet has been created");
        } else {
            Logger.warn(this, String.format("There is already a 'Language Variables' portlet [ID = '%s'" +
                            "] in the system. Adding it to the same Layouts as the 'Locales' portlet"
                    , languageVariablesPortletId));
        }
        final List<Object> layoutIDs = this.getLayoutIDsContainingLocalesPortletID();
        for (final Object layoutID : layoutIDs) {
            final boolean isLayoutMissingLangVarPortlet = 0 == new DotConnect()
                    .setSQL("SELECT COUNT(portlet_id) AS count FROM cms_layouts_portlets WHERE layout_id = ? AND portlet_id = ?")
                    .addParam(layoutID)
                    .addParam(languageVariablesPortletId)
                    .getInt("count");
            if (isLayoutMissingLangVarPortlet) {
                final int portletOrder = new DotConnect()
                        .setSQL("SELECT max(portlet_order) AS portlet_order FROM cms_layouts_portlets WHERE layout_id = ?")
                        .setMaxRows(1)
                        .addParam(layoutID)
                        .getInt("portlet_order");
                new DotConnect()
                        .setSQL("INSERT INTO cms_layouts_portlets(id, layout_id, portlet_id, portlet_order) VALUES (?, ?, ?, ?)")
                        .addParam(UUIDUtil.uuid())
                        .addParam(layoutID)
                        .addParam(languageVariablesPortletId)
                        .addParam(portletOrder + 1)
                        .loadResult();
            }
        }
        CacheLocator.getLayoutCache().clearCache();
        Logger.info(this, String.format("The 'Language Variables' portlet has been added to %d menu group(s) successfully!",
                layoutIDs.size()));
    }

    /**
     * Returns all the Layout IDs; i.e., menu groups, containing the {@code Locales} portlet. That
     * information will allow us to add the {@code Language Variables} portlet in the expected
     * location.
     *
     * @return List of Layout IDs where the {@code Locales} portlet is part of.
     *
     * @throws DotDataException An error occurred while querying the database.
     */
    private List<Object> getLayoutIDsContainingLocalesPortletID() throws DotDataException {
        return new DotConnect().setSQL("SELECT layout_id FROM cms_layouts_portlets WHERE portlet_id = ?")
                .addParam(LOCALES_PORTLET_ID)
                .loadObjectResults().stream().map(row -> row.get("layout_id"))
                .collect(Collectors.toList());
    }

    /**
     * Returns the ID of the {@code Language Variables} portlet, in case it already exists in the
     * system, or {@code null} if not present.
     *
     * @return The ID of the existing {@code Language Variables} portlet.
     *
     * @throws DotDataException An error occurred while querying the database.
     */
    private String getExistingLanguageVariablesPortletID() throws DotDataException {
        final List<Map<String, Object>> results = new DotConnect()
                .setSQL("SELECT portletid FROM portlet WHERE portletid = ? OR LOWER(defaultpreferences) LIKE '%<value>language variables</value>%'")
                .addParam(LANGUAGE_VARIABLES_PORTLET_ID)
                .loadObjectResults();
        return results.isEmpty() || !UtilMethods.isSet(results.get(0))
                ? null
                : results.get(0).get("portletid").toString();
    }

}
