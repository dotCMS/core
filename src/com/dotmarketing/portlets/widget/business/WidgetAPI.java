package com.dotmarketing.portlets.widget.business;

import java.util.List;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;

/**
 * 
 * @since 1.6.5
 * @author Jason Tesser
 *
 */
public interface WidgetAPI {

	public static final String WIDGET_CODE_FIELD_NAME = "Widget Code";
	public static final String WIDGET_USAGE_FIELD_NAME = "Widget Usage";
	public static final String WIDGET_TITLE_FIELD_NAME = "Widget Title";
	public static final String WIDGET_PRE_EXECUTE_FIELD_NAME = "Widget Pre-Execute";
	
	/**
	 * Will create the base widget fields for a specific structure.
	 * @param structure
	 * @throws DotDataException
	 * @throws DotStateException
	 */
	public void createBaseWidgetFields(Structure structure) throws DotDataException, DotStateException;
	
	/**
	 * Will return all widgets
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 */
	public List<Structure> findAll(User user, boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException;
	
}
