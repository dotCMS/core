package com;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.dotcms.cmis.DotCMSCMISTest;
import com.dotmarketing.business.PermissionAPITest;
import com.dotmarketing.portlets.containers.business.ContainerAPITest;
import com.dotmarketing.portlets.contentlet.business.ContentletAPITest;
import com.dotmarketing.portlets.contentlet.business.ContentletFactoryTest;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageAPITest;
import com.dotmarketing.portlets.linkchecker.business.LinkCheckerAPITest;
import com.dotmarketing.portlets.structure.factories.FieldFactoryTest;
import com.dotmarketing.portlets.structure.factories.StructureFactoryTest;
import com.dotmarketing.portlets.templates.business.TemplateAPITest;
import com.dotcms.publisher.endpoint.business.*;

/**
 * Created by Jonathan Gamba.
 * Date: 3/7/12
 * Time: 7:55 PM
 */
@RunWith ( Suite.class )
@Suite.SuiteClasses ( {
        FieldFactoryTest.class,
        StructureFactoryTest.class,
        ContentletFactoryTest.class,
        ContentletAPITest.class,
        PermissionAPITest.class,
        ContainerAPITest.class,
        DotCMSCMISTest.class,
        //LinkCheckerAPITest.class,
        TemplateAPITest.class,
        HTMLPageAPITest.class,
        PublishingEndPointAPITest.class

//        CMISBaseTest.class
} )
public class AllTestsSuite {

}
