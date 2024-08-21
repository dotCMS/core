package com.dotcms.rest.config;

import com.dotcms.ai.rest.CompletionsResource;
import com.dotcms.ai.rest.EmbeddingsResource;
import com.dotcms.ai.rest.ImageResource;
import com.dotcms.ai.rest.SearchResource;
import com.dotcms.ai.rest.TextResource;
import com.dotcms.contenttype.model.field.FieldTypeResource;
import com.dotcms.rendering.js.JsResource;
import com.dotcms.rest.AuditPublishingResource;
import com.dotcms.rest.BundlePublisherResource;
import com.dotcms.rest.BundleResource;
import com.dotcms.rest.CMSConfigResource;
import com.dotcms.rest.ClusterResource;
import com.dotcms.rest.EnvironmentResource;
import com.dotcms.rest.IntegrityResource;
import com.dotcms.rest.JSPPortlet;
import com.dotcms.rest.LicenseResource;
import com.dotcms.rest.OSGIResource;
import com.dotcms.rest.PublishQueueResource;
import com.dotcms.rest.RestExamplePortlet;
import com.dotcms.rest.RulesEnginePortlet;
import com.dotcms.rest.StructureResource;
import com.dotcms.rest.TagResource;
import com.dotcms.rest.WidgetResource;
import com.dotcms.rest.annotation.HeaderFilter;
import com.dotcms.rest.annotation.RequestFilter;
import com.dotcms.rest.api.CorsFilter;
import com.dotcms.rest.api.MyObjectMapperProvider;
import com.dotcms.rest.api.v1.announcements.AnnouncementsResource;
import com.dotcms.rest.api.v1.apps.AppsResource;
import com.dotcms.rest.api.v1.asset.WebAssetResource;
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
import com.dotcms.rest.api.v1.content.ContentReportResource;
import com.dotcms.rest.api.v1.content.ContentResource;
import com.dotcms.rest.api.v1.content.ContentVersionResource;
import com.dotcms.rest.api.v1.content.ResourceLinkResource;
import com.dotcms.rest.api.v1.contenttype.ContentTypeResource;
import com.dotcms.rest.api.v1.contenttype.FieldResource;
import com.dotcms.rest.api.v1.contenttype.FieldVariableResource;
import com.dotcms.rest.api.v1.ema.EMAResource;
import com.dotcms.rest.api.v1.event.EventsResource;
import com.dotcms.rest.api.v1.experiments.ExperimentsResource;
import com.dotcms.rest.api.v1.fileasset.FileAssetsResource;
import com.dotcms.rest.api.v1.folder.FolderResource;
import com.dotcms.rest.api.v1.form.FormResource;
import com.dotcms.rest.api.v1.index.ESIndexResource;
import com.dotcms.rest.api.v1.languages.LanguagesResource;
import com.dotcms.rest.api.v1.maintenance.JVMInfoResource;
import com.dotcms.rest.api.v1.maintenance.MaintenanceResource;
import com.dotcms.rest.api.v1.menu.MenuResource;
import com.dotcms.rest.api.v1.notification.NotificationResource;
import com.dotcms.rest.api.v1.page.NavResource;
import com.dotcms.rest.api.v1.page.PageResource;
import com.dotcms.rest.api.v1.personalization.PersonalizationResource;
import com.dotcms.rest.api.v1.personas.PersonaResource;
import com.dotcms.rest.api.v1.portlet.PortletResource;
import com.dotcms.rest.api.v1.portlet.ToolGroupResource;
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
import com.dotcms.rest.api.v1.system.SystemTableResource;
import com.dotcms.rest.api.v1.system.UpgradeTaskResource;
import com.dotcms.rest.api.v1.system.cache.CacheResource;
import com.dotcms.rest.api.v1.system.i18n.I18NResource;
import com.dotcms.rest.api.v1.system.logger.LoggerResource;
import com.dotcms.rest.api.v1.system.monitor.MonitorResource;
import com.dotcms.rest.api.v1.system.permission.PermissionResource;
import com.dotcms.rest.api.v1.system.role.RoleResource;
import com.dotcms.rest.api.v1.system.ruleengine.actionlets.ActionletsResource;
import com.dotcms.rest.api.v1.system.ruleengine.conditionlets.ConditionletsResource;
import com.dotcms.rest.api.v1.system.storage.StorageResource;
import com.dotcms.rest.api.v1.taillog.TailLogResource;
import com.dotcms.rest.api.v1.temp.TempFileResource;
import com.dotcms.rest.api.v1.template.TemplateResource;
import com.dotcms.rest.api.v1.theme.ThemeResource;
import com.dotcms.rest.api.v1.user.UserResource;
import com.dotcms.rest.api.v1.variants.VariantResource;
import com.dotcms.rest.api.v1.versionable.VersionableResource;
import com.dotcms.rest.api.v1.vtl.VTLResource;
import com.dotcms.rest.api.v1.workflow.WorkflowResource;
import com.dotcms.rest.elasticsearch.ESContentResourcePortlet;
import com.dotcms.rest.exception.mapper.DefaultDotBadRequestExceptionMapper;
import com.dotcms.rest.exception.mapper.DoesNotExistExceptionMapper;
import com.dotcms.rest.exception.mapper.DotBadRequestExceptionMapper;
import com.dotcms.rest.exception.mapper.DotDataExceptionMapper;
import com.dotcms.rest.exception.mapper.DotSecurityExceptionMapper;
import com.dotcms.rest.exception.mapper.ElasticsearchStatusExceptionMapper;
import com.dotcms.rest.exception.mapper.HttpStatusCodeExceptionMapper;
import com.dotcms.rest.exception.mapper.InvalidFormatExceptionMapper;
import com.dotcms.rest.exception.mapper.InvalidLicenseExceptionMapper;
import com.dotcms.rest.exception.mapper.JsonMappingExceptionMapper;
import com.dotcms.rest.exception.mapper.JsonParseExceptionMapper;
import com.dotcms.rest.exception.mapper.NotAllowedExceptionMapper;
import com.dotcms.rest.exception.mapper.NotFoundInDbExceptionMapper;
import com.dotcms.rest.exception.mapper.ParamExceptionMapper;
import com.dotcms.rest.exception.mapper.ResourceNotFoundExceptionMapper;
import com.dotcms.rest.exception.mapper.RuntimeExceptionMapper;
import com.dotcms.rest.exception.mapper.UnrecognizedPropertyExceptionMapper;
import com.dotcms.rest.exception.mapper.WorkflowPortletAccessExceptionMapper;
import com.dotcms.rest.personas.PersonasResourcePortlet;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.portlets.folders.exception.InvalidFolderNameException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.google.common.collect.ImmutableSet;
import io.swagger.v3.jaxrs2.integration.resources.AcceptHeaderOpenApiResource;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.ws.rs.ApplicationPath;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * This class provides the list of all the REST end-points in dotCMS. Every new
 * service needs to be added to this list in order to be available for use.
 *
 * @author Will Ezell
 * @version 2.5.3
 * @since Dec 5, 2013
 *
 */

@OpenAPIDefinition(
		info = @Info(
				title = "dotCMS REST API",
				version = "3"),
		servers = @Server(
						description = "dotCMS Server",
						url = "/api"),
		tags = {
				@Tag(name = "Workflow"),
				@Tag(name = "Page"),
				@Tag(name = "Content Type"),
				@Tag(name = "Content Delivery"),
				@Tag(name = "Bundle"),
				@Tag(name = "Navigation"),
				@Tag(name = "Experiment"),
				@Tag(name = "Content Report")
		}
)

@ApplicationPath("/api")
public class DotRestApplication extends ResourceConfig {

	public DotRestApplication() {
		registerClasses(INTERNAL_CLASSES).
		registerClasses(PROVIDER_CLASSES).
		registerClasses(customClasses.keySet());
		property("jersey.config.server.tracing", "ALL");
	}

	/**
	 * these are system resources and should never change
	 */
	private static final Set<Class<?>> INTERNAL_CLASSES = ImmutableSet.<Class<?>>builder()
			.add(MultiPartFeature.class)
			.add(ESIndexResource.class)
			.add(com.dotcms.rest.RoleResource.class)
			.add(BundleResource.class)
			.add(StructureResource.class)
			.add(com.dotcms.rest.ContentResource.class)
			.add(BundlePublisherResource.class)
			.add(JSPPortlet.class)
			.add(AuditPublishingResource.class)
			.add(WidgetResource.class)
			.add(CMSConfigResource.class)
			.add(OSGIResource.class)
			.add(com.dotcms.rest.api.v1.osgi.OSGIResource.class)
			.add(com.dotcms.rest.UserResource.class)
			.add(ClusterResource.class)
			.add(EnvironmentResource.class)
			.add(NotificationResource.class)
			.add(IntegrityResource.class)
			.add(LicenseResource.class)
			.add(RestExamplePortlet.class)
			.add(ESContentResourcePortlet.class)
			.add(PersonaResource.class)
			.add(UserResource.class)
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
			.add(WorkflowResource.class)
			.add(ContainerResource.class)
			.add(ThemeResource.class)
			.add(NavResource.class)
			.add(RelationshipsResource.class)
			.add(VTLResource.class)
			.add(JsResource.class)
			.add(ContentVersionResource.class)
			.add(FileAssetsResource.class)
			.add(PersonalizationResource.class)
			.add(TempFileResource.class)
			.add(UpgradeTaskResource.class)
			.add(AppsResource.class)
			.add(EMAResource.class)
			.add(BrowserResource.class)
			.add(ResourceLinkResource.class)
			.add(PushPublishFilterResource.class)
			.add(LoggerResource.class)
			.add(TemplateResource.class)
			.add(MaintenanceResource.class)
			.add(PublishQueueResource.class)
			.add(ToolGroupResource.class)
			.add(VersionableResource.class)
			.add(PermissionResource.class)
			.add(ContentResource.class)
			.add(CacheResource.class)
			.add(JVMInfoResource.class)
			.add(FormResource.class)
			.add(OpenApiResource.class)
			.add(AcceptHeaderOpenApiResource.class)
			.add(ExperimentsResource.class)
			.add(TailLogResource.class)
			.add(VariantResource.class)
			.add(WebAssetResource.class)
			.add(SystemTableResource.class)
			.add(StorageResource.class)
			.add(com.dotcms.rest.api.v2.tags.TagResource.class)
			.add(AnnouncementsResource.class)
			.add(CompletionsResource.class)
			.add(EmbeddingsResource.class)
			.add(ImageResource.class)
			.add(SearchResource.class)
			.add(TextResource.class)
			.add(ContentReportResource.class)
			.build();

	private static final Set<Class<?>> PROVIDER_CLASSES = ImmutableSet.<Class<?>>builder()
			.add(ContainerReloader.class)
			.add(RequestFilter.class)
			.add(HeaderFilter.class)
			.add(CorsFilter.class)
			.add(MyObjectMapperProvider.class)
			.add(JacksonJaxbJsonProvider.class)
			.add(HttpStatusCodeExceptionMapper.class)
			.add(ResourceNotFoundExceptionMapper.class)
			.add(InvalidFormatExceptionMapper.class)
			.add(JsonParseExceptionMapper.class)
			.add(ParamExceptionMapper.class)
			.add(JsonMappingExceptionMapper.class)
			.add(UnrecognizedPropertyExceptionMapper.class)
			.add(InvalidLicenseExceptionMapper.class)
			.add(WorkflowPortletAccessExceptionMapper.class)
			.add(NotFoundInDbExceptionMapper.class)
			.add(DoesNotExistExceptionMapper.class)
			.add((new DotBadRequestExceptionMapper<AlreadyExistException>(){}).getClass())
			.add((new DotBadRequestExceptionMapper<IllegalArgumentException>(){}).getClass())
			.add((new DotBadRequestExceptionMapper<DotStateException>(){}).getClass())
			.add(DefaultDotBadRequestExceptionMapper.class)
			.add((new DotBadRequestExceptionMapper<JsonProcessingException>(){}).getClass())
			.add((new DotBadRequestExceptionMapper<NumberFormatException>(){}).getClass())
			.add(DotSecurityExceptionMapper.class)
			.add(DotDataExceptionMapper.class)
			.add(ElasticsearchStatusExceptionMapper.class)
			.add((new DotBadRequestExceptionMapper<InvalidFolderNameException>(){}).getClass())
			.add(NotAllowedExceptionMapper.class)
			.add(RuntimeExceptionMapper.class)
			.build();

	/**
	 * This is the cheap way to create a concurrent set of user provided classes
	 */
	private static final Map<Class<?>, Boolean> customClasses = new ConcurrentHashMap<>();

	/**
	 * adds a class and reloads
	 * @param clazz the class ot add
	 */
	public static synchronized void addClass(Class<?> clazz) {
		if(clazz==null){
			return;
		}
		if (Boolean.TRUE.equals(customClasses.computeIfAbsent(clazz,c -> true))) {
			ContainerReloader.getInstance().reload();
		}
	}

	/**
	 * removes a class and reloads
	 * @param clazz
	 */
	public static synchronized void removeClass(Class<?> clazz) {
		if(clazz==null)return;
		if(customClasses.remove(clazz) != null){
			ContainerReloader.getInstance().reload();
		}
	}

}
