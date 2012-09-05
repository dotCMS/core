package com.dotmarketing.portlets.containers.business;

import static org.junit.Assert.assertTrue;

import org.apache.commons.beanutils.BeanUtils;
import org.junit.Test;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.portlets.ContentletBaseTest;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class ContainerAPITest extends ContentletBaseTest {
    @Test
    public void save() throws Exception {
        Container c = new Container();
        c.setCode("this is the code");
        c.setFriendlyName("test container");
        c.setTitle("his is the title");
        c.setMaxContentlets(5);
        c.setPreLoop("preloop code");
        c.setPostLoop("postloop code");
        
        Container cc = new Container();
        BeanUtils.copyProperties(cc, c);
        
        Structure st=StructureCache.getStructureByVelocityVarName("host");
        
        User user = APILocator.getUserAPI().getSystemUser();
        Host host = APILocator.getHostAPI().findDefaultHost(user, false);
        
        cc = APILocator.getContainerAPI().save(cc, st, host, user, false);
        
        assertTrue(UtilMethods.isSet(cc.getInode()));
        assertTrue(UtilMethods.isSet(cc.getIdentifier()));
        
        cc = APILocator.getContainerAPI().getWorkingContainerById(cc.getIdentifier(), user, false); 
        
        assertTrue(UtilMethods.isSet(cc.getInode()));
        assertTrue(UtilMethods.isSet(cc.getIdentifier()));
        
        assertTrue(cc.getCode().equals(c.getCode()));
        assertTrue(cc.getFriendlyName().equals(c.getFriendlyName()));
        assertTrue(cc.getTitle().equals(c.getTitle()));
        assertTrue(cc.getMaxContentlets()==c.getMaxContentlets());
        assertTrue(cc.getPreLoop().equals(c.getPreLoop()));
        assertTrue(cc.getPostLoop().equals(c.getPostLoop()));
    }
}
