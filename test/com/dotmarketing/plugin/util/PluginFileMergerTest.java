package com.dotmarketing.plugin.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

public class PluginFileMergerTest extends TestCase {

	public void testSeparateXML() {
		PluginFileMerger pl = new PluginFileMerger();
		String frag = "<servlet>\n"
				+ "<servlet-name>HelloWorldServlet</servlet-name>\n"
				+ "<servlet-class>com.dotmarketing.plugins.hello.world.HelloWorldServlet</servlet-class>\n"
				+ "</servlet>\n" + "<servlet-mapping>\n"
				+ "<servlet-name>HelloWorldServlet</servlet-name>\n"
				+ "<url-pattern>/plugins/hello.world/hello</url-pattern>\n"
				+ "</servlet-mapping>\n";
		List<String> list = new ArrayList<String>();
		list.add("servlet");
		list.add("servlet-mapping");
		Map<String, String> map = pl.separateXML(frag, list);
		String expected1 = "<servlet>\n"
				+ "<servlet-name>HelloWorldServlet</servlet-name>\n"
				+ "<servlet-class>com.dotmarketing.plugins.hello.world.HelloWorldServlet</servlet-class>\n"
				+ "</servlet>";
		assertEquals("Checking servlet items", expected1, map.get("servlet"));
	}

	public void testMerge() {
		PluginFileMerger pl = new PluginFileMerger();
		String source = "<?xml version=\"1.0\"?>\n" + "<toolbox>\n"
				+ "<!-- BEGIN PLUGINS -->\n" + "<!-- END PLUGINS -->\n"
				+ "<\toolbox>";
		String fragment = "<tool>\n"
				+ "<key>helloWorld</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.plugins.hello.world.HelloViewTool</class>\n"
				+ "</tool>";

		InputStream is = new ByteArrayInputStream(source.getBytes());

		String expected = "<?xml version=\"1.0\"?>\n"
				+ "<toolbox>\n"
				+ "<!-- BEGIN PLUGINS -->\n"
				+ "<!-- BEGIN PLUGIN:test -->\n"
				+ "<tool>\n"
				+ "<key>helloWorld</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.plugins.hello.world.HelloViewTool</class>\n"
				+ "</tool>\n" + "<!-- END PLUGIN:test -->\n"
				+ "<!-- END PLUGINS -->\n" + "<\toolbox>\n";
		try {
			String merge = pl.merge(is, "<!-- BEGIN PLUGINS -->",
					"<!-- END PLUGINS -->", "<!-- BEGIN PLUGIN:test -->",
					"<!-- END PLUGIN:test -->", fragment);
			assertEquals("Checking merged text", expected, merge);
			is = new ByteArrayInputStream(merge.getBytes());
			merge = pl.merge(is, "<!-- BEGIN PLUGINS -->",
					"<!-- END PLUGINS -->", "<!-- BEGIN PLUGIN:test -->",
					"<!-- END PLUGIN:test -->", fragment);
			assertEquals(
					"Checking merged text (second run to make sure it can replace correctly)",
					expected, merge);

			String source2 = "<?xml version=\"1.0\"?>\n"
					+ "<toolbox>\n"
					+ "<!-- BEGIN PLUGINS --><!-- BEGIN PLUGIN:test --><tool>"
					+ "<key>helloWorld</key><scope>application</scope>"
					+ "<class>com.dotmarketing.plugins.hello.world.HelloViewTool</class></tool>"
					+ "<!-- END PLUGIN:test --><!-- END PLUGINS -->\n"
					+ "<\toolbox>\n";
			is = new ByteArrayInputStream(source2.getBytes());
			merge = pl.merge(is, "<!-- BEGIN PLUGINS -->",
					"<!-- END PLUGINS -->", "<!-- BEGIN PLUGIN:test -->",
					"<!-- END PLUGIN:test -->", fragment);
			assertEquals(
					"Checking merged text when tag already exists (all on same line)",
					expected, merge);
		} catch (IOException e) {
			fail("IOException: " + e.getMessage());
		}

	}
	

	public void testMergeWithOverride() {
		PluginFileMerger pl = new PluginFileMerger();
		//The <name> element was added since it was broken in the key tag wasn't the first
		String source = "<?xml version=\"1.0\"?>\n" + "<toolbox>\n"
				+ "<tool>\n"
				+ "<name>XXX</name>"				
				+ "<key>helloWorld</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
				+ "</tool>\n" + "<tool>\n" + "<key>helloWorld2</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
				+ "</tool>\n" + "<!-- BEGIN PLUGINS -->\n"
				+ "<!-- END PLUGINS -->\n" + "</toolbox>";
		String fragment = "<tool>\n"
				+ "<key>helloWorld</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.plugins.hello.world.HelloViewTool</class>\n"
				+ "</tool>";

		InputStream is = new ByteArrayInputStream(source.getBytes());
		Map<String, String> overrideMap = new HashMap<String, String>();
		overrideMap.put("tool", "key");
		String expected = "<?xml version=\"1.0\"?>\n"
				+ "<toolbox>\n"
				+ "<!-- BEGIN OVERRIDE:test\n"
				+ "<tool>\n"
				+ "<name>XXX</name>"
				+ "<key>helloWorld</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
				+ "</tool>\n"
				+ " END OVERRIDE:test -->\n"
				+ "<tool>\n"
				+ "<key>helloWorld2</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
				+ "</tool>\n"
				+ "<!-- BEGIN PLUGINS -->\n"
				+ "<!-- BEGIN PLUGIN:test -->\n"
				+ "<tool>\n"
				+ "<key>helloWorld</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.plugins.hello.world.HelloViewTool</class>\n"
				+ "</tool>\n" + "<!-- END PLUGIN:test -->\n"
				+ "<!-- END PLUGINS -->\n" + "</toolbox>\n";
		try {
			String merge = pl.mergeByKey(is, "<!-- BEGIN PLUGINS -->",
					"<!-- END PLUGINS -->", "<!-- BEGIN PLUGIN:test -->",
					"<!-- END PLUGIN:test -->", fragment, overrideMap,
					"<!-- BEGIN OVERRIDE:test", " END OVERRIDE:test -->", "<!-- BEGIN OVERRIDE");
			assertEquals("Checking merged text", expected, merge);

		} catch (IOException e) {
			fail("IOException: " + e.getMessage());
		}

	}
	
	public void testMergeWithOverrideWithExtraElements() {
		//The merge used to break is the key element wasn't the first element
		PluginFileMerger pl = new PluginFileMerger();
		String source = "<?xml version=\"1.0\"?>\n" + "<toolbox>\n"
				+ "<tool>\n"  
				+ "<name>Name #1</name>\n"
				+"<key>helloWorld</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
				+ "</tool>\n" + "<tool>\n" + "<key>helloWorld2</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
				+ "</tool>\n" + "<!-- BEGIN PLUGINS -->\n"
				+ "<!-- END PLUGINS -->\n" + "</toolbox>";
		String fragment = "<tool>\n"
				+ "<key>helloWorld</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.plugins.hello.world.HelloViewTool</class>\n"
				+ "</tool>";

		InputStream is = new ByteArrayInputStream(source.getBytes());
		Map<String, String> overrideMap = new HashMap<String, String>();
		overrideMap.put("tool", "key");
		String expected = "<?xml version=\"1.0\"?>\n"
				+ "<toolbox>\n"
				+ "<!-- BEGIN OVERRIDE:test\n"
				+ "<tool>\n"
				+ "<name>Name #1</name>\n"
				+ "<key>helloWorld</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
				+ "</tool>\n"
				+ " END OVERRIDE:test -->\n"
				+ "<tool>\n"
				+ "<key>helloWorld2</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
				+ "</tool>\n"
				+ "<!-- BEGIN PLUGINS -->\n"
				+ "<!-- BEGIN PLUGIN:test -->\n"
				+ "<tool>\n"
				+ "<key>helloWorld</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.plugins.hello.world.HelloViewTool</class>\n"
				+ "</tool>\n" + "<!-- END PLUGIN:test -->\n"
				+ "<!-- END PLUGINS -->\n" + "</toolbox>\n";
		try {
			String merge = pl.mergeByKey(is, "<!-- BEGIN PLUGINS -->",
					"<!-- END PLUGINS -->", "<!-- BEGIN PLUGIN:test -->",
					"<!-- END PLUGIN:test -->", fragment, overrideMap,
					"<!-- BEGIN OVERRIDE:test", " END OVERRIDE:test -->", "<!-- BEGIN OVERRIDE");
			assertEquals("Checking merged text", expected, merge);

		} catch (IOException e) {
			fail("IOException: " + e.getMessage());
		}

	}
	
	public void testMergeWithOverrideAndComments() {
		PluginFileMerger pl = new PluginFileMerger();
		String source = "<?xml version=\"1.0\"?>\n" + "<toolbox>\n"
				+ "<tool>\n" 
				+ "<!-- TEST COMMENT -->\n"
				+ "<key>helloWorld</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
				+ "</tool>\n" + "<tool>\n" 
				+ "<key>helloWorld2</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
				+ "</tool>\n" + "<!-- BEGIN PLUGINS -->\n"
				+ "<!-- END PLUGINS -->\n" + "</toolbox>";
		String fragment = "<tool>\n"
				+ "<key>helloWorld</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.plugins.hello.world.HelloViewTool</class>\n"
				+ "</tool>";

		InputStream is = new ByteArrayInputStream(source.getBytes());
		Map<String, String> overrideMap = new HashMap<String, String>();
		overrideMap.put("tool", "key");
		String expected = "<?xml version=\"1.0\"?>\n"
				+ "<toolbox>\n"
				+ "<!-- BEGIN OVERRIDE:test\n"
				+ "<tool>\n"
				+ "#!-- TEST COMMENT --#\n"
				+ "<key>helloWorld</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
				+ "</tool>\n"
				+ " END OVERRIDE:test -->\n"
				+ "<tool>\n"
				+ "<key>helloWorld2</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
				+ "</tool>\n"
				+ "<!-- BEGIN PLUGINS -->\n"
				+ "<!-- BEGIN PLUGIN:test -->\n"
				+ "<tool>\n"
				+ "<key>helloWorld</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.plugins.hello.world.HelloViewTool</class>\n"
				+ "</tool>\n" + "<!-- END PLUGIN:test -->\n"
				+ "<!-- END PLUGINS -->\n" + "</toolbox>\n";
		try {
			String merge = pl.mergeByKey(is, "<!-- BEGIN PLUGINS -->",
					"<!-- END PLUGINS -->", "<!-- BEGIN PLUGIN:test -->",
					"<!-- END PLUGIN:test -->", fragment, overrideMap,
					"<!-- BEGIN OVERRIDE:test", " END OVERRIDE:test -->", "<!-- BEGIN OVERRIDE");
			assertEquals("Checking merged text", expected, merge);

		} catch (IOException e) {
			fail("IOException: " + e.getMessage());
		}

	}
	
	public void testMergeWithOverrideSecondTime() {
		PluginFileMerger pl = new PluginFileMerger();
		String source =  "<?xml version=\"1.0\"?>\n"
			+ "<toolbox>\n"
			+ "<!-- BEGIN OVERRIDE:test\n"
			+ "<tool>\n"
			+ "<key>helloWorld</key>\n"
			+ "<scope>application</scope>\n"
			+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
			+ "</tool>\n"
			+ " END OVERRIDE:test -->\n"
			+ "<tool>\n"
			+ "<key>helloWorld2</key>\n"
			+ "<scope>application</scope>\n"
			+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
			+ "</tool>\n"
			+ "<!-- BEGIN PLUGINS -->\n"
			+ "<!-- BEGIN PLUGIN:test -->\n"
			+ "<tool>\n"
			+ "<key>helloWorld</key>\n"
			+ "<scope>application</scope>\n"
			+ "<class>com.dotmarketing.plugins.hello.world.HelloViewTool</class>\n"
			+ "</tool>\n" + "<!-- END PLUGIN:test -->\n"
			+ "<!-- END PLUGINS -->\n" + "</toolbox>\n";
		String fragment = "<tool>\n"
				+ "<key>helloWorld</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.plugins.new.HelloViewTool</class>\n"
				+ "</tool>";

		InputStream is = new ByteArrayInputStream(source.getBytes());
		Map<String, String> overrideMap = new HashMap<String, String>();
		overrideMap.put("tool", "key");
		String expected = "<?xml version=\"1.0\"?>\n"
				+ "<toolbox>\n"
				+ "<!-- BEGIN OVERRIDE:test\n"
				+ "<tool>\n"
				+ "<key>helloWorld</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
				+ "</tool>\n"
				+ " END OVERRIDE:test -->\n"
				+ "<tool>\n"
				+ "<key>helloWorld2</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
				+ "</tool>\n"
				+ "<!-- BEGIN PLUGINS -->\n"
				+ "<!-- BEGIN PLUGIN:test -->\n"
				+ "<!-- BEGIN OVERRIDE:test2\n"
				+ "<tool>\n"
				+ "<key>helloWorld</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.plugins.hello.world.HelloViewTool</class>\n"
				+ "</tool>\n" 
				+ " END OVERRIDE:test2 -->\n"
				+ "<!-- END PLUGIN:test -->\n"
				+"<!-- BEGIN PLUGIN:test2 -->\n"
				+ "<tool>\n"
				+ "<key>helloWorld</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.plugins.new.HelloViewTool</class>\n"
				+ "</tool>\n"
				+ "<!-- END PLUGIN:test2 -->\n"
				+ "<!-- END PLUGINS -->\n" + "</toolbox>\n";
		try {
			String merge = pl.mergeByKey(is, "<!-- BEGIN PLUGINS -->",
					"<!-- END PLUGINS -->", "<!-- BEGIN PLUGIN:test2 -->",
					"<!-- END PLUGIN:test2 -->", fragment, overrideMap,
					"<!-- BEGIN OVERRIDE:test2", " END OVERRIDE:test2 -->", "<!-- BEGIN OVERRIDE");
			assertEquals("Checking merged text", expected, merge);

		} catch (IOException e) {
			fail("IOException: " + e.getMessage());
		}

	}
	
	
	
	public void testMergeWithOverrideSecondTimeByAttribute() {
		PluginFileMerger pl = new PluginFileMerger();
		String source =  "<?xml version=\"1.0\"?>\n"
			+ "<toolbox>\n"
			+ "<!-- BEGIN OVERRIDE:test\n"
			+ "<tool url=\"helloWorld\">\n"
			+ "<key>helloWorld</key>\n"
			+ "<scope>application</scope>\n"
			+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
			+ "</tool>\n"
			+ " END OVERRIDE:test -->\n"
			+ "<tool>\n"
			+ "<key>helloWorld2</key>\n"
			+ "<scope>application</scope>\n"
			+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
			+ "</tool>\n"
			+ "<!-- BEGIN PLUGINS -->\n"
			+ "<!-- BEGIN PLUGIN:test -->\n"
			+ "<tool url=\"helloWorld\">\n"
			+ "<key>helloWorld</key>\n"
			+ "<scope>application</scope>\n"
			+ "<class>com.dotmarketing.plugins.hello.world.HelloViewTool</class>\n"
			+ "</tool>\n" + "<!-- END PLUGIN:test -->\n"
			+ "<!-- END PLUGINS -->\n" + "</toolbox>\n";
		String fragment = "<tool url=\"helloWorld\">\n"
				+ "<key>helloWorld</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.plugins.new.HelloViewTool</class>\n"
				+ "</tool>";

		InputStream is = new ByteArrayInputStream(source.getBytes());
		Map<String, String> overrideMap = new HashMap<String, String>();
		overrideMap.put("tool", "url");
		String expected = "<?xml version=\"1.0\"?>\n"
				+ "<toolbox>\n"
				+ "<!-- BEGIN OVERRIDE:test\n"
				+ "<tool url=\"helloWorld\">\n"
				+ "<key>helloWorld</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
				+ "</tool>\n"
				+ " END OVERRIDE:test -->\n"
				+ "<tool>\n"
				+ "<key>helloWorld2</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
				+ "</tool>\n"
				+ "<!-- BEGIN PLUGINS -->\n"
				+ "<!-- BEGIN PLUGIN:test -->\n"
				+ "<!-- BEGIN OVERRIDE:test2\n"
				+ "<tool url=\"helloWorld\">\n"
				+ "<key>helloWorld</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.plugins.hello.world.HelloViewTool</class>\n"
				+ "</tool>\n" 
				+ " END OVERRIDE:test2 -->\n"
				+ "<!-- END PLUGIN:test -->\n"
				+"<!-- BEGIN PLUGIN:test2 -->\n"
				+ "<tool url=\"helloWorld\">\n"
				+ "<key>helloWorld</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.plugins.new.HelloViewTool</class>\n"
				+ "</tool>\n"
				+ "<!-- END PLUGIN:test2 -->\n"
				+ "<!-- END PLUGINS -->\n" + "</toolbox>\n";
		try {
			String merge = pl.mergeByAttribute(is, "<!-- BEGIN PLUGINS -->",
					"<!-- END PLUGINS -->", "<!-- BEGIN PLUGIN:test2 -->",
					"<!-- END PLUGIN:test2 -->", fragment, overrideMap,
					"<!-- BEGIN OVERRIDE:test2", " END OVERRIDE:test2 -->", "<!-- BEGIN OVERRIDE");
			assertEquals("Checking merged text", expected, merge);

		} catch (IOException e) {
			fail("IOException: " + e.getMessage());
		}

	}
	
	
	public void testMergeWithOverrideByAttributeWithComments() {
		PluginFileMerger pl = new PluginFileMerger();
		String source =  "<?xml version=\"1.0\"?>\n"
			+ "<toolbox>\n"			
			+ "<tool name=\"xx\" url=\"helloWorld\" value=\"xx\">\n"
			+ "<!-- MY COMMENT -->"
			+ "<key>helloWorld</key>\n"
			+ "<scope>application</scope>\n"
			+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
			+ "</tool>\n"			
			+ "<tool>\n"
			+ "<key>helloWorld2</key>\n"
			+ "<scope>application</scope>\n"
			+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
			+ "</tool>\n"
			+ "<!-- BEGIN PLUGINS -->\n"
			+ "<!-- END PLUGINS -->\n" + "</toolbox>\n";
		String fragment = "<tool url=\"helloWorld\">\n"
				+ "<key>helloWorld</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.plugins.new.HelloViewTool</class>\n"
				+ "</tool>";

		InputStream is = new ByteArrayInputStream(source.getBytes());
		Map<String, String> overrideMap = new HashMap<String, String>();
		overrideMap.put("tool", "url");
		String expected = "<?xml version=\"1.0\"?>\n"
				+ "<toolbox>\n"
				+ "<!-- BEGIN OVERRIDE:test2\n"
				+ "<tool name=\"xx\" url=\"helloWorld\" value=\"xx\">\n"
				+ "#!-- MY COMMENT --#"
				+ "<key>helloWorld</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
				+ "</tool>\n"
				+ " END OVERRIDE:test2 -->\n"
				+ "<tool>\n"
				+ "<key>helloWorld2</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
				+ "</tool>\n"
				+ "<!-- BEGIN PLUGINS -->\n"				
				+"<!-- BEGIN PLUGIN:test2 -->\n"
				+ "<tool url=\"helloWorld\">\n"
				+ "<key>helloWorld</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.plugins.new.HelloViewTool</class>\n"
				+ "</tool>\n"
				+ "<!-- END PLUGIN:test2 -->\n"
				+ "<!-- END PLUGINS -->\n" + "</toolbox>\n";
		try {
			String merge = pl.mergeByAttribute(is, "<!-- BEGIN PLUGINS -->",
					"<!-- END PLUGINS -->", "<!-- BEGIN PLUGIN:test2 -->",
					"<!-- END PLUGIN:test2 -->", fragment, overrideMap,
					"<!-- BEGIN OVERRIDE:test2", " END OVERRIDE:test2 -->", "<!-- BEGIN OVERRIDE");
			assertEquals("Checking merged text", expected, merge);

		} catch (IOException e) {
			fail("IOException: " + e.getMessage());
		}

	}

	public void testMergeWithOverrideProps() {
		PluginFileMerger pl = new PluginFileMerger();
		String source = "#\n"
				+ "# The file encoding must be set to UTF8 in order for the\n"
				+ "file.encoding=UTF8\n"
				+ "\n"
				+ "# to edit your start script to include this as a system property.\n"
				+ "#\n" + "java.awt.headless=true\n" + "## BEGIN PLUGINS\n"
				+ "\n" + "## END PLUGINS\n";
		String fragment = "java.awt.headless=true\n" + "java.test=Whatever\n";
		InputStream is = new ByteArrayInputStream(source.getBytes());
		String expected = "#\n"
				+ "# The file encoding must be set to UTF8 in order for the\n"
				+ "file.encoding=UTF8\n"
				+ "\n"
				+ "# to edit your start script to include this as a system property.\n"
				+ "#\n" + "## OVERRIDE:test\n" + "#java.awt.headless=true\n"
				+ "## BEGIN PLUGINS\n" + "## BEGIN PLUGIN:test\n"
				+ "java.awt.headless=true\n" + "java.test=Whatever\n" + "\n"
				+ "## END PLUGIN:test\n" + "## END PLUGINS\n";
		try {
			String merge = pl.merge(is, "## BEGIN PLUGINS", "## END PLUGINS",
					"## BEGIN PLUGIN:test", "## END PLUGIN:test", fragment,
					"#", "## OVERRIDE:test");
			assertEquals("Merge props with override", expected, merge);

		} catch (IOException e) {
			fail("IOException: " + e.getMessage());
		}
	}
	
	public void testMergeWithOverridePropsSecondTime() {
		PluginFileMerger pl = new PluginFileMerger();
		String source = "#\n"
			+ "# The file encoding must be set to UTF8 in order for the\n"
			+ "file.encoding=UTF8\n"
			+ "\n"
			+ "# to edit your start script to include this as a system property.\n"
			+ "#\n" + "## OVERRIDE:test\n" + "#java.awt.headless=true\n"
			+ "## BEGIN PLUGINS\n" + "## BEGIN PLUGIN:test\n"
			+ "java.awt.headless=true\n" + "java.test=Whatever\n" + "\n"
			+ "## END PLUGIN:test\n" + "## END PLUGINS\n";
		String fragment = "java.awt.headless=false\n" + "java.new.test=TESTtest\n";
		InputStream is = new ByteArrayInputStream(source.getBytes());
		String expected = "#\n"
				+ "# The file encoding must be set to UTF8 in order for the\n"
				+ "file.encoding=UTF8\n"
				+ "\n"
				+ "# to edit your start script to include this as a system property.\n"
				+ "#\n" 
				+ "## OVERRIDE:test\n" 
				+ "#java.awt.headless=true\n"
				+ "## BEGIN PLUGINS\n" 
				+ "## BEGIN PLUGIN:test\n"
				+ "## OVERRIDE:test2\n" 
				+ "#java.awt.headless=true\n" 
				+ "java.test=Whatever\n" 
				+ "\n"
				+ "## END PLUGIN:test\n" 
				+ "## BEGIN PLUGIN:test2\n"
				+ "java.awt.headless=false\n" 
				+ "java.new.test=TESTtest\n" 
				+ "\n"
				+ "## END PLUGIN:test2\n" 
				+ "## END PLUGINS\n";
		try {
			String merge = pl.merge(is, "## BEGIN PLUGINS", "## END PLUGINS",
					"## BEGIN PLUGIN:test2", "## END PLUGIN:test2", fragment,
					"#", "## OVERRIDE:test2");
			assertEquals("Merge props with override", expected, merge);

		} catch (IOException e) {
			fail("IOException: " + e.getMessage());
		}
	}

	public void testRemovePropertiesFragment() {
		PluginFileMerger pl = new PluginFileMerger();		
		String source = "#\n"
			+ "# The file encoding must be set to UTF8 in order for the\n"
			+ "file.encoding=UTF8\n"
			+ "\n"
			+ "# to edit your start script to include this as a system property.\n"
			+ "#\n" + "## OVERRIDE:test\n" + "#java.awt.headless=true\n"
			+ "## BEGIN PLUGINS\n" + "## BEGIN PLUGIN:test\n"
			+ "java.awt.headless=true\n" + "java.test=Whatever\n" + "\n"
			+ "## END PLUGIN:test\n" + "## END PLUGINS\n";
		InputStream is = new ByteArrayInputStream(source.getBytes());
		String expected = "#\n"
			+ "# The file encoding must be set to UTF8 in order for the\n"
			+ "file.encoding=UTF8\n"			
			+ "# to edit your start script to include this as a system property.\n"
			+ "#\n" + "java.awt.headless=true\n" + "## BEGIN PLUGINS\n"+ "## END PLUGINS\n";
		
		try {
			String remove = pl.removePropertiesFragment(is,
					"## BEGIN PLUGIN:test", "## END PLUGIN:test", 
					"#", "## OVERRIDE:test");
			assertEquals("Remove props with override", expected, remove);
		} catch (IOException e) {
			fail("IOException: " + e.getMessage());
		}
	}

	public void testRemoveFragmentXML() {
		PluginFileMerger pl = new PluginFileMerger();
		String source = "<?xml version=\"1.0\"?>\n"
				+ "<toolbox>\n"
				+ "<!-- BEGIN OVERRIDE:test\n"
				+ "<tool>\n"
				+ "<key>helloWorld</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
				+ "</tool>\n"
				+ " END OVERRIDE:test -->\n"
				+ "<tool>\n"
				+ "<key>helloWorld2</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
				+ "</tool>\n"
				+ "<!-- BEGIN PLUGINS -->\n"
				+ "<!-- BEGIN PLUGIN:test -->\n"
				+ "<tool>\n"
				+ "<key>helloWorld</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.plugins.hello.world.HelloViewTool</class>\n"
				+ "</tool>\n" + "<!-- END PLUGIN:test -->\n"
				+ "<!-- END PLUGINS -->\n" + "</toolbox>\n";
		InputStream is = new ByteArrayInputStream(source.getBytes());
		String expected = "<?xml version=\"1.0\"?>\n" + "<toolbox>\n"
				+ "<tool>\n" + "<key>helloWorld</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
				+ "</tool>\n" + "<tool>\n" + "<key>helloWorld2</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
				+ "</tool>\n" + "<!-- BEGIN PLUGINS -->\n"
				+ "<!-- END PLUGINS -->\n" + "</toolbox>\n";
		try {
			String remove = pl.removeFragmentXML(is,
					"<!-- BEGIN PLUGIN:test -->", "<!-- END PLUGIN:test -->",
					"<!-- BEGIN OVERRIDE:test", " END OVERRIDE:test -->");
			assertEquals("Remove xml with override", expected, remove);
		} catch (IOException e) {
			fail("IOException: " + e.getMessage());
		}
	}
	
	public void testRemoveFragmentXMLWithComment() {
		PluginFileMerger pl = new PluginFileMerger();
		String source = "<?xml version=\"1.0\"?>\n"
				+ "<toolbox>\n"
				+ "<!-- BEGIN OVERRIDE:test\n"
				+ "<tool>\n"
				+ "#!-- TEST COMMENT --#\n"
				+ "<key>helloWorld</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
				+ "</tool>\n"
				+ " END OVERRIDE:test -->\n"
				+ "<tool>\n"
				+ "<key>helloWorld2</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
				+ "</tool>\n"
				+ "<!-- BEGIN PLUGINS -->\n"
				+ "<!-- BEGIN PLUGIN:test -->\n"
				+ "<tool>\n"
				+ "<key>helloWorld</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.plugins.hello.world.HelloViewTool</class>\n"
				+ "</tool>\n" + "<!-- END PLUGIN:test -->\n"
				+ "<!-- END PLUGINS -->\n" + "</toolbox>\n";
		InputStream is = new ByteArrayInputStream(source.getBytes());
		String expected = "<?xml version=\"1.0\"?>\n" + "<toolbox>\n"
				+ "<tool>\n"
				+ "<!-- TEST COMMENT -->\n"
				+ "<key>helloWorld</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
				+ "</tool>\n" + "<tool>\n" + "<key>helloWorld2</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
				+ "</tool>\n" + "<!-- BEGIN PLUGINS -->\n"
				+ "<!-- END PLUGINS -->\n" + "</toolbox>\n";
		try {
			String remove = pl.removeFragmentXML(is,
					"<!-- BEGIN PLUGIN:test -->", "<!-- END PLUGIN:test -->",
					"<!-- BEGIN OVERRIDE:test", " END OVERRIDE:test -->");
			assertEquals("Remove xml with override", expected, remove);
		} catch (IOException e) {
			fail("IOException: " + e.getMessage());
		}
	}
	
	// As per AQUENT-437
	public void testOverideXMLWithAsterisk() {
		PluginFileMerger pl = new PluginFileMerger();
		String source = "<?xml version=\"1.0\"?>\n"
			+ "<servlet-mapping>\n"
			+ "<servlet-name>VelocityServlet</servlet-name>\n"
			+ "<url-pattern>*.htm</url-pattern>\n"
			+ "</servlet-mapping> \n"		
			+ "<!-- BEGIN PLUGINS -->\n"
			+ "<!-- END PLUGINS -->\n";
		
		
		String fragment =  "<servlet-mapping>\n"
			+ "<servlet-name>VelocityServlet2</servlet-name>\n"
			+ "<url-pattern>*.htm</url-pattern>\n"
			+ "</servlet-mapping>";
		
		InputStream is = new ByteArrayInputStream(source.getBytes());
		Map<String, String> overrideMap = new HashMap<String, String>();
		overrideMap.put("servlet-mapping", "url-pattern");
		String expected = "<?xml version=\"1.0\"?>\n"
			+ "<!-- BEGIN OVERRIDE:test\n"
			+ "<servlet-mapping>\n"
			+ "<servlet-name>VelocityServlet</servlet-name>\n"
			+ "<url-pattern>*.htm</url-pattern>\n"
			+ "</servlet-mapping>\n"	
			+ " END OVERRIDE:test --> \n"
			+ "<!-- BEGIN PLUGINS -->\n"
			+ "<!-- BEGIN PLUGIN:test -->\n"
			+ "<servlet-mapping>\n"
			+ "<servlet-name>VelocityServlet2</servlet-name>\n"
			+ "<url-pattern>*.htm</url-pattern>\n"
			+ "</servlet-mapping>\n"
			+ "<!-- END PLUGIN:test -->\n"
			+ "<!-- END PLUGINS -->\n" ;
		try {
			String merge = pl.mergeByKey(is, "<!-- BEGIN PLUGINS -->",
					"<!-- END PLUGINS -->", "<!-- BEGIN PLUGIN:test -->",
					"<!-- END PLUGIN:test -->", fragment, overrideMap,
					"<!-- BEGIN OVERRIDE:test", " END OVERRIDE:test -->", "<!-- BEGIN OVERRIDE");
			assertEquals("Checking merged text", expected, merge);

		} catch (IOException e) {
			fail("IOException: " + e.getMessage());
		}
	}
	
	
	public void testMergeWithOverrideByAttributeWithSlash() {
		PluginFileMerger pl = new PluginFileMerger();
		String source =  "<?xml version=\"1.0\"?>\n"
			+ "<toolbox>\n"			
			+ "<tool name=\"xx\" url=\"/helloWorld\" value=\"xx\">\n"
			+ "<key>helloWorld</key>\n"
			+ "<scope>application</scope>\n"
			+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
			+ "</tool>\n"			
			+ "<tool>\n"
			+ "<key>helloWorld2</key>\n"
			+ "<scope>application</scope>\n"
			+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
			+ "</tool>\n"
			+ "<!-- BEGIN PLUGINS -->\n"
			+ "<!-- END PLUGINS -->\n" + "</toolbox>\n";
		String fragment = "<tool url=\"/helloWorld\">\n"
				+ "<key>helloWorld</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.plugins.new.HelloViewTool</class>\n"
				+ "</tool>";

		InputStream is = new ByteArrayInputStream(source.getBytes());
		Map<String, String> overrideMap = new HashMap<String, String>();
		overrideMap.put("tool", "url");
		String expected = "<?xml version=\"1.0\"?>\n"
				+ "<toolbox>\n"
				+ "<!-- BEGIN OVERRIDE:test2\n"
				+ "<tool name=\"xx\" url=\"/helloWorld\" value=\"xx\">\n"
				+ "<key>helloWorld</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
				+ "</tool>\n"
				+ " END OVERRIDE:test2 -->\n"
				+ "<tool>\n"
				+ "<key>helloWorld2</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.original.HelloViewTool</class>\n"
				+ "</tool>\n"
				+ "<!-- BEGIN PLUGINS -->\n"				
				+"<!-- BEGIN PLUGIN:test2 -->\n"
				+ "<tool url=\"/helloWorld\">\n"
				+ "<key>helloWorld</key>\n"
				+ "<scope>application</scope>\n"
				+ "<class>com.dotmarketing.plugins.new.HelloViewTool</class>\n"
				+ "</tool>\n"
				+ "<!-- END PLUGIN:test2 -->\n"
				+ "<!-- END PLUGINS -->\n" + "</toolbox>\n";
		try {
			String merge = pl.mergeByAttribute(is, "<!-- BEGIN PLUGINS -->",
					"<!-- END PLUGINS -->", "<!-- BEGIN PLUGIN:test2 -->",
					"<!-- END PLUGIN:test2 -->", fragment, overrideMap,
					"<!-- BEGIN OVERRIDE:test2", " END OVERRIDE:test2 -->", "<!-- BEGIN OVERRIDE");
			assertEquals("Checking merged text", expected, merge);

		} catch (IOException e) {
			fail("IOException: " + e.getMessage());
		}

	}
	
	public void testOverideAction() {
		PluginFileMerger pl = new PluginFileMerger();
		String source =  "<!DOCTYPE struts-config PUBLIC \n" +
				"\"-//Apache Software Foundation//DTD Struts Configuration 1.1//EN\" \n" +
				"\"http://jakarta.apache.org/struts/dtds/struts-config_1_1.dtd\" > \n"+
				"<struts-config>\n" +
				"<form-beans>\n"+
			"<form-bean name=\"calendarReminderForm\" type=\"com.dotmarketing.portlets.calendar.struts.CalendarReminderForm\"></form-bean>\n" +
			"<!-- BEGIN FORM-BEANS -->\n" +
			"<!-- END FORM-BEANS -->		\n" +
	"</form-beans>\n" +
	    "<!--  Action Mapping Definitions  -->\n" +
	    "<action-mappings>\n" +
	    	"<action path=\"/reviewOrder\"  type=\"com.dotmarketing.cms.product.action.ReviewOrderAction\" name=\"orderForm\" scope=\"request\" validate=\"false\" parameter=\"dispatch\" input=\"/application/products/review_order.dot\">\n" +
				"<forward name=\"reviewOrder\" path=\"/application/products/review_order.dot\" />\n" +
				"<forward name=\"checkOut\" path=\"dotCMS/checkOutCart\" />\n" +
			"</action>\n" +
			
			"<action path=\"/purchaseOrder\"  type=\"com.dotmarketing.cms.product.action.PurchaseAction\" name=\"orderForm\" scope=\"request\" validate=\"false\" parameter=\"dispatch\" input=\"/application/products/review_order.dot\">\n" +
				"<forward name=\"invoice\" path=\"/application/products/invoice.dot\" redirect=\"true\"/>\n" +
				"<forward name=\"invoiceEmail\" path=\"/application/products/invoice_email.dot\"/>\n" +
				"</action>\n" +
			"<!-- END LIST PRODUCT -->\n" +
			"<!-- REGISTRATION -->\n" +
			"<action path=\"/addFacility\" type=\"com.dotmarketing.cms.registration.action.AddFacilityAction\" name=\"dotRegistrationForm\" input=\"/facility/facility.dot\" validate=\"false\" scope=\"request\" parameter=\"dispatch\">\n" +
				"<forward name=\"open\" path=\"/application/registration/facility.dot\" />\n" +
				"<forward name=\"fail\" path=\"/application/registration/facility.dot\" />\n" +
				"<forward name=\"success\" path=\"/application/registration/facility.dot?callback=true\" />\n" +
			"</action>\n" +
			
	        "<action path=\"/webEventComments\" forward=\"/conferences_events/comments_testimonials.dot\">\n" +
			"</action>\n" +
			"<!--  End web events registrations -->\n" +
			
			"<!-- Test CC -->\n" +
			"<action path=\"/testCC\" type=\"com.dotmarketing.cms.creditcard.action.CreditCardTestAction\" name=\"CreditCardTestForm\" scope=\"session\" validate=\"false\" parameter=\"dispatch\">\n" +
				"<forward name=\"success\" path=\"/home/testcc.dot\" />\n" +
				"<forward name=\"failure\" path=\"/home/testcc.dot\" />\n" +
			"</action>\n" +
			"<!-- END Test CC-->\n" +		
		    "<!-- BEGIN ACTIONS -->\n" +	
			"<!-- END ACTIONS --> \n" +
	    	    
	    "</action-mappings>\n" +
	"</struts-config>";

		String fragment = "<action path=\"/purchaseOrder\"  type=\"org.dotcms.plugin.ecommerce.processors.paypal.PurchaseAction\" name=\"orderForm\" scope=\"request\" validate=\"false\" parameter=\"dispatch\" input=\"/application/products/review_order.dot\">\n"+
	"<forward name=\"invoice\" path=\"/application/products/invoice.dot\" redirect=\"true\"/>\n"+
	"<forward name=\"invoiceEmail\" path=\"/application/products/invoice_email.dot\"/>\n"+
"</action>";

		InputStream is = new ByteArrayInputStream(source.getBytes());
		Map<String, String> overrideMap = new HashMap<String, String>();
		overrideMap.put("action", "path");
		String expected ="<!DOCTYPE struts-config PUBLIC \n" +
		"\"-//Apache Software Foundation//DTD Struts Configuration 1.1//EN\" \n" +
		"\"http://jakarta.apache.org/struts/dtds/struts-config_1_1.dtd\" > \n"+
		"<struts-config>\n" +
		"<form-beans>\n"+
	"<form-bean name=\"calendarReminderForm\" type=\"com.dotmarketing.portlets.calendar.struts.CalendarReminderForm\"></form-bean>\n" +
	"<!-- BEGIN FORM-BEANS -->\n" +
	"<!-- END FORM-BEANS -->		\n" +
"</form-beans>\n" +
"<!--  Action Mapping Definitions  -->\n" +
"<action-mappings>\n" +
	"<action path=\"/reviewOrder\"  type=\"com.dotmarketing.cms.product.action.ReviewOrderAction\" name=\"orderForm\" scope=\"request\" validate=\"false\" parameter=\"dispatch\" input=\"/application/products/review_order.dot\">\n" +
		"<forward name=\"reviewOrder\" path=\"/application/products/review_order.dot\" />\n" +
		"<forward name=\"checkOut\" path=\"dotCMS/checkOutCart\" />\n" +
	"</action>\n" +
	"<!-- BEGIN OVERRIDE:test2\n"+
	"<action path=\"/purchaseOrder\"  type=\"com.dotmarketing.cms.product.action.PurchaseAction\" name=\"orderForm\" scope=\"request\" validate=\"false\" parameter=\"dispatch\" input=\"/application/products/review_order.dot\">\n" +
		"<forward name=\"invoice\" path=\"/application/products/invoice.dot\" redirect=\"true\"/>\n" +
		"<forward name=\"invoiceEmail\" path=\"/application/products/invoice_email.dot\"/>\n" +
		"</action>\n" +
		" END OVERRIDE:test2 -->\n"+
	"<!-- END LIST PRODUCT -->\n" +
	"<!-- REGISTRATION -->\n" +
	"<action path=\"/addFacility\" type=\"com.dotmarketing.cms.registration.action.AddFacilityAction\" name=\"dotRegistrationForm\" input=\"/facility/facility.dot\" validate=\"false\" scope=\"request\" parameter=\"dispatch\">\n" +
		"<forward name=\"open\" path=\"/application/registration/facility.dot\" />\n" +
		"<forward name=\"fail\" path=\"/application/registration/facility.dot\" />\n" +
		"<forward name=\"success\" path=\"/application/registration/facility.dot?callback=true\" />\n" +
	"</action>\n" +
	
    "<action path=\"/webEventComments\" forward=\"/conferences_events/comments_testimonials.dot\">\n" +
	"</action>\n" +
	"<!--  End web events registrations -->\n" +
	
	"<!-- Test CC -->\n" +
	"<action path=\"/testCC\" type=\"com.dotmarketing.cms.creditcard.action.CreditCardTestAction\" name=\"CreditCardTestForm\" scope=\"session\" validate=\"false\" parameter=\"dispatch\">\n" +
		"<forward name=\"success\" path=\"/home/testcc.dot\" />\n" +
		"<forward name=\"failure\" path=\"/home/testcc.dot\" />\n" +
	"</action>\n" +
	"<!-- END Test CC-->\n" +		
    "<!-- BEGIN ACTIONS -->\n" +
    "<!-- BEGIN ACTION:test2 -->\n"+
    "<action path=\"/purchaseOrder\"  type=\"org.dotcms.plugin.ecommerce.processors.paypal.PurchaseAction\" name=\"orderForm\" scope=\"request\" validate=\"false\" parameter=\"dispatch\" input=\"/application/products/review_order.dot\">\n"+
	"<forward name=\"invoice\" path=\"/application/products/invoice.dot\" redirect=\"true\"/>\n"+
	"<forward name=\"invoiceEmail\" path=\"/application/products/invoice_email.dot\"/>\n"+
"</action>\n"+
"<!-- END ACTION:test2 -->\n"+
	"<!-- END ACTIONS --> \n" +
	    
"</action-mappings>\n" +
"</struts-config>\n";
		try {
			String merge = pl.mergeByAttribute(is, "<!-- BEGIN ACTIONS -->",
					"<!-- END ACTIONS -->", "<!-- BEGIN ACTION:test2 -->",
					"<!-- END ACTION:test2 -->", fragment, overrideMap,
					"<!-- BEGIN OVERRIDE:test2", " END OVERRIDE:test2 -->", "<!-- BEGIN OVERRIDE");
			assertEquals("Checking merged text", expected, merge);

		} catch (IOException e) {
			fail("IOException: " + e.getMessage());
		}
	}
	
	
	public void testAddFormBeam() {
		//DOTCMS-2547
		PluginFileMerger pl = new PluginFileMerger();
		String source =  "<!DOCTYPE struts-config PUBLIC \n" +
		"\"-//Apache Software Foundation//DTD Struts Configuration 1.1//EN\" \n" +
		"\"http://jakarta.apache.org/struts/dtds/struts-config_1_1.dtd\" > \n"+
		"<struts-config>\n" +
		"<form-beans>\n"+
		"<form-bean name=\"calendarReminderForm\" type=\"com.dotmarketing.portlets.calendar.struts.CalendarReminderForm\"></form-bean>\n" +
		"<!-- BEGIN FORM-BEANS -->\n" +
		"<!-- END FORM-BEANS -->		\n" +
		"</form-beans>\n";
		String fragment = "<form-bean name=\"createAccountFormGift\" type=\"edu.keystone.cms.createaccount.struts.CreateAccountFormGift\"></form-bean>\n"+
		"<form-bean name=\"athleticRecruitForm\" type=\"edu.keystone.cms.athleticrecruitquestionaire.struts.AthleticRecruitForm\"></form-bean> ";
		String expected ="<!DOCTYPE struts-config PUBLIC \n" +
		"\"-//Apache Software Foundation//DTD Struts Configuration 1.1//EN\" \n" +
		"\"http://jakarta.apache.org/struts/dtds/struts-config_1_1.dtd\" > \n"+
		"<struts-config>\n" +
		"<form-beans>\n"+
		"<form-bean name=\"calendarReminderForm\" type=\"com.dotmarketing.portlets.calendar.struts.CalendarReminderForm\"></form-bean>\n" +
		"<!-- BEGIN FORM-BEANS -->\n" +
		"<!-- BEGIN FORM-BEAN:test2 -->\n"+
		 "<form-bean name=\"createAccountFormGift\" type=\"edu.keystone.cms.createaccount.struts.CreateAccountFormGift\"></form-bean>\n"+
			"<form-bean name=\"athleticRecruitForm\" type=\"edu.keystone.cms.athleticrecruitquestionaire.struts.AthleticRecruitForm\"></form-bean> \n"+
			"<!-- END FORM-BEAN:test2 -->\n"+
			"<!-- END FORM-BEANS -->		\n" +
		"</form-beans>\n";
		InputStream is = new ByteArrayInputStream(source.getBytes());
		Map<String, String> overrideMap = new HashMap<String, String>();
		overrideMap.put("form-bean", "name");
		try {
			String merge = pl.mergeByAttribute(is, "<!-- BEGIN FORM-BEANS -->",
					"<!-- END FORM-BEANS -->", "<!-- BEGIN FORM-BEAN:test2 -->",
					"<!-- END FORM-BEAN:test2 -->", fragment, overrideMap,
					"<!-- BEGIN OVERRIDE:test2", " END OVERRIDE:test2 -->", "<!-- BEGIN OVERRIDE");
			assertEquals("Checking merged text", expected, merge);

		} catch (IOException e) {
			fail("IOException: " + e.getMessage());
		}
		
	}

	

}
