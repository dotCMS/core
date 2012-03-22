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

import java.util.ResourceBundle;

import com.dotmarketing.util.diff.html.ancestor.ChangeText;
import com.dotmarketing.util.diff.html.ancestor.TagChangeSematic;
import com.dotmarketing.util.diff.html.dom.TagNode;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

public class AnchorToString extends TagToString {

    protected AnchorToString(TagNode node, TagChangeSematic sem,
            ResourceBundle bundle) {
        super(node, sem, bundle);
    }

    @Override
    protected void addAttributes(ChangeText txt, Attributes attributes) {
        AttributesImpl newAttrs = new AttributesImpl(attributes);
        String href = newAttrs.getValue("href");

        if (href != null) {
            txt.addText(" " + getWithDestination().toLowerCase() + " " + href);
            newAttrs.removeAttribute(newAttrs.getIndex("href"));
        }
        super.addAttributes(txt, newAttrs);
    }

    private String getWithDestination() {
        return getString("diff-withdestination");
    }

}
