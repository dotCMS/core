package com.dotmarketing.osgi.custom.dwr.osgi;

import com.dotmarketing.filters.CMSFilter;
import com.dotmarketing.osgi.GenericBundleActivator;

import org.apache.felix.http.api.ExtHttpService;

import org.directwebremoting.servlet.DwrServlet;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * @author Nathan Keiter
 * Date: 12/05/13
 *
 */
public class Activator extends GenericBundleActivator
{
	private DwrServlet dwrServlet;
	private ExtHttpService extHttpService;
	
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public void start ( BundleContext context ) throws Exception 
    {
		try
        {	        
	        //************************************************************
	        //*****************REGISTER THE DWR SERVLET*******************
	        //************************************************************
	
	        //Initializing services...
	        initializeServices( context );
	        	
	        //Service reference to ExtHttpService that allows us to register servlets and filters
	        ServiceReference serviceReference = context.getServiceReference( ExtHttpService.class.getName() );
	        
	        if ( serviceReference != null )
	        {
	        	//Load http service extension object from service reference
	        	extHttpService = (ExtHttpService) context.getService( serviceReference );
	            
	            //Create our DwrServlet instance
	        	dwrServlet = new DwrServlet();
	            
	        	//Register our DwrServlet
	        	extHttpService.registerServlet( "/custom_dwr", dwrServlet, null, null );
	        }
	
	        //Add servlet path to CMS exclusion list
	        CMSFilter.addExclude( "/app/custom_dwr" );

        }
        catch ( Exception exception )
        {
            exception.printStackTrace();
            throw exception;
        }
    }

    public void stop ( BundleContext context ) throws Exception 
    {
		try
        {	        
	        //Unregister all the bundle services
			unregisterServices( context );

        }
        catch ( Exception exception )
        {
            exception.printStackTrace();
            throw exception;
        }
    }
    
}
