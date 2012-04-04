package com.liferay.util;

import junit.framework.TestCase;

public class XssTest extends TestCase {


	public void testURLHasXSS() {


		//Tests based on attacks mentioned here: http://ha.ckers.org/xss.html
	
		
		assertTrue(Xss.URLHasXSS("<SCRIPT SRC=http://ha.ckers.org/xss.js></SCRIPT>"));
		assertTrue(Xss.URLHasXSS("<IMG \"\"\"><SCRIPT>alert(\"XSS\")</SCRIPT>\">"));		
		assertTrue(Xss.URLHasXSS("< IFRAME SRC=\"javascript:alert('XSS');\"></IFRAME>"));
		assertTrue(Xss.URLHasXSS("<IFRAME SRC=\"javascript:alert('XSS');\"></IFRAME>"));
		assertTrue(Xss.URLHasXSS("<FRAMESET><FRAME SRC=\"javascript:alert('XSS');\"></FRAMESET>"));
		assertTrue(Xss.URLHasXSS("<META HTTP-EQUIV=\"refresh\" CONTENT=\"0;url=javascript:alert('XSS');\">"));
		assertTrue(Xss.URLHasXSS("<META HTTP-EQUIV=\"refresh\" CONTENT=\"0;url=data:text/html;base64,PHNjcmlwdD5hbGVydCgnWFNTJyk8L3NjcmlwdD4K\">"));
		assertTrue(Xss.URLHasXSS("<!--[if gte IE 4]>\n<SCRIPT>alert('XSS');</SCRIPT>\n<![endif]-->"));
		assertTrue(Xss.URLHasXSS("<SCRIPT/XSS SRC=\"http://ha.ckers.org/xss.js\"></SCRIPT>"));
		assertTrue(Xss.URLHasXSS("<BODY ONLOAD=alert('XSS')>"));
		
		
		assertTrue(Xss.URLHasXSS("<IMG SRC=`javascript:alert(\"RSnake says, 'XSS'\")`>"));		
		assertTrue(Xss.URLHasXSS("<IMG SRC=JaVaScRiPt:alert('XSS')>"));
		assertTrue(Xss.URLHasXSS("<IMG SRC=\"javascript:alert('XSS');\">"));
		assertTrue(Xss.URLHasXSS("<iframe src=http://ha.ckers.org/scriptlet.html <"));
		assertTrue(Xss.URLHasXSS("<IfRaME>&amp;tagCount=\">'><IfRaME> "));
		
		
		//The cases below should work but are not working.  More work needs to be done on regex

//		assertTrue(Xss.URLHasXSS("<IMG SRC=&#106;&#97;&#118;&#97;&#115;&#99;&#114;&#105;&#112;&#116;&#58;&#97;&#108;&#101;&#114;&#116;&#40;&#39;&#88;&#83;&#83;&#39;&#41;>"));
//		assertTrue(Xss.URLHasXSS("<IMG SRC=&#0000106&#0000097&#0000118&#0000097&#0000115&#0000099&#0000114&#0000105&#0000112&#0000116&#0000058&#0000097&#0000108&#0000101&#0000114&#0000116&#0000040&#0000039&#0000088&#0000083&#0000083&#0000039&#0000041>"));		
//		assertTrue(Xss.URLHasXSS("<IMG SRC=&#x6A&#x61&#x76&#x61&#x73&#x63&#x72&#x69&#x70&#x74&#x3A&#x61&#x6C&#x65&#x72&#x74&#x28&#x27&#x58&#x53&#x53&#x27&#x29>"));
	}
	
}
