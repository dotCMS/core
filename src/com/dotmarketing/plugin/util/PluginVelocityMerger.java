package com.dotmarketing.plugin.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.dotmarketing.util.Logger;

public class PluginVelocityMerger {

	public static String[] overrideFiles = {
			"WEB-INF" + File.separator + "velocity" + File.separator
					+ "VM_global_library.vm",
			"WEB-INF" + File.separator + "velocity" + File.separator
					+ "dotCMS_library.vm" };

	public static String destFile = "WEB-INF" + File.separator + "velocity"
			+ File.separator + "dotCMS_library_ext.vm";

	public static String beginOverrideStub = "## BEGIN OVERRIDE:";
	public static String endOverrideStub = "## END OVERRIDE:";

	public static String beginStub = "## BEGIN PLUGIN:";
	public static String endStub = "## END PLUGIN:";

	public static String beginTag = "## BEGIN PLUGINS";
	public static String endTag = "## END PLUGINS";
	public static String commentPrefix = "## ";

	public static String overrideMacro(String source, String macroName,
			String overrideCommentBegin, String overrideCommentEnd) {
		String patternText = "^\\s*?#macro[ ]*?\\([ ]*?" + macroName + "[ \\)]";
		String patternText2 = "^\\s*?#(if|foreach)[ ]*?\\(";
		String patternText3="^\\s*?#end";
		Pattern pattern = Pattern.compile(patternText);
		Pattern pattern2 = Pattern.compile(patternText2);
		Pattern pattern3 = Pattern.compile(patternText3);
		Matcher m = null;
		String ret = "";
		int count=0;
		
		boolean close=false;
		
		String[] sourceLines = source.split("\n");
		for (String sourceLine : sourceLines) {
			m = pattern.matcher(sourceLine);
			if (m.find()) {
				ret += overrideCommentBegin + "\n";
				count++;
			}
			if (count>0) {
				m=pattern2.matcher(sourceLine);
				if (m.find()) {
					count++;
				}				
				m=pattern3.matcher(sourceLine);
				if (m.find()) {
					count--;
					if (count==0) {
						close=true;
					}
				}
				
				
				ret += commentPrefix + sourceLine + "\n";
				if (close) {
					ret += overrideCommentEnd + "\n";
					close=false;
				}
			} else {
				ret += sourceLine + "\n";
			}
			
			
		}
		
		return ret;
	}
	

	public static List<String> getMacroNames(String fragment) {
		List<String> macroNames = new ArrayList<String>();
		if (fragment!=null) {			
			String[] fragLines = fragment.split("\n");
			String patternText = "^\\s*?#macro[ ]*?\\([ ]*?(\\w+)[ \\)]";
			Pattern p = Pattern.compile(patternText);	
			for (String fragLine : fragLines) {
				Matcher m = p.matcher(fragLine);
				if (m.find()) {
					String macroName = m.group(1);
					macroNames.add(macroName);
				}
			}
		}
		return macroNames;
	}

	public static String mergeVelocity(String source, String fragment,
			String pluginName) {
		List<String> macroNames = getMacroNames(fragment);
		String ret = source;
		String overrideCommentBegin = beginOverrideStub + pluginName;
		String overrideCommentEnd = endOverrideStub + pluginName;
		for (String macroName : macroNames) {
			ret = overrideMacro(ret, macroName, overrideCommentBegin,
					overrideCommentEnd);
		}
		PluginFileMerger pm = new PluginFileMerger();

		try {
			String buf = pm.merge(new ByteArrayInputStream(ret.getBytes()),
					beginTag, endTag, beginStub + pluginName, endStub
							+ pluginName, fragment);
			return buf;
		} catch (IOException e) {
			Logger.debug(PluginVelocityMerger.class, "IOException: "
					+ e.getMessage(), e);
		}
		return null;
	}

	public static String removeFragment(String text, String startComment,
			String endComment, String beginOverride, String endOverride) {
		String ret = "";
		String[] lines = text.split("\n");
		String blockEnd = null;
		for (String line : lines) {
			if (blockEnd == null) {
				if (line.startsWith(startComment)) {
					blockEnd = endComment;
				}
				if (line.startsWith(beginOverride)) {
					blockEnd = endOverride;
				}
				if (blockEnd == null) {
					ret += line + "\n";
				}
			} else {
				if (line.startsWith(blockEnd)) {
					blockEnd = null;
				} else {
					if (blockEnd.equalsIgnoreCase(endOverride)) {
						ret += line.substring(commentPrefix.length()) + "\n";
					}
				}
			}
		}
		return ret;
	}

	public static void mergeVelocityFile(String root, String fragment,
			String pluginName) throws IOException {
		List<String> macros = getMacroNames(fragment);
		for (String fileName : overrideFiles) {
			String fullFileName = root + File.separator + fileName;
			File f = new File(fullFileName);
			if (f.exists()) {
				String data = readFile(f);
				for (String macroName : macros) {
					data = overrideMacro(data, macroName, beginOverrideStub
							+ pluginName, endOverrideStub + pluginName);
				}
				writeFile(f, data);
			}
		}
		File f = new File(root + File.separator + destFile);
		String data = readFile(f);
		data = mergeVelocity(data, fragment, pluginName);
		writeFile(f, data);
	}

	public static void removeFragments(String root, String pluginName)
			throws IOException {
		for (String fileName : overrideFiles) {
			String fullFileName = root + File.separator + fileName;
			File f = new File(fullFileName);
			if (f.exists()) {
				String data = readFile(f);
				data = removeFragment(data, beginStub + pluginName, endStub
						+ pluginName, beginOverrideStub + pluginName,
						endOverrideStub + pluginName);
				writeFile(f, data);
			}
		}
		File f = new File(root + File.separator + destFile);
		String data = readFile(f);
		data = removeFragment(data, beginStub + pluginName, endStub
				+ pluginName, beginOverrideStub + pluginName, endOverrideStub
				+ pluginName);
		writeFile(f, data);
	}

	public static String readFile(File f) throws IOException {
		String line;
		StringBuffer ret = new StringBuffer();
		InputStream input = new FileInputStream(f);
		InputStreamReader isr = new InputStreamReader(input);
		BufferedReader reader = new BufferedReader(isr);
		while ((line = reader.readLine()) != null) {
			ret.append(line);
			ret.append("\n");
		}
		return ret.toString();

	}

	public static void writeFile(File f, String text) throws IOException {
		FileWriter fstream = new FileWriter(f);
		BufferedWriter out = new BufferedWriter(fstream);
		out.write(text);
		out.close();
	}

}
