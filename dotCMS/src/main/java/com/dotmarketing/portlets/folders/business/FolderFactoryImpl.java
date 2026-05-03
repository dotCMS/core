package com.dotmarketing.portlets.folders.business;

import static com.dotmarketing.portlets.folders.business.FolderAPI.OLD_SYSTEM_FOLDER_ID;
import static com.dotmarketing.portlets.folders.business.FolderAPI.SYSTEM_FOLDER;
import static com.dotmarketing.portlets.folders.business.FolderAPI.SYSTEM_FOLDER_ASSET_NAME;
import static com.dotmarketing.portlets.folders.business.FolderAPI.SYSTEM_FOLDER_PARENT_PATH;
import static com.dotmarketing.portlets.folders.business.FolderFactorySql.GET_CONTENT_REPORT;
import static com.dotmarketing.portlets.folders.business.FolderFactorySql.GET_CONTENT_TYPE_COUNT;

import com.dotcms.browser.BrowserQuery;
import com.dotcms.variant.VariantAPI;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.system.SimpleMapAppContext;
import com.dotcms.util.transform.DBTransformer;
import com.dotcms.util.transform.TransformerLocator;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotIdentifierStateException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.IdentifierCache;
import com.dotmarketing.business.IdentifierFactory;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.cache.FolderCache;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.commands.DatabaseCommand.QueryReplacements;
import com.dotmarketing.db.commands.UpsertCommand;
import com.dotmarketing.db.commands.UpsertCommandFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicyProvider;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.factories.LinkFactory;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.util.AssetsComparator;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;

/**
 * This class extends the {@link FolderFactory} class. It provides access to Folder information stored in the database.
 * All CRUD operations related to Folders must be provided by this class.
 *
 * @author maria
 * @since Mar 22nd, 2012
 */
public class FolderFactoryImpl extends FolderFactory {

  private static final String[] UPSERT_EXTRA_COLUMNS = {"name", "title", "show_on_menu",
      "sort_order", "files_masks", "identifier", "default_file_type", "mod_date", "owner", "idate"};
  private final FolderCache folderCache = CacheLocator.getFolderCache();

  @Override
  protected boolean exists(String folderIdOrInode) throws DotDataException {
    DotConnect dc = new DotConnect();
    dc.setSQL("select inode  from folder where identifier = ? or inode = ?");
    dc.addParam(folderIdOrInode);
    dc.addParam(folderIdOrInode);
    return !dc.loadResults().isEmpty();
  }

  @Override
  protected void delete(final Folder folder) throws DotDataException {

    final Identifier id = APILocator.getIdentifierAPI().find(folder.getIdentifier());
    new DotConnect()
        .setSQL("delete from folder where folder.inode = ? ")
        .addParam(folder.getInode()).loadResult();

    new DotConnect()
        .setSQL("delete from inode where inode = ? ")
        .addParam(folder.getInode()).loadResult();

    folderCache.removeFolder(folder, id);

		   /*Folder reference is not explicitly deleted from the identifier table because there is
		   a db trigger that executes the delete operation once the folder table is cleaned up.
		   Only cache is flushed.
		   Trigger name: folder_identifier_check_trigger
		   */
    CacheLocator.getIdentifierCache().removeFromCacheByIdentifier(id.getId());
  }


  @Override
  protected Folder find(final String folderIdOrInode) throws DotDataException {
    Optional<Folder> folder = Optional.ofNullable(folderCache.getFolder(folderIdOrInode));
    if (folder.isPresent()) {
      return folder.get();
    }

    DotConnect dc = new DotConnect()
        .setSQL("select * from folder where identifier = ? or inode = ?")
        .addParam(folderIdOrInode)
        .addParam(folderIdOrInode);

    folder = Try.of(() -> TransformerLocator.createFolderTransformer(dc.loadObjectResults()).asList().get(0))
        .onFailure(e -> Logger.debug(FolderFactoryImpl.class, e.getMessage(), e))
        .toJavaOptional();

    if (folder.isPresent()) {
      Identifier id = APILocator.getIdentifierAPI().find(folder.get().getIdentifier());
      folderCache.addFolder(folder.get(), id);
      return folder.get();
    }

    // if this is the old system folder id, return the new SYSTEM_FOLDER
    if (OLD_SYSTEM_FOLDER_ID.equals(folderIdOrInode)) {
      return find(SYSTEM_FOLDER);
    }

    return null;


  }

  /**
   * @param folder
   * @return
   * @throws DotStateException
   * @throws DotDataException
   */
  @SuppressWarnings("unchecked")
  @Override
  @Deprecated
  protected List<Folder> getSubFolders(Folder folder) throws DotStateException, DotDataException {

    return getSubFoldersTitleSort(folder);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected List<Folder> getSubFoldersTitleSort(Folder folder) throws DotDataException {
    Identifier id = APILocator.getIdentifierAPI().find(folder.getIdentifier());

    return getSubFolders(null, id.getPath(), id.getHostId(), "lower(folder.title)");
  }

  @SuppressWarnings("unchecked")
  @Override
  protected List<Folder> getSubFoldersNameSort(Folder folder) throws DotDataException {
    Identifier id = APILocator.getIdentifierAPI().find(folder.getIdentifier());

    return getSubFolders(null, id.getPath(), id.getHostId(), "lower(folder.name)");
  }

  @SuppressWarnings("unchecked")
  @Override
  protected List<Folder> findSubFolders(final Folder folder, Boolean showOnMenu)
      throws DotStateException, DotDataException {
    Identifier id = APILocator.getIdentifierAPI().find(folder.getIdentifier());
    return getSubFolders(showOnMenu, id.getPath(), id.getHostId(), null);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected List<Folder> findSubFolders(final Host host, Boolean showOnMenu) {

    return getSubFolders(showOnMenu, "/", host.getIdentifier(), null);
  }

  private List<Folder> getSubFolders(Boolean showOnMenu, String path, final String hostId, String order) {
    DotConnect dc = new DotConnect();
    String condition = "";
    StringBuilder orderBy = new StringBuilder(" order by ");
    StringBuilder query = new StringBuilder();

    if (UtilMethods.isSet(showOnMenu)) {
      condition = "show_on_menu = " + (showOnMenu ? DbConnectionFactory
          .getDBTrue() : DbConnectionFactory.getDBFalse());
    }

    if (!UtilMethods.isSet(order)) {
      orderBy.append("sort_order, name");
    } else {
      orderBy.append(order);
    }

    query.append("SELECT folder.* from folder folder, identifier identifier ").
        append("where folder.identifier = identifier.id and ").
        append("identifier.parent_path = ? and identifier.host_inode = ? ");

    query.append((!condition.isEmpty() ? " and " + condition : condition) + orderBy);

    dc.setSQL(query.toString());
    dc.addParam(path);
    dc.addParam(hostId);

    try {

      return TransformerLocator.createFolderTransformer(dc.loadObjectResults()).asList();

    } catch (DotDataException e) {
      Logger.error(this, e.getMessage(), e);
    }

    return Collections.emptyList();
  }

  @Override
  protected Folder findFolderByPath(String path, final Host site) throws DotDataException {

    final String originalPath = path;
    Folder folder;
    List<Folder> result;

    if (site == null || path == null) {
      return null;
    }
    // replace nasty double //
    path = path.replaceAll(StringPool.DOUBLE_SLASH, StringPool.SLASH);

    if (path.equals("/") || path.equals(SYSTEM_FOLDER_PARENT_PATH)) {
      folder = this.findSystemFolder();
    } else {
      folder = folderCache.getFolderByPathAndHost(path, site);
    }

    if (folder == null) {
      String parentPath;
      String assetName;
      String siteId;

      try {
        // trailing / is removed
        if (path.endsWith("/")) {
          path = path.substring(0, path.length() - 1);
        }
        // split path into parent and asset name
        int idx = path.lastIndexOf('/');
        parentPath = path.substring(0, idx + 1);
        assetName = path.substring(idx + 1);
        siteId = site.getIdentifier();

        DotConnect dc = new DotConnect();
        dc.setSQL("select folder.* from folder, identifier i where i.full_path_lc = ? "
            + " and folder.identifier = i.id and i.host_inode = ?");

        dc.addParam((parentPath + assetName).toLowerCase());

        dc.addParam(siteId);

        result = TransformerLocator.createFolderTransformer(dc.loadObjectResults()).asList();

        if (result != null && !result.isEmpty()) {
          folder = result.get(0);
        } else {
          folder = new Folder();
        }

        // if it is found add it to folder cache
        if (UtilMethods.isSet(folder) && UtilMethods.isSet(folder.getInode())) {
          final Identifier id = APILocator.getIdentifierAPI().find(folder.getIdentifier());
          folderCache.addFolder(folder, id);
        } else {
          String parentFolder = originalPath;

          //If the path ends with a / we assume that the last part of the path is the folder.
          //We just have to remove tha last / and substring from the last / remaining.
          //For example: /application/themes/ => themes
          if (parentFolder.endsWith("/")) {
            parentFolder = parentFolder.substring(0, parentFolder.length() - 1);
            parentPath = parentFolder.substring(0, parentFolder.lastIndexOf("/") + 1);

            if (parentFolder.contains("/")) {
              parentFolder = parentFolder.substring(parentFolder.lastIndexOf("/") + 1);
            }

            //If the path doesn't end in / we assume the last part is a page or something else.
            //We have to remove the last part plus the / and then remove the path from 0 to /
            //For example: /application/themes/example-page => themes
          } else {
            if (parentFolder.contains("/")) {
              parentFolder = parentFolder.substring(0, parentFolder.lastIndexOf("/"));
              parentPath = parentFolder.substring(0, parentFolder.lastIndexOf("/") + 1);
              parentFolder = parentFolder.substring(parentFolder.lastIndexOf("/") + 1);
            }
          }

          dc = new DotConnect();
          dc.setSQL("select folder.* from folder, identifier i"
              + " where i.full_path_lc = ?"
              + " and folder.identifier = i.id"
              + " and i.host_inode = ?");
          dc.addParam((parentPath + parentFolder).toLowerCase());
          dc.addParam(siteId);

          result = TransformerLocator.createFolderTransformer(dc.loadObjectResults())
              .asList();

          if (result != null && !result.isEmpty()) {
            folder = result.get(0);
          }

          // if it is found add it to folder cache
          if (UtilMethods.isSet(folder) && UtilMethods.isSet(folder.getInode())) {
            Identifier id = APILocator.getIdentifierAPI().find(folder.getIdentifier());
            folderCache.addFolder(folder, id);
          }
        }
      } catch (final Exception e) {
        final String errorMsg = String.format("An error occurred when finding path '%s' in Site '%s' [%s]: " +
            "%s", path, site.getHostname(), site.getIdentifier(), e.getMessage());
        throw new DotDataException(errorMsg, e);
      }
    }
    return folder;
  }


  protected List<Folder> getFoldersByParent(Folder folder, User user, boolean respectFrontendRoles)
      throws DotDataException {
    List<Folder> entries = new ArrayList<>();
    //the subfolders are sorted by name
    List<Folder> elements = getSubFoldersNameSort(folder);
    for (Folder childFolder : elements) {
      if (APILocator.getPermissionAPI()
          .doesUserHavePermission(childFolder, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
        entries.add(childFolder);
      }
    }
    return entries;
  }

  @SuppressWarnings("unchecked")
  protected java.util.List getMenuItems(Folder folder) throws DotDataException {
    return getMenuItems(folder, 1);
  }

  @SuppressWarnings("unchecked")
  protected java.util.List getMenuItems(Host host) throws DotDataException, DotSecurityException {
    return getMenuItems(host, 1);
  }

  @SuppressWarnings("unchecked")
  protected java.util.List getMenuItems(Folder folder, int orderDirection) throws DotDataException {
    List<Folder> folders = new ArrayList<>();
    folders.add(folder);
    return getMenuItems(folders, orderDirection);
  }

  @SuppressWarnings("unchecked")
  protected java.util.List getMenuItems(Host host, int orderDirection) throws DotDataException, DotSecurityException {
    List<Folder> subFolders = APILocator.getFolderAPI()
        .findSubFolders(host, APILocator.getUserAPI().getSystemUser(), false);
    return getMenuItems(subFolders, orderDirection);
  }

  @SuppressWarnings("unchecked")
  private List getMenuItems(List<Folder> folders, int orderDirection) {

    List menuList = new ArrayList();

    for (Folder folder : folders) {
      final BrowserQuery browserQuery = BrowserQuery.builder()
          .withHostOrFolderId(folder.getIdentifier())
          .hostIdSystemFolder(folder.getHostId())
          .showOnlyMenuItems(true)
          .showFolders(!folder.getInode()
              .equals(FolderAPI.SYSTEM_FOLDER))
          .showFiles(true)
          .showLinks(true)
          .showPages(true)
          .build();
      final List contentList = Try.of(() -> APILocator.getBrowserAPI().getFolderContentList(browserQuery))
          .getOrElseThrow(DotRuntimeException::new);

      Comparator comparator = new AssetsComparator(orderDirection);
      java.util.Collections.sort(contentList, comparator);
      menuList.addAll(contentList);
    }

    return menuList;
  }


  @SuppressWarnings("unchecked")
  private Folder copy(Folder folder, Host destination, Hashtable copiedObjects)
      throws DotDataException, DotSecurityException, DotStateException, IOException {

    boolean rename = APILocator.getHostAPI().doesHostContainsFolder(destination, folder.getName());

    final Folder newFolder = new Folder();
    newFolder.copy(folder);
    newFolder.setName(folder.getName());
    while (rename) {
      newFolder.setName(newFolder.getName() + "_copy");
      rename = APILocator.getHostAPI().doesHostContainsFolder(destination, newFolder.getName());
    }

    newFolder.setHostId(destination.getIdentifier());

    final Identifier newFolderId = APILocator.getIdentifierAPI().createNew(newFolder, destination);

    newFolder.setIdentifier(newFolderId.getId());
    newFolder.setModDate(new Date());

    save(newFolder);

    saveCopiedFolder(folder, newFolder, copiedObjects);
    return newFolder;
  }

  @SuppressWarnings("unchecked")
  private Folder copy(Folder folder, Folder destination, Hashtable copiedObjects)
      throws DotDataException, DotStateException, DotSecurityException, IOException {

    boolean rename = folderContains(folder.getName(), destination);

    final Folder newFolder = new Folder();
    newFolder.copy(folder);
    newFolder.setName(folder.getName());
    while (rename) {
      newFolder.setName(newFolder.getName() + "_copy");
      rename = folderContains(newFolder.getName(), destination);
    }

    newFolder.setHostId(destination.getHostId());

    final Identifier newFolderId = APILocator.getIdentifierAPI().createNew(newFolder, destination);
    newFolder.setIdentifier(newFolderId.getId());
    newFolder.setModDate(new Date());

    save(newFolder);

    saveCopiedFolder(folder, newFolder, copiedObjects);
    return newFolder;
  }

  private void saveCopiedFolder(Folder source, Folder newFolder, Hashtable copiedObjects)
      throws DotDataException, DotStateException, DotSecurityException, IOException {
    User systemUser = APILocator.getUserAPI().getSystemUser();

    if (copiedObjects == null) {
			copiedObjects = new Hashtable();
    }

    // Copying folder permissions
    APILocator.getPermissionAPI().copyPermissions(source, newFolder);

    // Copying children html pages
    Map<String, IHTMLPage[]> pagesCopied;
    if (copiedObjects.get("HTMLPages") == null) {
      pagesCopied = new HashMap<String, IHTMLPage[]>();
      copiedObjects.put("HTMLPages", pagesCopied);
    } else {
      pagesCopied = (Map<String, IHTMLPage[]>) copiedObjects.get("HTMLPages");
    }

    // Copying Files
    Map<String, IFileAsset[]> filesCopied;
    if (copiedObjects.get("Files") == null) {
      filesCopied = new HashMap<String, IFileAsset[]>();
      copiedObjects.put("Files", filesCopied);
    } else {
      filesCopied = (Map<String, IFileAsset[]>) copiedObjects.get("Files");
    }

    //Content Files
    List<FileAsset> faConts = APILocator.getFileAssetAPI()
        .findFileAssetsByFolder(source, APILocator.getUserAPI().getSystemUser(), false);
    for (FileAsset fa : faConts) {
      if (fa.isWorking() && !fa.isArchived() && !filesCopied.containsKey(fa.getIdentifier())) {
        Contentlet cont = APILocator.getContentletAPI()
            .find(fa.getInode(), APILocator.getUserAPI().getSystemUser(), false);
        cont.setIndexPolicy(IndexPolicyProvider.getInstance().forSingleContent());

        APILocator.getContentletAPI().copyContentlet(cont, newFolder, APILocator.getUserAPI().getSystemUser(), false);
        filesCopied.put(cont.getIdentifier(), new IFileAsset[]{fa, APILocator.getFileAssetAPI().fromContentlet(cont)});
      }
    }

    //Content Pages
    Set<IHTMLPage> pageAssetList = new HashSet<>();
    pageAssetList.addAll(
        APILocator.getHTMLPageAssetAPI().getWorkingHTMLPages(source, APILocator.getUserAPI().getSystemUser(), false));
    for (IHTMLPage page : pageAssetList) {
      if (!pagesCopied.containsKey(page.getIdentifier())) {
        Contentlet cont = APILocator.getContentletAPI()
            .find(page.getInode(), APILocator.getUserAPI().getSystemUser(), false);
        APILocator.getContentletAPI().copyContentlet(cont, newFolder, APILocator.getUserAPI().getSystemUser(), false);
        pagesCopied.put(cont.getIdentifier(),
            new IHTMLPage[]{page, APILocator.getHTMLPageAssetAPI().fromContentlet(cont)});
      }
    }

    // issues/1736
    APILocator.getContentletAPI().refreshContentUnderFolder(newFolder);

    // Copying links
    Map<String, Link[]> linksCopied;
    if (copiedObjects.get("Links") == null) {
      linksCopied = new HashMap<String, Link[]>();
      copiedObjects.put("Links", linksCopied);
    } else {
      linksCopied = (Map<String, Link[]>) copiedObjects.get("Links");
    }

    List links = getChildrenClass(source, Link.class);
    for (Link link : (List<Link>) links) {
      if (link.isWorking()) {
        Link newLink = LinkFactory.copyLink(link, newFolder);
        // Saving copied pages to update template - pages relationships
        // later
        linksCopied.put(link.getInode(), new Link[]{link, newLink});
      }
    }

    // Copying Inner Folders
    List<Folder> childrenFolder = APILocator.getFolderAPI().findSubFolders(source, systemUser, false);
    for (Folder childFolder : (List<Folder>) childrenFolder) {
      copy(childFolder, newFolder, copiedObjects);
    }

  }

  @Override
  protected Folder copy(Folder folder, Host destination)
      throws DotDataException, DotSecurityException, DotStateException, IOException {
    return copy(folder, destination, null);
  }

  @Override
  protected Folder copy(Folder folder, Folder destination)
      throws DotDataException, DotStateException, DotSecurityException, IOException {
    return copy(folder, destination, null);
  }

  @SuppressWarnings("unchecked")
  private boolean folderContains(String name, Folder destination)
      throws DotStateException, DotDataException, DotSecurityException {
    List<Folder> children = APILocator.getFolderAPI()
        .findSubFolders(destination, APILocator.getUserAPI().getSystemUser(), false);
    for (Folder folder : children) {
      if (folder.getName().equals(name)) {
				return true;
      }
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  private Optional<Folder> move(final Folder folder, final Object destination)
      throws DotDataException, DotStateException, DotSecurityException {

    final MutableBoolean successOperation = new MutableBoolean(true);
    final IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();
    final ContentletAPI contentletAPI = APILocator.getContentletAPI();
    final Identifier folderId = identifierAPI.find(folder.getIdentifier());

    //Clean up the cache
    this.cleanUpTheFolderCache(folder, folderId);

    final User systemUser = APILocator.systemUser();
    boolean contains;
    String newParentPath;
    String newParentHostId;

    if (destination instanceof Folder) {

      contains = this.folderContains(folder.getName(), (Folder) destination);
      final Identifier destinationId = identifierAPI.find(((Folder) destination).getIdentifier());
      newParentPath = destinationId.getPath();
      newParentHostId = destinationId.getHostId();
      if (!contains) {
        CacheLocator.getNavToolCache().removeNavByPath(destinationId.getHostId(), destinationId.getPath());
      }

    } else {

      contains = APILocator.getHostAPI().doesHostContainsFolder((Host) destination, folder.getName());
      newParentPath = StringPool.FORWARD_SLASH;
      newParentHostId = ((Host) destination).getIdentifier();
      if (!contains) {
        CacheLocator.getNavToolCache().removeNav(newParentHostId, SYSTEM_FOLDER);
      }
    }

    if (contains) {

      return Optional.empty();
    }

    final List<Folder> subFolders = this.getSubFoldersTitleSort(folder);
    final List<Link> links = (List<Link>) (List<?>) this.getChildrenClass(folder, Link.class);
    final List<Contentlet> contentlets = contentletAPI.
        findContentletsByFolder(folder, systemUser, false);

    final Folder newFolder = getNewFolderRecord(folder, systemUser,
        newParentPath, newParentHostId);

    this.moveLinks(newFolder, links);
    this.moveChildContentlets(newFolder, systemUser, contentlets);

    successOperation.setValue(this.moveChildFolders(newFolder, subFolders));

    //update permission and structure references
    updateOtherFolderReferences(newFolder.getInode(), folder.getInode());

    delete(folder);

    return successOperation.getValue() ? Optional.of(newFolder) : Optional.empty();
  }

  private void updateOtherFolderReferences(final String newFolderInode, final String oldFolderInode)
      throws DotDataException {
    final DotConnect dc = new DotConnect();
    dc.executeUpdate(
        "UPDATE structure SET folder = ? WHERE folder = ?", newFolderInode, oldFolderInode);
    APILocator.getContentTypeAPI(APILocator.systemUser())
        .search("folder='" + newFolderInode + "'", "mod_date", -1, 0)
        .forEach(CacheLocator.getContentTypeCache2()::remove);
    dc.executeUpdate(
        "UPDATE permission SET inode_id = ? WHERE inode_id = ?", newFolderInode, oldFolderInode);
    dc.executeUpdate(
        "UPDATE permission_reference SET asset_id = ? WHERE asset_id = ?", newFolderInode, oldFolderInode);
    APILocator.getPermissionAPI().removePermissionableFromCache(oldFolderInode);
    APILocator.getPermissionAPI().removePermissionableFromCache(newFolderInode);
  }

  /**
   * This method creates a new initialFolder based on another folder. This new folder will be in a new path/host
   *
   * @param initialFolder   Folder to based on
   * @param systemUser
   * @param newParentPath   New path where the folder will be in (if applies)
   * @param newParentHostId New host where the folder will be in (if applies)
   * @return New folder
   * @throws DotDataException
   * @throws DotSecurityException
   */
  private Folder getNewFolderRecord(final Folder initialFolder, final User systemUser,
      final String newParentPath, final String newParentHostId) throws DotDataException, DotSecurityException {

    final Host newHost = APILocator.getHostAPI().find(newParentHostId, systemUser, false);

    final Folder newParentFolder = findFolderByPath(newParentPath, newHost);

    final Folder newFolder = new Folder();
    newFolder.setName(initialFolder.getName());
    newFolder.setTitle(initialFolder.getTitle());
    newFolder.setShowOnMenu(initialFolder.isShowOnMenu());
    newFolder.setSortOrder(initialFolder.getSortOrder());
    newFolder.setFilesMasks(initialFolder.getFilesMasks());
    newFolder.setDefaultFileType(initialFolder.getDefaultFileType());
    newFolder.setOwner(initialFolder.getOwner());
    newFolder.setIDate(initialFolder.getIDate());
    newFolder.setHostId(newParentHostId);

    final Identifier newIdentifier = !UtilMethods.isSet(newParentFolder) || newParentFolder.isSystemFolder() ?
        APILocator.getIdentifierAPI().createNew(newFolder, newHost) :
        APILocator.getIdentifierAPI().createNew(newFolder, newParentFolder);

    newFolder.setIdentifier(newIdentifier.getId());
    newFolder.setModDate(new Date());

    save(newFolder);
    return newFolder;
  }

  private boolean moveChildFolders(final Object folder, final List<Folder> subFolders)
      throws DotDataException, DotSecurityException {

    boolean moved = true;

    for (final Folder subFolder : subFolders) {

      moved &= move(subFolder, folder).isPresent();
    }

    return moved;
  }

  private void moveChildContentlets(final Folder folder,
      final User systemUser,
      final List<Contentlet> contentlets) throws DotDataException, DotSecurityException {

    for (final Contentlet contentlet : contentlets) {
      if (contentlet.isFileAsset()) {
        final FileAssetAPI fileAssetAPI = APILocator.getFileAssetAPI();
        fileAssetAPI.moveFile(contentlet, folder, systemUser, false);
      } else if (contentlet.isHTMLPage()) {
        HTMLPageAssetAPI pageAssetAPI = APILocator.getHTMLPageAssetAPI();
        pageAssetAPI.move(pageAssetAPI.fromContentlet(contentlet), folder, systemUser);
      } else {
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        contentletAPI.move(contentlet, systemUser, folder.getHost(), folder, false);
      }
    }
  }

  private void moveLinks(final Folder folder, final List links) throws DotDataException, DotSecurityException {

    for (final Object linkObject : links) {

      final Link link = (Link) linkObject;
      if (link.isWorking()) {

        LinkFactory.moveLink(link, folder);
      }
    }
  }

  private void cleanUpTheFolderCache(final Folder folder, final Identifier folderId) {

    if (folder.isShowOnMenu()) {

      CacheLocator.getNavToolCache().removeNav(folder.getHostId(), folder.getInode());
    }

    CacheLocator.getNavToolCache().removeNavByPath(folderId.getHostId(), folderId.getParentPath());
    this.folderCache.removeFolder(folder, folderId);
    CacheLocator.getIdentifierCache().removeFromCacheByIdentifier(folderId.getId());
  }


  @Override
  protected Optional<Folder> move(Folder folder, Folder destination) throws DotDataException, DotSecurityException {
    return move(folder, (Object) destination);
  }

  @Override
  protected Optional<Folder> move(final Folder folder, final Host destination) throws DotDataException, DotSecurityException {
    return move(folder, (Object) destination);
  }

  /**
   * Checks if folder1 is child of folder2
   *
   * @param childFolder
   * @param parentFolderParam
   * @return
   * @throws DotDataException
   * @throws DotIdentifierStateException
   * @throws DotSecurityException
   */
  protected boolean isChildFolder(final Folder childFolder, final Folder parentFolderParam)
      throws DotDataException, DotSecurityException {

    final Folder parentFolder = APILocator.getFolderAPI()
        .findParentFolder(childFolder, APILocator.getUserAPI().getSystemUser(), false);
    if (parentFolder == null || !InodeUtils.isSet(parentFolder.getInode())) {
			return false;
    } else {
			if (parentFolder.getInode().equalsIgnoreCase(parentFolderParam.getInode())) {
				return true;
			}
			return isChildFolder(parentFolder, parentFolderParam);
		}
  }

  @WrapInTransaction
  @Override
  protected boolean renameFolder(final Folder folder, final String newName, final User user,
      final boolean respectFrontEndPermissions) throws DotDataException, DotSecurityException {

    final Identifier ident = APILocator.getIdentifierAPI().loadFromDb(folder.getIdentifier());
    if (ident == null) {
      throw new DotDataException("Identifier not found in DB for folder inode='"
          + folder.getInode() + "' id='" + folder.getIdentifier() + "'");
    }
    final String parentPath = ident.getParentPath();
    final String hostId = ident.getHostId();
    // Use ident.getAssetName() (DB value), not folder.getName(): the folder object may already
    // carry the new name when called from saveFolder, which would make oldPath == newPath.
    final String oldPath = parentPath + ident.getAssetName() + "/";
    final String newPath = parentPath + newName + (newName.endsWith("/") ? "" : "/");

    final Host host = APILocator.getHostAPI().find(folder.getHostId(), user, respectFrontEndPermissions);
    final Folder existing = findFolderByPath(newPath, host);
    if (UtilMethods.isSet(existing.getInode()) && !folder.getIdentifier().equals(existing.getIdentifier())) {
      return false;
    }

    // Snapshot sub-folder data before any modification so cache eviction can target old paths.
    final List<Map<String, Object>> subFolderSnapshot = loadSubFolderSnapshot(oldPath, hostId);

    // Set the new name before creating the new record. @WrapInTransaction rolls back DB changes
    // on failure, but Java object state is not restored — restore the name explicitly on error.
    folder.setName(newName);

    // Each folder's identifier is deterministic (hash of assetType:hostname:parentPath:name),
    // so any folder whose path changes must get a new identifier via getNewFolderRecord().
    // Process parents before children (ascending path-length order) so findFolderByPath() inside
    // getNewFolderRecord() can resolve the new parent record, which must already exist in the DB.
    final Folder newFolder;
    try {
      newFolder = getNewFolderRecord(folder, user, parentPath, hostId);
    } catch (final DotDataException | RuntimeException e) {
      folder.setName(ident.getAssetName());
      if (DbConnectionFactory.isConstraintViolationException(e.getCause() != null ? e.getCause() : e)) {
        return false;
      }
      throw e;
    }

    final List<Map<String, Object>> sortedSnapshot = new ArrayList<>(subFolderSnapshot);
    sortedSnapshot.sort(Comparator.comparingInt(
        row -> ((String) row.get("parent_path") + (String) row.get("asset_name") + "/").length()));

    final List<String[]> childFolderInodePairs = new ArrayList<>();
    for (final Map<String, Object> row : sortedSnapshot) {
      final String oldChildInode = (String) row.get("inode");
      final String oldChildParentPath = (String) row.get("parent_path");
      final String newChildParentPath = newPath + oldChildParentPath.substring(oldPath.length());
      final Folder childFolder = find(oldChildInode);
      if (childFolder == null || !InodeUtils.isSet(childFolder.getInode())) {
        throw new DotDataException("Cannot find child folder with inode='" + oldChildInode
            + "' while renaming '" + oldPath + "' to '" + newPath + "'");
      }
      final Folder newChildFolder = getNewFolderRecord(childFolder, user, newChildParentPath, hostId);
      childFolderInodePairs.add(new String[]{oldChildInode, newChildFolder.getInode()});
    }

    // Evict caches before the bulk update so stale old-path entries are removed first.
    clearIdentifierCacheForSubtree(oldPath, hostId);
    evictContentletCacheForSubtree(oldPath, hostId);

    // Bulk-update parent_path for non-folder identifiers. Folder identifier rows are replaced
    // by the new records above and their old rows are deleted below, so they are excluded here.
    updateChildPaths(oldPath, newPath, hostId, subFolderSnapshot);

    // Bump contentlet version_ts so push-publish detects them as changed after the path move.
    bumpVersionTsForSubtree(newPath, hostId);

    clearIdentifierCacheForSubtree(newPath, hostId);
    evictSubFolderCache(subFolderSnapshot, hostId);

    CacheLocator.getNavToolCache().removeNav(folder.getHostId(), folder.getInode());
    CacheLocator.getNavToolCache().removeNavByPath(hostId, parentPath);
    for (final Map<String, Object> row : subFolderSnapshot) {
      CacheLocator.getNavToolCache().removeNav(hostId, (String) row.get("inode"));
    }

    updateOtherFolderReferences(newFolder.getInode(), folder.getInode());
    deleteOldChildFolders(childFolderInodePairs);
    delete(folder);

    folder.setInode(newFolder.getInode());
    folder.setIdentifier(newFolder.getIdentifier());
    // Evict the renamed folder's own cache entry (not covered by clearIdentifierCacheForSubtree,
    // which only targets items whose parent_path starts with newPath).
    CacheLocator.getIdentifierCache().removeFromCacheByIdentifier(newFolder.getIdentifier());

    return true;
  }

  /**
   * Transfers references and deletes old folder records for replaced child subfolders, in
   * child-before-parent (reverse) order. The DB trigger {@code check_child_assets} blocks deletion
   * of a folder when identifiers still reference its path as {@code parent_path}, so leaves must
   * be deleted before their parents.
   *
   * @param childFolderInodePairs list of {@code [oldInode, newInode]} pairs in parent-before-child order
   */
  private void deleteOldChildFolders(final List<String[]> childFolderInodePairs)
      throws DotDataException {
    final List<String[]> reversed = new ArrayList<>(childFolderInodePairs);
    Collections.reverse(reversed);
    for (final String[] pair : reversed) {
      updateOtherFolderReferences(pair[1], pair[0]);
      final Folder oldChildFolder = find(pair[0]);
      if (oldChildFolder != null && InodeUtils.isSet(oldChildFolder.getInode())) {
        delete(oldChildFolder);
      }
    }
  }

  /**
   * Captures inode, parent_path, and asset_name of every sub-folder whose path falls under
   * {@code oldPath}. Must be called <em>before</em> the bulk path update so old path data is
   * available for targeted {@link FolderCache} eviction afterwards.
   */
  private List<Map<String, Object>> loadSubFolderSnapshot(final String oldPath, final String hostId)
      throws DotDataException {

    // LIKE 'oldPath%' covers ALL levels of the sub-tree:
    //   - Direct sub-folders have parent_path = oldPath (e.g. '/original/').
    //     Since oldPath ends with '/' and '%' matches zero or more chars,
    //     '/original/' LIKE '/original/%' evaluates to TRUE.
    //   - Deeper descendants have parent_path starting with oldPath (e.g. '/original/sub/').
    // Escape '%' and '_' so folder names containing those characters do not widen the match.
    final String likeParam = escapeLikeParam(oldPath) + "%";
    return new DotConnect()
        .setSQL("SELECT f.inode, i.parent_path, i.asset_name"
            + " FROM identifier i JOIN folder f ON f.identifier = i.id"
            + " WHERE i.parent_path LIKE ? ESCAPE '\\' AND i.asset_type = 'folder' AND i.host_inode = ?")
        .addParam(likeParam)
        .addParam(hostId)
        .loadResults();
  }

  /**
   * Bulk-updates the {@code parent_path} column for every identifier in the sub-tree rooted at
   * {@code startOldPath}, processing parent folders before their children to satisfy the
   * {@code identifier_parent_path_trigger} ordering requirement.
   * <p>
   * Instead of issuing a SELECT after each UPDATE to discover the next folder level (which would
   * cause N+1 queries for wide, flat trees), this method derives all (oldPath → newPath) pairs
   * directly from {@code subFolderSnapshot} — data already loaded before the rename began.
   * Each pair is sorted by ascending old-path length: a parent path is always shorter than any
   * of its descendant paths, so length-order guarantees the required parent-before-child sequence.
   */
  private void updateChildPaths(final String startOldPath, final String startNewPath,
      final String hostId, final List<Map<String, Object>> subFolderSnapshot)
      throws DotDataException {

    // Build the complete set of (oldPath, newPath) pairs: root level plus one pair per sub-folder.
    final List<String[]> levels = new ArrayList<>();
    levels.add(new String[]{startOldPath, startNewPath});

    for (final Map<String, Object> row : subFolderSnapshot) {
      final String oldFolderPath = (String) row.get("parent_path") + (String) row.get("asset_name") + "/";
      // Guard against corrupt snapshot data: every sub-folder path must start with the root.
      // Throw rather than skipping: a partial rename that returns true is worse than a rollback,
      // because the caller has no way to know which levels were left with a stale parent_path.
      if (!oldFolderPath.startsWith(startOldPath)) {
        throw new DotDataException(
            "Rename aborted: sub-folder path '"
            + oldFolderPath.replaceAll("[\\r\\n]", " ")
            + "' does not start with expected prefix '"
            + startOldPath.replaceAll("[\\r\\n]", " ")
            + "'. This indicates corrupt parent_path data in the identifier table."
            + " The transaction will be rolled back.");
      }
      // Compute new path by replacing the startOldPath prefix with startNewPath
      final String newFolderPath = startNewPath + oldFolderPath.substring(startOldPath.length());
      levels.add(new String[]{oldFolderPath, newFolderPath});
    }

    // Sort by old-path length ascending: shorter (shallower) paths are processed first,
    // ensuring every parent folder's parent_path is updated before its children's rows are touched.
    levels.sort(Comparator.comparingInt(pair -> pair[0].length()));

    for (final String[] pair : levels) {
      // Exclude folder identifiers: child subfolder records are replaced by new ones created via
      // getNewFolderRecord() (which generates a new deterministic UUID for the changed path) and
      // their old identifier rows are deleted afterwards. Updating them here would produce a stale
      // row that conflicts with the newly-created identifier at the same path.
      new DotConnect().executeUpdate(
          "UPDATE identifier SET parent_path = ? WHERE parent_path = ? AND host_inode = ? AND asset_type != 'folder'",
          pair[1], pair[0], hostId);
    }
  }

  /**
   * Evicts path-keyed entries from the folder cache for all sub-folders captured in the snapshot.
   * Uses the <em>old</em> path data so stale cache entries keyed by the pre-rename paths are
   * properly removed.
   * <p>
   * The stub objects set only the fields consumed by {@link com.dotmarketing.cache.FolderCacheImpl#removeFolder}:
   * {@code inode} and {@code hostId} for the inode-keyed cache entry, and {@code parentPath} +
   * {@code assetName} (via {@link com.dotmarketing.beans.Identifier#getPath()}) for the
   * path-keyed cache entry. No other fields are needed.
   */
  private void evictSubFolderCache(final List<Map<String, Object>> subFolderSnapshot,
      final String hostId) {

    for (final Map<String, Object> row : subFolderSnapshot) {
      final Folder stub = new Folder();
      stub.setInode((String) row.get("inode"));
      stub.setHostId(hostId);

      final Identifier oldIdent = new Identifier();
      oldIdent.setParentPath((String) row.get("parent_path"));
      oldIdent.setAssetName((String) row.get("asset_name"));
      // assetType must be "folder" so Identifier.getPath() appends a trailing '/'.
      // FolderCacheImpl keys path-based entries with the slash, so without this the eviction
      // silently misses every sub-folder path-keyed cache entry.
      oldIdent.setAssetType("folder");

      folderCache.removeFolder(stub, oldIdent);
    }
  }

  /**
   * Evicts from the identifier cache every identifier whose {@code parent_path} starts
   * with {@code rootPath} (direct children and all nested descendants).
   * A single prefix query covers all levels in one round-trip.
   * <p>
   * Uses {@link com.dotmarketing.business.IdentifierCache#removeFromCacheDirect} which removes
   * both the UUID-keyed and URI-keyed cache entries without triggering the recursive
   * {@code findByParentPath} DB call that the standard eviction methods perform for folder
   * entries. Because this method already iterates the full flat set of descendants, the
   * recursive re-discovery would cause O(F × depth) redundant DB queries.
   * <p>
   * Note: {@code ContentletCache} is keyed by inode but stores derived fields such as the folder
   * inode; that stale field is handled separately by {@link #evictContentletCacheForSubtree}.
   */
  private void clearIdentifierCacheForSubtree(final String rootPath, final String hostId)
      throws DotDataException {

    // rootPath always ends with '/' so it cannot accidentally match sibling folder paths.
    // Escape '%' and '_' so folder names containing those characters do not widen the match.
    final String likeParam = escapeLikeParam(rootPath) + "%";
    final List<Map<String, Object>> rows = new DotConnect()
        .setSQL("SELECT id, parent_path, asset_name FROM identifier"
            + " WHERE parent_path LIKE ? ESCAPE '\\' AND host_inode = ?")
        .addParam(likeParam)
        .addParam(hostId)
        .loadResults();

    final IdentifierCache identifierCache = CacheLocator.getIdentifierCache();
    for (final Map<String, Object> row : rows) {
      final String oldUri = (String) row.get("parent_path") + (String) row.get("asset_name");
      identifierCache.removeFromCacheDirect((String) row.get("id"), hostId, oldUri);
    }
  }

  /**
   * Bumps {@code contentlet_version_info.version_ts} for every contentlet whose identifier lives
   * directly under or anywhere beneath {@code rootPath} in the given host. Must be called after
   * {@link #updateChildPaths} so the query finds identifiers by their post-rename
   * {@code parent_path}. Without this bump, push-publish compares the last push date against an
   * unchanged {@code version_ts} and concludes the content has not changed since the last push,
   * excluding it from the bundle even though its folder path changed. The old
   * {@code contentletAPI.move()} approach updated {@code version_ts} per-contentlet for this
   * exact reason; this method replicates that behaviour in bulk.
   */
  private void bumpVersionTsForSubtree(final String rootPath, final String hostId)
      throws DotDataException {

    final String likeParam = escapeLikeParam(rootPath) + "%";

    // Collect affected identifiers first. Using these IDs for the subsequent UPDATE
    // avoids a duplicate scan of the identifier table and lets us pass an explicit list
    // to the UPDATE rather than a correlated sub-select.
    final List<Map<String, Object>> affected = new DotConnect()
        .setSQL("SELECT i.id FROM identifier i"
            + " WHERE i.parent_path LIKE ? ESCAPE '\\'"
            + "   AND i.host_inode = ?"
            + "   AND i.asset_type != 'folder'")
        .addParam(likeParam)
        .addParam(hostId)
        .loadObjectResults();

    if (affected.isEmpty()) {
      return;
    }

    final List<String> ids = affected.stream()
        .map(r -> (String) r.get("id"))
        .collect(Collectors.toList());

    // Bump version_ts only for the DEFAULT variant rows. Push-publish reads the DEFAULT
    // variant exclusively, and restricting the UPDATE to DEFAULT keeps its scope exactly
    // aligned with the cache eviction below (which also targets DEFAULT).
    final String placeholders = ids.stream().map(id -> "?").collect(Collectors.joining(", "));
    final Object[] params = new Object[ids.size() + 2];
    params[0] = new Date();
    for (int i = 0; i < ids.size(); i++) {
      params[i + 1] = ids.get(i);
    }
    params[ids.size() + 1] = VariantAPI.DEFAULT_VARIANT.name();
    new DotConnect().executeUpdate(
        "UPDATE contentlet_version_info SET version_ts = ?"
            + " WHERE identifier IN (" + placeholders + ")"
            + "   AND variant_id = ?",
        params);

    // Evict the DEFAULT-variant cache entries so the next read goes to DB and picks up
    // the bumped version_ts. Without this, DependencyModDateUtil.chekModDateInAllLanguages
    // reads the per-language pre-bump value from cache and incorrectly excludes the content
    // from the bundle.
    final IdentifierCache identifierCache = CacheLocator.getIdentifierCache();
    final List<Language> languages = APILocator.getLanguageAPI().getLanguages();
    for (final String identifierId : ids) {
      for (final Language lang : languages) {
        identifierCache.removeContentletVersionInfoToCache(identifierId, lang.getId());
      }
    }

    Logger.debug(FolderFactoryImpl.class,
        "Bumped version_ts and evicted version-info cache for " + ids.size()
            + " contentlet(s) under path '" + rootPath + "'");
  }

  /**
   * Evicts from the contentlet cache every contentlet whose identifier lives directly under or
   * anywhere beneath {@code rootPath} in the given host. Must be called before
   * {@link #updateChildPaths} so the query matches identifiers by their pre-rename
   * {@code parent_path}. Without this eviction, cached contentlets carry the old (now-deleted)
   * folder inode in their {@code folder} field; the next load from DB re-derives the correct
   * value via {@code ContentletTransformer} from the updated {@code identifier.parent_path}.
   */
  private void evictContentletCacheForSubtree(final String rootPath, final String hostId)
      throws DotDataException {

    final String likeParam = escapeLikeParam(rootPath) + "%";
    final List<Map<String, Object>> rows = new DotConnect()
        .setSQL("SELECT c.inode FROM identifier i"
            + " JOIN contentlet c ON c.identifier = i.id"
            + " WHERE i.parent_path LIKE ? ESCAPE '\\'"
            + "   AND i.host_inode = ?"
            + "   AND i.asset_type != 'folder'")
        .addParam(likeParam)
        .addParam(hostId)
        .loadObjectResults();

    for (final Map<String, Object> row : rows) {
      CacheLocator.getContentletCache().remove((String) row.get("inode"));
    }

    Logger.debug(FolderFactoryImpl.class,
        "Evicted " + rows.size() + " contentlet cache entries under path '" + rootPath + "'");
  }

  /**
   * Escapes {@code %} and {@code _} in a SQL LIKE pattern parameter so that folder names
   * containing those characters do not unintentionally widen the match.
   * Use in conjunction with {@code ESCAPE '\\'} in the LIKE clause.
   */
  private static String escapeLikeParam(final String value) {
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  protected boolean matchFilter(Folder folder, String fileName) {
    // return value
    Perl5Matcher p5m = new Perl5Matcher();
    Perl5Compiler p5c = new Perl5Compiler();
    boolean match = false;
    try {
      // Obtain the filters
      String filesMasks = folder.getFilesMasks();
      filesMasks = (filesMasks != null ? filesMasks.trim() : filesMasks);

      if (UtilMethods.isSet(filesMasks)) {
        String[] filesMasksArray = filesMasks.split(",");

        // Try to match the filters
        for (String s : filesMasksArray) {
          String regex = s;
          regex = regex.replace(".", "\\.");
          regex = regex.replace("*", ".*");
          regex = "^" + regex.trim() + "$";
          Pattern pattern = p5c.compile(regex, Perl5Compiler.CASE_INSENSITIVE_MASK);
          match = match || p5m.matches(fileName, pattern);
          if (match) {
            break;
          }
        }
      } else {
        match = true;
      }
    } catch (Exception ex) {
      Logger.debug(FolderFactoryImpl.class, ex.toString());
    }
    return match;
  }


  // http://jira.dotmarketing.net/browse/DOTCMS-3232
  protected Folder findSystemFolder() throws DotDataException {
    Folder folder = find(SYSTEM_FOLDER);
    if (folder != null && UtilMethods.isSet(folder.getInode())) {
      return folder;
    }
    folder = find(OLD_SYSTEM_FOLDER_ID);

    if (folder != null && UtilMethods.isSet(folder.getInode())) {
      return folder;
    }

    try (Connection connection = DbConnectionFactory.getConnection()) {
      DotConnect dc = new DotConnect();

      // if we are missing the system folder identifier, we create it
      if (dc.setSQL(
          "select count(id) as test from identifier where asset_type='folder' and host_inode='SYSTEM_HOST' "
              + "and ( id = 'SYSTEM_FOLDER' or lower(parent_path) = '/system folder') ").loadInt("test", connection)
          == 0) {

        dc.setSQL("INSERT INTO IDENTIFIER(ID,PARENT_PATH,ASSET_NAME,HOST_INODE,ASSET_TYPE) VALUES(?,?,?,?,?)");
        dc.addParam(SYSTEM_FOLDER);
        dc.addParam(SYSTEM_FOLDER_PARENT_PATH);
        dc.addParam(SYSTEM_FOLDER_ASSET_NAME);
        dc.addParam(Host.SYSTEM_HOST);
        dc.addParam(Folder.FOLDER_TYPE);
        dc.loadResult(connection);

      }

      // if we are missing the system folder folder, we create it
      if (dc.setSQL(
              "select count(inode) as test from folder where inode='SYSTEM_FOLDER' or identifier='SYSTEM_FOLDER' ")
          .loadInt("test", connection) == 0) {

        String INSERT_FOLDER = "INSERT INTO FOLDER (INODE, NAME, TITLE, SHOW_ON_MENU, SORT_ORDER, IDENTIFIER, OWNER, IDATE) VALUES (?,?,?,?,?,?,?,?)";
        dc.setSQL(INSERT_FOLDER);
        dc.addParam(SYSTEM_FOLDER);
        dc.addParam(SYSTEM_FOLDER_ASSET_NAME);
        dc.addParam(SYSTEM_FOLDER_ASSET_NAME);
        dc.addParam(false);
        dc.addParam(0);
        dc.addParam(SYSTEM_FOLDER);
        dc.addParam(APILocator.getUserAPI().getSystemUser().getUserId());
        dc.addParam(new Date());
        dc.loadResult(connection);

      }
      return find(SYSTEM_FOLDER);


    } catch (Exception e) {
      Logger.error(FolderFactoryImpl.class, "Error while trying to find or create the system folder", e);
      throw new DotDataException("Error while trying to find or create the system folder", e);
    }


  }

  @SuppressWarnings("unchecked")
  protected List<Folder> findFoldersByHost(Host host) {
    List<Folder> folderList = getSubFolders(null, "/", host.getIdentifier(), null);
    Collections.sort(folderList, (Folder folder1, Folder folder2) -> folder1.getName()
        .compareToIgnoreCase(folder2.getName()));
    return folderList;
  }

  @SuppressWarnings("unchecked")
  protected List<Folder> findThemesByHost(Host host) {
    List<Folder> folderList = getSubFolders(null, "/application/themes/", host.getIdentifier(),
        null);
    Collections.sort(folderList, (Folder folder1, Folder folder2) -> folder1.getName()
        .compareToIgnoreCase(folder2.getName()));
    return folderList;
  }

  protected List<Treeable> getChildrenClass(Folder parent, Class clazz) throws DotStateException, DotDataException {
    return getChildrenClass(parent, clazz, null, null, 0, 1000);
  }

  protected List<Treeable> getChildrenClass(Host host, Class clazz) throws DotStateException, DotDataException {
    Identifier identifier = APILocator.getIdentifierAPI().find(host.getIdentifier());
    return getChildrenClass(identifier, clazz, null, null, 0, 1000);
  }

  protected List<Treeable> getChildrenClass(Host host, Class clazz, ChildrenCondition cond)
      throws DotStateException, DotDataException {
    Identifier identifier = APILocator.getIdentifierAPI().find(host.getIdentifier());
    return getChildrenClass(identifier, clazz, cond, null, 0, 1000);
  }

  protected List<Treeable> getChildrenClass(Folder parent, Class clazz, ChildrenCondition cond)
      throws DotStateException, DotDataException {
    return getChildrenClass(parent, clazz, cond, null, 0, 1000);
  }

  protected List<Treeable> getChildrenClass(Folder parent, Class clazz, ChildrenCondition cond, String orderBy,
      int offset, int limit) throws DotStateException, DotDataException {
    Identifier identifier = APILocator.getIdentifierAPI().find(parent.getIdentifier());
    return getChildrenClass(identifier, clazz, cond, orderBy, offset, limit);
  }

  protected List<Treeable> getChildrenClass(Identifier identifier, Class clazz, ChildrenCondition cond, String orderBy,
      int offset, int limit) throws DotStateException, DotDataException {

    String tableName;
    String type;

    try {
      Object obj;
      obj = clazz.newInstance();

      if (obj instanceof Treeable) {
        type = ((Treeable) obj).getType();
        tableName = Inode.Type.valueOf(type.toUpperCase()).getTableName();
      } else {
        throw new DotStateException("Unable to getType for child asset");
      }
    } catch (InstantiationException e) {
      throw new DotStateException("Unable to getType for child asset");
    } catch (IllegalAccessException e) {
      throw new DotStateException("Unable to getType for child asset");
    }

    String versionTable = Inode.Type.valueOf(type.toUpperCase()).getVersionTableName();

    DotConnect dc = new DotConnect();
    String sql = "SELECT " + tableName + ".*" + " from " + tableName + " " + tableName + ",  inode " + tableName
        + "_1_, identifier " + tableName + "_2_ ";

    if (cond != null && versionTable != null && (cond.deleted != null || cond.working != null || cond.live != null)) {
      sql += ", " + versionTable;
    }

    sql += " where " + tableName + "_2_.parent_path = ? " + " and " + tableName
        + ".identifier = " + tableName + "_2_.id " + " and " + tableName + "_1_.inode = " + tableName + ".inode "
        + " and ";

    if (cond != null && cond.deleted != null) {
      if (versionTable != null) {
        sql += versionTable + ".deleted=" + ((cond.deleted) ? DbConnectionFactory.getDBTrue()
            : DbConnectionFactory.getDBFalse()) + " and ";
      } else {
        sql += " deleted=" + ((cond.deleted) ? DbConnectionFactory.getDBTrue() : DbConnectionFactory.getDBFalse())
            + " and ";
      }
    }

    if (cond != null && cond.working != null) {
      if (versionTable != null) {
        sql += versionTable + ".working_inode" + (cond.working ? "=" : "<>") + tableName + "_1_.inode and ";
      } else {
        sql += " working=" + ((cond.working) ? DbConnectionFactory.getDBTrue() : DbConnectionFactory.getDBFalse())
            + " and ";
      }
    }

    if (cond != null && cond.live != null) {
      if (versionTable != null) {
        sql += versionTable + ".live_inode" + (cond.live ? "=" : "<>") + tableName + "_1_.inode and ";
      } else {
        sql += " live=" + ((cond.live) ? DbConnectionFactory.getDBTrue() : DbConnectionFactory.getDBFalse()) + " and ";
      }
    }

    sql += tableName + "_1_.type = '" + tableName + "' " + " and " + tableName + "_2_.host_inode = ? ";

    if (cond != null && cond.showOnMenu != null) {
      sql += " and " + tableName + ".show_on_menu=" + (cond.showOnMenu ? DbConnectionFactory.getDBTrue()
          : DbConnectionFactory.getDBFalse());
    }

    if (orderBy != null) {
      sql = sql + " order by " + orderBy;
    }

    dc.setSQL(sql);
    dc.setStartRow(offset);
    dc.setMaxRows(limit);
    if (identifier.getHostId().equals(Host.SYSTEM_HOST)) {
      dc.addParam("/");
      dc.addParam(identifier.getId());
    } else {
      dc.addParam(identifier.getURI() + "/");
      dc.addParam(identifier.getHostId());
    }

    try {

      DBTransformer transformer = TransformerLocator.createDBTransformer(dc.loadObjectResults(), clazz);

      if (transformer != null) {
        return transformer.asList();
      }


    } catch (Exception e) {
      Logger.warn(this, e.getMessage(), e);
    }

    return Collections.emptyList();
  }

  @Override
  public void save(Folder folder) throws DotDataException {
    if (folder != null && OLD_SYSTEM_FOLDER_ID.equals(folder.getIdentifier())) {
      folder.setIdentifier(SYSTEM_FOLDER);
    }
    Identifier id = APILocator.getIdentifierAPI().find(folder.getIdentifier());
    if (UtilMethods.isSet(() -> id.getId())) {
      folderCache.removeFolder(folder, id);
    }
    upsertFolder(folder);
  }


  private void upsertFolder(final Folder folder) throws DotDataException {

    final UpsertCommand upsertContentletCommand = UpsertCommandFactory.getUpsertCommand();
    final SimpleMapAppContext replacements = new SimpleMapAppContext();

    replacements.setAttribute(QueryReplacements.TABLE, "folder");
    replacements.setAttribute(QueryReplacements.CONDITIONAL_COLUMN, "inode");
    replacements.setAttribute(QueryReplacements.CONDITIONAL_VALUE, folder.getInode());
    replacements.setAttribute(QueryReplacements.EXTRA_COLUMNS, UPSERT_EXTRA_COLUMNS);

    final List<Object> parameters = new ArrayList<>();
    parameters.add(folder.getInode());
    parameters.add(folder.getName());
    parameters.add(folder.getTitle());
    parameters.add(folder.isShowOnMenu());
    parameters.add(folder.getSortOrder());
    parameters.add(folder.getFilesMasks());
    parameters.add(folder.getIdentifier());
    parameters.add(folder.getDefaultFileType());
    parameters.add(new Timestamp(folder.getModDate().getTime()));
    parameters.add(folder.getOwner());

    if (!UtilMethods.isSet(folder.getIDate())) {
      parameters.add(new Timestamp(new Date().getTime()));
    } else {
      parameters.add(new Timestamp(folder.getIDate().getTime()));
    }

    Logger.info(this, "Upserting Folder: " + folder.getPath());

    upsertContentletCommand.execute(new DotConnect(), replacements, parameters.toArray());

  }

  @Override
  public void updateUserReferences(final String userId, final String replacementUserId)
      throws DotDataException {
    final DotConnect dc = new DotConnect();

    final List<String> folderIDs = dc.setSQL("select identifier from folder where owner=?")
        .addParam(userId)
        .loadObjectResults()
        .stream()
        .map(r -> String.valueOf(r.get("identifier")))
        .collect(Collectors.toList());

    dc.setSQL("update folder set owner = ? where owner = ?");
    dc.addParam(replacementUserId);
    dc.addParam(userId);
    dc.loadResult();

    folderIDs.forEach(id -> folderCache.removeFolder(Try.of(() -> find(id)).get(),
        Try.of(() -> APILocator.getIdentifierAPI().find(id)).get()));
  }

  @Override
  public List<Map<String, Object>> getContentReport(final String folderPath,
      final String siteId,
      final String orderBy,
      final String orderDirection,
      final int limit, final int offset) throws DotDataException {
    final DotConnect dc = new DotConnect()
        .setSQL(String.format(GET_CONTENT_REPORT, IdentifierFactory.ASSET_SUBTYPE,
            orderDirection))
        .setMaxRows(limit)
        .setStartRow(offset)
        .addParam(Identifier.ASSET_TYPE_CONTENTLET)
        .addParam(folderPath + StringPool.PERCENT)
        .addParam(siteId);
    return dc.loadObjectResults();
  }

  @Override
  public int getContentTypeCount(final String folderPath, final String siteId) throws DotDataException {
    final DotConnect dc = new DotConnect()
        .setSQL(GET_CONTENT_TYPE_COUNT)
        .addParam(Identifier.ASSET_TYPE_CONTENTLET)
        .addParam(folderPath + StringPool.PERCENT)
        .addParam(siteId);
    return dc.getInt("count");
  }

}
