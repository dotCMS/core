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

import java.util.LinkedList;
import java.util.List;

import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.rangedifferencer.RangeDifferencer;
import com.dotmarketing.util.diff.output.TextDiffOutput;
import com.dotmarketing.util.diff.output.TextDiffer;

/**
 * Takes 2 AtomSplitters and computes the difference between them. Output is
 * sent to a given <code>HTMLSaxDiffOutput</code> and tags are diffed
 * internally on a second iteration. The results are processed as to combine
 * small subsequent changes in to larger changes.
 */
public class TagDiffer implements TextDiffer{

    private TextDiffOutput output;

    public TagDiffer(TextDiffOutput output) {
        this.output = output;
    }

    /**
     * {@inheritDoc}
     */
    public void diff(IAtomSplitter leftComparator, IAtomSplitter rightComparator)
            throws Exception {

        RangeDifference[] differences = RangeDifferencer.findDifferences(
                leftComparator, rightComparator);

        List<RangeDifference> pdifferences = preProcess(differences,
                leftComparator);

        int rightAtom = 0;
        int leftAtom = 0;

        for (int i = 0; i < pdifferences.size(); i++) {

            parseNoChange(leftAtom, pdifferences.get(i).leftStart(), rightAtom,
                    pdifferences.get(i).rightStart(), leftComparator,
                    rightComparator);

            String leftString = leftComparator.substring(pdifferences.get(i)
                    .leftStart(), pdifferences.get(i).leftEnd());
            String rightString = rightComparator.substring(pdifferences.get(i)
                    .rightStart(), pdifferences.get(i).rightEnd());

            if (pdifferences.get(i).leftLength() > 0)
                output.addRemovedPart(leftString);

            if (pdifferences.get(i).rightLength() > 0)
                output.addAddedPart(rightString);

            rightAtom = pdifferences.get(i).rightEnd();
            leftAtom = pdifferences.get(i).leftEnd();

        }
        if (rightAtom < rightComparator.getRangeCount())
            parseNoChange(leftAtom, leftComparator.getRangeCount(), rightAtom,
                    rightComparator.getRangeCount(), leftComparator,
                    rightComparator);

    }

    private void parseNoChange(int beginLeft, int endLeft, int beginRight,
            int endRight, IAtomSplitter leftComparator,
            IAtomSplitter rightComparator) throws Exception {

        StringBuilder sb = new StringBuilder();

        /*
         * We can assume that the LCS is correct and that there are exacly as
         * many atoms left and right
         */
        while (beginLeft < endLeft) {

            while (beginLeft < endLeft
                    && !rightComparator.getAtom(beginRight)
                            .hasInternalIdentifiers()
                    && !leftComparator.getAtom(beginLeft)
                            .hasInternalIdentifiers()) {
                sb.append(rightComparator.getAtom(beginRight).getFullText());
                beginRight++;
                beginLeft++;
            }

            if (sb.length() > 0) {
                output.addClearPart(sb.toString());
                sb.setLength(0);
            }

            if (beginLeft < endLeft) {

                IAtomSplitter leftComparator2 = new ArgumentComparator(
                        leftComparator.getAtom(beginLeft).getFullText());
                IAtomSplitter rightComparator2 = new ArgumentComparator(
                        rightComparator.getAtom(beginRight).getFullText());

                RangeDifference[] differences2 = RangeDifferencer
                        .findDifferences(leftComparator2, rightComparator2);
                List<RangeDifference> pdifferences2 = preProcess(differences2,
                        2);

                int rightAtom2 = 0;
                for (int j = 0; j < pdifferences2.size(); j++) {
                    if (rightAtom2 < pdifferences2.get(j).rightStart()) {
                        output.addClearPart(rightComparator2.substring(
                                rightAtom2, pdifferences2.get(j).rightStart()));
                    }
                    if (pdifferences2.get(j).leftLength() > 0) {
                        output.addRemovedPart(leftComparator2.substring(
                                pdifferences2.get(j).leftStart(), pdifferences2
                                        .get(j).leftEnd()));
                    }
                    if (pdifferences2.get(j).rightLength() > 0) {
                        output.addAddedPart(rightComparator2.substring(
                                pdifferences2.get(j).rightStart(),
                                pdifferences2.get(j).rightEnd()));
                    }

                    rightAtom2 = pdifferences2.get(j).rightEnd();

                }
                if (rightAtom2 < rightComparator2.getRangeCount())
                    output.addClearPart(rightComparator2.substring(rightAtom2));
                beginLeft++;
                beginRight++;
            }

        }

    }

    private List<RangeDifference> preProcess(RangeDifference[] differences,
            IAtomSplitter leftComparator) {

        List<RangeDifference> newRanges = new LinkedList<RangeDifference>();

        for (int i = 0; i < differences.length; i++) {

            int leftStart = differences[i].leftStart();
            int leftEnd = differences[i].leftEnd();
            int rightStart = differences[i].rightStart();
            int rightEnd = differences[i].rightEnd();
            int kind = differences[i].kind();
            int temp = leftEnd;
            boolean connecting = true;

            while (connecting && i + 1 < differences.length
                    && differences[i + 1].kind() == kind) {

                int bridgelength = 0;

                int nbtokens = Math.max((leftEnd - leftStart),
                        (rightEnd - rightStart));
                if (nbtokens > 5) {
                    if (nbtokens > 10) {
                        bridgelength = 3;
                    } else
                        bridgelength = 2;
                }

                while ((leftComparator.getAtom(temp) instanceof DelimiterAtom || (bridgelength-- > 0))
                        && temp < differences[i + 1].leftStart()) {

                    temp++;
                }
                if (temp == differences[i + 1].leftStart()) {
                    leftEnd = differences[i + 1].leftEnd();
                    rightEnd = differences[i + 1].rightEnd();
                    temp = leftEnd;
                    i++;
                } else {
                    connecting = false;
                    if (!(leftComparator.getAtom(temp) instanceof DelimiterAtom)) {
                        if (leftComparator.getAtom(temp).getFullText().equals(
                                " "))
                            throw new IllegalStateException(
                                    "space found aiaiai");
                    }
                }
            }
            newRanges.add(new RangeDifference(kind, rightStart, rightEnd
                    - rightStart, leftStart, leftEnd - leftStart));
        }

        return newRanges;
    }

    private List<RangeDifference> preProcess(RangeDifference[] differences,
            int span) {

        List<RangeDifference> newRanges = new LinkedList<RangeDifference>();

        for (int i = 0; i < differences.length; i++) {

            int leftStart = differences[i].leftStart();
            int leftEnd = differences[i].leftEnd();
            int rightStart = differences[i].rightStart();
            int rightEnd = differences[i].rightEnd();
            int kind = differences[i].kind();

            while (i + 1 < differences.length
                    && differences[i + 1].kind() == kind
                    && differences[i + 1].leftStart() <= leftEnd + span
                    && differences[i + 1].rightStart() <= rightEnd + span) {
                leftEnd = differences[i + 1].leftEnd();
                rightEnd = differences[i + 1].rightEnd();
                i++;
            }

            newRanges.add(new RangeDifference(kind, rightStart, rightEnd
                    - rightStart, leftStart, leftEnd - leftStart));
        }

        return newRanges;
    }

}
