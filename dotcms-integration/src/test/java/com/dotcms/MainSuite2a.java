package com.dotcms;

import com.dotcms.analytics.ContentAnalyticsPersistenceModeIT;
import com.dotcms.business.interceptor.InterceptorHandlerTest;
import com.dotcms.enterprise.publishing.remote.handler.FolderHandlerTest;
import com.dotcms.inference.rest.ChatCompletionsStreamingTest;
import com.dotcms.inference.rest.ChatCompletionsTest;
import com.dotcms.inference.rest.InferenceAuthorizationTest;
import com.dotcms.inference.rest.InferenceClientConformanceTest;
import com.dotcms.inference.rest.InferenceEmbeddingsTest;
import com.dotcms.inference.rest.InferenceFallbackTest;
import com.dotcms.inference.rest.InferenceImagesTest;
import com.dotcms.inference.rest.InferenceLoggingTest;
import com.dotcms.inference.rest.InferenceModelValidationTest;
import com.dotcms.inference.rest.InferenceModelsTest;
import com.dotcms.inference.rest.InferenceSiteIsolationTest;
import com.dotcms.inference.rest.InferenceSiteResolutionTest;
import com.dotcms.inference.rest.InferenceTestsAreRegisteredTest;
import com.dotcms.junit.MainBaseSuite;
import com.dotcms.rest.api.v1.maintenance.MaintenanceResourceIntegrationTest;
import com.dotmarketing.portlets.containers.business.ContainerStructureFinderStrategyResolverTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/* grep -l -r "@Test" dotCMS/src/integration-test */
/* ./gradlew integrationTest -Dtest.single=com.dotcms.MainSuite */

/**
 * NOTE: LET'S AVOID ADDING MORE TESTS TO THIS SUITE, THIS ONE IS TAKING ALMOST TWICE THE TIME TO RUN THAN THE OTHERS
 */
@RunWith(MainBaseSuite.class)
@SuiteClasses({
        ChatCompletionsTest.class,
        ChatCompletionsStreamingTest.class,
        InferenceAuthorizationTest.class,
        InferenceClientConformanceTest.class,
        InferenceEmbeddingsTest.class,
        InferenceFallbackTest.class,
        InferenceImagesTest.class,
        InferenceLoggingTest.class,
        InferenceModelsTest.class,
        InferenceModelValidationTest.class,
        InferenceSiteIsolationTest.class,
        InferenceTestsAreRegisteredTest.class,
        InferenceSiteResolutionTest.class,
        // Data-scanning tests run FIRST on purpose.
        // Integration tests accumulate content and never clean up, so anything
        // that walks the whole dataset (findAllContent) costs O(all content
        // created so far). Scheduled late these pay for every preceding test's
        // leftovers. Keep new full-scan tests in this block.
        com.dotmarketing.factories.MultiTreeAPITest.class,

        com.dotcms.rest.api.v1.workflow.WorkflowResourceResponseCodeIntegrationTest.class,
        com.dotcms.rest.api.v1.workflow.WorkflowResourceIntegrationTest.class,
        com.dotcms.rest.api.v1.workflow.WorkflowResourceLockUnlockIntegrationTest.class,
        com.dotcms.rest.api.v1.workflow.WorkflowResourceLicenseIntegrationTest.class,
        com.dotcms.rest.api.v1.authentication.ResetPasswordResourceIntegrationTest.class,
        com.dotcms.rest.api.v1.authentication.CreateJsonWebTokenResourceIntegrationTest.class,
        com.dotcms.rest.api.v1.relationships.RelationshipsResourceTest.class,
        com.dotcms.rest.api.v1.contenttype.ContentTypeResourceUpdateMetadataTest.class,
        com.dotcms.rest.api.v2.contenttype.FieldResourceTest.class,
        com.dotcms.rest.api.v3.contenttype.FieldResourceTest.class,
        com.dotcms.rest.api.v3.contenttype.MoveFieldFormTest.class,
        com.dotcms.rest.api.CorsFilterTest.class,
        com.dotcms.rest.elasticsearch.ESContentResourcePortletTest.class,
        com.dotcms.filters.VanityUrlFilterTest.class,
        com.dotcms.vanityurl.business.VanityUrlAPITest.class,
        com.dotmarketing.portlets.fileassets.business.FileAssetAPITest.class,
        com.dotmarketing.portlets.languagesmanager.business.LanguageAPITest.class,
        com.dotmarketing.portlets.languagesmanager.business.LanguageFactoryIntegrationTest.class,
        com.dotmarketing.portlets.linkchecker.business.LinkCheckerAPITest.class,
        com.dotmarketing.portlets.contentlet.util.ContentletUtilTest.class,
        com.dotmarketing.portlets.contentlet.business.ContentletCheckInTest.class,
        com.dotmarketing.portlets.contentlet.business.ContentletFactoryTest.class,
        ContainerStructureFinderStrategyResolverTest.class,
        com.dotmarketing.portlets.contentlet.business.ContentletAPITest.class,
        com.dotmarketing.portlets.contentlet.model.ContentletIntegrationTest.class,
        com.dotmarketing.portlets.contentlet.transform.BinaryToMapTransformerTest.class,
        com.dotmarketing.portlets.contentlet.transform.ContentletTransformerTest.class,
        com.dotmarketing.portlets.contentlet.transform.WidgetViewStrategyTest.class,
        com.dotmarketing.portlets.contentlet.ajax.ContentletAjaxTest.class,
        com.dotmarketing.portlets.workflows.business.SaveContentDraftActionletTest.class,
        com.dotmarketing.portlets.workflows.business.WorkflowFactoryTest.class,
        com.dotmarketing.portlets.workflows.business.SaveContentActionletTest.class,
        com.dotmarketing.portlets.workflows.business.WorkflowAPIMultiLanguageTest.class,
        com.dotmarketing.portlets.workflows.business.WorkflowAPITest.class,
        com.dotmarketing.portlets.workflows.model.WorkflowSearcherTest.class,
        com.dotmarketing.portlets.workflows.model.SystemActionWorkflowActionMappingTest.class,
        com.dotmarketing.portlets.workflows.actionlet.FourEyeApproverActionletTest.class,
        com.dotmarketing.portlets.workflows.actionlet.SaveContentActionletTest.class,
        com.dotmarketing.portlets.workflows.actionlet.SaveContentActionletWithTagsTest.class,
        com.dotmarketing.portlets.workflows.actionlet.CopyActionletTest.class,
        com.dotmarketing.portlets.workflows.actionlet.VelocityScriptActionletTest.class,
        com.dotmarketing.portlets.personas.business.PersonaAPITest.class,
        com.dotmarketing.portlets.personas.business.DeleteMultiTreeUsedPersonaTagJobTest.class,
        com.dotmarketing.portlets.links.business.MenuLinkAPITest.class,
        com.dotmarketing.portlets.links.factories.LinkFactoryTest.class,
        com.dotmarketing.portlets.categories.business.CategoryAPITest.class,
        com.dotmarketing.filters.FiltersTest.class,
        InterceptorHandlerTest.class,
        com.dotcms.graphql.datafetcher.page.NumberContentsDataFetcherTest.class,
        com.dotcms.rest.AuditPublishingResourceTest.class,
        MaintenanceResourceIntegrationTest.class,
        FolderHandlerTest.class,
        ContentAnalyticsPersistenceModeIT.class
})
public class MainSuite2a {

}
