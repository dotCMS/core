/**
 *  com.dotmarketing.viewtools
 */
package com.dotmarketing.viewtools;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.util.DateUtil;

/**
 * WebAPI class to manage custom date views
 *
 * @author  Armando Siem
 * @since   1.6.0
 */
public class DateViewWebAPI extends DateTool implements ViewTool {
	
	/**
	  * Init method of the WebAPI
	  * @param		obj Obj
	  */
	
	public void init(Object obj) {
	}
	
	/**
	  * Method of the API to show custom diff date result with the current date.
	  * @param		date date to diff with the current date.
	  * @return		String with a result message.
	  */
	public static String friendly(Date date) {
		return DateUtil.prettyDateSince(date);
	}
	
	public static int getOffSet()
	{
		Date now = new Date();
		return getOffSet(now);
	}
	
	public static int getOffSet(Date date)
	{
		GregorianCalendar gc = new GregorianCalendar();		
 		TimeZone tz = TimeZone.getDefault();
	 	int offset = tz.getOffset((date).getTime());
	 	return offset;	 	
	}
}