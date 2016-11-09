package com;

import com.dotcms.cmis.DotCMSCMISTest;
import com.dotcms.concurrent.DotConcurrentFactoryTest;
import com.dotcms.content.elasticsearch.business.ESContentFactoryImplTest;
import com.dotcms.content.elasticsearch.business.ESContentletIndexAPITest;
import com.dotcms.content.elasticsearch.business.ESIndexAPITest;
import com.dotcms.content.elasticsearch.business.ESIndexSpeedTest;
import com.dotcms.csspreproc.CSSPreProcessServletTest;
import com.dotcms.csspreproc.LessCompilerTest;
import com.dotcms.csspreproc.SassCompilerTest;
import com.dotcms.notification.business.NotificationAPITest;
import com.dotcms.publisher.ajax.RemotePublishAjaxActionTest;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPITest;
import com.dotcms.publisher.endpoint.business.PublishingEndPointFactoryImplTest;
import com.dotcms.rest.ContentResourceTest;
import com.dotcms.rest.RoleResourceTest;
import com.dotcms.rest.WebResourceTest;
import com.dotcms.rest.api.v1.authentication.ResetPasswordResourceIntegrationTest;
import com.dotcms.rest.api.v1.configuration.ConfigurationResourceTest;
import com.dotcms.rest.api.v1.sites.rules.ActionResourceFTest;
import com.dotcms.rest.api.v1.sites.rules.ConditionGroupResourceFTest;
import com.dotcms.rest.api.v1.sites.rules.RuleResourceFTest;
import com.dotcms.rest.api.v1.system.i18n.I8NResourceFTest;
import com.dotcms.rest.api.v1.system.ruleengine.ActionletResourceFTest;
import com.dotcms.util.marshal.MarshalUtilsIntegrationTest;
import com.dotmarketing.business.*;
import com.dotmarketing.business.cache.provider.h22.H22CacheTest;
import com.dotmarketing.business.web.LanguageWebApiTest;
import com.dotmarketing.db.DbConnectionFactoryUtilTest;
import com.dotmarketing.db.HibernateUtilTest;
import com.dotmarketing.filters.CMSFilterTest;
import com.dotmarketing.portlets.categories.business.CategoryAPITest;
import com.dotmarketing.portlets.containers.business.ContainerAPITest;
import com.dotmarketing.portlets.contentlet.ajax.ContentletAjaxTest;
import com.dotmarketing.portlets.contentlet.business.ContentletAPITest;
import com.dotmarketing.portlets.contentlet.business.ContentletFactoryTest;
import com.dotmarketing.portlets.contentlet.business.FileAssetTest;
import com.dotmarketing.portlets.contentlet.business.HostAPITest;
import com.dotmarketing.portlets.folder.business.FolderAPITest;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageAPITest;
import com.dotmarketing.portlets.linkchecker.business.LinkCheckerAPITest;
import com.dotmarketing.portlets.links.business.MenuLinkAPITest;
import com.dotmarketing.portlets.rules.RulesUnderPageAssetsFTest;
import com.dotmarketing.portlets.rules.actionlet.PersonaActionletFTest;
import com.dotmarketing.portlets.rules.actionlet.SetResponseHeaderActionletFTest;
import com.dotmarketing.portlets.rules.actionlet.SetSessionAttributeActionletFTest;
import com.dotmarketing.portlets.rules.actionlet.VisitorsTagsActionletFTest;
import com.dotmarketing.portlets.rules.business.RulesAPIFTest;
import com.dotmarketing.portlets.rules.business.RulesCacheFTest;
import com.dotmarketing.portlets.rules.conditionlet.*;
import com.dotmarketing.portlets.structure.business.FieldAPITest;
import com.dotmarketing.portlets.structure.business.URLMapTest;
import com.dotmarketing.portlets.structure.factories.FieldFactoryTest;
import com.dotmarketing.portlets.structure.factories.StructureFactoryTest;
import com.dotmarketing.portlets.templates.business.TemplateAPITest;
import com.dotmarketing.portlets.virtuallinks.factories.VirtualLinkFactoryTest;
import com.dotmarketing.portlets.workflows.business.WorkflowAPITest;
import com.dotmarketing.portlets.workflows.model.WorkflowSearcherTest;
import com.dotmarketing.sitesearch.ajax.SiteSearchAjaxActionTest;
import com.dotmarketing.tag.business.TagAPITest;
import com.dotmarketing.util.ImportUtilTest;
import com.dotmarketing.viewtools.navigation.NavToolTest;
import com.dotmarketing.webdav.WebDavTest;
import com.liferay.portal.ejb.UserLocalManagerTest;
import com.liferay.portal.ejb.UserUtilTest;
import com.liferay.util.LocaleUtilTest;

import org.apache.velocity.runtime.parser.node.SimpleNodeTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author Jonathan Gamba.
 *         Date: 3/7/12
 */
@RunWith (Suite.class)
@Suite.SuiteClasses ({

    /**************************
     *      e2e tests         *
     **************************/
    CSSPreProcessServletTest.class, //Needs Enterprise License
    LessCompilerTest.class,
    SassCompilerTest.class,
    RemotePublishAjaxActionTest.class, //Needs Enterprise License
    ActionResourceFTest.class,
    ConditionGroupResourceFTest.class,
    RuleResourceFTest.class,
    I8NResourceFTest.class,

    //Rules:Conditionlets.
    ActionletResourceFTest.class, //Needs Enterprise License.
    CurrentSessionLanguageConditionletFTest.class, //Needs Enterprise License.
    NumberOfTimesPreviouslyVisitedConditionletFTest.class, //Needs Enterprise License.
    PagesViewedConditionletFTest.class, //Needs Enterprise License.
    UsersBrowserLanguageConditionletFTest.class, //Needs Enterprise License.
    UsersSiteVisitsConditionletFTest.class, //Needs Enterprise License.
    VisitedUrlConditionletFTest.class, //Needs Enterprise License.
    VisitorOperatingSystemConditionletFTest.class, //Needs Enterprise License.
    VisitorsCurrentUrlConditionletFTest.class, //Needs Enterprise License.

    ContentResourceTest.class, //Needs Enterprise License
    RoleResourceTest.class,
    WebResourceTest.class,
    UserAPITest.class, //Needs Enterprise License.
    PermissionAPITest.class,
    FileAssetTest.class,
    PersonaActionletFTest.class, //Needs Enterprise License.
    SetResponseHeaderActionletFTest.class, //Needs Enterprise License.
    SetSessionAttributeActionletFTest.class, //Needs Enterprise License.
    VisitorsTagsActionletFTest.class, //Needs Enterprise License.

    //Rules.
    RulesAPIFTest.class, //Needs Enterprise License.
    RulesUnderPageAssetsFTest.class, //Needs Enterprise License.

    URLMapTest.class,
    WorkflowAPITest.class,
    SiteSearchAjaxActionTest.class,
    WebDavTest.class,


    /**************************
     *    Integration tests   *
     **************************/
    DotCMSCMISTest.class,
    ESContentFactoryImplTest.class,
    ESContentletIndexAPITest.class,
    ESIndexSpeedTest.class,
    NotificationAPITest.class,
    PublishingEndPointAPITest.class,
    ResetPasswordResourceIntegrationTest.class,
    ConfigurationResourceTest.class,
    MarshalUtilsIntegrationTest.class,
    H22CacheTest.class,
    LanguageWebApiTest.class,
    IdentifierAPITest.class,
    LanguageAPITest.class,
    RoleAPITest.class,
    UserProxyFactoryTest.class,
    DbConnectionFactoryUtilTest.class,
    HibernateUtilTest.class,
    CategoryAPITest.class,
    ContainerAPITest.class,
    ContentletAjaxTest.class,
    ContentletFactoryTest.class,
    HostAPITest.class, //Needs Enterprise License
    FolderAPITest.class,
    HTMLPageAPITest.class,
    LinkCheckerAPITest.class, //Needs Enterprise License
    MenuLinkAPITest.class,

    //Rules.
    RulesCacheFTest.class, //Needs Enterprise License.

    //Rules:Conditionlets.
    ConditionletOSGIFTest.class, //Needs Enterprise License.
    VisitedUrlConditionletTest.class, //Needs Enterprise License.

    FieldAPITest.class,
    FieldFactoryTest.class,
    StructureFactoryTest.class,
    TemplateAPITest.class,
    WorkflowSearcherTest.class,
    TagAPITest.class,
    ImportUtilTest.class,
    UserLocalManagerTest.class,
    UserUtilTest.class,
    LocaleUtilTest.class,
    SimpleNodeTest.class,

    ESIndexAPITest.class,
    DotConcurrentFactoryTest.class,
    PublishingEndPointFactoryImplTest.class,
    CMSFilterTest.class,
    ContentletAPITest.class,
    VirtualLinkFactoryTest.class,
    NavToolTest.class
})
public class AllTestsSuite {}
