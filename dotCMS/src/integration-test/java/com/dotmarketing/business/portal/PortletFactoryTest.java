package com.dotmarketing.business.portal;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.datagen.PortletDataGen;
import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.PortletContext;
import com.dotcms.repackage.org.nfunk.jep.function.Dot;
import com.dotcms.rest.elasticsearch.ESContentResourcePortlet;
import com.dotcms.spring.portlet.PortletController;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.ejb.PortletManagerFactory;
import com.liferay.portal.model.Portlet;
import com.liferay.portlet.ConcretePortletWrapper;
import com.liferay.portlet.JSPPortlet;
import com.liferay.portlet.StrutsPortlet;
import com.liferay.portlet.StrutsResourceBundle;

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

    assert (content.getPortletClass().equals(StrutsPortlet.class.getName()));
    Portlet esSearch = map.get("es-search");
    assert (esSearch.getPortletClass().equals(ESContentResourcePortlet.class.getName()));
    Portlet rules = map.get("rules");
    assert (rules.getPortletClass().equals(PortletController.class.getName()));
    
    Portlet querytool = map.get("query-tool");
    assert (querytool.getPortletClass().equals(JSPPortlet.class.getName()));
    assert (querytool.getResourceBundle().equals(StrutsResourceBundle.class.getName()));
    assert (querytool.getInitParams().get("view-jsp").equals("/jsp/lucene/lucene_search.jsp"));
    assert (querytool.getInitParams().get("useWEBINFDIR").equals("true"));
    

  }

  public void test_find_portlets_in_db() throws Exception {
    PortletFactory portletFactory = PortletManagerFactory.getManager();
    String[] filePath = {"/tomcat8/webapps/ROOT/WEB-INF/portlet.xml"};
    Map<String, Portlet> map = portletFactory.xmlToPortlets(filePath);
    Portlet content = map.get("content");
    Portlet port = new PortletDataGen().extendsPortlet(content).next();
    port = portletFactory.findById(port.getPortletId());
    assert (port != null);

    Portlet port2 = new PortletDataGen().next();
    assert (portletFactory.findById(port2.getPortletId()) != null);

  }

  @Test
  public void test_portlet_cache() throws Exception {
    PortletFactory fac = new PortletFactoryImpl(new String[] {"/tomcat8/webapps/ROOT/WEB-INF/portlet.xml"});
    Collection<Portlet> allPortlets = fac.getPortlets();

    assert (allPortlets == fac.getPortlets());
    Portlet port = new PortletDataGen().nextPersisted();
    // assert we have added to cache
    assert (allPortlets != fac.getPortlets());
    assert (allPortlets.size() < fac.getPortlets().size());
    assert (fac.findById(port.getPortletId()) != null);
    // assert we are returning the same object from cache
    assert (fac.findById(port.getPortletId()) == fac.findById(port.getPortletId()));

  }

  @Test
  public void test_adding_a_custom_content_portlet() throws Exception {
    PortletFactory portletFactory = PortletManagerFactory.getManager();
    String[] xml = {"/tomcat8/webapps/ROOT/WEB-INF/portlet.xml"};
    Map<String, Portlet> map = portletFactory.xmlToPortlets(xml);

    Portlet contentPortlet = map.get("content");

    
    final String portletId="custom" + System.currentTimeMillis();

    Map<String,String> initValues=new HashMap<>();
    initValues.putAll(contentPortlet.getInitParams());
    initValues.put("baseTypes", "PERSONA");
    initValues.put("name", "Test Portlet " + System.currentTimeMillis());

    Portlet newPortlet = new DotPortlet(portletId, contentPortlet.getPortletClass(), initValues);

    APILocator.getPortletAPI().savePortlet(newPortlet);

    Portlet dbPortlet = APILocator.getPortletAPI().findPortlet(newPortlet.getPortletId());
    
    Map<String,String> params=dbPortlet.getInitParams();
    
    
    assert(params.keySet().contains("baseTypes"));
    assertEquals(params.get("baseTypes"), "PERSONA");
    assert(params.keySet().contains("name"));
    
    
    
    
  }
  
  @Test
  public void tests_portlet_to_xml_back_to_portlet_roundtrip() throws Exception {
    PortletFactory portletFactory = PortletManagerFactory.getManager();


    
    final String portletId="custom" + System.currentTimeMillis();
    final Map<String,String> initValues=ImmutableMap.of("baseTypes", "PERSONA","name", "Test Portlet " + System.currentTimeMillis(),"123", "456");
    final String portletClass=JSPPortlet.class.getName();
    
    
    final Portlet oldPortlet = new DotPortlet(portletId, portletClass, initValues);
    String xml = portletFactory.portletToXml(oldPortlet);
    
    final Portlet newPortlet=portletFactory.xmlToPortlet(xml).get();

    
    assertEquals(oldPortlet.getPortletId(), newPortlet.getPortletId());
    assertEquals(oldPortlet.getPortletClass(), newPortlet.getPortletClass());
    
    assert(oldPortlet.getInitParams().size() == newPortlet.getInitParams().size());
    for(Map.Entry<String,String> entry : initValues.entrySet()) {
      assert(newPortlet.getInitParams().containsKey(entry.getKey()));
      assert(oldPortlet.getInitParams().containsKey(entry.getKey()));
      assertEquals(newPortlet.getInitParams().get(entry.getKey()), initValues.get(entry.getKey()));
      assertEquals(oldPortlet.getInitParams().get(entry.getKey()), initValues.get(entry.getKey()));

    }
    

    
    
    
  }
  
  
  
  

}
