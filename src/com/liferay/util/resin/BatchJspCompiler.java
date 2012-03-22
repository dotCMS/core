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

package com.liferay.util.resin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.util.Logger;
import com.liferay.util.FileUtil;
import com.liferay.util.StringUtil;
import com.liferay.util.lang.MethodInvoker;
import com.liferay.util.lang.MethodWrapper;

/**
 * <a href="BatchJspCompiler.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.3 $
 *
 */
public class BatchJspCompiler {

	public static void main(String[] args) {
		if (args.length == 2) {
			new BatchJspCompiler(args[0], args[1]);
		}
		else {
			throw new IllegalArgumentException();
		}
	}

	public BatchJspCompiler(String appDir, String classDir) {
		try {
			_appDir = appDir;
			_classDir = classDir;

			_compile(new File(appDir));
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
				else if (file.getName().endsWith(".jsp")) {
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

			String fileName = file.toString();

			String[] args = new String[] {
				"-app-dir", _appDir, "-class-dir", _classDir, fileName
			};

			MethodWrapper methodWrapper = new MethodWrapper(
				"com.caucho.jsp.JspCompiler", "main", new Object[] {args});

			try {
				MethodInvoker.invoke(methodWrapper);
			}
			catch (Exception e) {
				FileUtil.write(
					fileName + ".jspc_error", StringUtil.stackTrace(e));
			}
		}
	}

	private String _appDir;
	private String _classDir;

}