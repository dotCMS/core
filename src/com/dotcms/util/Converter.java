package com.dotcms.util;

import java.io.Serializable;

/**
 * A contract to convert an original bean to destiny bean.
 * 
 * @author jsanca
 * @version 3.7
 * @since Jun 8, 2016
 */
public interface Converter<Original, Destiny> extends Serializable {

	/**
	 * Perform the conversion process to transform an original object into a
	 * destination object.
	 * 
	 * @param original
	 *            - Original
	 * @return Destiny
	 */
    Destiny convert (Original original);

} // E:O:F:Converter.
