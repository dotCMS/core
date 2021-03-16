package com.dotmarketing.startup.runonce;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import io.vavr.control.Try;

/**
 * This UT is since we removed font awesome, so icons for the layouts (toolgroups) are not showing.
 * Setting all the font awesome icons to label_important so user at least see an icon.
 */
public class Task210316UpdateLayoutIcons implements StartupTask {

    private static final String CHECK_FA_ICONS_QUERY = "select count(*) from cms_layout where description like 'fa-%'";
    private static final String UPDATE_ICONS_QUERY = "update cms_layout set description = ? where description like 'fa-%'";
    private static final String LABEL_IMPORTANT_ICON = "label_important";

    @Override
    public boolean forceRun() {
        //Only runs the UT if at least 1 Layout is still using the FA icons
        final int amountLayoutsWithFaIcon = Try.of(()->new DotConnect().setSQL(CHECK_FA_ICONS_QUERY)
                .getInt("count")).getOrElse(0);
        return amountLayoutsWithFaIcon == 0 ? false : true;
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        new DotConnect()
                .setSQL(UPDATE_ICONS_QUERY)
                .addParam(LABEL_IMPORTANT_ICON)
                .loadResult();

        CacheLocator.getLayoutCache().clearCache();
    }
}
