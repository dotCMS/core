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

import com.dotmarketing.util.diff.tag.TagSaxDiffOutput;


/**
 * 
 * Interface for classes that need to process the result from the tag-like
 * represenation of the output.
 * 
 * @author kapelonk
 * @see TagSaxDiffOutput
 *
 */
public interface TextDiffOutput {

	/**
	 * Handles normal text.
	 * @param text string that was not changed.
	 * @throws Exception something went wrong.
	 */
	void addClearPart(String text) throws Exception;
	
	/**
	 * Handles a deletion.
	 * @param text string that was removed.
	 * @throws Exception something went wrong.
	 */
	void addRemovedPart(String text) throws Exception;

	/**
	 * Handles an addition.
	 * @param text string that was added.
	 * @throws Exception something went wrong.
	 */
	void addAddedPart(String text) throws Exception;
}
