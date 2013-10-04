package com;

import com.dotcms.cmis.DotCMSCMISTest;
import com.dotcms.content.elasticsearch.business.ESContentletIndexAPITest;
import com.dotcms.content.elasticsearch.business.ESIndexSpeedTest;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPITest;
import com.dotcms.rest.RoleResourceTest;
import com.dotcms.rest.WebResourceTest;
import com.dotmarketing.business.PermissionAPITest;
import com.dotmarketing.business.RoleAPITest;
import com.dotmarketing.portlets.categories.business.CategoryAPITest;
import com.dotmarketing.portlets.containers.business.ContainerAPITest;
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
import com.dotmarketing.portlets.workflows.model.WorkflowSearcherTest;
import com.dotmarketing.plugin.PluginMergerTest;
import com.dotmarketing.webdav.WebDavTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Created by Jonathan Gamba.
 * Date: 3/7/12
 * Time: 7:55 PM
 */

@RunWith (Suite.class)
@Suite.SuiteClasses ({
    //LinkCheckerAPITest.class,
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
    RoleAPITest.class,
    FolderAPITest.class,
    HostAPITest.class,
    WorkflowSearcherTest.class
})
public class AllTestsSuite {

}
