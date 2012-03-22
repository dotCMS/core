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

package com.liferay.util.xml;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * <a href="XMLMergerTask.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Jorge Ferrer
 * @version $Revision: 1.6 $
 *
 */
public class XMLMergerTask extends Task {

	public void setMasterFile(File masterFile) {
		_masterFile = masterFile;
	}

	public void setOutputFile(File outputFile) {
		_outputFile = outputFile;
	}

	public void setSlaveFile(File slaveFile) {
		_slaveFile = slaveFile;
	}

	public void setType(String type) {
		_type = type;
	}

	public void execute() throws BuildException {
		_validateAttributes();

		try {
			XMLMergerRunner runner = new XMLMergerRunner(_type);

			runner.mergeAndSave(_masterFile, _slaveFile, _outputFile);
		}
		catch (Exception e) {
			throw new BuildException(e);
		}
	}

	private void _validateAttributes() {
		_validateMandatoryAttribute(_masterFile, "masterFile");
		_validateMandatoryAttribute(_slaveFile, "slaveFile");
		_validateMandatoryAttribute(_outputFile, "outputFile");
	}

	private void _validateMandatoryAttribute(File value, String name) {
		if (value == null) {
			throw new BuildException(name + " is a required attribute");
		}
	}

 	private File _masterFile;
 	private File _slaveFile;
 	private File _outputFile;
 	private String _type;

}