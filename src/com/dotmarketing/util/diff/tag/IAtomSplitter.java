/*
 * Copyright 2007 Guy Van den Broeck
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
package com.dotmarketing.util.diff.tag;

import org.eclipse.compare.rangedifferencer.IRangeComparator;

/**
 * Extens the IRangeComparator interface with functionality to recreate parts of
 * the original document.
 */
public interface IAtomSplitter extends IRangeComparator {

    public Atom getAtom(int i);

    public String substring(int startAtom, int endAtom);

    public String substring(int startAtom);

}
