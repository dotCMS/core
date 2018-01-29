/*
 * Copyright 2009 Guy Van den Broeck
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dotmarketing.util.diff.output;

import com.dotmarketing.util.diff.html.HtmlSaxDiffOutput;
import com.dotmarketing.util.diff.html.dom.TagNode;
import org.xml.sax.SAXException;

/**
 * Interface for classes that need to process the result from the tree-like
 * represenation of the output.
 * 
 * @author kapelonk
 * @see HtmlSaxDiffOutput
 *
 */
public interface DiffOutput {

	/**
	 * Parses a Node Tree and produces an output format.
	 * @param node Root not of the tree
	 * @throws SAXException something went wrong with parsing.
	 */
	void generateOutput(TagNode node) throws SAXException;
}
