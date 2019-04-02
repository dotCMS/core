package com.dotmarketing.business.portal;

import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.datagen.PortletDataGen;
import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.PortletContext;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.liferay.portal.ejb.PortletManagerFactory;
import com.liferay.portal.model.Portlet;
import com.liferay.portlet.CachePortlet;
import com.liferay.portlet.JSPPortlet;

public class PortletFactoryTest {


  
  
  
  @BeforeClass
  public static void prepare() throws Exception {
    // Setting web app environment
    IntegrationTestInitService.getInstance().init();
   
  }

  @Test
  public void test_xml_to_portlet_parsing() throws Exception {
    PortletFactory portletFactory = PortletManagerFactory.getManager();
    String[] xml = {"/tomcat8/webapps/ROOT/WEB-INF/portlet.xml"};
    Map<String, Portlet> map = portletFactory.xmlToPortlets(xml);
    assert (!map.isEmpty());
    assert (map.containsKey("content"));
    Portlet content = map.get("content");
    assert (content.getPortletClass().equals(JSPPortlet.class.getName()));
  }

  
  public void test_find_portlets_in_db() throws Exception {
    PortletFactory portletFactory = PortletManagerFactory.getManager();
    String[] filePath = {"/tomcat8/webapps/ROOT/WEB-INF/portlet.xml"};
    Map<String, Portlet> map = portletFactory.xmlToPortlets(filePath);
    Portlet content = map.get("content");
    Portlet port = new PortletDataGen().extendsPortlet(content).next();
    port = portletFactory.findById(port.getPortletId());
    assert(port!=null);
    
    Portlet port2 = new PortletDataGen().next();
    assert(portletFactory.findById(port2.getPortletId())!=null);
    
  }
  
  @Test
  public void test_portlet_cache() throws Exception {
    PortletFactory fac = new PortletFactoryImpl(new String[] {"/tomcat8/webapps/ROOT/WEB-INF/portlet.xml"});
    Collection<Portlet> allPortlets = fac.getPortlets();
    
    assert(allPortlets == fac.getPortlets());
    Portlet port = new PortletDataGen().nextPersisted();
    // assert we have added to cache
    assert(allPortlets != fac.getPortlets());
    assert(allPortlets.size() < fac.getPortlets().size());
    assert(fac.findById(port.getPortletId())!=null);
    // assert we are returning the same object from cache
    assert(fac.findById(port.getPortletId()) == fac.findById(port.getPortletId()));
    
  }
  
  @Test
  public void test_portlet_xml_to_portlet_instance() throws Exception {
    PortletFactory portletFactory = PortletManagerFactory.getManager();
    String[] xml = {"/tomcat8/webapps/ROOT/WEB-INF/portlet.xml"};
    Map<String, Portlet> map = portletFactory.xmlToPortlets(xml);
    PortletContext ctx = APILocator.getPortletAPI().getPortletContext();
    
    
    for(final Portlet portlet : map.values()) {
      PortletConfig config = APILocator.getPortletAPI().getPortletConfig(portlet);
      
      // get the implementation portlet
      CachePortlet cachePort = (CachePortlet) APILocator.getPortletAPI().getImplementingInstance(portlet);
      
      // make sure we are getting the right portlet classes - StrutsPortlet, JspPortlet, etc...
      assert(cachePort.getPortletInstance().getClass().getName().equals(portlet.getPortletClass()));

      // make sure the portlets have the correct init properties
      Enumeration<String> keys = config.getInitParameterNames();
      while(keys.hasMoreElements()) {
        String key=keys.nextElement();
        assert(portlet.getInitParams().containsKey(key));
        assert(portlet.getInitParams().get(key).equals(config.getInitParameter(key)));
      }
      
    }
    
    
    
    
    
  }
  
  
  
  
  

}
