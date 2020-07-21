package com.dotcms.rest.config;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.dotcms.rest.api.v1.system.logger.LoggerResource;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
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
import com.dotcms.rest.servlet.ReloadableServletContainer;
import com.google.common.collect.ImmutableSet;

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


	/**
	 * these are system resources and should never change
	 */
	private final static Set<Class<?>> INTERNAL_CLASSES = ImmutableSet.<Class<?>>builder()
			.add(MultiPartFeature.class)
			.add(com.dotcms.rest.api.v1.index.ESIndexResource.class)
			.add(com.dotcms.rest.RoleResource.class)
			.add(com.dotcms.rest.BundleResource.class)
			.add(com.dotcms.rest.StructureResource.class)
			.add(com.dotcms.rest.ContentResource.class)
			.add(com.dotcms.rest.BundlePublisherResource.class)
			.add(com.dotcms.rest.JSPPortlet.class)
			.add(com.dotcms.rest.AuditPublishingResource.class)
			.add(com.dotcms.rest.WidgetResource.class)
			.add(com.dotcms.rest.CMSConfigResource.class)
			.add(com.dotcms.rest.OSGIResource.class)
			.add(com.dotcms.rest.UserResource.class)
			.add(com.dotcms.rest.ClusterResource.class)
			.add(com.dotcms.rest.EnvironmentResource.class)
			.add(com.dotcms.rest.api.v1.notification.NotificationResource.class)
			.add(com.dotcms.rest.IntegrityResource.class)
			.add(com.dotcms.rest.LicenseResource.class)
			.add(com.dotcms.rest.WorkflowResource.class)
			.add(com.dotcms.rest.RestExamplePortlet.class)
			.add(com.dotcms.rest.elasticsearch.ESContentResourcePortlet.class)
			.add(PersonaResource.class)
			.add(UserResource.class)
			.add(com.dotcms.rest.api.v2.user.UserResource.class)
			.add(TagResource.class)
			.add(RulesEnginePortlet.class)
			.add(RuleResource.class)
			.add(ConditionGroupResource.class)
			.add(ConditionResource.class)
			.add(ConditionValueResource.class)
			.add(PersonasResourcePortlet.class)
			.add(ConditionletsResource.class)
			.add(MonitorResource.class)
			.add(ActionResource.class)
			.add(ActionletsResource.class)
			.add(I18NResource.class)
			.add(LanguagesResource.class)
			.add(com.dotcms.rest.api.v2.languages.LanguagesResource.class)
			.add(MenuResource.class)
			.add(AuthenticationResource.class)
			.add(LogoutResource.class)
			.add(LoginFormResource.class)
			.add(ForgotPasswordResource.class)
			.add(ConfigurationResource.class)
			.add(AppContextInitResource.class)
			.add(SiteResource.class)
			.add(ContentTypeResource.class)
			.add(FieldResource.class)
			.add(com.dotcms.rest.api.v2.contenttype.FieldResource.class)
			.add(com.dotcms.rest.api.v3.contenttype.FieldResource.class)
			.add(FieldTypeResource.class)
			.add(FieldVariableResource.class)
			.add(ResetPasswordResource.class)
			.add(RoleResource.class)
			.add(CreateJsonWebTokenResource.class)
			.add(ApiTokenResource.class)
			.add(PortletResource.class)
			.add(EventsResource.class)
			.add(FolderResource.class)
			.add(BrowserTreeResource.class)
			.add(CategoriesResource.class)
			.add(PageResource.class)
			.add(ContentRelationshipsResource.class)
			.add(com.dotcms.rest.api.v1.workflow.WorkflowResource.class)
			.add(ContainerResource.class)
			.add(ThemeResource.class)
			.add(NavResource.class)
			.add(RelationshipsResource.class)
			.add(VTLResource.class)
			.add(ContentVersionResource.class)
			.add(FileAssetsResource.class)
			.add(PersonalizationResource.class)
			.add(TempFileResource.class)
			.add(UpgradeTaskResource.class)
			.add(AppsResource.class)
			.add(BrowserResource.class)
			.add(ResourceLinkResource.class)
			.add(PushPublishFilterResource.class)
			.add(LoggerResource.class)
			.build();


	/**
	 * This is the cheap way to create a concurrent set of user provided classes
	 */
	private final static Map<Class<?>, Boolean> customClasses = new ConcurrentHashMap<>();

	/**
	 * adds a class and reloads
	 * @param clazz
	 */
	public synchronized static void addClass(Class<?> clazz) {
		if(clazz==null)return;
		if(!customClasses.containsKey(clazz)) {
			customClasses.put(clazz, true);
			ReloadableServletContainer.reload(new DotRestApplication());
		}
	}

	/**
	 * removes a class and reloads
	 * @param clazz
	 */
	public synchronized static void removeClass(Class<?> clazz) {
		if(clazz==null)return;
		if(customClasses.containsKey(clazz)) {
			customClasses.remove(clazz);
			ReloadableServletContainer.reload(new DotRestApplication());
		}
	}

	@Override
	public Set<Class<?>> getClasses() {
		return ImmutableSet.<Class<?>>builder()
				.addAll(customClasses.keySet())
				.addAll(INTERNAL_CLASSES)
				.build();

	}



}
