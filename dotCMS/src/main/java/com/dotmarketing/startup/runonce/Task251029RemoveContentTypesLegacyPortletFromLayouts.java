package com.dotmarketing.startup.runonce;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import io.vavr.control.Try;

/**
 * Removes the legacy content-types portlet from all layouts to prevent
 * Portlet Exception Error when users try to search using the legacy tool.
 * The new content-types-angular portlet should be used instead.
 * @author Neeha kethi
 */
public class Task251029RemoveContentTypesLegacyPortletFromLayouts implements StartupTask {

	@Override
	public boolean forceRun() {
		return Try.of(() -> new DotConnect()
				.setSQL("select count(portlet_id) as count from cms_layouts_portlets where portlet_id = 'content-types'")
				.getInt("count")).getOrElse(0) > 0;
	}

	@Override
	public void executeUpgrade() throws DotDataException, DotRuntimeException {
		new DotConnect()
				.setSQL("delete from cms_layouts_portlets where portlet_id = 'content-types'")
				.loadResult();

		CacheLocator.getLayoutCache().clearCache();
	}
}
