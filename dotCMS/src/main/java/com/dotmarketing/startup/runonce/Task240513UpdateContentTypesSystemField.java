package com.dotmarketing.startup.runonce;

import com.dotcms.contenttype.business.ContentTypeInitializer;
import com.dotmarketing.beans.Host;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.form.business.FormAPI;
import com.dotmarketing.startup.StartupTask;

/**
 * This UT is to update the column system for the Content Types. This value needs to be false
 * since system content types are being hidden from the Content Search Portlet.
 *
 * The system content types are: dotFavoritePage, forms, and Host.
 */
public class Task240513UpdateContentTypesSystemField implements StartupTask {

    @Override
    public boolean forceRun() {
        try {
            return new DotConnect().setSQL("select count(*) from structure where system = true")
                    .loadInt("count")>3;
        } catch (DotDataException e) {
            throw new RuntimeException(e);
        }


    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        new DotConnect().setSQL("update structure set system = false where velocity_var_name not in (?, ?, ?)")
                .addParam(ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME)
                .addParam(FormAPI.FORM_WIDGET_STRUCTURE_NAME_VELOCITY_VAR_NAME)
                .addParam(Host.HOST_VELOCITY_VAR_NAME)
                .loadResult();

    }
}
