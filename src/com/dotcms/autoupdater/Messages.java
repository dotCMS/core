package com.dotcms.autoupdater;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {
	private static final String BUNDLE_NAME = "com.dotcms.autoupdater.messages"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
			.getBundle(BUNDLE_NAME);

	private Messages() {
	}

	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
	
	public static String getString(String key, String... args) {
		try {
			String message=RESOURCE_BUNDLE.getString(key);
			
			for (int i=1;i<=args.length;i++) {
				String arg=args[i-1];
				if (arg!=null ) {
					String placeHolder="{"+i+"}";				
					int index=message.indexOf(placeHolder);
					if (index>=0) {
						String ret=message.substring(0, index);
						ret += arg + message.substring(index+placeHolder.length());
						message =ret;
					}
				}
			}
			return message; 
			
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		} 
	}
	
	
 }
