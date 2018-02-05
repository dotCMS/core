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

import com.dotmarketing.util.diff.html.TextNodeComparator;
import org.xml.sax.SAXException;

/**
 * Interface for classes that are interested in the tree-like result structure
 * as produced by DaisyDiff.
 * 
 * @author kapelonk
 * @see HtmlDiffer
 *
 */
public interface Differ {
	
	/**
	 * Compares two Node Trees.
	 * 
	 * @param leftComparator Root of the first tree.
	 * @param rightComparator Root of the second tree.
	 * @throws SAXException something went wrong with parsing of the trees.
	 */
	void diff(TextNodeComparator leftComparator,
            TextNodeComparator rightComparator) throws SAXException;
}
