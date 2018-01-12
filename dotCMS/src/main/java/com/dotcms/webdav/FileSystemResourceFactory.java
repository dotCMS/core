/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.dotcms.webdav;

import java.io.File;

import com.dotcms.repackage.com.bradmcevoy.common.Path;
import com.dotcms.repackage.com.bradmcevoy.http.Resource;
import com.dotcms.repackage.com.bradmcevoy.http.ResourceFactory;
import com.dotcms.repackage.com.bradmcevoy.http.SecurityManager;
import com.dotcms.repackage.com.ettrema.http.fs.LockManager;
import com.dotcms.repackage.com.ettrema.http.fs.NullSecurityManager;
import com.dotmarketing.util.Logger;


/**
 * A resource factory which provides access to files in a file system.
 *
 * Using this with milton is equivalent to using the dav servlet in tomcat
 *
 */
public final class FileSystemResourceFactory implements ResourceFactory {


  private FileContentService contentService;
  File root;
  SecurityManager securityManager;
  LockManager lockManager;
  Long maxAgeSeconds;
  String contextPath;
  boolean allowDirectoryBrowsing;
  String defaultPage;
  boolean digestAllowed = true;
  private String ssoPrefix;

  /**
   * Creates and (optionally) initialises the factory. This looks for a properties file
   * FileSystemResourceFactory.properties in the classpath If one is found it uses the root and
   * realm properties to initialise
   *
   * If not found the factory is initialised with the defaults root: user.home system property
   * realm: milton-fs-test
   *
   * These initialised values are not final, and may be changed through the setters or init method
   *
   * To be honest its pretty naf configuring like this, but i don't want to force people to use
   * spring or any other particular configuration tool
   *
   */
  public FileSystemResourceFactory() {
    Logger.info(FileSystemResourceFactory.class, "setting default configuration...");
    String sRoot = System.getProperty("user.home");
    SecurityManager sm = new NullSecurityManager();
    contentService = new SimpleFileContentService();
    setLockManager(new FsMemoryLockManager());
    init(sRoot, sm);
  }

  protected void init(String sRoot, SecurityManager securityManager) {
    setRoot(new File(sRoot + "/git/webdav"));
    setSecurityManager(securityManager);
    setContextPath("/webdav/live");
  }

  /**
   *
   * @param root - the root folder of the filesystem to expose. This must include the context path.
   *        Eg, if you've deployed to webdav-fs, root must contain a folder called webdav-fs
   * @param securityManager
   */
  public FileSystemResourceFactory(File root, SecurityManager securityManager) {
    setRoot(root);
    setSecurityManager(securityManager);
  }

  /**
   *
   * @param root - the root folder of the filesystem to expose. called webdav-fs
   * @param securityManager
   * @param contextPath - this is the leading part of URL's to ignore. For example if you're
   *        application is deployed to http://localhost:8080/webdav-fs, the context path should be
   *        webdav-fs
   */
  public FileSystemResourceFactory(File root, SecurityManager securityManager, String contextPath) {
    setRoot(root);
    setSecurityManager(securityManager);
    setContextPath(contextPath);
  }

  public File getRoot() {
    return root;
  }

  public final void setRoot(File root) {
    Logger.info(FileSystemResourceFactory.class, "root: " + root.getAbsolutePath());
    this.root = root;
    if (!root.exists()) {
      Logger.info(FileSystemResourceFactory.class, "Root folder does not exist: " + root.getAbsolutePath());
    }
    if (!root.isDirectory()) {
      Logger.info(FileSystemResourceFactory.class, "Root exists but is not a directory: " + root.getAbsolutePath());
    }
  }

  @Override
  public Resource getResource(String host, String url) {
    Logger.info(FileSystemResourceFactory.class, "getResource: host: " + host + " - url:" + url);
    url = stripContext(url);
    File requested = resolvePath(root, url);
    return resolveFile(host, requested);
  }


  public FsResource resolveFile(String host, File file) {
    FsResource r;
    if (!file.exists()) {
      Logger.info(FileSystemResourceFactory.class, "file not found: " + file.getAbsolutePath());
      return null;
    } else if (file.isDirectory()) {
      r = new FsDirectoryResource(host, this, file, contentService);
    } else {
      r = new FsFileResource(host, this, file, contentService);
    }
    if (r != null) {
      r.ssoPrefix = ssoPrefix;
    }
    return r;
  }

  public File resolvePath(File root, String url) {
    Path path = Path.path(url);
    File f = root;
    for (String s : path.getParts()) {
      f = new File(f, s);
    }
    return f;
  }

  public String getRealm(String host) {
    return securityManager.getRealm(host);
  }

  /**
   *
   * @return - the caching time for files
   */
  public Long maxAgeSeconds(FsResource resource) {
    return maxAgeSeconds;
  }

  public void setSecurityManager(SecurityManager securityManager) {
    if (securityManager != null) {
      Logger.info(FileSystemResourceFactory.class, "securityManager: " + securityManager.getClass());
    } else {
      Logger.info(FileSystemResourceFactory.class, "Setting null FsSecurityManager. This WILL cause null pointer exceptions");
    }
    this.securityManager = securityManager;
  }

  public SecurityManager getSecurityManager() {
    return securityManager;
  }

  public void setMaxAgeSeconds(Long maxAgeSeconds) {
    this.maxAgeSeconds = maxAgeSeconds;
  }

  public Long getMaxAgeSeconds() {
    return maxAgeSeconds;
  }

  public LockManager getLockManager() {
    return lockManager;
  }

  public void setLockManager(LockManager lockManager) {
    this.lockManager = lockManager;
  }

  public void setContextPath(String contextPath) {
    this.contextPath = contextPath;
  }

  public String getContextPath() {
    return contextPath;
  }

  /**
   * Whether to generate an index page.
   *
   * @return
   */
  public boolean isAllowDirectoryBrowsing() {
    return allowDirectoryBrowsing;
  }

  public void setAllowDirectoryBrowsing(boolean allowDirectoryBrowsing) {
    this.allowDirectoryBrowsing = allowDirectoryBrowsing;
  }

  /**
   * if provided GET requests to a folder will redirect to a page of this name within the folder
   *
   * @return - E.g. index.html
   */
  public String getDefaultPage() {
    return defaultPage;
  }

  public void setDefaultPage(String defaultPage) {
    this.defaultPage = defaultPage;
  }

  private String stripContext(String url) {
    if (this.contextPath != null && contextPath.length() > 0) {
      url = url.replaceFirst(contextPath, "");
      Logger.info(FileSystemResourceFactory.class, "stripped context: " + url);
      return url;
    } else {
      return url;
    }
  }

  boolean isDigestAllowed() {
    boolean b = digestAllowed && securityManager != null && securityManager.isDigestAllowed();

    Logger.info(FileSystemResourceFactory.class, "isDigestAllowed: " + b);

    return b;
  }

  public void setDigestAllowed(boolean digestAllowed) {
    this.digestAllowed = digestAllowed;
  }

  public void setSsoPrefix(String ssoPrefix) {
    this.ssoPrefix = ssoPrefix;
  }

  public String getSsoPrefix() {
    return ssoPrefix;
  }

  public FileContentService getContentService() {
    return contentService;
  }

  public void setContentService(FileContentService contentService) {
    this.contentService = contentService;
  }
}
