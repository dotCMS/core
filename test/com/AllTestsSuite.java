package com;

import com.dotmarketing.business.PermissionAPITest;
import com.dotmarketing.portlets.contentlet.business.ContentletAPITest;
import com.dotmarketing.portlets.contentlet.business.ContentletFactoryTest;
import com.dotmarketing.portlets.structure.factories.FieldFactoryTest;
import com.dotmarketing.portlets.structure.factories.StructureFactoryTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

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
        PermissionAPITest.class
} )
public class AllTestsSuite {

}