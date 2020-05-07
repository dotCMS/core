package com.dotcms.rest.config;

import com.dotcms.contenttype.model.field.FieldTypeResource;
import com.dotcms.rest.RulesEnginePortlet;
import com.dotcms.rest.TagResource;
import com.dotcms.rest.api.v1.apps.AppsResource;
import com.dotcms.rest.api.v1.authentication.ApiTokenResource;
import com.dotcms.rest.api.v1.authentication.AuthenticationResource;
import com.dotcms.rest.api.v1.authentication.CreateJsonWebTokenResource;
import com.dotcms.rest.api.v1.authentication.ForgotPasswordResource;
import com.dotcms.rest.api.v1.authentication.LoginFormResource;
import com.dotcms.rest.api.v1.authentication.LogoutResource;
import com.dotcms.rest.api.v1.authentication.ResetPasswordResource;
import com.dotcms.rest.api.v1.browser.BrowserResource;
import com.dotcms.rest.api.v1.browsertree.BrowserTreeResource;
import com.dotcms.rest.api.v1.categories.CategoriesResource;
import com.dotcms.rest.api.v1.container.ContainerResource;
import com.dotcms.rest.api.v1.content.ContentRelationshipsResource;
import com.dotcms.rest.api.v1.content.ContentVersionResource;
import com.dotcms.rest.api.v1.content.ResourceLinkResource;
import com.dotcms.rest.api.v1.contenttype.ContentTypeResource;
import com.dotcms.rest.api.v1.contenttype.FieldResource;
import com.dotcms.rest.api.v1.contenttype.FieldVariableResource;
import com.dotcms.rest.api.v1.event.EventsResource;
import com.dotcms.rest.api.v1.fileasset.FileAssetsResource;
import com.dotcms.rest.api.v1.folder.FolderResource;
import com.dotcms.rest.api.v1.languages.LanguagesResource;
import com.dotcms.rest.api.v1.menu.MenuResource;
import com.dotcms.rest.api.v1.page.NavResource;
import com.dotcms.rest.api.v1.page.PageResource;
import com.dotcms.rest.api.v1.personalization.PersonalizationResource;
import com.dotcms.rest.api.v1.personas.PersonaResource;
import com.dotcms.rest.api.v1.portlet.PortletResource;
import com.dotcms.rest.api.v1.pushpublish.PushPublishFilterResource;
import com.dotcms.rest.api.v1.relationships.RelationshipsResource;
import com.dotcms.rest.api.v1.site.SiteResource;
import com.dotcms.rest.api.v1.sites.ruleengine.rules.RuleResource;
import com.dotcms.rest.api.v1.sites.ruleengine.rules.actions.ActionResource;
import com.dotcms.rest.api.v1.sites.ruleengine.rules.conditions.ConditionGroupResource;
import com.dotcms.rest.api.v1.sites.ruleengine.rules.conditions.ConditionResource;
import com.dotcms.rest.api.v1.sites.ruleengine.rules.conditions.ConditionValueResource;
import com.dotcms.rest.api.v1.system.AppContextInitResource;
import com.dotcms.rest.api.v1.system.ConfigurationResource;
import com.dotcms.rest.api.v1.system.UpgradeTaskResource;
import com.dotcms.rest.api.v1.system.i18n.I18NResource;
import com.dotcms.rest.api.v1.system.monitor.MonitorResource;
import com.dotcms.rest.api.v1.system.role.RoleResource;
import com.dotcms.rest.api.v1.system.ruleengine.actionlets.ActionletsResource;
import com.dotcms.rest.api.v1.system.ruleengine.conditionlets.ConditionletsResource;
import com.dotcms.rest.api.v1.temp.TempFileResource;
import com.dotcms.rest.api.v1.theme.ThemeResource;
import com.dotcms.rest.api.v1.user.UserResource;
import com.dotcms.rest.api.v1.vtl.VTLResource;
import com.dotcms.rest.personas.PersonasResourcePortlet;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import java.util.HashSet;
import java.util.Set;

/**
 * This class provides the list of all the REST end-points in dotCMS. Every new
 * service needs to be added to this list in order to be available for use.
 *
 * @author Will Ezell
 * @version 2.5.3
 * @since Dec 5, 2013
 *
 */
public class DotRestApplication extends javax.ws.rs.core.Application {

	protected volatile static Set<Class<?>> REST_CLASSES = null;

	@Override
	public Set<Class<?>> getClasses() {
		if(REST_CLASSES == null){
			synchronized (this.getClass().getName().intern()) {
				if(REST_CLASSES == null){
					REST_CLASSES = new HashSet<>();
					REST_CLASSES.add(MultiPartFeature.class);
					REST_CLASSES.add(com.dotcms.rest.api.v1.index.ESIndexResource.class);
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
					REST_CLASSES.add(com.dotcms.rest.api.v1.notification.NotificationResource.class);
					REST_CLASSES.add(com.dotcms.rest.IntegrityResource.class);
					REST_CLASSES.add(com.dotcms.rest.LicenseResource.class);
					REST_CLASSES.add(com.dotcms.rest.WorkflowResource.class);

					REST_CLASSES.add(com.dotcms.rest.RestExamplePortlet.class);
					REST_CLASSES.add(com.dotcms.rest.elasticsearch.ESContentResourcePortlet.class);

					REST_CLASSES.add(PersonaResource.class);
					REST_CLASSES.add(UserResource.class);
					REST_CLASSES.add(com.dotcms.rest.api.v2.user.UserResource.class);
					REST_CLASSES.add(TagResource.class);

					REST_CLASSES.add(RulesEnginePortlet.class);
					REST_CLASSES.add(RuleResource.class);
					REST_CLASSES.add(ConditionGroupResource.class);
					REST_CLASSES.add(ConditionResource.class);
					REST_CLASSES.add(ConditionValueResource.class);
					REST_CLASSES.add(PersonasResourcePortlet.class);

					REST_CLASSES.add(ConditionletsResource.class);
					REST_CLASSES.add(MonitorResource.class);
					REST_CLASSES.add(ActionResource.class);
					REST_CLASSES.add(ActionletsResource.class);
					REST_CLASSES.add(I18NResource.class);
					REST_CLASSES.add(LanguagesResource.class);
					REST_CLASSES.add(com.dotcms.rest.api.v2.languages.LanguagesResource.class);

					REST_CLASSES.add(MenuResource.class);

					REST_CLASSES.add(AuthenticationResource.class);
					REST_CLASSES.add(LogoutResource.class);
					REST_CLASSES.add(LoginFormResource.class);
					REST_CLASSES.add(ForgotPasswordResource.class);
					REST_CLASSES.add(ConfigurationResource.class);
					REST_CLASSES.add(AppContextInitResource.class);
					REST_CLASSES.add(SiteResource.class);
					REST_CLASSES.add(ContentTypeResource.class);
					REST_CLASSES.add(FieldResource.class);
					REST_CLASSES.add(com.dotcms.rest.api.v2.contenttype.FieldResource.class);
					REST_CLASSES.add(com.dotcms.rest.api.v3.contenttype.FieldResource.class);
					REST_CLASSES.add(FieldTypeResource.class);
					REST_CLASSES.add(FieldVariableResource.class);
					REST_CLASSES.add(ResetPasswordResource.class);
					REST_CLASSES.add(RoleResource.class);
					REST_CLASSES.add(CreateJsonWebTokenResource.class);
					REST_CLASSES.add(ApiTokenResource.class);
					REST_CLASSES.add(PortletResource.class);
					REST_CLASSES.add(EventsResource.class);
					REST_CLASSES.add(FolderResource.class);

					REST_CLASSES.add(BrowserTreeResource.class);

					REST_CLASSES.add(CategoriesResource.class);
					REST_CLASSES.add(PageResource.class);
					REST_CLASSES.add(ContentRelationshipsResource.class);

					REST_CLASSES.add(com.dotcms.rest.api.v1.workflow.WorkflowResource.class);
					REST_CLASSES.add(ContainerResource.class);

					REST_CLASSES.add(ThemeResource.class);
					REST_CLASSES.add(NavResource.class);
					REST_CLASSES.add(RelationshipsResource.class);

					REST_CLASSES.add(VTLResource.class);
					REST_CLASSES.add(ContentVersionResource.class);
					REST_CLASSES.add(FileAssetsResource.class);

					REST_CLASSES.add(PersonalizationResource.class);
					REST_CLASSES.add(TempFileResource.class);

					REST_CLASSES.add(UpgradeTaskResource.class);

					REST_CLASSES.add(AppsResource.class);

					REST_CLASSES.add(PushPublishFilterResource.class);
					REST_CLASSES.add(BrowserResource.class);

					REST_CLASSES.add(ResourceLinkResource.class);
				}
			}
		}
		return REST_CLASSES;
	}

}