/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.portal.struts;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.util.MessageResourcesFactory;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.LanguageKey;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringUtil;
import com.oroad.stxx.util.PropertyMessageResources;

/**
 * <a href="MultiMessageResources.java.html"><b><i>View Source </i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.6 $
 *
 */
public class MultiMessageResources extends PropertyMessageResources {

	public MultiMessageResources(MessageResourcesFactory factory,
								 String config) {

		super(factory, config);
	}

	public MultiMessageResources(MessageResourcesFactory factory,
								 String config, boolean returnNull) {

		super(factory, config, returnNull);
	}

	public Map getMessages() {
		return messages;
	}

	public void setServletContext(ServletContext servletContext) {
		_servletContext = servletContext;
	}

	protected void loadLocale(String localeKey) {
		synchronized (locales) {
			if (locales.get(localeKey) != null) {
				return;
			}

			locales.put(localeKey, localeKey);
		}

		String[] names = StringUtil.split(config.replace('.', '/'));

		for (int i = 0; i < names.length; i++) {
			String name = names[i];
			if (localeKey.length() > 0) {
				name += "_" + localeKey;
			}
			name += ".properties";

			_loadProps(name, localeKey);
		}

		for (int i = 0; i < names.length; i++) {
			String name = names[i];
			if (localeKey.length() > 0) {
				name += "_" + localeKey;
			}
			name += ".properties";

			_loadProps(name, localeKey);
		}
	}

	private void _loadProps(
		String name, String localeKey) {

		if(name.contains("cms_language")) {
			LanguageAPI langAPI = APILocator.getLanguageAPI();
			List<LanguageKey> keys;
			if(localeKey.split("_").length > 1) {
				keys = langAPI.getLanguageKeys(localeKey.split("_")[0], localeKey.split("_")[1]);
			} else {
				keys = langAPI.getLanguageKeys(localeKey.split("_")[0]);
				
			}

			if (keys.size() < 1) {
				return;
			}

			synchronized (messages) {
				Iterator<LanguageKey> names = keys.iterator();
	
				while (names.hasNext()) {
					LanguageKey langkey = (LanguageKey)names.next();
					String key = langkey.getKey();
					messages.put(messageKey(localeKey, key),
							langkey.getValue());
				}
			}
			
		} else {
		Properties props = new Properties();

		try {
			URL url = null;

				url = _servletContext.getResource("/WEB-INF/" + name);
			
			if (url != null) {
				InputStream is = url.openStream();

				BufferedReader buffy = new BufferedReader( new InputStreamReader(is));
				String line = null;
				

					while ((line = buffy.readLine()) != null) {
					if(UtilMethods.isSet(line) 
							&& line.indexOf("=") > -1 
							&& ! line.startsWith("#")){
						String[] arr = line.split("=", 2);
						if(arr.length > 1){
							String key = arr[0].trim();
							String val = arr[1].trim();
							if(val.indexOf("\\u") >-1){
								
								if(val.indexOf("\\u") >-1){
									
									StringBuffer buffer = new StringBuffer( val.length() );
									boolean precedingBackslash = false;
									for (int i = 0; i < val.length(); i++) {
							            char c = val.charAt(i);
							            if (precedingBackslash) {
							            	switch (c) {
							            	case 'f': c = '\f'; break;
							            	case 'n': c = '\n'; break;
							            	case 'r': c = '\r'; break;
							            	case 't': c = '\t'; break;
							            	case 'u':
							            		String hex = val.substring( i + 1, i + 5 );
							            		c = (char) Integer.parseInt(hex, 16 );
							            		i += 4;
							            	}
							            	precedingBackslash = false;
							            } else {
							            	precedingBackslash = (c == '\\');
							            }
							            if (!precedingBackslash) {
							                buffer.append(c);
							            }
							        }
									val= buffer.toString();}
								
								
							}
							props.put(key, val);
						}

					}
					
			    }
				buffy.close();
				is.close();
			}
		}
		catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		}

		if (props.size() < 1) {
			return;
		}

		synchronized (messages) {
			Enumeration names = props.keys();

			while (names.hasMoreElements()) {
				String key = (String)names.nextElement();

				messages.put(messageKey(localeKey, key),
							 props.getProperty(key));
			}
		}
	}
	}

	public synchronized void reload() { 
	    locales.clear();
	    messages.clear();
	    formats.clear();
	 }
	
	private static final Log _log =
		LogFactory.getLog(MultiMessageResources.class);

	private transient ServletContext _servletContext;

}