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
package com.dotmarketing.util.diff.html.ancestor;

import java.util.List;
import java.util.Locale;

import org.eclipse.compare.rangedifferencer.IRangeComparator;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.rangedifferencer.RangeDifferencer;
import com.dotmarketing.util.diff.html.dom.TagNode;

/**
 * A comparator used when calculating the difference in ancestry of two Nodes.
 */
public class AncestorComparator implements IRangeComparator {

    private List<TagNode> ancestors;

    public AncestorComparator(List<TagNode> ancestors) {
        this.ancestors = ancestors;
    }

    public int getRangeCount() {
        return ancestors.size();
    }

    public boolean rangesEqual(int owni, IRangeComparator otherComp, int otheri) {
        AncestorComparator other;
        try {
            other = (AncestorComparator) otherComp;
        } catch (ClassCastException e) {
            return false;
        }

        return other.getAncestor(otheri).isSameTag(getAncestor(owni));
    }

    public boolean skipRangeComparison(int arg0, int arg1, IRangeComparator arg2) {
        return false;
    }

    public TagNode getAncestor(int i) {
        return ancestors.get(i);
    }

    private String compareTxt = "";

    public String getCompareTxt() {
        return compareTxt;
    }

    public AncestorComparatorResult getResult(AncestorComparator other,
            Locale locale) {

        AncestorComparatorResult result = new AncestorComparatorResult();

        RangeDifference[] differences = RangeDifferencer.findDifferences(other,
                this);

        if (differences.length == 0)
            return result;

        ChangeTextGenerator changeTxt = new ChangeTextGenerator(this, other,
                locale);

        result.setChanged(true);
        result.setChanges(changeTxt.getChanged(differences).toString());
        result.setHtmlLayoutChanges(changeTxt.getHtmlLayoutChanges());

        return result;

    }

}
