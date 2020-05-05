package com.dotcms.publishing.listener;

import static com.dotcms.publisher.business.PublisherTestUtil.createEndpoint;
import static com.dotcms.publisher.business.PublisherTestUtil.createEnvironment;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.bundle.business.BundleAPI;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.MultiMessageResources;
import com.liferay.portal.struts.MultiMessageResourcesFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;

public class SecurityKeyResetTest extends IntegrationTestBase {

     @BeforeClass
     public static void prepare() throws Exception {
          //Setting web app environment
          IntegrationTestInitService.getInstance().init();

          //If I don't set this we'll end-up getting a ClassCasException at LanguageVariablesHandler.java:75
          when(Config.CONTEXT.getAttribute(Globals.MESSAGES_KEY))
                  .thenReturn(new MultiMessageResources( MultiMessageResourcesFactory.createFactory(),""));

          LicenseTestUtil.getLicense();
/*
          contentletAPI = APILocator.getContentletAPI();

          final User user = mock(User.class);
          when(user.getUserId()).thenReturn(ADMIN_DEFAULT_ID);
          when(user.getEmailAddress()).thenReturn(ADMIN_DEFAULT_MAIL);
          when(user.getFullName()).thenReturn(ADMIN_NAME);
          when(user.getLocale()).thenReturn(Locale.getDefault());

          final WebResource webResource = mock(WebResource.class);
          final InitDataObject dataObject = mock(InitDataObject.class);
          when(dataObject.getUser()).thenReturn(user);
          when(webResource
                  .init(anyString(), any(HttpServletRequest.class),  any(HttpServletResponse.class), anyBoolean(),
                          anyString())).thenReturn(dataObject);

          languageAPI = APILocator.getLanguageAPI();

          host = new SiteDataGen().nextPersisted();

          adminUser = TestUserUtils.getAdminUser();
*/
     }

     @Test
     public void Test_Push_Publish_After_Key_Reset_Expect_Success() throws Exception{

          final List<String> assetIds = new ArrayList<>();
          final PublisherAPI publisherAPI = PublisherAPI.getInstance();
          final BundleAPI bundleAPI = APILocator.getBundleAPI();
          Bundle bundle = null;

          final List<Contentlet> contentlets = new ArrayList<>();
          final Language lang = APILocator.getLanguageAPI().getDefaultLanguage();

               //final Contentlet contentlet = contentletDataGen.host(host).setProperty("title","lang is "+language.toString()).languageId(language.getId()).nextPersisted();
               //assetIds.add(contentlet.getIdentifier());
               //contentlets.add(contentlet);


          File file = null;
           User admin = TestUserUtils.getAdminUser();

              Environment environment = createEnvironment(admin);
              PublishingEndPoint endpoint = createEndpoint(environment);

               //Save the endpoint.
               bundle = new Bundle("testBundle", null, null, admin.getUserId());
               bundleAPI.saveBundle(bundle);

               publisherAPI.saveBundleAssets(assetIds, bundle.getId(), admin);
     }

}
