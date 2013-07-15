package com.dotmarketing.plugin;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.dotcms.TestBase;
import com.dotmarketing.plugin.util.PluginFileMerger;

public class PluginMergerTest extends TestBase {

	 @Test
	public void testMergeByAttribute() throws IOException {
		PluginFileMerger fileMerger = new PluginFileMerger();

		String name = "override-test";

		StringBuilder sb = new StringBuilder();
		String newline = System.getProperty("line.separator");

		sb.append("<create creator=\"new\" javascript=\"UserAjax\" scope=\"application\">");
		sb.append(newline).append("<param name=\"class\" value=\"com.mycompany.plugins.ajax.MyCompanyUserAjax\"/>");
		sb.append(newline).append("</create>");

		String dwr = sb.toString();

		Map<String,String> overrideMap = new HashMap<String, String>();
		overrideMap.put("create", "javascript");
		sb = new StringBuilder("<!DOCTYPE dwr PUBLIC \"-//GetAhead Limited//DTD Direct Web Remoting 3.0//EN\" \"http://getahead.org/dwr//dwr30.dtd\">");
		sb.append("<dwr>");
		sb.append("<allow>");
		sb.append("<create creator=\"new\" javascript=\"UserAjax\" scope=\"application\">");
		sb.append("<param name=\"class\" value=\"com.dotmarketing.portlets.user.ajax.UserAjax\"/>");
		sb.append("</create>");
		sb.append("<!-- Don't ever delete the following comment tags, it will break the plugin system -->");
		sb.append("<!-- BEGIN PLUGINS -->");
		sb.append("<!-- END PLUGINS -->");
		sb.append("</allow>");
		sb.append("</dwr>");

		InputStream input = new ByteArrayInputStream(sb.toString().getBytes());

		String fileContent = fileMerger.mergeByAttribute(input, "<!-- BEGIN PLUGINS -->",
				"<!-- END PLUGINS -->", "<!-- BEGIN PLUGIN:" + name + " -->", "<!-- END PLUGIN:" + name + " -->", dwr,
				overrideMap, "<!-- BEGIN OVERRIDE:" + name, " END OVERRIDE:" + name + " -->", "<!-- BEGIN OVERRIDE");


		sb = new StringBuilder("<!-- BEGIN OVERRIDE:override-test");
		sb.append(newline).append("<create creator=\"new\" javascript=\"UserAjax\" scope=\"application\"><param name=\"class\" value=\"com.dotmarketing.portlets.user.ajax.UserAjax\"/></create>");
		sb.append(newline).append(" END OVERRIDE:override-test -->");

		String comentedPart = sb.toString();

		sb = new StringBuilder("<!-- BEGIN PLUGIN:override-test -->");
		sb.append(newline).append("<create creator=\"new\" javascript=\"UserAjax\" scope=\"application\">");
		sb.append(newline).append("<param name=\"class\" value=\"com.mycompany.plugins.ajax.MyCompanyUserAjax\"/>");
		sb.append(newline).append("</create>");
		sb.append(newline).append("<!-- END PLUGIN:override-test -->");

		String newPart = sb.toString();

		assertTrue(fileContent.toString().contains(comentedPart));
		assertTrue(fileContent.toString().contains(newPart));


	}

}
