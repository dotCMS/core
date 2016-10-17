package com.dotcms.publishing;

import com.dotcms.repackage.com.thoughtworks.xstream.core.util.QuickWriter;
import com.dotcms.repackage.com.thoughtworks.xstream.io.xml.PrettyPrintWriter;

import java.io.Writer;

/**
 * There a set of characters that are valid in UTF-8 but not valid in XML
 * This class should be use to validate and delete those invalid characters
 * See BundlerUtil.objectToXML() as example
 * 
 * @author Oscar Arrieta
 *
 */
public class DotPrettyPrintWriter extends PrettyPrintWriter {

	public DotPrettyPrintWriter(Writer writer) {
        super(writer);
    }
	
    /* (non-Javadoc)
     * @see com.dotcms.repackage.com.thoughtworks.xstream.io.xml.PrettyPrintWriter#writeText(com.dotcms.repackage.com.thoughtworks.xstream.core.util.QuickWriter, java.lang.String)
     */
    protected void writeText(QuickWriter writer, String text) {
        
    	//Pattern to validate invalid XML characters. 
    	String xml10pattern = "[^"
                + "\u0009\r\n"
                + "\u0020-\uD7FF"
                + "\uE000-\uFFFD"
                + "\ud800\udc00-\udbff\udfff"
                + "]";
    	
    	//Removes all the invalid XML characters. 
        String legalText = text.replaceAll(xml10pattern, "");
        super.writeText(writer, legalText);
    }
    
}
