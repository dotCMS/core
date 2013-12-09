package com.dotcms.rest.config;

import java.util.HashSet;
import java.util.Set;

import com.dotcms.rest.EnvironmentResource;
import com.dotcms.rest.RestExamplePortlet;

public class DotRestApplication extends javax.ws.rs.core.Application {
	protected static Set<Class<?>> REST_CLASSES = null;

	@Override
	public Set<Class<?>> getClasses() {
		if(REST_CLASSES ==null){
			synchronized (this.getClass().getName().intern()) {
				if(REST_CLASSES ==null){

					REST_CLASSES = 	new HashSet<Class<?>>();
					REST_CLASSES.add(com.dotcms.rest.ESIndexResource.class);
					REST_CLASSES.add(com.dotcms.rest.RoleResource.class);
					REST_CLASSES.add(com.dotcms.rest.BundleResource.class);
					REST_CLASSES.add(com.dotcms.rest.StructureResource.class);
					REST_CLASSES.add(com.dotcms.rest.ContentResource.class);
					REST_CLASSES.add(com.dotcms.rest.BundlePublisherResource.class);
					REST_CLASSES.add(com.dotcms.rest.JSPPortlet.class);
					REST_CLASSES.add(com.dotcms.rest.AuditPublishingResource.class);
					REST_CLASSES.add(com.dotcms.rest.WidgetResource.class);
					REST_CLASSES.add(com.dotcms.rest.CMSConfigResource.class);
					REST_CLASSES.add(com.dotcms.rest.UserResource.class);
					REST_CLASSES.add(EnvironmentResource.class);
					REST_CLASSES.add(RestExamplePortlet.class);
				}
			}
		}
		return REST_CLASSES;

	}

}