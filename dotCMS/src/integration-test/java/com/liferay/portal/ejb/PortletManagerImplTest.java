package com.liferay.portal.ejb;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.util.IntegrationTestInitService;
import com.liferay.portal.model.Portlet;

public class PortletManagerImplTest {

  @BeforeClass
  public static void prepare() throws Exception{
    //Setting web app environment
        IntegrationTestInitService.getInstance().init();
  }
  
  
  
  
  @Test
  public void test_xml_to_portlet_parsing() throws Exception{
    
    String[] xml = {"/tomcat8/webapps/ROOT/WEB-INF/portlet.xml"};
    final PortletManager portletManager = PortletManagerFactory.getManager();
    
    Map<String, Portlet> map  = portletManager.addPortlets(xml);
    assert(!map.isEmpty());
    assert(map.containsKey("content"));
    
    
    
    
    
  }

}
