package com.dotcms.content.elasticsearch.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import com.dotcms.util.ConfigTestHelper;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.File;
import java.io.IOException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class ES6UpgradeTest extends IntegrationTestBase {

  private static final String RESOURCE_DIR = "com/dotcms/content/elasticsearch/business/json";

  private static User systemUser;

  @BeforeClass
  public static void prepare() throws Exception {

    // Setting web app environment
    IntegrationTestInitService.getInstance().init();

    systemUser = APILocator.getUserAPI().getSystemUser();
  }

  /**
   * Gets an array of JSon files containing Elastic Search queries to be tested
   *
   * @return list of Files
   */
  @DataProvider
  public static Object[] getJsonFilesDataProvider() {
    final String resource = ConfigTestHelper.getPathToTestResource(RESOURCE_DIR);
    final File directory = new File(resource);
    return directory.listFiles();
  }

  @Test
  @UseDataProvider("getJsonFilesDataProvider")
  public void testElasticSearchJson(final Object objectFile)
      throws DotSecurityException, DotDataException, IOException {

    final File file = (File) objectFile;
    Logger.info(this, "Testing File: " + file.getName());

    final String json = FileUtils.readFileToString(file);
    final ESSearchResults results =
        APILocator.getContentletAPI().esSearch(json, false, systemUser, false);

    Assert.assertNotNull(results);

    Logger.info(this, "Results size: " + results.getTotalResults());
    Assert.assertTrue(results.getTotalResults() > 0);

    if (json.contains("agg")) {
      // This is an aggregation
      Assert.assertFalse(results.getAggregations().asList().isEmpty());
    } else {
      // Contentlets
      Assert.assertFalse(results.isEmpty());
      for (final Object res : results) {
        final Contentlet contentlet = (Contentlet) res;
        Assert.assertTrue(
            APILocator.getPermissionAPI()
                .doesUserHavePermission(
                    contentlet, PermissionAPI.PERMISSION_READ, systemUser, false));
      }
    }

    Logger.info(this, "Success Testing File: " + file.getName());
  }
}
