package com.dotmarketing.startup.runonce;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.UUIDUtil;
import io.vavr.control.Try;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Adds the dotAI portlet to all layouts which have API Playground portlet too, if it does not already exists.
 * @author dsilvam
 */
public class Task240530AddDotAIPortletToLayout implements StartupTask {

	private static final String DOT_AI_PORTLET_ID = "dotai";

	@Override
	public boolean forceRun() {

		// first we get all layouts we have maintenance portelt
		final List<Object> apiPlaygroundLayouts = getAPIPlaygroundLayouts();

		// then check if all layouts which contains API Playground also have dotAI
		return
				apiPlaygroundLayouts.stream().map(layoutId -> Try.of(()->new DotConnect()
				.setSQL("select count(portlet_id) as count from cms_layouts_portlets where layout_id = ? and portlet_id = '"+DOT_AI_PORTLET_ID+"'")
				.addParam(layoutId)
				.getInt("count")).getOrElse(0))
				.anyMatch(count -> count == 0); // with just one layout without dotAI, needs to run
	}

	private List<Object> getAPIPlaygroundLayouts() {
		return Try.of(() -> new DotConnect()
				.setSQL("select layout_id from cms_layouts_portlets where portlet_id = 'api_playground'")
				.loadObjectResults().stream().map(row -> row.get("layout_id")).collect(Collectors.toList()))
				.getOrElse(Collections.emptyList());
	}

	@Override
	public void executeUpgrade() throws DotDataException, DotRuntimeException {

		// first we get all layouts we have API Playground portlet
		final List<Object> apiPlaygroundLayouts = getAPIPlaygroundLayouts();

		for (final Object layoutId : apiPlaygroundLayouts) {

			// if the layout does not have dotai. Go and add it
			if (Try.of(()->new DotConnect()
					.setSQL("select count(portlet_id) as count from cms_layouts_portlets where layout_id = ? and portlet_id = '"+DOT_AI_PORTLET_ID+"'")
					.addParam(layoutId)
					.getInt("count")).getOrElse(0) == 0) {

				final int portletOrder = Try.of(()->new DotConnect()
						.setSQL("select max(portlet_order) as portlet_order from cms_layouts_portlets where layout_id = ?")
						.setMaxRows(1)
						.addParam(layoutId)
						.getInt("portlet_order")).getOrElse(0);

				new DotConnect()
						.setSQL("insert into cms_layouts_portlets(id, layout_id, portlet_id, portlet_order) values (?,?,?,?)")
						.addParam(UUIDUtil.uuid())
						.addParam(layoutId)
						.addParam(DOT_AI_PORTLET_ID)
						.addParam(portletOrder+1)
						.loadResult();
			}
		}

		CacheLocator.getLayoutCache().clearCache();
	}
}
