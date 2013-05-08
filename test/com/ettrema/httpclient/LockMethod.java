package com.ettrema.httpclient;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;

/**
 *
 * @author mcevoyb
 */
public class LockMethod extends EntityEnclosingMethod {

    public LockMethod( String uri ) {
        super( uri );
    }

    @Override
    public String getName() {
        return "LOCK";
    }
	
    public String getLockToken() {
        try {

            Document document = getResponseAsDocument();
            if( document == null ) {
                throw new RuntimeException("Got empty response to LOCK request");
            }
            Element root = document.getRootElement();
            List<Element> lockTokenEls = RespUtils.getElements(root, "locktoken");
            for( Element el : lockTokenEls) {
				String token = RespUtils.asString( el, "href" );
				if( token == null ) {
					throw new RuntimeException("No href element in locktoken");
				}
				return token;
            }			
			throw new RuntimeException("Didnt find a locktoken/href element in LOCK response");
        } catch( IOException ex ) {
            throw new RuntimeException( ex );
        }
    }	
	
    public Document getResponseAsDocument() throws IOException {        
        InputStream in = getResponseBodyAsStream();
//        IOUtils.copy( in, out );
//        String xml = out.toString();
        try {
            Document document = RespUtils.getJDomDocument(in);
            return document;
        } catch( JDOMException ex ) {
            throw new RuntimeException( ex );
        }
    }	
}
