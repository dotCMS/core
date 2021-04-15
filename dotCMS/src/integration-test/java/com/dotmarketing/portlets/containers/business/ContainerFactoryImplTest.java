package com.dotmarketing.portlets.containers.business;

import static org.junit.Assert.*;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.util.SQLUtilTest;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.util.UUIDGenerator;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;

public class ContainerFactoryImplTest {


    private static HostAPI hostAPI;
    private static TemplateAPI templateAPI;
    private static User user;
    private static UserAPI userAPI;
    private static Host host;

    @BeforeClass
    public static void prepare() throws Exception {

        // Setting web app environment
        IntegrationTestInitService.getInstance().init();


        hostAPI = APILocator.getHostAPI();
        templateAPI = APILocator.getTemplateAPI();
        userAPI = APILocator.getUserAPI();
        user = userAPI.getSystemUser();

        host = hostAPI.findDefaultHost(user, false);
    }
    
    
    
    /**
     * this tests whether we properly escaping the orderby clause in the 
     * find containers method
     * @throws DotDataException
     * @throws DotSecurityException
     */
    
    @Test
    public void test_container_find_by_query_no_SQL_injection_in_orderby() throws DotDataException, DotSecurityException {

      final Host daHost = new SiteDataGen().nextPersisted();

      
      Container container =  new ContainerDataGen()
                      .site(daHost)
                      .title(UUIDGenerator.generateUuid() + SQLUtilTest.MALICIOUS_SQL_CONDITION)
                      .nextPersisted();

      assertNotNull(container);
      assertNotNull(container.getInode());
      Container containerFromDB = APILocator.getContainerAPI().find(container.getInode(), APILocator.systemUser(), false);

      
      // get normally
      List<Container> containers = FactoryLocator.getContainerFactory().findContainers(user, false, ImmutableMap.of("title",container.getTitle() ), daHost.getIdentifier(), null, null, null, 0, 100, "mod_date");
      
      assert ! containers.isEmpty() && containers.contains(containerFromDB);
       
      
      // get with a malicious SQL order by
      containers = FactoryLocator.getContainerFactory().findContainers(user, false, ImmutableMap.of("title",container.getTitle() ), daHost.getIdentifier(), null, null, null, 0, 100, SQLUtilTest.MALICIOUS_SQL_ORDER_BY);
      
      assert ! containers.isEmpty() && containers.contains(containerFromDB);
       
      
      
    }



}
