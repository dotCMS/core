package com.dotmarketing.startup.runonce;

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
 * Adds the dotAI portlet to all layouts which have API Playground portlet too, if it does not already exists.
 * @author vico
 */
public class Task241009ReplaceLanguagesWithLocalesPortlet implements StartupTask {

    private static final String LANGUAGES_PORTLET_ID = "languages";
    private static final String LOCALES_PORTLET_ID = "locales";
    private static final String WORKFLOW_PORTLET_ID = "workflow-schemes";

    @Override
    public boolean forceRun() {
        // if no locales found then  flag it as true
        return getLayoutPortletsByPortletId(LOCALES_PORTLET_ID).isEmpty();
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        final List<Map<String, Object>> languagesLayoutPortlets = getLayoutPortletsByPortletId(LANGUAGES_PORTLET_ID);

        if (languagesLayoutPortlets.isEmpty()) {
            final List<Map<String, Object>> workflowLayoutPortlets = getLayoutPortletsByPortletId(WORKFLOW_PORTLET_ID);
            if (!workflowLayoutPortlets.isEmpty()) {
                final Map<String, Object> row = workflowLayoutPortlets.get(0);
                final String layoutId = (String) row.get("layout_id");
                final int portletOrder = Optional.ofNullable((Integer) row.get("portlet_order")).orElse(0) + 1;
                insertLocale(layoutId, portletOrder);
            }
        } else {
            languagesLayoutPortlets.forEach(this::replaceLanguage);
        }

        CacheLocator.getLayoutCache().clearCache();
    }

    private void replaceLanguage(final Map<String, Object> row) {
        final String layoutId = (String) row.get("layout_id");
        final int count = Try.of(
                        () -> new DotConnect()
                                .setSQL("SELECT COUNT(portlet_id) AS count" +
                                        " FROM cms_layouts_portlets" +
                                        " WHERE layout_id = ? AND portlet_id = ?")
                                .addParam(layoutId)
                                .getInt("count"))
                .getOrElse(0);
        if (count == 0) {
            final int portletOrder = Optional.ofNullable((Integer) row.get("portlet_order")).orElse(1);
            insertLocale(layoutId, portletOrder);

            final String id = (String) row.get("id");
            deleteLanguage(id);
        }
    }

    private void deleteLanguage(final String id) {
        Try.run(() -> new DotConnect()
                        .setSQL("DELETE FROM cms_layouts_portlets WHERE id = ?")
                        .addParam(id)
                        .loadResult())
                .getOrElseThrow(ex -> new RuntimeException(ex));
    }

    private static void insertLocale(final String layoutId, final int portletOrder) {
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
                                .setSQL("SELECT layout_id FROM cms_layouts_portlets WHERE portlet_id = ?")
                                .addParam(portletId)
                                .loadObjectResults())
                .getOrElse(List.of());
    }

}
