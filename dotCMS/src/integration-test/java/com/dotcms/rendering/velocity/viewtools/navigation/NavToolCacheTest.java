package com.dotcms.rendering.velocity.viewtools.navigation;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ChainedContext;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.LinkDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;

public class NavToolCacheTest extends IntegrationTestBase {

  private static Folder folder;
  private static User user;
  private static Host site;

  @BeforeClass
  public static void prepare() throws Exception {
    // Setting web app environment
    IntegrationTestInitService.getInstance().init();

    user = APILocator.getUserAPI().getSystemUser();
    site = new SiteDataGen().nextPersisted();
    System.out.println("");
    System.out.println("navcache test site: " + site.getHostname());
    System.out.println("");
  }

  
  /**
   * @throws Exception
   * issue: https://github.com/dotCMS/core/issues/16951
   * This test makes sure using the NavTool returns hydrated results, but only
   * stored the unhydrated results in cache.
   */

  @Test
  public void test_navtool_cache_does_not_return_hydrated_navresults() throws Exception {
    createData();

    HttpServletRequest request =
        new MockHeaderRequest(new MockHttpRequest(site.getHostname(), "/").request(), "Origin", "localhost").request();
    HttpServletResponse response = new MockHttpResponse().response();

    // fat context
    Context context = new ChainedContext(VelocityUtil.getEngine(), request, response, Config.CONTEXT);

    NavToolCache navCache = (NavToolCacheImpl) CacheLocator.getNavToolCache();
    navCache.clearCache();
    final NavTool navTool = new NavTool();
    navTool.init(context);
    NavResult navResult = navTool.getNav();
    assertNotNull(navResult);

    List<NavResult> navResultList = loadResults(navResult, new ArrayList<>());
    assert (navResultList.size() == 7);
    for (NavResult result : navResultList) {
      assertTrue(result instanceof NavResultHydrated);
      assertTrue(!(result.getUnhydratedNavResult() instanceof NavResultHydrated));
      if(result.getType().equals("folder")) {
        NavResult folderNav = navCache.getNav(result.getHostId(), result.getFolderId(), result.getLanguageId());
        assert (folderNav instanceof NavResult);
        assert (!(folderNav instanceof NavResultHydrated));
        List<? extends NavResult> children = folderNav.getChildren();
        for(NavResult child: children) {
          assert (child instanceof NavResult);
          assert (!(child instanceof NavResultHydrated));
        }
        
        
      }
    }

  }
  /**
   * @throws Exception
   * issue: https://github.com/dotCMS/core/issues/16951
   * This test makes sure that the NavCache is not storing any hydrated navs - only the unhydrated ones.
   */

  @Test
  public void test_NavToolCache_cannot_take_a_hydrated_result() throws Exception {

    
    final Folder testFolder = new FolderDataGen().showOnMenu(true).site(site).nextPersisted();
    
    HttpServletRequest request =
        new MockHeaderRequest(new MockHttpRequest(site.getHostname(), "/").request(), "Origin", "localhost").request();
    HttpServletResponse response = new MockHttpResponse().response();

    // fat context
    Context context = new ChainedContext(VelocityUtil.getEngine(), request, response, Config.CONTEXT);

    // wipe out navcache
    NavToolCache navCache = (NavToolCacheImpl) CacheLocator.getNavToolCache();
    navCache.clearCache();
    
    final NavTool navTool = new NavTool();
    navTool.init(context);
    
    // navtool gives us hydrated results
    NavResult navResultHydrated = navTool.getNav(testFolder.getPath());
    assertTrue(navResultHydrated instanceof NavResultHydrated);
    
    assertTrue(!(navResultHydrated.getUnhydratedNavResult() instanceof NavResultHydrated));
    
    // navCache gives us non=hydrated results
    NavResult navResult = navCache.getNav(site.getIdentifier(), testFolder.getInode(), APILocator.getLanguageAPI().getDefaultLanguage().getId());
    assertTrue(!(navResult instanceof NavResultHydrated));
    
    // try to add the hydrated value to cache, and still we get back non-hydrated results
    navCache.putNav(site.getIdentifier(), testFolder.getInode(), navResultHydrated,APILocator.getLanguageAPI().getDefaultLanguage().getId());
    navResult = navCache.getNav(site.getIdentifier(), testFolder.getInode(), APILocator.getLanguageAPI().getDefaultLanguage().getId());
    assertTrue(!(navResult instanceof NavResultHydrated));
    
    
    // requested through the navtool we get a hydrated result
    navResultHydrated = navTool.getNav(testFolder.getPath());
    assertTrue(navResultHydrated instanceof NavResultHydrated);
    
    assertTrue(!(navResultHydrated.getUnhydratedNavResult() instanceof NavResultHydrated));
    
    
    
    
    
  }
  
  /**
   * This helper loads a hierarchy of navs as a flat List<NavResult>
   * @param nav
   * @param addToList
   * @return
   * @throws Exception
   */
  private List<NavResult> loadResults(NavResult nav, List<NavResult> addToList) throws Exception {
    System.out.println("adding:" + nav.getType() + ":" + nav.getTitle());

    addToList.add(nav);
    for (NavResult child : nav.getChildren()) {
      loadResults(child, addToList);
    }
    return addToList;

  }

  private void createData() throws Exception {
    new FolderDataGen().showOnMenu(true).site(site).nextPersisted();

    File file1 = File.createTempFile("test", "test");
    try (FileWriter out = new FileWriter(file1)) {
      out.append("testing");
    }

    new FileAssetDataGen(site, file1).setProperty(FileAssetAPI.SHOW_ON_MENU, "true");

    // Create Folder
    folder = new FolderDataGen().showOnMenu(true).site(site).nextPersisted();
    new FileAssetDataGen(folder, file1).setProperty(FileAssetAPI.SHOW_ON_MENU, "true").nextPersistedWithSampleTextValues();

    // New template
    final Template template = new TemplateDataGen().nextPersisted();

    // Create 2 Pages (One with show on Menu and one without) in English
    final HTMLPageAsset pageAsset1 = new HTMLPageDataGen(folder, template).showOnMenu(true).languageId(1).nextPersisted();
    final HTMLPageAsset pageAsset2 = new HTMLPageDataGen(folder, template).showOnMenu(false).languageId(1).nextPersisted();
    pageAsset1.setIndexPolicy(IndexPolicy.FORCE);
    pageAsset2.setIndexPolicy(IndexPolicy.FORCE);
    APILocator.getContentletAPI().publish(pageAsset1, user, true);
    APILocator.getContentletAPI().publish(pageAsset2, user, true);

    // Create 2 Folders (One with show on Menu and one without)
    final Folder subFolder1 = new FolderDataGen().parent(folder).showOnMenu(true).nextPersisted();

    // Create 2 Pages (One with show on Menu and one without) in English
    final HTMLPageAsset pageAsset3 = new HTMLPageDataGen(subFolder1, template).showOnMenu(true).languageId(1).nextPersisted();
    final HTMLPageAsset pageAsset4 = new HTMLPageDataGen(subFolder1, template).showOnMenu(false).languageId(1).nextPersisted();
    pageAsset3.setIndexPolicy(IndexPolicy.FORCE);
    pageAsset4.setIndexPolicy(IndexPolicy.FORCE);
    APILocator.getContentletAPI().publish(pageAsset3, user, true);
    APILocator.getContentletAPI().publish(pageAsset4, user, true);

    // create a menu link just to test
    Link menuLink = new LinkDataGen(folder).hostId(site.getIdentifier()).showOnMenu(true).nextPersisted();
    APILocator.getVersionableAPI().setLive(menuLink);

  }

}
