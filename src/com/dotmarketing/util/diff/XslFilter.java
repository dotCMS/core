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

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.ContentHandler;

import com.dotmarketing.util.Logger;

public class XslFilter {

	
    public ContentHandler xsl(ContentHandler consumer, String xslPath)
            throws IOException {
    	Logger.debug(this, "xslPath1 : " +  xslPath);
        try {
        	Logger.debug(this, "xslPath2 : " +  xslPath);
            // Create transformer factory
            TransformerFactory factory = TransformerFactory.newInstance();

            Logger.debug(this, "factory : " +  factory);
            // Use the factory to create a template containing the xsl file
            Templates template = factory.newTemplates(new StreamSource(
                    getClass().getClassLoader().getResourceAsStream(xslPath)));

            
            System.out.println("template : " +  template);
            // Use the template to create a transformer
            TransformerFactory transFact = TransformerFactory.newInstance();
            SAXTransformerFactory saxTransFact = (SAXTransformerFactory) transFact;
            // create a ContentHandler
            TransformerHandler transHand = saxTransFact
                    .newTransformerHandler(template);

            transHand.setResult(new SAXResult(consumer));

            return transHand;

        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (TransformerFactoryConfigurationError e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("Can't transform xml.");

    }

}
