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

package com.dotmarketing.util.diff;

import java.io.IOException;

import com.dotmarketing.util.diff.helper.NekoHtmlParser;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class HtmlCleaner {

    private NekoHtmlParser parser;

    private XslFilter filter;

    public HtmlCleaner() {
        this.parser = new NekoHtmlParser();
        this.filter = new XslFilter();
    }

    public void cleanAndParse(InputSource source, ContentHandler consumer)
            throws IOException, SAXException {
        ContentHandler cleanupFilter = filter.xsl(consumer,
                "diff/xsl/cleanup.xsl");
        parser.parse(source, cleanupFilter);
    }

}
