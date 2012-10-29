package com.dotcms.cmis;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;

import org.junit.Test;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.UtilMethods;

public class DotCMSCMISTest extends CMISBaseTest {

    @Test
    public void testCMISReadWrite () throws Exception {    	

        //Validations
        assertTrue( UtilMethods.isSet(getdefaultHostId()) && 
        		APILocator.getHostAPI().findDefaultHost(
        				APILocator.getUserAPI().getSystemUser(), false).getInode().equals(getdefaultHostId()));
        
        String folderId = createFolder("CMISJunitTest" + new java.util.Date().getTime()); 
      //Validations
        assertNotNull( folderId );

        assertNotNull(createFile("test.txt", folderId));
        
        assertTrue( ! doQuery("select * from webPageContent WHERE title LIKE '%a%'").getNumItems().equals(BigInteger.valueOf(0)));
    }
}
