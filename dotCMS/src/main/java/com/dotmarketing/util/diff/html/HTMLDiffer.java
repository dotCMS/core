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

import com.dotcms.repackage.org.eclipse.compare.internal.LCSSettings;
import com.dotcms.repackage.org.eclipse.compare.rangedifferencer.RangeDifference;
import com.dotcms.repackage.org.eclipse.compare.rangedifferencer.RangeDifferencer;
import com.dotmarketing.util.diff.output.DiffOutput;
import com.dotmarketing.util.diff.output.Differ;
import java.util.LinkedList;
import java.util.List;
import org.xml.sax.SAXException;

/**
 * Takes two {@link TextNodeComparator} instances, computes the difference
 * between them, marks the changes, and outputs a merged tree to a
 * {@link HtmlSaxDiffOutput} instance.
 */
public class HTMLDiffer implements Differ{

    private DiffOutput output;
    private RangeDifference[] differences = null;

    public HTMLDiffer(DiffOutput dm) {
        output = dm;
    }

    /**
     * Compares two given Text nodes and returns the differences between them.
     *
     * @param leftComparator
     * @param rightComparator
     * @return
     */
    public RangeDifference[] getDifferences ( TextNodeComparator leftComparator,
                                              TextNodeComparator rightComparator ) {

        if ( differences == null ) {
            LCSSettings settings = new LCSSettings();
            settings.setUseGreedyMethod(false);
            // settings.setPowLimit(1.5);
            // settings.setTooLong(100000*100000);

            differences = RangeDifferencer.findDifferences(settings, leftComparator, rightComparator);
        }

        return differences;
    }

    /**
     * {@inheritDoc}
     */
    public void diff(TextNodeComparator leftComparator, TextNodeComparator rightComparator) throws SAXException {

        //Search for the differences
        RangeDifference[] differences = getDifferences(leftComparator, rightComparator);

        List<RangeDifference> pdifferences = preProcess(differences);

        int currentIndexLeft = 0;
        int currentIndexRight = 0;
        for (RangeDifference d : pdifferences) {

            if (d.leftStart() > currentIndexLeft) {
                rightComparator.handlePossibleChangedPart(currentIndexLeft, d
                        .leftStart(), currentIndexRight, d.rightStart(),
                        leftComparator);
            }
            if (d.leftLength() > 0) {
                rightComparator.markAsDeleted(d.leftStart(), d.leftEnd(),
                        leftComparator, d.rightStart());
            }
            rightComparator.markAsNew(d.rightStart(), d.rightEnd());

            currentIndexLeft = d.leftEnd();
            currentIndexRight = d.rightEnd();
        }
        if (currentIndexLeft < leftComparator.getRangeCount()) {
            rightComparator.handlePossibleChangedPart(currentIndexLeft,
                    leftComparator.getRangeCount(), currentIndexRight,
                    rightComparator.getRangeCount(), leftComparator);
        }

        rightComparator.expandWhiteSpace();
        output.generateOutput(rightComparator.getBodyNode());
    }

    private List<RangeDifference> preProcess(RangeDifference[] differences) {

        List<RangeDifference> newRanges = new LinkedList<RangeDifference>();

        for (int i = 0; i < differences.length; i++) {

            int leftStart = differences[i].leftStart();
            int leftEnd = differences[i].leftEnd();
            int rightStart = differences[i].rightStart();
            int rightEnd = differences[i].rightEnd();
            int kind = differences[i].kind();

            int leftLength = leftEnd - leftStart;
            int rightLength = rightEnd - rightStart;

            while (i + 1 < differences.length
                    && differences[i + 1].kind() == kind
                    && score(leftLength, differences[i + 1].leftLength(),
                            rightLength, differences[i + 1].rightLength()) > (differences[i + 1]
                            .leftStart() - leftEnd)) {
                leftEnd = differences[i + 1].leftEnd();
                rightEnd = differences[i + 1].rightEnd();
                leftLength = leftEnd - leftStart;
                rightLength = rightEnd - rightStart;
                i++;
            }

            newRanges.add(new RangeDifference(kind, rightStart, rightLength, leftStart, leftLength));
        }

        return newRanges;
    }

    public static double score(int... numbers) {
        if ((numbers[0] == 0 && numbers[1] == 0)
                || (numbers[2] == 0 && numbers[3] == 0))
            return 0;

        double d = 0;
        for (double number : numbers) {
            while (number > 3) {
                d += 3;
                number -= 3;
                number *= 0.5;
            }
            d += number;

        }
        return d / (1.5 * numbers.length);
    }
}
