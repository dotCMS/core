package com.dotmarketing.plugin.util;

import java.util.List;

import junit.framework.TestCase;

public class PluginVelocityMergerTest extends TestCase {
	
	public void testOverrideTwoMacros() {
		String source = "Test1\n" +
		"Test2\n" +
		"#macro( d )\n" +
"Hello old world!\n" +
"#end\n"+
"Test3\n" +
"Test4\n" +
"#macro(d)\n" +
"SECOND MACRO!\n" +
"#end\n"+
"Test5\n" +
"Test6\n";
		String expected ="Test1\n" +
		"Test2\n" + 
			"## BEGIN OVERRIDE:test.plugins\n" +
		"## #macro( d )\n" +
		"## Hello old world!\n" +
		"## #end\n"+
		"## END OVERRIDE:test.plugins\n"+
		"Test3\n" +
		"Test4\n" +
		"## BEGIN OVERRIDE:test.plugins\n" +
		"## #macro(d)\n" +
		"## SECOND MACRO!\n" +
		"## #end\n"+
		"## END OVERRIDE:test.plugins\n"+
		"Test5\n" +
		"Test6\n";
		String ret=PluginVelocityMerger.overrideMacro(source, "d", "## BEGIN OVERRIDE:test.plugins","## END OVERRIDE:test.plugins");
		assertEquals("Checking overriden text", expected, ret);
		
	}
	
	public void testOverrideSimple() {
		
		String source = "Hello1\n" +
		"Hello2\n" +
				"#macro( d )\n" +
				"Hello old world!\n" +
				"#end";
		String expected =
			 "Hello1\n" +
				"Hello2\n" +
				"## BEGIN OVERRIDE:test.plugins\n" +
				"## #macro( d )\n" +
		"## Hello old world!\n" +
		"## #end\n"+
		"## END OVERRIDE:test.plugins\n";
		
		String ret=PluginVelocityMerger.overrideMacro(source, "d", "## BEGIN OVERRIDE:test.plugins","## END OVERRIDE:test.plugins");
		assertEquals("Checking overriden text", expected, ret);
		ret=PluginVelocityMerger.overrideMacro(ret, "d", "## BEGIN OVERRIDE:test.plugins","## END OVERRIDE:test.plugins");
		assertEquals("Checking overriden text after second iteration", expected, ret);
	}
	
	public void testMergeFragment () {
		String source = "Test1\n" +
				"Test2\n" +
				"#macro( d )\n" +
		"Hello old world!\n" +
		"#end\n"+
		"## BEGIN PLUGINS\n"+
		"## END PLUGINS\n";
		
		String fragment="#macro( d)\n" +
				"HELLO WORLD NEW\n" +
				"#end";
		String expected ="Test1\n" +
		"Test2\n" + 
			"## BEGIN OVERRIDE:test.plugins\n" +
		"## #macro( d )\n" +
		"## Hello old world!\n" +
		"## #end\n"+
		"## END OVERRIDE:test.plugins\n"+
		"## BEGIN PLUGINS\n"+
		"## BEGIN PLUGIN:test.plugins\n"+
		fragment+ "\n" +
		"## END PLUGIN:test.plugins\n"+
		"## END PLUGINS\n";
		
		
		String ret=PluginVelocityMerger.mergeVelocity(source, fragment, "test.plugins");
		assertEquals("Checking merged text", expected, ret);

	}
	
	public void testMergeFragment2 () {
		String source = "Test1\n" +
				"Test2\n" +
				"#macro( d )\n" +
		"Hello old world!\n" +
		"#end\n"+
		"Test3\n" +
		"Test4\n" +
		"#macro(d)\n" +
		"SECOND MACRO!\n" +
		"#end\n"+
		"Test5\n" +
		"Test6\n" +
		"## BEGIN PLUGINS\n"+
		"## END PLUGINS\n" +
		"Test6\n" +
		"Test7\n" ;
		
		String fragment="#macro( d)\n" +
				"HELLO WORLD NEW\n" +
				"#end";
		String expected ="Test1\n" +
		"Test2\n" + 
			"## BEGIN OVERRIDE:test.plugins\n" +
		"## #macro( d )\n" +
		"## Hello old world!\n" +
		"## #end\n"+
		"## END OVERRIDE:test.plugins\n"+
		"Test3\n" +
		"Test4\n" +
		"## BEGIN OVERRIDE:test.plugins\n" +
		"## #macro(d)\n" +
		"## SECOND MACRO!\n" +
		"## #end\n"+
		"## END OVERRIDE:test.plugins\n"+
		"Test5\n" +
		"Test6\n" +
		"## BEGIN PLUGINS\n"+
		"## BEGIN PLUGIN:test.plugins\n"+
		fragment+ "\n" +
		"## END PLUGIN:test.plugins\n"+
		"## END PLUGINS\n"+
		"Test6\n" +
		"Test7\n" ;
		
		
		String ret=PluginVelocityMerger.mergeVelocity(source, fragment, "test.plugins");
		assertEquals("Checking merged text", expected, ret);

	}
	
	public void testRemoveFragment() {
		String expected = "#macro( d )\n" +
		"Hello old world!\n" +
		"#end\n"+
		"#macro(e)\n" +
		"Valid macro\n" +
		"#end\n"+
		"## BEGIN PLUGINS\n"+
		"## END PLUGINS\n";
		
		String fragment="#macro( d)\n" +
				"HELLO WORLD NEW\n" +
				"#end";
		String source = "## BEGIN OVERRIDE:test.plugins\n" +
		"## #macro( d )\n" +
		"## Hello old world!\n" +
		"## #end\n"+
		"## END OVERRIDE:test.plugins\n" +
		"#macro(e)\n" +
		"Valid macro\n" +
		"#end\n"+
		"## BEGIN PLUGINS\n"+
		"## BEGIN PLUGIN:test.plugins\n"+
		fragment+ "\n" +
		"## END PLUGIN:test.plugins\n"+
		"## END PLUGINS\n";
		
		String ret=PluginVelocityMerger.removeFragment(source, "## BEGIN PLUGIN:test.plugins","## END PLUGIN:test.plugins","## BEGIN OVERRIDE:test.plugins","## END OVERRIDE:test.plugins");
		assertEquals("Checking cleared text", expected, ret);
	}
	
	
	public void testGetMacroName() {
		String source = "#macro( d )\n" +
		"Hello old world!\n" +
		"#end\n" +
		"#macro (c)\n" +
		"#end\n" +
		" #macro(b)\n" +
		"Hello World\n" +
		"#end\n" +
		"##macro(a)\n" +
		"##Disabled\n" +
		"##end\n";
		List<String> macroNames=PluginVelocityMerger.getMacroNames(source);
		assertEquals("Check number of items",3,macroNames.size());		
		assertTrue("Check correct items in list",macroNames.contains("b"));
		assertTrue("Check correct items in list",macroNames.contains("c"));
		assertTrue("Check correct items in list",macroNames.contains("d"));
	}
	
	public void testGetMacroNameWithNull() {
		//Just to check if takes a null DOTCMS-2634
		PluginVelocityMerger.getMacroNames(null);
		
	}
	
	
	
	public void testOverrideWithComments() {
		
		String source = "#macro( d )\n" +
				"Hello old world!\n" +				
				"##end\n"+
				"#end";
		String expected = "## BEGIN OVERRIDE:test.plugins\n" +
				"## #macro( d )\n" +
		"## Hello old world!\n" +
		"## ##end\n"+
		"## #end\n"+
		"## END OVERRIDE:test.plugins\n";
		
		String ret=PluginVelocityMerger.overrideMacro(source, "d", "## BEGIN OVERRIDE:test.plugins","## END OVERRIDE:test.plugins");
		assertEquals("Checking overriden text", expected, ret);
	}
	
		
	public void testOverrideSimpleNoMatch() {		
		String source = "#macro( d )\n" +
				"Hello old world!\n" +
				"#end\n";		
		String ret=PluginVelocityMerger.overrideMacro(source, "c", "## BEGIN OVERRIDE:test.plugins","## END OVERRIDE:test.plugins");
		assertEquals("Checking overriden text", source, ret);
	}

}
