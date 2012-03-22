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
package com.dotmarketing.util.diff.html;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.eclipse.compare.rangedifferencer.IRangeComparator;
import com.dotmarketing.util.diff.html.ancestor.AncestorComparator;
import com.dotmarketing.util.diff.html.ancestor.AncestorComparatorResult;
import com.dotmarketing.util.diff.html.dom.BodyNode;
import com.dotmarketing.util.diff.html.dom.DomTree;
import com.dotmarketing.util.diff.html.dom.Node;
import com.dotmarketing.util.diff.html.dom.TextNode;
import com.dotmarketing.util.diff.html.dom.helper.LastCommonParentResult;
import com.dotmarketing.util.diff.html.modification.Modification;
import com.dotmarketing.util.diff.html.modification.ModificationType;

/**
 * A comparator that generates a DOM tree of sorts from handling SAX events.
 * Then it can be used to compute the difference between DOM trees and mark
 * elements accordingly.
 */
public class TextNodeComparator implements IRangeComparator, Iterable<TextNode> {

    private List<TextNode> textNodes = new ArrayList<TextNode>(50);

    private List<Modification> lastModified = new ArrayList<Modification>();

    private BodyNode bodyNode;

    private Locale locale;

    public TextNodeComparator(DomTree tree, Locale locale) {
        super();
        this.locale = locale;
        textNodes = tree.getTextNodes();
        bodyNode = tree.getBodyNode();
    }

    public BodyNode getBodyNode() {
        return bodyNode;
    }

    public int getRangeCount() {
        return textNodes.size();
    }

    public TextNode getTextNode(int i) {
        return textNodes.get(i);
    }

    private long newID = 0;

    public void markAsNew(int start, int end) {
        if (end <= start)
            return;

        if (whiteAfterLastChangedPart)
            getTextNode(start).setWhiteBefore(false);

        List<Modification> nextLastModified = new ArrayList<Modification>();

        for (int i = start; i < end; i++) {
            Modification mod = new Modification(ModificationType.ADDED);
            mod.setID(newID);
            if (lastModified.size() > 0) {
                mod.setPrevious(lastModified.get(0));
                if (lastModified.get(0).getNext() == null) {
                    for (Modification lastMod : lastModified) {
                        lastMod.setNext(mod);
                    }
                }
            }
            nextLastModified.add(mod);
            getTextNode(i).setModification(mod);
        }
        getTextNode(start).getModification().setFirstOfID(true);
        newID++;
        lastModified = nextLastModified;
    }

    public boolean rangesEqual(int i1, IRangeComparator rangeComp, int i2) {
        TextNodeComparator comp;
        try {
            comp = (TextNodeComparator) rangeComp;
        } catch (RuntimeException e) {
            return false;
        }

        return getTextNode(i1).isSameText(comp.getTextNode(i2));
    }

    public boolean skipRangeComparison(int arg0, int arg1, IRangeComparator arg2) {
        return false;
    }

    private long changedID = 0;

    private boolean changedIDUsed = false;

    public void handlePossibleChangedPart(int leftstart, int leftend,
            int rightstart, int rightend, TextNodeComparator leftComparator) {
        int i = rightstart;
        int j = leftstart;

        if (changedIDUsed) {
            changedID++;
            changedIDUsed = false;
        }

        List<Modification> nextLastModified = new ArrayList<Modification>();

        String changes = null;
        while (i < rightend) {
            AncestorComparator acthis = new AncestorComparator(getTextNode(i)
                    .getParentTree());
            AncestorComparator acother = new AncestorComparator(leftComparator
                    .getTextNode(j).getParentTree());

            AncestorComparatorResult result = acthis.getResult(acother, locale);

            if (result.isChanged()) {

                Modification mod = new Modification(ModificationType.CHANGED);

                if (!changedIDUsed) {
                    mod.setFirstOfID(true);
                    if (nextLastModified.size() > 0) {
                        lastModified = nextLastModified;
                        nextLastModified = new ArrayList<Modification>();
                    }
                } else if (result.getChanges() != null
                        && !result.getChanges().equals(changes)) {
                    changedID++;
                    mod.setFirstOfID(true);
                    if (nextLastModified.size() > 0) {
                        lastModified = nextLastModified;
                        nextLastModified = new ArrayList<Modification>();
                    }
                }

                if (lastModified.size() > 0) {
                    mod.setPrevious(lastModified.get(0));
                    if (lastModified.get(0).getNext() == null) {
                        for (Modification lastMod : lastModified) {
                            lastMod.setNext(mod);
                        }
                    }
                }
                nextLastModified.add(mod);

                mod.setChanges(result.getChanges());
                mod.setHtmlLayoutChanges(result.getHtmlLayoutChanges());
                mod.setID(changedID);

                getTextNode(i).setModification(mod);
                changes = result.getChanges();
                changedIDUsed = true;
            } else if (changedIDUsed) {
                changedID++;
                changedIDUsed = false;
            }

            i++;
            j++;
        }

        if (nextLastModified.size() > 0)
            lastModified = nextLastModified;

    }

    // used to remove the whitespace between a red and green block
    private boolean whiteAfterLastChangedPart = false;

    private long deletedID = 0;

    public void markAsDeleted(int start, int end, TextNodeComparator oldComp,
            int before) {

        if (end <= start)
            return;

        if (before > 0 && getTextNode(before - 1).isWhiteAfter()) {
            whiteAfterLastChangedPart = true;
        } else {
            whiteAfterLastChangedPart = false;
        }

        List<Modification> nextLastModified = new ArrayList<Modification>();

        for (int i = start; i < end; i++) {
            Modification mod = new Modification(ModificationType.REMOVED);
            mod.setID(deletedID);
            if (lastModified.size() > 0) {
                mod.setPrevious(lastModified.get(0));
                if (lastModified.get(0).getNext() == null) {
                    for (Modification lastMod : lastModified) {
                        lastMod.setNext(mod);
                    }
                }
            }
            nextLastModified.add(mod);

            // oldComp is used here because we're going to move its deleted
            // elements
            // to this tree!
            oldComp.getTextNode(i).setModification(mod);
        }
        oldComp.getTextNode(start).getModification().setFirstOfID(true);

        List<Node> deletedNodes = oldComp.getBodyNode().getMinimalDeletedSet(
                deletedID);

        // Set prevLeaf to the leaf after which the old HTML needs to be
        // inserted
        Node prevLeaf = null;
        if (before > 0)
            prevLeaf = getTextNode(before - 1);

        // Set nextLeaf to the leaf before which the old HTML needs to be
        // inserted
        Node nextLeaf = null;
        if (before < getRangeCount())
            nextLeaf = getTextNode(before);


        while (deletedNodes.size() > 0) {
            LastCommonParentResult prevResult, nextResult;
            if (prevLeaf != null) {
                prevResult = prevLeaf.getLastCommonParent(deletedNodes
                        .get(0));
            } else {
                prevResult = new LastCommonParentResult();
                prevResult.setLastCommonParent(getBodyNode());
                prevResult.setIndexInLastCommonParent(-1);
            }
            if (nextLeaf != null) {
                nextResult = nextLeaf.getLastCommonParent(deletedNodes
                        .get(deletedNodes.size() - 1));
            } else {
                nextResult = new LastCommonParentResult();
                nextResult.setLastCommonParent(getBodyNode());
                nextResult.setIndexInLastCommonParent(getBodyNode()
                        .getNbChildren());
            }

            if (prevResult.getLastCommonParentDepth() == nextResult
                    .getLastCommonParentDepth()) {
                // We need some metric to choose which way to add...
                if (deletedNodes.get(0).getParent() == deletedNodes.get(
                        deletedNodes.size() - 1).getParent()
                        && prevResult.getLastCommonParent() == nextResult
                        .getLastCommonParent()) {
                    // The difference is not in the parent
                    prevResult.setLastCommonParentDepth(prevResult
                            .getLastCommonParentDepth() + 1);

                } else {
                    // The difference is in the parent, so compare them
                    // now THIS is tricky
                    double distancePrev = deletedNodes
                    .get(0)
                    .getParent()
                    .getMatchRatio(prevResult.getLastCommonParent());
                    double distanceNext = deletedNodes
                    .get(deletedNodes.size() - 1)
                    .getParent()
                    .getMatchRatio(nextResult.getLastCommonParent());

                    if (distancePrev <= distanceNext) {
                        prevResult.setLastCommonParentDepth(prevResult
                                .getLastCommonParentDepth() + 1);
                    } else {
                        nextResult.setLastCommonParentDepth(nextResult
                                .getLastCommonParentDepth() + 1);
                    }
                }

            }

            if (prevResult.getLastCommonParentDepth() > nextResult
                    .getLastCommonParentDepth()) {

                // Inserting at the front
                if (prevResult.isSplittingNeeded()) {
                    prevLeaf.getParent().splitUntill(
                            prevResult.getLastCommonParent(), prevLeaf,
                            true);
                }
                prevLeaf = deletedNodes.remove(0).copyTree();
                prevLeaf.setParent(prevResult.getLastCommonParent());
                prevResult.getLastCommonParent().addChild(
                        prevResult.getIndexInLastCommonParent() + 1,
                        prevLeaf);

            } else if (prevResult.getLastCommonParentDepth() < nextResult
                    .getLastCommonParentDepth()) {
                // Inserting at the back
                if (nextResult.isSplittingNeeded()) {
                    boolean splitOccured = nextLeaf.getParent()
                    .splitUntill(nextResult.getLastCommonParent(),
                            nextLeaf, false);

                    if (splitOccured) {
                        // The place where to insert is shifted one place to the
                        // right
                        nextResult.setIndexInLastCommonParent(nextResult
                                .getIndexInLastCommonParent() + 1);
                    }
                }
                nextLeaf = deletedNodes.remove(deletedNodes.size() - 1)
                .copyTree();
                nextLeaf.setParent(nextResult.getLastCommonParent());
                nextResult.getLastCommonParent().addChild(
                        nextResult.getIndexInLastCommonParent(), nextLeaf);
            } else
                throw new IllegalStateException();

        }
        lastModified = nextLastModified;
        deletedID++;
    }

    public void expandWhiteSpace() {
        getBodyNode().expandWhiteSpace();
    }

    public Iterator<TextNode> iterator() {
        return textNodes.iterator();
    }
}