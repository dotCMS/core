package com.dotmarketing.startup.runonce;

import com.dotcms.util.ConversionUtils;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.UUIDUtil;
import io.vavr.control.Try;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Adds the dotAI portlet to all layouts which have API Playground portlet too, if it does not already exist.
 * @author vico
 */
public class Task241015ReplaceLanguagesWithLocalesPortlet implements StartupTask {

    public static final String LANGUAGES_PORTLET_ID = "languages";
    public static final String LOCALES_PORTLET_ID = "locales";
    public static final String WORKFLOW_PORTLET_ID = "workflow-schemes";

    /**
     * Determines if the task should be forced to run.
     *
     * @return true if no locales portlet is found in any layout, false otherwise.
     */
    @Override
    public boolean forceRun() {
        // if no locales found then  flag it as true
        return getLayoutPortletsByPortletId(LOCALES_PORTLET_ID).isEmpty();
    }

    /**
     * Executes the upgrade task.
     * Adds the locales portlet to layouts containing the languages portlet or the workflow portlet.
     * Replaces the languages portlet with the locales portlet.
     * Clears the layout cache.
     *
     * @throws DotDataException if there is an error accessing the database.
     * @throws DotRuntimeException if there is a runtime error during execution.
     */
    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        final List<Map<String, Object>> languagesLayoutPortlets = getLayoutPortletsByPortletId(LANGUAGES_PORTLET_ID);

        if (languagesLayoutPortlets.isEmpty()) {
            final List<Map<String, Object>> workflowLayoutPortlets = getLayoutPortletsByPortletId(WORKFLOW_PORTLET_ID);
            if (!workflowLayoutPortlets.isEmpty()) {
                final Map<String, Object> row = workflowLayoutPortlets.get(0);
                final String layoutId = (String) row.get("layout_id");
                final int portletOrder = Optional.ofNullable(ConversionUtils.toInt(row.get("portlet_order"), 0)).orElse(0) + 1;
                insertLocalesPortlet(layoutId, portletOrder);
            }
        } else {
            languagesLayoutPortlets.forEach(this::replaceLanguage);
        }

        CacheLocator.getLayoutCache().clearCache();
    }

    private void replaceLanguage(final Map<String, Object> row) {
        final String layoutId = (String) row.get("layout_id");
        final int portletOrder = Optional.ofNullable(ConversionUtils.toInt(row.get("portlet_order"), 0)).orElse(1);
        insertLocalesPortlet(layoutId, portletOrder);

        final String id = (String) row.get("id");
        deleteLanguagesPortlet(id);
    }

    private void deleteLanguagesPortlet(final String id) {
        Try.run(() -> new DotConnect()
                        .executeStatement(String.format("DELETE FROM cms_layouts_portlets WHERE id = '%s'", id)))
                .getOrElseThrow(ex -> new RuntimeException(ex));
    }

    private static void insertLocalesPortlet(final String layoutId, final int portletOrder) {
        Try.run(() -> new DotConnect()
                        .setSQL("INSERT INTO cms_layouts_portlets(id, layout_id, portlet_id, portlet_order)" +
                                " VALUES (?, ?, ?, ?)")
                        .addParam(UUIDUtil.uuid())
                        .addParam(layoutId)
                        .addParam(LOCALES_PORTLET_ID)
                        .addParam(portletOrder)
                        .loadResult())
                .getOrElseThrow(ex -> new RuntimeException(ex));
    }

    private List<Map<String, Object>> getLayoutPortletsByPortletId(final String portletId) {
        return Try.of(
                        () -> new DotConnect()
                                .setSQL("SELECT * FROM cms_layouts_portlets WHERE portlet_id = ?")
                                .addParam(portletId)
                                .loadObjectResults())
                .getOrElse(List.of());
    }

}
