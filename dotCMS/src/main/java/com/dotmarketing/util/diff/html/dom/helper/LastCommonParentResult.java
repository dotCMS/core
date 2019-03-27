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
package com.dotmarketing.util.diff.html.dom.helper;

import com.dotmarketing.util.diff.html.dom.TagNode;

/**
 * When detecting the last common parent of two nodes, all results are stored as
 * a {@link LastCommonParentResult}.
 */
public class LastCommonParentResult {

    public LastCommonParentResult() {

    }

    // Parent
    private TagNode parent;

    public TagNode getLastCommonParent() {
        return parent;
    }

    public void setLastCommonParent(TagNode parent) {
        this.parent = parent;
    }

    // Splitting
    private boolean splittingNeeded = false;

    public boolean isSplittingNeeded() {
        return splittingNeeded;
    }

    public void setSplittingNeeded() {
        splittingNeeded = true;
    }

    // Depth
    private int lastCommonParentDepth = -1;

    public int getLastCommonParentDepth() {
        return lastCommonParentDepth;
    }

    public void setLastCommonParentDepth(int depth) {
        lastCommonParentDepth = depth;
    }

    // Index
    private int indexInLastCommonParent = -1;

    public int getIndexInLastCommonParent() {
        return indexInLastCommonParent;
    }

    public void setIndexInLastCommonParent(int index) {
        indexInLastCommonParent = index;
    }

}
