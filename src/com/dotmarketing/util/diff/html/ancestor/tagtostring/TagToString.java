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

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.dotmarketing.util.diff.html.ancestor.ChangeText;
import com.dotmarketing.util.diff.html.ancestor.TagChangeSematic;
import com.dotmarketing.util.diff.html.dom.TagNode;
import com.dotmarketing.util.diff.html.modification.HtmlLayoutChange;
import com.dotmarketing.util.diff.html.modification.HtmlLayoutChange.Type;
import org.xml.sax.Attributes;

public class TagToString {

    protected TagNode node;

    protected TagChangeSematic sem;

    private ResourceBundle bundle;
    
    private HtmlLayoutChange htmlLayoutChange = null;

    protected TagToString(TagNode node, TagChangeSematic sem,
            ResourceBundle bundle) {
        this.node = node;
        this.sem = sem;
        this.bundle = bundle;
        
    }

    public String getDescription() {
    	
        return getString("diff-" + node.getQName());
        
    }

    public void getRemovedDescription(ChangeText txt) {
    	htmlLayoutChange = new HtmlLayoutChange();
    	htmlLayoutChange.setEndingTag(node.getEndTag());
    	htmlLayoutChange.setOpeningTag(node.getOpeningTag());
    	htmlLayoutChange.setType(Type.TAG_REMOVED);
        if (sem == TagChangeSematic.MOVED) {
            txt.addText(getMovedOutOf() + " " + getArticle().toLowerCase()
                    + " ");
            txt.addHtml("<b>");
            txt.addText(getDescription().toLowerCase());
            txt.addHtml("</b>");
        } else if (sem == TagChangeSematic.STYLE) {
            txt.addHtml("<b>");
            txt.addText(getDescription());
            txt.addHtml("</b>");
            txt.addText(" " + getStyleRemoved().toLowerCase());
        } else {
            txt.addHtml("<b>");
            txt.addText(getDescription());
            txt.addHtml("</b>");
            txt.addText(" " + getRemoved().toLowerCase());
        }
        addAttributes(txt, node.getAttributes());
        txt.addText(".");
    }

    public void getAddedDescription(ChangeText txt) {
    	htmlLayoutChange = new HtmlLayoutChange();
    	htmlLayoutChange.setEndingTag(node.getEndTag());
    	htmlLayoutChange.setOpeningTag(node.getOpeningTag());
    	htmlLayoutChange.setType(Type.TAG_ADDED);
        if (sem == TagChangeSematic.MOVED) {
            txt.addText(getMovedTo() + " " + getArticle().toLowerCase() + " ");
            txt.addHtml("<b>");
            txt.addText(getDescription().toLowerCase());
            txt.addHtml("</b>");
        } else if (sem == TagChangeSematic.STYLE) {
            txt.addHtml("<b>");
            txt.addText(getDescription());
            txt.addHtml("</b>");
            txt.addText(" " + getStyleAdded().toLowerCase());
        } else {
            txt.addHtml("<b>");
            txt.addText(getDescription());
            txt.addHtml("</b>");
            txt.addText(" " + getAdded().toLowerCase());
        }
        addAttributes(txt, node.getAttributes());
        txt.addText(".");
    }

    protected String getMovedTo() {
        return getString("diff-movedto");
    }

    protected String getStyleAdded() {
        return getString("diff-styleadded");
    }

    protected String getAdded() {
        return getString("diff-added");
    }

    protected String getMovedOutOf() {
        return getString("diff-movedoutof");
    }

    protected String getStyleRemoved() {
        return getString("diff-styleremoved");
    }

    protected String getRemoved() {
        return getString("diff-removed");
    }

    protected void addAttributes(ChangeText txt, Attributes attributes) {
        if (attributes.getLength() < 1)
            return;

        txt.addText(" " + getWith().toLowerCase() + " "
                + translateArgument(attributes.getQName(0)) + " "
                + attributes.getValue(0));
        for (int i = 1; i < attributes.getLength() - 1; i++) {
            txt.addText(", " + translateArgument(attributes.getQName(i)) + " "
                    + attributes.getValue(i));
        }
        if (attributes.getLength() > 1) {
            txt.addText(" "
                    + getAnd().toLowerCase()
                    + " "
                    + translateArgument(attributes.getQName(attributes
                            .getLength() - 1)) + " "
                    + attributes.getValue(attributes.getLength() - 1));
        }
    }

    private String getAnd() {
        return getString("diff-and");
    }

    private String getWith() {
        return getString("diff-with");
    }

    protected String translateArgument(String name) {
        if (name.equalsIgnoreCase("src"))
            return getSource().toLowerCase();
        if (name.equalsIgnoreCase("width"))
            return getWidth().toLowerCase();
        if (name.equalsIgnoreCase("height"))
            return getHeight().toLowerCase();
        return name;
    }

    private String getHeight() {
        return getString("diff-height");
    }

    private String getWidth() {
        return getString("diff-width");
    }

    protected String getSource() {
        return getString("diff-source");
    }

    protected String getArticle() {
        return getString("diff-" + node.getQName() + "-article");
    }

    public String getString(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

	/**
	 * @return the htmlChange
	 */
	public HtmlLayoutChange getHtmlLayoutChange() {
		return htmlLayoutChange;
	}
    
    

}
