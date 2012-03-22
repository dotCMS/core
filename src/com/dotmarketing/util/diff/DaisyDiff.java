/*
 * Copyright 2004 Guy Van den Broeck
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
package com.dotmarketing.util.diff;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Locale;

import com.dotmarketing.util.diff.html.HTMLDiffer;
import com.dotmarketing.util.diff.html.HtmlSaxDiffOutput;
import com.dotmarketing.util.diff.html.TextNodeComparator;
import com.dotmarketing.util.diff.html.dom.DomTreeBuilder;
import com.dotmarketing.util.diff.tag.TagComparator;
import com.dotmarketing.util.diff.tag.TagDiffer;
import com.dotmarketing.util.diff.tag.TagSaxDiffOutput;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class DaisyDiff {

    /**
     * Diffs two html files, outputting the result to the specified consumer.
     */
    public static void diffHTML(InputSource oldSource, InputSource newSource,
            ContentHandler consumer, String prefix, Locale locale)
            throws SAXException, IOException {

        DomTreeBuilder oldHandler = new DomTreeBuilder();
        XMLReader xr1 = XMLReaderFactory.createXMLReader();
        xr1.setContentHandler(oldHandler);
        xr1.parse(oldSource);
        TextNodeComparator leftComparator = new TextNodeComparator(oldHandler,
                locale);

        DomTreeBuilder newHandler = new DomTreeBuilder();
        XMLReader xr2 = XMLReaderFactory.createXMLReader();
        xr2.setContentHandler(newHandler);
        xr2.parse(newSource);

        TextNodeComparator rightComparator = new TextNodeComparator(newHandler,
                locale);

        HtmlSaxDiffOutput output = new HtmlSaxDiffOutput(consumer, prefix);
        HTMLDiffer differ = new HTMLDiffer(output);
        differ.diff(leftComparator, rightComparator);
    }

    /**
     * Diffs two html files word for word as source, outputting the result to
     * the specified consumer.
     */
    public static void diffTag(String oldText, String newText,
            ContentHandler consumer) throws Exception {
        consumer.startDocument();
        TagComparator oldComp = new TagComparator(oldText);
        TagComparator newComp = new TagComparator(newText);

        TagSaxDiffOutput output = new TagSaxDiffOutput(consumer);
        TagDiffer differ = new TagDiffer(output);
        differ.diff(oldComp, newComp);
        consumer.endDocument();
    }

    /**
     * Diffs two html files word for word as source, outputting the result to
     * the specified consumer.
     */
    public static void diffTag(BufferedReader oldText, BufferedReader newText,
            ContentHandler consumer) throws Exception {

        TagComparator oldComp = new TagComparator(oldText);
        TagComparator newComp = new TagComparator(newText);

        TagSaxDiffOutput output = new TagSaxDiffOutput(consumer);
        TagDiffer differ = new TagDiffer(output);
        differ.diff(oldComp, newComp);
    }

}
