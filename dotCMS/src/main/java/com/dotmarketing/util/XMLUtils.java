package com.dotmarketing.util;

import com.dotmarketing.exception.DotRuntimeException;
import org.apache.commons.io.IOUtils;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.regex.Pattern;

public class XMLUtils {

    private static final DocumentBuilderFactory factory  = DocumentBuilderFactory.newInstance();

	/**
	 * This will take the three pre-defined entities in XML 1.0 (used
	 * specifically in XML elements) and convert their character representation
	 * to the appropriate entity reference, suitable for XML element content.
	 * 
	 * @param str
	 *            <code>String</code> input to escape.
	 * @return <code>String</code> with escaped content.
	 */
	public static String xmlEscape(String str) {

	        return org.apache.commons.lang.StringEscapeUtils.escapeXml(str);
	   
	}
	
    public static final String prologue = "<?xml version=\"1.1\" encoding=\"UTF-8\"?>\n";
	
	public static boolean needsPrologue(File file) {
	    return !readFirstLine(file).startsWith("<?xml version=");
	}
	
    public static boolean addPrologueIfNeeded(final File file)  {
        if(!needsPrologue(file)) {
            return false;
        }
        Logger.info(XMLUtils.class, "prepending XML 1.1 prologue  to :" + file);
        final File tempFile = new File(file.getParentFile(), "prologue" + file.getName());
        try(InputStream input = new SequenceInputStream(new ByteArrayInputStream(prologue.getBytes("UTF-8")), Files.newInputStream(file.toPath())); OutputStream output = Files.newOutputStream(tempFile.toPath()) ){
            IOUtils.copy(input, output );
        }
        catch(Exception e) {
            throw new DotRuntimeException(e);
        }
        if(tempFile.length()<= file.length()) {
            Logger.warn(XMLUtils.class, "unable to add prologue to xml file");
            tempFile.delete();
            return false;
        }
        return tempFile.renameTo(file);
        
        
    }
	
	
    public static String toXML10(final String input) {
        return xml10pattern.matcher(input).replaceAll("");
    }
    
    public static String toXML11(final String input) {
        return xml11pattern.matcher(input).replaceAll("");
    }

    /**
     * Returns true if the XML is schemeless valid
     * @param xml {@link String}
     * @return boolean
     */
	public static boolean isValidXML (final String xml) {

	    if (UtilMethods.isNotSet(xml)) {
	        return false;
        }

        try {

            final DocumentBuilder documentBuilder = factory.newDocumentBuilder();
            documentBuilder.parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {

            Logger.error(XMLUtils.class, e.getMessage(), e);
            return false;
        }

        return true;
    }

	private static String readFirstLine(File file) {
	    try(BufferedReader fileReader = new BufferedReader(Files.newBufferedReader(file.toPath()))){
	        return fileReader.readLine().trim();
	    }
	    catch(Exception e) {
	        throw new DotRuntimeException(e);
	    }
	}
	
	
	
	final static Pattern xml11pattern = Pattern.compile("[^"
                    + "\u0001-\uD7FF"
                    + "\uE000-\uFFFD"
                    + "\ud800\udc00-\udbff\udfff"
                    + "]+");
	
	
	final static Pattern xml10pattern = Pattern.compile("[^"
                    + "\u0009\r\n"
                    + "\u0020-\uD7FF"
                    + "\uE000-\uFFFD"
                    + "\ud800\udc00-\udbff\udfff"
                    + "]");
	
	
	
	
}
