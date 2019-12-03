package com.dotcms.rest.param;

import com.liferay.util.StringPool;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Just a simple cast to bind a list of String comma separated into an array list.
 * This could be used as:
 * <code>
 *     @Path("/ids/{strings}")
 *     public Response mymethod(@Context HttpServletRequest request,
 *                              @Context final HttpServletResponse response,
 *                              final StringCommaSeparatedListParam idList) {
 * </code>
 *
 * @author jsanca
 */
public class StringCommaSeparatedListParam extends ArrayList<String> {

    /**
     * Constructor
     * @param params String comma separated strings
     */
    public StringCommaSeparatedListParam(final String params) {

        super(Arrays.asList(params.split(StringPool.COMMA)));
    }
} // StringCommaSeparatedList
