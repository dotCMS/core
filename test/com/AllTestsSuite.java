package com;


import org.apache.velocity.runtime.parser.node.SimpleNodeTest;

import com.dotcms.cmis.DotCMSCMISTest;
import com.dotcms.content.elasticsearch.business.ESContentFactoryImplTest;
import com.dotcms.content.elasticsearch.business.ESContentletIndexAPITest;
import com.dotcms.content.elasticsearch.business.ESIndexSpeedTest;
import com.dotcms.csspreproc.CSSPreProcessServletTest;
import com.dotcms.csspreproc.LessCompilerTest;
import com.dotcms.csspreproc.SassCompilerTest;
import com.dotcms.notification.business.NotificationAPITest;
import com.dotcms.publisher.ajax.RemotePublishAjaxActionTest;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPITest;
import com.dotcms.repackage.org.junit.runner.RunWith;
import com.dotcms.repackage.org.junit.runners.Suite;
import com.dotcms.rest.ContentResourceTest;
import com.dotcms.rest.RoleResourceTest;
import com.dotcms.rest.WebResourceTest;
import com.dotmarketing.business.IdentifierAPITest;
import com.dotmarketing.business.PermissionAPITest;
import com.dotmarketing.business.RoleAPITest;
import com.dotmarketing.db.HibernateUtilTest;
import com.dotmarketing.plugin.PluginMergerTest;
import com.dotmarketing.portlets.categories.business.CategoryAPITest;
import com.dotmarketing.portlets.containers.business.ContainerAPITest;
import com.dotmarketing.portlets.contentlet.ajax.ContentletAjaxTest;
import com.dotmarketing.portlets.contentlet.business.ContentletAPITest;
import com.dotmarketing.portlets.contentlet.business.ContentletFactoryTest;
import com.dotmarketing.portlets.contentlet.business.HostAPITest;
import com.dotmarketing.portlets.folder.business.FolderAPITest;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageAPITest;
import com.dotmarketing.portlets.linkchecker.business.LinkCheckerAPITest;
import com.dotmarketing.portlets.links.business.MenuLinkAPITest;
import com.dotmarketing.portlets.structure.business.FieldAPITest;
import com.dotmarketing.portlets.structure.business.URLMapTest;
import com.dotmarketing.portlets.structure.factories.FieldFactoryTest;
import com.dotmarketing.portlets.structure.factories.StructureFactoryTest;
import com.dotmarketing.portlets.templates.business.TemplateAPITest;
import com.dotmarketing.portlets.workflows.business.WorkflowAPITest;
import com.dotmarketing.portlets.workflows.model.WorkflowSearcherTest;
import com.dotmarketing.util.ImportUtilTest;
import com.dotmarketing.sitesearch.ajax.SiteSearchAjaxActionTest;
import com.dotmarketing.webdav.WebDavTest;

/**
 * @author Jonathan Gamba.
 *         Date: 3/7/12
 */
@RunWith (Suite.class)
@Suite.SuiteClasses ({
    LinkCheckerAPITest.class, //Needs Enterprise License
    TemplateAPITest.class,
    HTMLPageAPITest.class,
    CategoryAPITest.class,
    MenuLinkAPITest.class,
    ContentletFactoryTest.class,
    ContentletAPITest.class,
    ContainerAPITest.class,
    FieldFactoryTest.class,
    StructureFactoryTest.class,
    FieldAPITest.class,
    URLMapTest.class,
    PermissionAPITest.class,
    PublishingEndPointAPITest.class,
    ESContentletIndexAPITest.class,
    DotCMSCMISTest.class,
    WebResourceTest.class,
    RoleResourceTest.class,
    ESIndexSpeedTest.class,
    PluginMergerTest.class,
    WebDavTest.class,
    ContentResourceTest.class,
    RoleAPITest.class,
    FolderAPITest.class,
    HostAPITest.class, //Needs Enterprise License
    WorkflowSearcherTest.class,
    NotificationAPITest.class,
    SassCompilerTest.class,
    LessCompilerTest.class,
    IdentifierAPITest.class,
    ImportUtilTest.class,
    SiteSearchAjaxActionTest.class,
    RemotePublishAjaxActionTest.class, //Needs Enterprise License
    CSSPreProcessServletTest.class, //Needs Enterprise License
    ESContentFactoryImplTest.class,
    HibernateUtilTest.class,
    WorkflowAPITest.class,
    ContentletAjaxTest.class,
    SimpleNodeTest.class
})
public class AllTestsSuite {

}
