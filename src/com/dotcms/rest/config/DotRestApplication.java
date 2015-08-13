package com.dotcms.rest.config;

import com.dotcms.repackage.org.glassfish.jersey.media.multipart.MultiPartFeature;
import com.dotcms.rest.RulesEngineResourcePortlet;
import com.dotcms.rest.api.v1.sites.ruleengine.ConditionValuesResource;
import com.dotcms.rest.api.v1.sites.ruleengine.ConditionsResource;
import com.dotcms.rest.api.v1.sites.ruleengine.ActionsResource;
import com.dotcms.rest.api.v1.sites.rules.ConditionGroupsResource;
import com.dotcms.rest.api.v1.sites.rules.RulesResource;
import com.dotcms.rest.api.v1.system.ActionletsResource;
import com.dotcms.rest.api.v1.system.ConditionletsResource;
import java.util.HashSet;
import java.util.Set;

public class DotRestApplication extends com.dotcms.repackage.javax.ws.rs.core.Application {
	protected volatile static Set<Class<?>> REST_CLASSES = null;

	@Override
	public Set<Class<?>> getClasses() {
		if(REST_CLASSES == null){
			synchronized (this.getClass().getName().intern()) {
				if(REST_CLASSES == null){

					REST_CLASSES = new HashSet<>();
                    REST_CLASSES.add(MultiPartFeature.class);
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
					REST_CLASSES.add(com.dotcms.rest.OSGIResource.class);
					REST_CLASSES.add(com.dotcms.rest.UserResource.class);
					REST_CLASSES.add(com.dotcms.rest.ClusterResource.class);
					REST_CLASSES.add(com.dotcms.rest.EnvironmentResource.class);
					REST_CLASSES.add(com.dotcms.rest.NotificationResource.class);
					REST_CLASSES.add(com.dotcms.rest.IntegrityResource.class);
					REST_CLASSES.add(com.dotcms.rest.LicenseResource.class);
					REST_CLASSES.add(com.dotcms.rest.WorkflowResource.class);

                    REST_CLASSES.add(com.dotcms.rest.RestExamplePortlet.class);
					REST_CLASSES.add(com.dotcms.rest.elasticsearch.ESContentResourcePortlet.class);
					REST_CLASSES.add(RulesEngineResourcePortlet.class);

					REST_CLASSES.add(RulesResource.class);
					REST_CLASSES.add(ConditionGroupsResource.class);
					REST_CLASSES.add(ConditionsResource.class);
					REST_CLASSES.add(ConditionValuesResource.class);
					REST_CLASSES.add(ConditionletsResource.class);
					REST_CLASSES.add(ActionsResource.class);
					REST_CLASSES.add(ActionletsResource.class);

				}
			}
		}
		return REST_CLASSES;

	}

}