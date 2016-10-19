/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.portal.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.util.Logger;
import com.liferay.util.FileUtil;
import com.liferay.util.StringUtil;

/**
 * <a href="JSPCompiler.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.16 $
 *
 */
public class JSPCompiler {

	public static void main(String[] args) {
		if (args.length == 4) {
			new JSPCompiler(args[0], args[1], args[2], args[3]);
		}
		else {
			throw new IllegalArgumentException();
		}
	}

	public JSPCompiler(String appServerType, String compiler, String classPath,
					   String directory) {

		try {
			_appServerType = appServerType;

			_compiler = compiler;
			if (!_compiler.equals("jikes")) {
				_compiler = "javac";
			}

			_classPath = classPath;
			_directory = directory;

			_compile(new File(directory));
		}
		catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		}
	}

	private void _compile(File directory) throws Exception {
		if (directory.exists() && directory.isDirectory()) {
			List fileList = new ArrayList();

			File[] fileArray = FileUtil.sortFiles(directory.listFiles());

			for (int i = 0; i < fileArray.length; i++) {
				File file = fileArray[i];

				if (file.isDirectory()) {
					_compile(fileArray[i]);
				}
				else if (file.getName().endsWith(".java")) {
					fileList.add(file);
				}
			}

			_compile(directory.getPath(), fileList);
		}
	}

	private void _compile(String sourcePath, List files) throws Exception {
		if (files.size() == 0) {
			return;
		}

		Logger.info(this, sourcePath);

		for (int i = 0; i < files.size(); i++) {
			File file = (File)files.get(i);

			/*if (!_appServerType.equals("jboss-tomcat")) {
				String content = FileUtil.read(file);

				if (content.indexOf("package org.apache.jsp;") == -1) {
					content =
						"package org.apache.jsp;" +
						content.substring(
							content.indexOf(";") + 1, content.length());
				}

				FileUtil.write(file, content);
			}*/

			String classDestination = _directory;
			/*if (!_appServerType.equals("jboss-tomcat")) {
				classDestination = sourcePath;
			}*/

			String cmd =
				_compiler + " -classpath " + _classPath +
				" -d " + classDestination + " " +
				file.toString();

			File classFile = new File(
				sourcePath + File.separator +
				StringUtil.replace(file.getName(), ".java", ".class"));

			if (!classFile.exists()) {
				Runtime rt = Runtime.getRuntime();

				try {
					Process p = rt.exec(cmd);

					BufferedReader br = new BufferedReader(
						new InputStreamReader(p.getErrorStream()));

					StringBuffer sb = new StringBuffer();
					String line = null;

					while ((line = br.readLine()) != null) {
						sb.append(line).append("\n");
					}

					br.close();

					p.waitFor();
					p.destroy();

					if (!classFile.exists()) {
						FileUtil.write(classFile, sb.toString());
					}
				}
				catch (Exception e) {
					Logger.error(this,e.getMessage(),e);
				}
			}
		}

		/*if (!_appServerType.equals("jboss-tomcat")) {
			_migrate(sourcePath);
		}*/
	}

	private void _migrate(String sourcePath) throws Exception {
		File directory =
			new File(
				sourcePath + File.separator + "org" + File.separator +
				"apache" + File.separator + "jsp");

		if (directory.exists() && directory.isDirectory()) {
			File[] fileArray = directory.listFiles();

			for (int i = 0; i < fileArray.length; i++) {
				File file = fileArray[i];

				FileUtil.copyFile(
					file,
					new File(sourcePath + File.separator + file.getName()));
			}
		}

		FileUtil.deltree(new File(sourcePath + File.separator + "org"));
	}

	private String _appServerType;
	private String _compiler;
	private String _classPath;
	private String _directory;

}