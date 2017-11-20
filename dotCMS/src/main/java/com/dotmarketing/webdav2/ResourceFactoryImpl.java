/**
 * 
 */
package com.dotmarketing.webdav2;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.repackage.com.bradmcevoy.common.Path;
import com.dotcms.repackage.com.bradmcevoy.http.ApplicationConfig;
import com.dotcms.repackage.com.bradmcevoy.http.HttpManager;
import com.dotcms.repackage.com.bradmcevoy.http.Initable;
import com.dotcms.repackage.com.bradmcevoy.http.Resource;
import com.dotcms.repackage.com.bradmcevoy.http.ResourceFactory;
import com.dotmarketing.util.Logger;

/**
 * @author Jason Tesser
 *
 */
public class ResourceFactoryImpl implements ResourceFactory, Initable {


  public ResourceFactoryImpl() {


  }



  /*
   * (non-Javadoc)
   * 
   * @see com.dotcms.repackage.com.bradmcevoy.http.ResourceFactory#getResource(java.lang.String,
   * java.lang.String)
   */
  @Override
  @WrapInTransaction
  public Resource getResource(final String davHost, final String url) {

    final Path path = Path.path(url);
    DotWebDavObject helper = new DotWebDavObject(path);

    Logger.debug(this, "WebDav ResourceFactory: Host is " + davHost + " and the url is " + url);


    // DAV ROOT
    if (helper.root) {
      WebdavRootResourceImpl wr = new WebdavRootResourceImpl(helper);
      return wr;
    }



    // handle crappy dav clients temp files
    if (true) {

      java.io.File tempFile = helper.loadTempFile(url);
      if (tempFile == null || !tempFile.exists()) {
        return null;
      } else if (tempFile.isDirectory()) {
        
        TempFolderResourceImpl tr = new TempFolderResourceImpl(tempFile, helper);
        return tr;
      } else {
        TempFileResourceImpl tr = new TempFileResourceImpl(tempFile, helper);
        return tr;
      }
    }
    return null;


  }



  /*
   * (non-Javadoc)
   * 
   * @see com.dotcms.repackage.com.bradmcevoy.http.ResourceFactory#getSupportedLevels()
   */
  public String getSupportedLevels() {
    return "1,2";
  }

  public void init(ApplicationConfig config, HttpManager manager) {
    manager.setEnableExpectContinue(false);



  }

  public void destroy(HttpManager manager) {
    // TODO Auto-generated method stub

  }

}
