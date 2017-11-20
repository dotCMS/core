/**
 * 
 */
package com.dotmarketing.webdav2;

import com.dotcms.repackage.com.bradmcevoy.common.Path;
import com.dotcms.repackage.com.bradmcevoy.http.Auth;
import com.dotcms.repackage.com.bradmcevoy.http.CollectionResource;
import com.dotcms.repackage.com.bradmcevoy.http.FolderResource;
import com.dotcms.repackage.com.bradmcevoy.http.HttpManager;
import com.dotcms.repackage.com.bradmcevoy.http.LockInfo;
import com.dotcms.repackage.com.bradmcevoy.http.LockResult;
import com.dotcms.repackage.com.bradmcevoy.http.LockTimeout;
import com.dotcms.repackage.com.bradmcevoy.http.LockToken;
import com.dotcms.repackage.com.bradmcevoy.http.LockableResource;
import com.dotcms.repackage.com.bradmcevoy.http.LockingCollectionResource;
import com.dotcms.repackage.com.bradmcevoy.http.MakeCollectionableResource;
import com.dotcms.repackage.com.bradmcevoy.http.Request;
import com.dotcms.repackage.com.bradmcevoy.http.Request.Method;
import com.dotcms.repackage.com.bradmcevoy.http.Resource;
import com.dotcms.repackage.com.bradmcevoy.http.exceptions.BadRequestException;
import com.dotcms.repackage.com.bradmcevoy.http.exceptions.ConflictException;
import com.dotcms.repackage.com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.CompanyUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * @author Jason Tesser
 *
 */
public class FolderResourceImpl extends BasicFolderResourceImpl
    implements LockableResource, LockingCollectionResource, FolderResource, MakeCollectionableResource {

  private final DotWebDavObject davObject;;
  private Folder folder;
  private PermissionAPI perAPI;
  private HostAPI hostAPI;

  public FolderResourceImpl(Folder folder, DotWebDavObject davObject) {
    super(davObject);
    this.davObject = davObject;
    this.perAPI = APILocator.getPermissionAPI();
    this.folder = folder;
    this.hostAPI = APILocator.getHostAPI();
  }

  public FolderResourceImpl(Folder folder, String fullPath) {
    this(folder, new DotWebDavObject(fullPath));
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.dotcms.repackage.com.bradmcevoy.http.MakeCollectionableResource#createCollection(java.lang.
   * String)
   */
  public CollectionResource createCollection(String newName) throws DotRuntimeException {
    newName = newName.toLowerCase();

    User user = (User) HttpManager.request().getAuthorization().getTag();
    String folderPath = "";

    final String newPath = davObject.assetPath + "/" + newName;
    try {
      Folder newfolder = davObject.createFolder(newPath, user);

      DotWebDavObject helper = new DotWebDavObject(Path.path(newPath));

      FolderResourceImpl folderResource = new FolderResourceImpl(newfolder, helper);
      return folderResource;
    } catch (Exception e) {
      Logger.error(this, e.getMessage(), e);
      throw new DotRuntimeException(e.getMessage(), e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.dotcms.repackage.com.bradmcevoy.http.CollectionResource#child(java.lang.String)
   */
  public Resource child(String childName) {
    User user = (User) HttpManager.request().getAuthorization().getTag();
    List<Resource> children;
    try {
      children = davObject.getChildrenOfFolder(folder, user, davObject.live, davObject.languageId);
    } catch (IOException e) {
      Logger.error(FolderResourceImpl.class, e.getMessage(), e);
      throw new DotRuntimeException(e.getMessage(), e);
    }
    for (Resource resource : children) {
      if (resource instanceof FolderResourceImpl) {
        String name = ((FolderResourceImpl) resource).getFolder().getName();
        if (name.equalsIgnoreCase(childName)) {
          return resource;
        }
      } else if (resource instanceof TempFolderResourceImpl) {
        String name = ((TempFolderResourceImpl) resource).getFolder().getName();
        if (name.equalsIgnoreCase(childName)) {
          return resource;
        }
      } else if (resource instanceof TempFileResourceImpl) {
        String name = ((TempFileResourceImpl) resource).getFile().getName();
        if (name.equalsIgnoreCase(childName)) {
          return resource;
        }
      } else {
        String name = ((FileResourceImpl) resource).getFile().getFileName();
        if (name.equalsIgnoreCase(childName)) {
          return resource;
        }
      }
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.dotcms.repackage.com.bradmcevoy.http.CollectionResource#getChildren()
   */
  public List<? extends Resource> getChildren() {
    User user = (User) HttpManager.request().getAuthorization().getTag();
    List<Resource> children;
    try {
      children = davObject.getChildrenOfFolder(folder, user, davObject.live, davObject.languageId);
    } catch (IOException e) {
      Logger.error(FolderResourceImpl.class, e.getMessage(), e);
      throw new DotRuntimeException(e.getMessage(), e);
    }
    return children;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.dotcms.repackage.com.bradmcevoy.http.Resource#authenticate(java.lang.String,
   * java.lang.String)
   */
  public Object authenticate(String username, String password) {
    try {
      return davObject.authorizePrincipal(username, password);
    } catch (Exception e) {
      Logger.error(this, e.getMessage(), e);
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.dotcms.repackage.com.bradmcevoy.http.Resource#authorise(com.dotcms.repackage.com.bradmcevoy
   * .http.Request, com.dotcms.repackage.com.bradmcevoy.http.Request.Method,
   * com.dotcms.repackage.com.bradmcevoy.http.Auth)
   */
  public boolean authorise(Request req, Method method, Auth auth) {
    try {

      if (auth == null)
        return false;
      else {
        User user = (User) auth.getTag();
        if (method.isWrite) {
          return perAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user, false);
        } else if (!method.isWrite) {
          return perAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_READ, user, false);
        }
      }

    } catch (DotDataException e) {
      Logger.error(FolderResourceImpl.class, e.getMessage(), e);
      throw new DotRuntimeException(e.getMessage(), e);
    }
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.dotcms.repackage.com.bradmcevoy.http.Resource#checkRedirect(com.dotcms.repackage.com.
   * bradmcevoy.http.Request)
   */
  public String checkRedirect(Request req) {
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.dotcms.repackage.com.bradmcevoy.http.Resource#getContentLength()
   */
  public Long getContentLength() {
    return (long) 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.dotcms.repackage.com.bradmcevoy.http.Resource#getContentType(java.lang.String)
   */
  public String getContentType(String arg0) {
    return "folder";
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.dotcms.repackage.com.bradmcevoy.http.Resource#getModifiedDate()
   */
  public Date getModifiedDate() {
    return folder.getiDate();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.dotcms.repackage.com.bradmcevoy.http.Resource#getRealm()
   */
  public String getRealm() {
    return CompanyUtils.getDefaultCompany().getName();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.dotcms.repackage.com.bradmcevoy.http.Resource#getUniqueId()
   */
  public String getUniqueId() {
    return folder.getInode();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.dotcms.repackage.com.bradmcevoy.http.DeletableResource#delete()
   */
  public void delete() throws DotRuntimeException {
    User user = (User) HttpManager.request().getAuthorization().getTag();
    try {
      APILocator.getFolderAPI().delete(folder, user, false);
    } catch (Exception e) {
      Logger.error(this, e.getMessage(), e);
      throw new DotRuntimeException(e.getMessage(), e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.dotcms.repackage.com.bradmcevoy.http.GetableResource#getMaxAgeSeconds()
   */
  public Long getMaxAgeSeconds() {
    return new Long(0);
  }



  public void copyTo(TempFolderResourceImpl collRes, String name)
      throws NotAuthorizedException, BadRequestException, ConflictException {

    try {
      davObject.copyFolderToTemp(folder, collRes.getFolder(), davObject.user, name, davObject.live,
          davObject.languageId);
    } catch (Exception e) {
      Logger.error(this, e.getMessage(), e);
      throw new BadRequestException(e.getMessage());
    }
  }

  public void copyTo(FolderResourceImpl collRes, String name)
      throws NotAuthorizedException, BadRequestException, ConflictException {

    FolderResourceImpl fr = (FolderResourceImpl) collRes;
    try {
      String p = fr.getPath();
      if (!p.endsWith("/"))
        p = p + "/";
      davObject.copyFolder(davObject.assetPath.toPath(), p + name, davObject.user, davObject.live);
    } catch (Exception e) {
      Logger.error(this, e.getMessage(), e);
    }
  }

  public void copyTo(HostResourceImpl collRes, String name)
      throws NotAuthorizedException, BadRequestException, ConflictException {

    HostResourceImpl hr = (HostResourceImpl) collRes;
    String p = hr.getPath();
    if (!p.endsWith("/"))
      p = p + "/";
    try {
      davObject.copyFolder(p, "/" + hr.getName() + "/" + name, davObject.user, davObject.live);
    } catch (Exception e) {
      Logger.error(this, e.getMessage(), e);
    }
  }


  @Override
  public void copyTo(CollectionResource collRes, String name)
      throws NotAuthorizedException, BadRequestException, ConflictException {
    User user = (User) HttpManager.request().getAuthorization().getTag();

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.dotcms.repackage.com.bradmcevoy.http.MoveableResource#moveTo(com.dotcms.repackage.com.
   * bradmcevoy.http.CollectionResource, java.lang.String)
   */
  public void moveTo(CollectionResource collRes, String name) throws DotRuntimeException {
    User user = (User) HttpManager.request().getAuthorization().getTag();
    if (collRes instanceof TempFolderResourceImpl) {
      Logger.debug(this,
          "Webdav clients wants to move a file from dotcms to a tempory storage but we don't allow this in fear that the tranaction may break and delete a file from dotcms");
      TempFolderResourceImpl tr = (TempFolderResourceImpl) collRes;
      try {
        davObject.copyFolderToTemp(folder, tr.getFolder(), user, name, davObject.live, davObject.languageId);
      } catch (IOException e) {
        Logger.error(this, e.getMessage(), e);
        return;
      }
    } else if (collRes instanceof FolderResourceImpl) {
      FolderResourceImpl fr = (FolderResourceImpl) collRes;

      try {
        String p = fr.getPath();
        if (!p.endsWith("/"))
          p = p + "/";
        davObject.move(this.getPath(), p + name, user, davObject.live);
      } catch (Exception e) {
        Logger.error(this, e.getMessage(), e);
        throw new DotRuntimeException(e.getMessage(), e);
      }
    } else if (collRes instanceof HostResourceImpl) {
      HostResourceImpl hr = (HostResourceImpl) collRes;

      try {
        String p = this.getPath();
        if (!p.endsWith("/"))
          p = p + "/";
        davObject.move(p, "/" + hr.getName() + "/" + name, user, davObject.live);
      } catch (Exception e) {
        Logger.error(this, e.getMessage(), e);
        throw new DotRuntimeException(e.getMessage(), e);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.dotcms.repackage.com.bradmcevoy.http.PropFindableResource#getCreateDate()
   */
  public Date getCreateDate() {
    return folder.getiDate();
  }

  public String getName() {
    return UtilMethods.escapeHTMLSpecialChars(folder.getName());
  }

  public String getPath() {
    return davObject.assetPath.toPath();
  }

  public int compareTo(Object o) {
    // TODO Auto-generated method stub
    return 0;
  }

  public Folder getFolder() {
    return folder;
  }

  public void setFolder(Folder folder) {
    this.folder = folder;
  }



  public LockResult lock(LockTimeout timeout, LockInfo lockInfo) {
    return davObject.lock(timeout, lockInfo, getUniqueId());
    // return davObject.lock(lockInfo, user, file.getIdentifier() + "");
  }

  public LockResult refreshLock(String token) {
    return davObject.refreshLock(getUniqueId());
    // return davObject.refreshLock(token);
  }

  public void unlock(String tokenId) {
    davObject.unlock(getUniqueId());
    // davObject.unlock(tokenId);
  }

  public LockToken getCurrentLock() {
    return davObject.getCurrentLock(getUniqueId());
  }

  public Long getMaxAgeSeconds(Auth arg0) {
    return (long) 60;
  }


  public LockToken createAndLock(String name, LockTimeout timeout, LockInfo lockInfo) throws NotAuthorizedException {
    createCollection(name);
    return lock(timeout, lockInfo).getLockToken();
  }

}
