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

package com.dotmarketing.util.diff.html.ancestor.tagtostring;

import java.util.HashSet;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import com.dotmarketing.util.diff.html.ancestor.TagChangeSematic;
import com.dotmarketing.util.diff.html.dom.TagNode;

public class TagToStringFactory {

    private static final Set<String> containerTags = new HashSet<String>();

    private static final Set<String> styleTags = new HashSet<String>();

    static {
        containerTags.add("html");
        containerTags.add("body");
        containerTags.add("p");
        containerTags.add("blockquote");
        containerTags.add("h1");
        containerTags.add("h2");
        containerTags.add("h3");
        containerTags.add("h4");
        containerTags.add("h5");
        containerTags.add("pre");
        containerTags.add("div");
        containerTags.add("ul");
        containerTags.add("ol");
        containerTags.add("li");
        containerTags.add("table");
        containerTags.add("tbody");
        containerTags.add("tr");
        containerTags.add("td");
        containerTags.add("th");
        containerTags.add("br");
        containerTags.add("hr");
        containerTags.add("code");
        containerTags.add("dl");
        containerTags.add("dt");
        containerTags.add("dd");
        containerTags.add("input");
        containerTags.add("form");
        containerTags.add("img");
        // in-line tags that can be considered containers not styles
        containerTags.add("span");
        containerTags.add("a");

        styleTags.add("i");
        styleTags.add("b");
        styleTags.add("strong");
        styleTags.add("em");
        styleTags.add("font");
        styleTags.add("big");
        styleTags.add("del");
        styleTags.add("tt");
        styleTags.add("sub");
        styleTags.add("sup");
        styleTags.add("strike");
    }

    public TagToString create(TagNode node, Locale locale) {
        TagChangeSematic sem = getChangeSemantic(node.getQName());
        ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_NAME, locale);
        if (node.getQName().equalsIgnoreCase("a")) //$NON-NLS-1$
            return new AnchorToString(node, sem, bundle);
        if (node.getQName().equalsIgnoreCase("img")) //$NON-NLS-1$
            return new NoContentTagToString(node, sem, bundle);
        return new TagToString(node, sem, bundle);
    }

    protected TagChangeSematic getChangeSemantic(String string) {
        if (containerTags.contains(string.toLowerCase()))
            return TagChangeSematic.MOVED;
        if (styleTags.contains(string.toLowerCase()))
            return TagChangeSematic.STYLE;
        return TagChangeSematic.UNKNOWN;
    }

    private static final String BUNDLE_NAME = "diff/messages";

}
