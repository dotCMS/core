package com.dotmarketing.plugin.util;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.dotcms.TestBase;
import com.liferay.util.FileUtil;

public class PluginMergerTest extends TestBase {

	@Test
	public void testMergeByAttribute() throws IOException {
		PluginFileMerger fileMerger = new PluginFileMerger();

		String rootPath = System.getProperty( "user.dir" ) + File.separator + "dotCMS";
		String name = "override-test";
		String dwr = "<create creator=\"new\" javascript=\"UserAjax\" scope=\"application\">\n"
				+ "<param name=\"class\" value=\"com.arqiva.plugins.ajax.ArqivaUserAjax\"/>\n"
				+ "</create>";

		Map<String,String> overrideMap = new HashMap<String, String>();
		overrideMap.put("create", "javascript");


		File dwrFile = new File(rootPath + File.separator + "WEB-INF"
				+ File.separator + "dwr.xml");

		File dwrCopy = new File(rootPath + File.separator + "WEB-INF"
				+ File.separator + "dwr-test.xml");

		FileUtil.copyFile(dwrFile, dwrCopy, false);


		fileMerger.mergeByAttribute(dwrCopy, "<!-- BEGIN PLUGINS -->",
				"<!-- END PLUGINS -->", "<!-- BEGIN PLUGIN:" + name + " -->",
				"<!-- END PLUGIN:" + name + " -->", dwr, overrideMap,
				"<!-- BEGIN OVERRIDE:" + name,
				" END OVERRIDE:" + name + " -->", "<!-- BEGIN OVERRIDE");


		FileReader fr = new FileReader(dwrCopy);
		BufferedReader br = new BufferedReader(fr);
		StringBuilder fileContent = new StringBuilder();
		String line = null;

		while((line=br.readLine())!=null) {
			fileContent.append(line);
		}

		br.close();

		String comentedPart = "<!-- BEGIN OVERRIDE:dwr-override<create creator=\"new\" javascript=\"UserAjax\" scope=\"application\">"
				+ "      <param name=\"class\" value=\"com.dotmarketing.portlets.user.ajax.UserAjax\"/>"
				+ "    </create> END OVERRIDE:dwr-override -->";

		String newPart = "<!-- BEGIN PLUGIN:dwr-override --><!-- BEGIN OVERRIDE:override-test<create creator=\"new\" javascript=\"UserAjax\" scope=\"application\">"
				+ "      <param name=\"class\" value=\"com.arqiva.plugins.ajax.ArqivaUserAjax\"/>"
				+ "   </create> END OVERRIDE:override-test --><!-- END PLUGIN:dwr-override -->";

		System.out.println(fileContent);

		assertTrue(fileContent.toString().contains(comentedPart));
		assertTrue(fileContent.toString().contains(newPart));


	}

}
