package com.dotcms.cmis;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;

import org.junit.Test;

import com.dotcms.enterprise.cmis.DotCMSUtils;
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
        
        //assertTrue( ! doQuery("SELECT * FROM cmis:document WHERE cmis:name LIKE '%a%'").getNumItems().equals(BigInteger.valueOf(0)));
        
        //assertTrue( ! doQuery("SELECT * FROM cmis:folder WHERE IN_FOLDER('" + DotCMSUtils.ROOT_ID + "')").getNumItems().equals(BigInteger.valueOf(0)));
        
    }
}
