package com.dotcms.publishing;

import java.io.Writer;
import com.dotmarketing.util.XMLUtils;
import com.thoughtworks.xstream.core.util.QuickWriter;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;

/**
 * There a set of characters that are valid in UTF-8 but not valid in XML 1.0
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
     * @see com.thoughtworks.xstream.io.xml.PrettyPrintWriter#writeText(com.thoughtworks.xstream.core.util.QuickWriter, java.lang.String)
     */
    protected void writeText(QuickWriter writer, String text) {
        

    	//Removes all the invalid XML characters. 
        super.writeText(writer, XMLUtils.toXML10(text));
    }

    
}
