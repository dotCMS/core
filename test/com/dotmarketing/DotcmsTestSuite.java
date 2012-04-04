package com.dotmarketing;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.dotmarketing.business.APILocatorTest;
import com.dotmarketing.business.FactoryLocatorTest;
import com.dotmarketing.business.PermissionAPITest;
import com.dotmarketing.portlets.categories.business.CategoryFactoryImplTest;
import com.dotmarketing.portlets.chains.ChainServletTest;
import com.dotmarketing.portlets.chains.business.ChainAPITest;
import com.dotmarketing.portlets.contentlet.ajax.ContentletAjaxTest;
import com.dotmarketing.portlets.contentlet.business.ContentletAPITest;
import com.dotmarketing.viewtools.CategoriesWebAPITest;
import com.liferay.util.XssTest;

public class DotcmsTestSuite {

	public DotcmsTestSuite(String name) {
	}
	
    public static Test suite()
    {
        return new TestSuite(new Class[] { 
        		ChainAPITest.class, 
        		ChainServletTest.class,
        		APILocatorTest.class,
        		FactoryLocatorTest.class,
        		PermissionAPITest.class,
        		ContentletAjaxTest.class,
        		ContentletAPITest.class,
        		CategoryFactoryImplTest.class,
        		CategoriesWebAPITest.class,
        		XssTest.class
        		});
    }


}
