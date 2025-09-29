package com.dotmarketing.portlets.containers.business;

import static com.dotcms.util.FunctionUtils.ifOrElse;
import static com.dotmarketing.util.StringUtils.builder;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rendering.velocity.services.ContainerLoader;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.transform.TransformerLocator;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.Inode.Type;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.ContainerVersionInfo;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.*;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation class for the {@link ContainerFactory}. This class provides database-level access to information
 * related to Containers in the current dotCMS content repository.
 *
 * @author root
 * @since Mar 22nd, 2012
 */
public class ContainerFactoryImpl implements ContainerFactory {

	static ContainerCache  containerCache       = CacheLocator.getContainerCache();
	private final PermissionAPI  permissionAPI  = APILocator.getPermissionAPI();
	private final FolderAPI      folderAPI      = APILocator.getFolderAPI();
    private final FileAssetAPI   fileAssetAPI   = APILocator.getFileAssetAPI();
    private final HostAPI        hostAPI        = APILocator.getHostAPI();
	private final IdentifierAPI  identifierAPI  = APILocator.getIdentifierAPI();

	@Override
	public Container find(final String inode) throws DotStateException, DotDataException {

		Container container = containerCache.get(inode);

		if(container==null){
			final List<Map<String, Object>> containerResults = new DotConnect()
					.setSQL(ContainerSQL.FIND_BY_INODE)
					.addParam(inode)
					.loadObjectResults();
			if (containerResults.isEmpty()) {
				Logger.debug(this, "Container with inode: " + inode + " not found");
				return null;
			}

			container = TransformerLocator.createContainerTransformer(containerResults).findFirst();

			if(container != null && container.getInode() != null) {
				containerCache.add(container.getInode(), container);
			}
		}

		return container;
	}

	@WrapInTransaction
	public void save(final Container container) throws DotDataException {
		save(container, UUIDGenerator.generateUuid());
	}

	@WrapInTransaction
	public void save(final Container container, final String existingId) throws DotDataException {
		if(!UtilMethods.isSet(container.getIdentifier())){
			throw new DotStateException("Cannot save a container without an Identifier");
		}

		if(!UtilMethods.isSet(container.getInode())) {
			container.setInode(UUIDGenerator.generateUuid());
		}

		if(!UtilMethods.isSet(find(container.getInode()))) {
			insertInodeInDB(container);
			insertContainerInDB(container);
		} else {
			updateInodeInDB(container);
			updateContainerInDB(container);
		}

		containerCache.add(container.getInode(), container);
		new ContainerLoader().invalidate(container);
	}

	private void executeQueryWithData(final String SQL, final String inode, final String code, final String preLoop,
			final String postLoop, final boolean isShowOnMenu, final String title,
			final Date modDate, final String modUser, final int sortOrder, final String friendlyName, final int maxContentlets,
			final boolean isUseDiv, final boolean isStaticify,
			final String sortContentletsBy, final String luceneQuery, final String notes, final String identifier, boolean createNew)
			throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(SQL);

		if(createNew) {
			dc.addParam(inode);
		}

		dc.addParam(code);
		dc.addParam(preLoop);
		dc.addParam(postLoop);
		dc.addParam(isShowOnMenu);
		dc.addParam(title);
		dc.addParam(modDate);
		dc.addParam(modUser);
		dc.addParam(sortOrder);
		dc.addParam(friendlyName);
		dc.addParam(maxContentlets);
		dc.addParam(isUseDiv);
		dc.addParam(isStaticify);
		dc.addParam(sortContentletsBy);
		dc.addParam(luceneQuery);
		dc.addParam(notes);
		dc.addParam(identifier);

		if(!createNew) {
			dc.addParam(inode);
		}

		dc.loadResult();
	}

	private void executeQueryWithData(final String SQL, final String inode, final Date iDate, final String owner, boolean createNew)
			throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(SQL);

		if(createNew) {
			dc.addParam(inode);
		}

		dc.addParam(iDate);
		dc.addParam(owner);

		if(!createNew) {
			dc.addParam(inode);
		}

		dc.loadResult();
	}
	private void insertContainerInDB(final Container container) throws DotDataException {
		executeQueryWithData(ContainerSQL.INSERT_CONTAINER, container.getInode(),
				container.getCode(), container.getPreLoop(), container.getPostLoop(),
				container.isShowOnMenu(), container.getTitle(), container.getModDate(),
				container.getModUser(), container.getSortOrder(),
				container.getFriendlyName(), container.getMaxContentlets(), container.isUseDiv(),
				container.isStaticify(), container.getSortContentletsBy(),
				container.getLuceneQuery(), container.getNotes(), container.getIdentifier(), true);
	}
	private void updateContainerInDB(final Container container) throws DotDataException {
		executeQueryWithData(ContainerSQL.UPDATE_CONTAINER, container.getInode(),
				container.getCode(), container.getPreLoop(), container.getPostLoop(),
				container.isShowOnMenu(), container.getTitle(), container.getModDate(),
				container.getModUser(), container.getSortOrder(),
				container.getFriendlyName(), container.getMaxContentlets(), container.isUseDiv(),
				container.isStaticify(), container.getSortContentletsBy(),
				container.getLuceneQuery(), container.getNotes(), container.getIdentifier(), false);
	}

	private void insertInodeInDB(final Container container) throws DotDataException{
		executeQueryWithData(ContainerSQL.INSERT_INODE, container.getInode(), new Date(), container.getOwner(), true);
	}
	private void updateInodeInDB(final Container container) throws DotDataException{
		executeQueryWithData(ContainerSQL.UPDATE_INODE, container.getInode(), new Date(), container.getOwner(), false);
	}
	@Override
	public List<Container> findContainersUnder(final Host parentPermissionable) throws DotDataException {

		final List<Container> containers = new ArrayList<>();
		final String tableName 			 = Type.CONTAINERS.getTableName();
		final StringBuilder sql = new StringBuilder();

		sql.append("SELECT ")
			.append(tableName)
			.append(".*, dot_containers_1_.* from ")
			.append(tableName)
			.append(", inode dot_containers_1_, identifier ident, container_version_info vv where vv.working_inode=")
			.append(tableName)
			.append(".inode and ")
			.append(tableName)
			.append(".inode = dot_containers_1_.inode and vv.identifier = ident.id and host_inode = '")
			.append(parentPermissionable.getIdentifier() )
			.append('\'');

		try {

			containers.addAll(this.findHostContainers(parentPermissionable, APILocator.systemUser(), false));
		} catch (DotSecurityException e) {
			throw new DotDataException(e);
		}

		containers.addAll(TransformerLocator.createContainerTransformer
				(new DotConnect().setSQL(sql.toString()).loadObjectResults()).asList());

		return containers;
	}

	@Override
	public List<Container> findAllContainers(final Host currentHost) throws DotDataException {

		final List<Container> containers = new ArrayList<>();

		try {
			containers.addAll(this.findAllHostFolderAssetContainers(currentHost));
		} catch (DotSecurityException e) {
			throw new DotDataException(e);
		}

		containers.addAll(this.findAllHostDataBaseContainers());

		return containers;
	}

	/*
	* Finds all containers for all sites on the data base.
	 */
	private List<Container> findAllHostDataBaseContainers() throws DotDataException {

		final String tableName 			 = Type.CONTAINERS.getTableName();

		final StringBuilder sql = new StringBuilder()
				.append("SELECT ")
				.append(tableName)
				.append(".*, dot_containers_1_.* from ")
				.append(tableName)
				.append(", inode dot_containers_1_, container_version_info vv where vv.working_inode= ")
				.append(tableName)
				.append(".inode and ")
				.append(tableName)
				.append(".inode = dot_containers_1_.inode and dot_containers_1_.type='containers' order by ")
				.append(tableName)
				.append(".title");

		return TransformerLocator.createContainerTransformer
				(new DotConnect().setSQL(sql.toString()).loadObjectResults()).asList();
	}

	@Override
	public void deleteContainerByInode(String containerInode) throws DotDataException {
		deleteContainerInDB(containerInode);
		deleteInodeInDB(containerInode);
	}

	@Override
	public List<ContainerStructure> getRelatedContainerContentTypes(final Container container) throws DotHibernateException {
		final ImmutableList.Builder<ContainerStructure> builder =
				new ImmutableList.Builder<>();
		final HibernateUtil dh = new HibernateUtil(ContainerStructure.class);
		dh.setSQLQuery("select {container_structures.*} from container_structures " +
				"where container_structures.container_id = ? " +
				"and container_structures.container_inode = ?");
		dh.setParam(container.getIdentifier());
		dh.setParam(container.getInode());
		builder.addAll(dh.list());
		final List<ContainerStructure> containerStructures = builder.build();
		return containerStructures;
	}

	private void deleteInodeInDB(final String inode) throws DotDataException{
		DotConnect dc = new DotConnect();
		dc.setSQL("delete from inode where inode = ? and type='containers'");
		dc.addParam(inode);
		dc.loadResult();
	}

	private void deleteContainerInDB(final String inode) throws DotDataException{
		DotConnect dc = new DotConnect();
		dc.setSQL("delete from " + Type.CONTAINERS.getTableName() + " where inode = ?");
		dc.addParam(inode);
		dc.loadResult();
	}

	@Override
	public Container getLiveContainerByFolderPath(final String path, final Host host, final User user,
												  final boolean respectFrontEndPermissions) throws DotSecurityException, DotDataException {

		final String folderHostId           = host.getIdentifier();
		final Optional<Host> currentHostOpt = HostUtil.tryToFindCurrentHost(user);
		boolean includeHostOnPath           = false;

		if (currentHostOpt.isPresent()) {

			includeHostOnPath = !folderHostId.equals(currentHostOpt.get().getIdentifier());
		}

		return this.getContainerByFolder(host, this.folderAPI.findFolderByPath(path, host, user, respectFrontEndPermissions), user,true, includeHostOnPath);
	}

    @Override
    public Container getWorkingContainerByFolderPath(final String path, final Host host, final User user,
                                                     final boolean respectFrontEndPermissions) throws DotSecurityException, DotDataException {
		return getContainerByFolderPath(path, host, user, false, respectFrontEndPermissions);
	}

	@Override
	public Container getContainerByFolderPath(final String path, final Host host, final User user, final boolean live,
													 final boolean respectFrontEndPermissions) throws DotSecurityException, DotDataException {
		final String folderHostId           = host.getIdentifier();
		final Optional<Host> currentHostOpt = HostUtil.tryToFindCurrentHost(user);
		boolean includeHostOnPath           = false;

		if (currentHostOpt.isPresent()) {

			includeHostOnPath = !folderHostId.equals(currentHostOpt.get().getIdentifier());
		}

        return this.getContainerByFolder(host, this.folderAPI.findFolderByPath(path, host, user, respectFrontEndPermissions), user,live, includeHostOnPath);
    }

	public Container getWorkingArchiveContainerByFolderPath(String path, Host host, User user,
													 boolean respectFrontEndPermissions) throws DotSecurityException, DotDataException {

		final boolean live					= false;
		final String folderHostId           = host.getIdentifier();
		final Optional<Host> currentHostOpt = HostUtil.tryToFindCurrentHost(user);
		boolean includeHostOnPath           = false;

		if (currentHostOpt.isPresent()) {

			includeHostOnPath = !folderHostId.equals(currentHostOpt.get().getIdentifier());
		}

		return this.getContainerArchiveByFolder(host, this.folderAPI.findFolderByPath(path, host, user, respectFrontEndPermissions), user,live, includeHostOnPath);
	}

	@Override
	public Container getContainerArchiveByFolder(final Host host, final Folder folder, final User user, final boolean showLive, final boolean includeHostOnPath) throws DotSecurityException, DotDataException {

		if (!this.isValidContainerPath (folder)) {

			throw new NotFoundInDbException("On getting the container by folder, the folder: " + (folder != null ? folder.getPath() : "Unknown" ) +
					" is not valid, it must be under: " + Constants.CONTAINER_FOLDER_PATH + " and must have a child file asset called: " +
					Constants.CONTAINER_META_INFO_FILE_NAME);
		}
		final Identifier identifier = getContainerAsset(host, folder);
		if(identifier==null) {

			throw new NotFoundInDbException("no container found under: " + folder.getPath() );
		}

		final Optional<ContentletVersionInfo> contentletVersionInfo = APILocator.getVersionableAPI().
				getContentletVersionInfo(identifier.getId(), APILocator.getLanguageAPI().getDefaultLanguage().getId());

		if(contentletVersionInfo.isEmpty()) {
			throw new DotDataException("Can't find ContentletVersionInfo. Identifier:"
					+ identifier.getId() + ". Lang:"
					+ APILocator.getLanguageAPI().getDefaultLanguage().getId());
		}

		final String inode = showLive && UtilMethods.isSet(contentletVersionInfo.get().getLiveInode()) ?
				contentletVersionInfo.get().getLiveInode() : contentletVersionInfo.get().getWorkingInode();
		Container container = containerCache.get(inode);

		if(container==null || !InodeUtils.isSet(container.getInode())) {

			synchronized (identifier) {

				if(container==null || !InodeUtils.isSet(container.getInode())) {

					container = FileAssetContainerUtil.getInstance().fromAssets (host, folder,
							this.fileAssetAPI.findFileAssetsByParentable(folder, null , !showLive,true, user, false),
							showLive, includeHostOnPath);
					if(container != null && InodeUtils.isSet(container.getInode())) {

						containerCache.add(container.getInode(), container);
					}
				}
			}
		}

		return container;
	}

    @Override
	public Container getContainerByFolder(final Host host, final Folder folder, final User user, final boolean showLive, final boolean includeHostOnPath) throws DotSecurityException, DotDataException {

        if (!this.isValidContainerPath (folder)) {

        	throw new NotFoundInDbException("On getting the container by folder, the folder: " + (folder != null ? folder.getPath() : "Unknown" ) +
					" is not valid, it must be under: " + Constants.CONTAINER_FOLDER_PATH + " and must have a child file asset called: " +
					Constants.CONTAINER_META_INFO_FILE_NAME);
		}
        final Identifier identifier = getContainerAsset(host, folder);
        if(identifier==null) {

            throw new NotFoundInDbException("no container found under: " + folder.getPath() );
        }

        final Optional<ContentletVersionInfo> contentletVersionInfo = APILocator.getVersionableAPI().
				getContentletVersionInfo(identifier.getId(), APILocator.getLanguageAPI().getDefaultLanguage().getId());

        if(contentletVersionInfo.isEmpty()) {
        	throw new DotDataException("Can't find ContentletVersionInfo. Identifier:"
					+ identifier.getId() + ". Lang:"
					+ APILocator.getLanguageAPI().getDefaultLanguage().getId());
		}

        final String inode = showLive && UtilMethods.isSet(contentletVersionInfo.get().getLiveInode()) ?
				contentletVersionInfo.get().getLiveInode() : contentletVersionInfo.get().getWorkingInode();
        Container container = containerCache.get(inode);

        if(container==null || !InodeUtils.isSet(container.getInode())) {

            synchronized (identifier) {

                if(container==null || !InodeUtils.isSet(container.getInode())) {

                    container = FileAssetContainerUtil.getInstance().fromAssets (host, folder,
							this.findContainerAssets(folder, user, showLive), showLive, includeHostOnPath);
                    if(container != null && InodeUtils.isSet(container.getInode())) {

                        containerCache.add(container.getInode(), container);
                    }
                }
            }
        }

        return container;
    }

    /*
    * Finds the containers on the file system, base on a folder
    * showLive in true means get just container published.
    */
	private List<FileAsset> findContainerAssets(final Folder folder, final User user, final boolean showLive) throws DotDataException, DotSecurityException {
		return this.fileAssetAPI.findFileAssetsByFolder(folder, null, showLive, user, false);
	}

	/**
	 * If a folder under application/containers/ has a vtl with the name: container.vtl, means it is a container file based.
	 * @param host    {@link Host}
	 * @param folder  folder
	 * @return boolean
	 */
	private Identifier getContainerAsset(final Host host, final Folder folder) {
		try {

			final Identifier identifier = this.identifierAPI.find(host, builder(folder.getPath(),
					 Constants.CONTAINER_META_INFO_FILE_NAME).toString());
			return identifier!=null  && UtilMethods.isSet(identifier.getId()) ? identifier : null;
		} catch (Exception  e) {
			Logger.warnAndDebug(this.getClass(),e);
			return null;
		}
		
	}

	/*
	 * Determine if the container is a file based.
	 */
	private boolean isValidContainerPath(final Folder folder) {
		return null != folder && UtilMethods.isSet(folder.getPath()) && folder.getPath().contains(Constants.CONTAINER_FOLDER_PATH);
	}

	@Override
	public List<Container> findContainers(final User user, final boolean includeArchived,
			final Map<String, Object> params, final String siteId,
			final String inode, final String identifier, final String parent,
			final int offset, final int limit, final String orderByParam) throws DotSecurityException,
			DotDataException {
		final ContainerAPI.SearchParams searchParams = ContainerAPI.SearchParams.newBuilder()
				.includeArchived(includeArchived)
				.filteringCriterion(params)
				.siteId(siteId)
				.containerInode(inode)
				.containerIdentifier(identifier)
				.contentTypeIdOrVar(parent)
				.offset(offset)
				.limit(limit)
				.orderBy(orderByParam).build();
		return this.findContainers(user, searchParams);
	}

	@Override
	public List<Container> findContainers(final User user, final ContainerAPI.SearchParams searchParams) throws DotSecurityException, DotDataException {
		final ContentTypeAPI contentTypeAPI        = APILocator.getContentTypeAPI(user);
		final StringBuffer conditionBuffer         = new StringBuffer();
		final List<Object> paramValues 			   = this.getConditionParametersAndBuildConditionQuery(searchParams.filteringCriteria(), conditionBuffer);
		final PaginatedArrayList<Container> assets = new PaginatedArrayList<>();
		final List<Permissionable> toReturn        = new ArrayList<>();
		int     internalLimit                      = 500;
		int     internalOffset                     = 0;
		boolean done                               = false;
		String  orderBy                            = SQLUtil.sanitizeSortBy(searchParams.orderBy()) ;
		final StringBuilder query 				   = new StringBuilder().append("select asset.*, inode.* from ")
				.append(Type.CONTAINERS.getTableName()).append(" asset, inode, identifier, ")
				.append(Type.CONTAINERS.getVersionTableName()).append(" vinfo");

		this.buildFindContainersQuery(searchParams, contentTypeAPI, query, paramValues);

		orderBy = UtilMethods.isEmpty(orderBy) ? "mod_date desc" : orderBy;

		List<Container> resultList;
		final DotConnect dotConnect  = new DotConnect();
		int countLimit 		         = 100;

		try {

			query.append(conditionBuffer.toString());
			query.append(" order by asset.");
			query.append(orderBy);
			dotConnect.setSQL(query.toString());

			if(paramValues!=null && paramValues.size()>0) {
				for (final Object value : paramValues) {
					dotConnect.addParam((String)value);
				}
			}

			// Adding Containers as Files located in the /application/containers/ folder
			toReturn.addAll(this.findFolderAssetContainers(user, searchParams));

			while(!done) {

				dotConnect.setStartRow(internalOffset).setMaxRows(internalLimit);
				resultList = TransformerLocator.createContainerTransformer(dotConnect.loadObjectResults()).asList();
				toReturn.addAll(this.permissionAPI.filterCollection(resultList, PermissionAPI.PERMISSION_READ, false, user));
				if (countLimit > 0 && toReturn.size() >= countLimit + searchParams.offset()) {
					done = true;
				} else if(resultList.size() < internalLimit) {
					done = true;
				}

				internalOffset += internalLimit;
			}

			getPaginatedAssets(searchParams.offset(), searchParams.limit(), assets, toReturn);
			if (searchParams.includeSystemContainer()) {
				// System Container is being included, so increase the total result count by 1
				assets.setTotalResults(assets.getTotalResults() + 1L);
			}
		} catch (final Exception e) {
			Logger.error(ContainerFactoryImpl.class,
					String.format("An error occurred when finding Containers [ %s ]: '%s'", searchParams, e), e);
			throw new DotRuntimeException(String.format("An error occurred when finding Containers: '%s'", e));
		}

		return assets;
	}

	private void getPaginatedAssets(final int offset,
									final int limit,
									final PaginatedArrayList<Container> assets,
									final List<Permissionable> toReturn) {

		assets.setTotalResults(toReturn.size());

		if(limit!=-1) {

			final int from = offset<toReturn.size()?offset:0;
			int pageLimit  = 0;

			for(int i=from;i<toReturn.size();i++) {

				if(pageLimit < limit) {
					assets.add((Container) toReturn.get(i));
					pageLimit+=1;
				} else {
					break;
				}
			}
		} else {
			for(int i=0;i<toReturn.size();i++) {

				assets.add((Container) toReturn.get(i));
			}
		}
	}

	private boolean containsContentType (final FileAssetContainer fileAssetContainer, final String velocityVarName) {

		return UtilMethods.isSet(fileAssetContainer.getContainerStructuresAssets())
				&& fileAssetContainer.getContainerStructuresAssets().stream()
				.filter(fileAsset -> null != fileAsset.getFileName())
				.anyMatch(fileAsset -> fileAsset.getFileName().equalsIgnoreCase(velocityVarName));
	}

	private List<Container> filterFileAssetContainersByContentType (final List<Container> containers, final String contentTypeId, final User user) throws DotDataException, DotSecurityException {

		final ContentType contentType       =  APILocator.getContentTypeAPI(user).find(contentTypeId);

		if (null == contentType) {
			return containers;
		}

		final String contentTypeVelocityVar = contentType.variable() + ".vtl";
		return containers.stream().map(container ->  FileAssetContainer.class.cast(container) ).
				filter(container -> containsContentType(container, contentTypeVelocityVar)).
				collect(Collectors.toList());
	}

	/**
	 * Finds all File-bases Containers for a given User and sorts the results based on the specified search parameter.
	 *
	 * @param user         The {@link User} performing this action.
	 * @param searchParams Query parameters used to filter the expected Containers.
	 *
	 * @return The sorted list of {@link Container} objects based on the search parameters.
	 **/
	private Collection<? extends Permissionable> findFolderAssetContainers(final User user, final ContainerAPI.SearchParams searchParams) {

		try {

			final Host site     			 = this.hostAPI.find(searchParams.siteId(), user, false);
			final List<Folder> subFolders    = this.findContainersAssetsByHost(site, user, searchParams.includeArchived());
			List<Container> containers = this.getFolderContainers(site, user, subFolders, false);

			if (UtilMethods.isSet(searchParams.contentTypeIdOrVar())) {

				containers = this.filterFileAssetContainersByContentType(containers, searchParams.contentTypeIdOrVar(), user);

			}

			if (UtilMethods.isSet(searchParams.filteringCriteria())) {

				containers = containers.stream().filter(container -> {

					for (final Object name : searchParams.filteringCriteria().values()) {

						if (container.getName().toLowerCase().contains(name.toString().toLowerCase())) {
							return true;
						}
					}
					return false;
				}).collect(Collectors.toList());
			}

			if (UtilMethods.isSet(searchParams.orderBy())) {
				switch (searchParams.orderBy().toLowerCase()) {
					case "title asc":
						containers.sort(Comparator.comparing(Container::getTitle));
					break;

                    case "title desc":
                        containers.sort(Comparator.comparing(Container::getTitle).reversed());
                        break;

					case "moddate asc":
						containers.sort(Comparator.comparing(Container::getModDate));
						break;

                    case "moddate desc":
                        containers.sort(Comparator.comparing(Container::getModDate).reversed());
                        break;
				}
			}

			return containers;
		} catch (final Exception e) {
			Logger.warn(this, String.format("An error occurred when finding File-based Containers for Site '%s': %s",
					searchParams.siteId(), e.getMessage()), e);
			return Collections.emptyList();
		}
	}

	/**
	 * Finds all host folder container based on the system user
	 * @return Collection
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	private Collection<Container> findAllHostFolderAssetContainers(final Host currentHost) throws DotSecurityException, DotDataException {

		final User       user  = APILocator.systemUser();
		final List<Host> hosts = this.hostAPI.findAll(user, false);
		final ImmutableList.Builder<Container> containers = new ImmutableList.Builder<>();

		for (final Host host : hosts) {

			containers.addAll(this.findHostContainers(host, user, !currentHost.getIdentifier().equals(host.getIdentifier())));
		}

		return containers.build();
	}

	/**
	 * Find host container, check the permissions based on an user
	 * @param host {@link Host}
	 * @param user {@link User}
	 * @param includeHostOnPath {@link String} if the host is the same of the requested, this should be false, otherwise true in order to qualified the whole  path with the hostname to on the container path
	 * @return List
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	private List<Container> findHostContainers(final Host host, final User user, final boolean includeHostOnPath) throws DotDataException, DotSecurityException {

		final List<Folder> subFolders = this.findContainersAssetsByHost(host, user, false);
		return this.getFolderContainers(host, user, subFolders, includeHostOnPath);
	}

	/**
	 * Finds the container.vtl on a specific host
	 * returns even working versions but not archived
	 * the search is based on the DB.
	 * If exists multiple language version, will consider only the versions based on the default language, so if only exists a container.vtl with a non-default language it will be skipped.
	 *
	 * @param host {@link Host}
	 * @param user {@link User}
	 * @param includeArchived {@link Boolean} if wants to include archive containers
	 * @return List of Folder
	 */
	private List<Folder> findContainersAssetsByHost(final Host host, final User user, final boolean includeArchived) {

		List<Contentlet>           containers = null;
		final List<Folder>         folders    = new ArrayList<>();

		final StringBuilder sqlQuery = new StringBuilder("select cvi.working_inode as inode from contentlet_version_info cvi, identifier id where"
				+ " id.parent_path like ? and id.asset_name = ? and cvi.identifier = id.id");
		final List<Object> parameters = new ArrayList<>();
		parameters.add(Constants.CONTAINER_FOLDER_PATH + StringPool.FORWARD_SLASH + StringPool.PERCENT);
		parameters.add(Constants.CONTAINER_META_INFO_FILE_NAME);

		if(!includeArchived){
			sqlQuery.append(" and cvi.deleted = " + DbConnectionFactory.getDBFalse());
		}

		if(null != host){
			sqlQuery.append(" and id.host_inode = ?");
			parameters.add(host.getIdentifier());
		}

		final DotConnect dc = new DotConnect().setSQL(sqlQuery.toString());
		parameters.forEach(param -> dc.addParam(param));

		try {
			final List<Map<String,String>> inodesMapList =  dc.loadResults();

			final List<String> inodes = new ArrayList<>();
			for (final Map<String, String> versionInfoMap : inodesMapList) {
				inodes.add(versionInfoMap.get("inode"));
			}

			final List<Contentlet> contentletList  = APILocator.getContentletAPI().findContentlets(inodes);

            containers =
					this.filterContainersAssetsByLanguage (this.permissionAPI.filterCollection(contentletList,
							PermissionAPI.PERMISSION_READ, false, user),
							APILocator.getLanguageAPI().getDefaultLanguage().getId());



			for(final Contentlet container : containers) {

				folders.add(this.folderAPI.find(container.getFolder(), user, false));
			}
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}

		return folders;
	}

	private List<Contentlet> filterContainersAssetsByLanguage(final List<Contentlet> contentContainers, final long defaultLanguageId) {

		final List<Contentlet> uniqueContentContainers   				 = new ArrayList<>();
		final Map<String, List<Contentlet>> contentletsGroupByIdentifier = CollectionsUtils.groupByKey(contentContainers, Contentlet::getIdentifier);

		for (final Map.Entry<String, List<Contentlet>> entry : contentletsGroupByIdentifier.entrySet()) {

			if (entry.getValue().size() <= 1) {

				uniqueContentContainers.addAll(entry.getValue());
			} else {

				ifOrElse(entry.getValue().stream()
								.filter(contentlet -> contentlet.getLanguageId() == defaultLanguageId).findFirst(), // if present the one with the default lang take it
						uniqueContentContainers::add,
						()->uniqueContentContainers.add(entry.getValue().get(0))); // otherwise take any one, such as the first one
			}
		}

		return uniqueContentContainers;
	}

	/**
	 * Gets the folder container based on the host and sub folders.
	 * @param host {@link Host}
	 * @param user {@link User}
	 * @param subFolders {@link List}
	 * @param includeHostOnPath {@link Boolean} true if you want to  include the host on the container path
	 * @return List of Containers
	 * @throws DotDataException
	 */
	private List<Container> getFolderContainers(final Host host, final User user,
												final List<Folder> subFolders, final boolean includeHostOnPath) throws DotDataException {

		final List<Container> containers = new ArrayList<>();
		for (final Folder subFolder : subFolders) {

			try {

			    final User      userFinal = null != user? user: APILocator.systemUser();
				final Container container = this.getContainerByFolder(null != host? host:APILocator.getHostAPI().find(subFolder.getHostId(), user, includeHostOnPath),
						subFolder, userFinal, false, includeHostOnPath);
				containers.add(container);
			} catch (DotSecurityException e) {

				Logger.debug(this, () -> "Does not have permission to read the folder container: " + subFolder.getPath());
			} catch (NotFoundInDbException e) {

                Logger.debug(this, () -> "The folder: " + subFolder.getPath() + ", is not a container");
            }
		}

		return containers;
	}

	/**
	 * Builds the main part of the SQL query that will be used to find Containers in the dotCMS repository.
	 * Uses parameterized queries to prevent SQL injection attacks.
	 *
	 * @param searchParams   User-specified search criteria.
	 * @param contentTypeAPI An instance of the {@link ContentTypeAPI}.
	 * @param query          The SQL query being built.
	 * @param paramValues    List to collect the query parameters for parameterized execution.
	 *
	 * @throws DotSecurityException The user cannot perform this action or invalid input detected.
	 * @throws DotDataException     An error occurred when interacting with the data source.
	 */
	private void buildFindContainersQuery(final ContainerAPI.SearchParams searchParams, final ContentTypeAPI contentTypeAPI,
										  final StringBuilder query, final List<Object> paramValues) throws DotSecurityException, DotDataException {

		if(UtilMethods.isSet(searchParams.contentTypeIdOrVar())) {

			//Search for the given ContentType inode
			final ContentType foundContentType = contentTypeAPI.find(searchParams.contentTypeIdOrVar());

			if (null != foundContentType && InodeUtils.isSet(foundContentType.inode())) {
				// Use parameterized query to prevent SQL injection
				query.append(
								" where asset.inode = inode.inode and asset.identifier = identifier.id")
						.append(
								" and exists (select * from container_structures cs where cs.container_id = asset.identifier")
						.append(" and cs.structure_id = ? ) ");
				// Validate that inode is a valid UUID
				final String inode = foundContentType.inode();
				if (!UtilMethods.isSet(inode) || !UUIDUtil.isUUID(inode)) {
					throw new DotSecurityException("Invalid inode format: " + inode);
				}
				paramValues.add(inode);
			}else {
				// Use parameterized query to prevent SQL injection
				query.append(
								" ,tree where asset.inode = inode.inode and asset.identifier = identifier.id")
						.append(" and tree.parent = ? and tree.child=asset.inode");
				// Validate the contentTypeIdOrVar to prevent injection (UUID, variable name, or identifier)
				final String contentTypeIdOrVar = searchParams.contentTypeIdOrVar();
				if (!UtilMethods.isSet(contentTypeIdOrVar) || !isValidIdentifier(contentTypeIdOrVar)) {
					throw new DotSecurityException("Invalid content type identifier: " + contentTypeIdOrVar);
				}
				paramValues.add(contentTypeIdOrVar);
			}
		} else {
			query.append(" where asset.inode = inode.inode and asset.identifier = identifier.id");
		}

		query.append(" and vinfo.identifier=identifier.id and vinfo.working_inode=asset.inode ");

		if(!searchParams.includeArchived()) {
			query.append(" and vinfo.deleted=");
			query.append(DbConnectionFactory.getDBFalse());
		}

		if(UtilMethods.isSet(searchParams.siteId())) {
			// Use parameterized query to prevent SQL injection
			query.append(" and identifier.host_inode = ?");
			// Validate siteId format (UUID or special identifiers like SYSTEM_HOST)
			final String siteId = searchParams.siteId();
			if (!isValidIdentifier(siteId)) {
				throw new DotSecurityException("Invalid site ID format: " + siteId);
			}
			paramValues.add(siteId);
		}

		if(UtilMethods.isSet(searchParams.containerInode())) {
			// Use parameterized query to prevent SQL injection
			query.append(" and asset.inode = ?");
			// Validate containerInode format (UUID or system identifier)
			final String containerInode = searchParams.containerInode();
			if (!isValidIdentifier(containerInode)) {
				throw new DotSecurityException("Invalid container inode format: " + containerInode);
			}
			paramValues.add(containerInode);
		}

		if(UtilMethods.isSet(searchParams.containerIdentifier())) {
			// Use parameterized query to prevent SQL injection
			query.append(" and asset.identifier = ?");
			// Validate containerIdentifier format (UUID or special identifier)
			final String containerIdentifier = searchParams.containerIdentifier();
			if (!isValidIdentifier(containerIdentifier)) {
				throw new DotSecurityException("Invalid container identifier format: " + containerIdentifier);
			}
			paramValues.add(containerIdentifier);
		}
	}

	/**
	 * Traverses the map of optional query parameters that are used to search for Containers in the system. Such
	 * parameters are completely optional, and are simply meant to narrow down potential results. You can take a look at
	 * the database definition of the {@code dot_containers} table in order to determine what specific columns can be
	 * queried.
	 *
	 * @param params               User-specified search parameters.
	 * @param conditionQueryBuffer The SQL query that is being put together based on the different search parameters.
	 *
	 * @return The values for each specific search parameter.
	 */
	private List<Object> getConditionParametersAndBuildConditionQuery(final Map<String, Object> params, final StringBuffer conditionQueryBuffer) throws DotSecurityException {

		List<Object> paramValues = null;

		if (params != null && !params.isEmpty()) {

			conditionQueryBuffer.append(" and (");
			paramValues = new ArrayList<>();
			int counter = 0;

			for (final Map.Entry<String, Object> entry : params.entrySet()) {

				this.buildConditionParameterAndBuildConditionQuery(entry, paramValues, conditionQueryBuffer,
						(counter==0)?
								Optional.empty():
								Optional.of(" OR"));
				counter+=1;
			}

			conditionQueryBuffer.append(" ) ");
		}
		return paramValues;
	}

	/**
	 * Generates the appropriate SQL condition based on the nature of the specified search parameter. In summary:
	 * <ul>
	 *     <li>For String parameters -- except for {@code identifier} and {@code inode} -- the query will use the
	 *     {@code LIKE} operator to compare the existing value.</li>
	 *     <li>For any other data types, the {@code "="} operator will be used.</li>
	 * </ul>
	 *
	 * @param entry                The search parameter in the form of key/value, represented as an {@link Map.Entry}
	 *                             object.
	 * @param paramValues          The list of String parameters, which require the appended {@code "%"} signs.
	 * @param conditionQueryBuffer The resulting SQL query.
	 * @param prefix               The connecting command between every SQL condition. For example, the {@code OR} or
	 *                             {@code AND} commands from SQL.
	 */
	private void buildConditionParameterAndBuildConditionQuery (final Map.Entry<String, Object> entry,
																final List<Object> paramValues,
																final StringBuffer conditionQueryBuffer,
																final Optional<String> prefix) throws DotSecurityException {

		if(entry.getValue() instanceof String){
			if (entry.getKey().equalsIgnoreCase("inode") || entry.getKey()
					.equalsIgnoreCase("identifier")) {

				if (prefix.isPresent()) {
					conditionQueryBuffer.append(prefix.get());
				}
				conditionQueryBuffer.append(" asset.");
				conditionQueryBuffer.append(entry.getKey());
				conditionQueryBuffer.append(" = ?");
				// Validate identifier/inode format to prevent SQL injection
				final String value = (String) entry.getValue();
				if (!UtilMethods.isSet(value) || !isValidIdentifier(value)) {
					throw new DotSecurityException("Invalid " + entry.getKey() + " format: " + value);
				}
				paramValues.add(value);
			} else {

				if (prefix.isPresent()) {
					conditionQueryBuffer.append(prefix.get());
				}
				conditionQueryBuffer.append(" lower(asset.");
				conditionQueryBuffer.append(entry.getKey());
				conditionQueryBuffer.append(") like ? ");
				paramValues.add("%"+ ((String)entry.getValue()).toLowerCase()+"%");
			}
		} else {

			if (prefix.isPresent()) {
				conditionQueryBuffer.append(prefix.get());
			}
			conditionQueryBuffer.append(" asset.");
			conditionQueryBuffer.append(entry.getKey());
			conditionQueryBuffer.append(" = ");
			conditionQueryBuffer.append(entry.getValue());
		}
	}

	@Override
	public List<Container> findContainersForStructure(final String structureIdentifier) throws DotDataException {
        return findContainersForStructure(structureIdentifier, false);
    }

	@Override
	public List<Container> findContainersForStructure(final String structureIdentifier,
			final boolean workingOrLiveOnly) throws DotDataException {

		final List<Container> containers = new ArrayList<>();

		containers.addAll(this.findFolderAssetContainersForContentType(structureIdentifier));
		containers.addAll(this.findDataBaseContainersForContentType   (structureIdentifier, workingOrLiveOnly));

		return containers;
	}

	private List<Container> findFolderAssetContainersForContentType(final String contentTypeIdentifier) throws DotDataException {

		final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
		// get the content type, if exists look for all content type by varname case insensitive
		// for each varname try to get the container as a folder

		try {

			final ContentType contentType = contentTypeAPI.find(contentTypeIdentifier);
			if (null != contentType) {

				final  List<Folder> containerFolders = findContainersAssetsByContentType(contentType.variable());
				if (containerFolders.size() > 0) {

					final List<Container> fileAssetContainers = new ArrayList<>();

					for (final Folder folder: containerFolders) {

						final Host host           		    = this.hostAPI.find(folder.getHostId(), APILocator.systemUser(), false);
						final String folderHostId           = folder.getHostId();
						final Optional<Host> currentHostOpt = HostUtil.tryToFindCurrentHost(APILocator.systemUser());
						boolean includeHostOnPath           = false;

						if (currentHostOpt.isPresent()) {

							includeHostOnPath = !folderHostId.equals(currentHostOpt.get().getIdentifier());
						}
						try {
							final Container container = this.getContainerByFolder(host, folder, APILocator.systemUser(), false, includeHostOnPath);

							if (null != container) {

								fileAssetContainers.add(container);
							}
						} catch (NotFoundInDbException e) {

							Logger.debug(this, ()-> "Getting the container folder: " + folder.getPath()
									+ ", throws NotFoundInDbException: " + e.getMessage());
						}
					}

					return fileAssetContainers;
				}
			}
		} catch (DotSecurityException e) {
			throw new DotDataException(e);
		}

		return Collections.emptyList();
	}

	/**
	 * Finds all container that have the contentTypeVarNameFileName as a vtl in their folders
	 * (reference to the content type for the file asset containers)
	 * Will include working content type but not archived
	 * @param contentTypeVarNameFileName {@link String}
	 * @return List of Folder
	 */
	private List<Folder> findContainersAssetsByContentType(final String contentTypeVarNameFileName) {

		List<Contentlet>           containers = null;
		final List<Folder>         folders    = new ArrayList<>();
		final User 				   user       = APILocator.systemUser();

		final StringBuilder sqlQuery = new StringBuilder("select cvi.working_inode as inode from contentlet_version_info cvi, identifier id where"
				+ " id.parent_path like ? and id.asset_name = ? and cvi.identifier = id.id");
		final List<Object> parameters = new ArrayList<>();
		parameters.add(Constants.CONTAINER_FOLDER_PATH + StringPool.FORWARD_SLASH + StringPool.PERCENT);
		parameters.add(contentTypeVarNameFileName + StringPool.PERIOD + "vtl");

		sqlQuery.append(" and cvi.deleted = " + DbConnectionFactory.getDBFalse());

		final DotConnect dc = new DotConnect().setSQL(sqlQuery.toString());
		parameters.forEach(param -> dc.addParam(param));

		try {
			final List<Map<String,String>> inodesMapList =  dc.loadResults();

			final List<String> inodes = new ArrayList<>();
			for (final Map<String, String> versionInfoMap : inodesMapList) {
				inodes.add(versionInfoMap.get("inode"));
			}

			containers  = APILocator.getContentletAPI().findContentlets(inodes);

			for(final Contentlet container : containers) {

				folders.add(this.folderAPI.find(container.getFolder(), user, false));
			}
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}

		return folders;
	}

	private List<Container> findDataBaseContainersForContentType(final String contentTypeIdentifier,
													  final boolean workingOrLiveOnly) throws DotDataException {

		final HibernateUtil hibernateUtil  = new HibernateUtil(Container.class);
		final StringBuilder query 		   = new StringBuilder();

		query.append("FROM c IN CLASS ");
		query.append(Container.class);
		query.append(" WHERE  exists ( from cs in class ");
		query.append(ContainerStructure.class.getName());
		query.append(" where cs.containerId = c.identifier and cs.structureId = ? ");
		if (workingOrLiveOnly) {
			query.append(" AND EXISTS ( FROM vi IN CLASS ");
			query.append(ContainerVersionInfo.class.getName());
			query.append(" WHERE vi.identifier = c.identifier AND ");
			query.append(" (cs.containerInode = vi.workingInode OR cs.containerInode = vi.liveInode ) ) ");
		}
		query.append(") ");
		hibernateUtil.setQuery(query.toString());
		hibernateUtil.setParam(contentTypeIdentifier);
		return hibernateUtil.list();
	}

	/**
	 * Method will replace user references of the given userId in containers 
	 * with the replacement user id 
	 * @param userId User Identifier
	 * @param replacementUserId The user id of the replacement user
	 * @throws DotDataException There is a data inconsistency
	 * @throws DotStateException There is a data inconsistency
	 * @throws DotSecurityException 
	 */
	@Override
	public void updateUserReferences(String userId, String replacementUserId)throws DotDataException, DotSecurityException{
		DotConnect dc = new DotConnect();
        StringBuilder query = new StringBuilder();

        query.append("select distinct(identifier) from ");
		query.append(Inode.Type.CONTAINERS.getTableName());
		query.append(" where mod_user = ?");
        try {
           dc.setSQL(query.toString());
           dc.addParam(userId);
           List<HashMap<String, String>> containers = dc.loadResults();

           query = new StringBuilder();
           query.append("UPDATE ");
           query.append(Inode.Type.CONTAINERS.getTableName());
           query.append(" set mod_user = ? where mod_user = ? ");
           dc.setSQL(query.toString());
           dc.addParam(replacementUserId);
           dc.addParam(userId);
           dc.loadResult();

           dc.setSQL("update container_version_info set locked_by=? where locked_by  = ?");
           dc.addParam(replacementUserId);
           dc.addParam(userId);
           dc.loadResult();

           for(HashMap<String, String> ident:containers){
               String identifier = ident.get("identifier");
               if (UtilMethods.isSet(identifier)) {
        			   final VersionInfo info =APILocator.getVersionableAPI().getVersionInfo(identifier);
        			   CacheLocator.getContainerCache().remove(info);

			   }
           }
        } catch (DotDataException e) {
            Logger.error(ContainerFactory.class,e.getMessage(),e);
            throw new DotDataException(e.getMessage(), e);
        }
	}


	/**
	 * Validates if a string is a valid dotCMS identifier.
	 * Accepts UUIDs, system identifiers (SYSTEM_HOST, SYSTEM_FOLDER), and variable names.
	 * More flexible validation to avoid being too restrictive.
	 *
	 * @param identifier the identifier to validate
	 * @return true if the identifier is valid, false otherwise
	 */
	private boolean isValidIdentifier(final String identifier) {
		if (!UtilMethods.isSet(identifier)) {
			return false;
		}
		// Allow UUIDs, known system identifiers, or valid variable names
		return UUIDUtil.isUUID(identifier) ||
			   isSystemIdentifier(identifier) ||
			   identifier.matches(com.dotmarketing.portlets.workflows.business.WorkflowFactoryImpl.VALID_VARIABLE_NAME_REGEX);
	}

	/**
	 * Checks if the identifier is a known system identifier.
	 * Based on fabrizzio's suggestion to handle system identifiers like SYSTEM_HOST, SYSTEM_FOLDER.
	 *
	 * @param identifier the identifier to check
	 * @return true if it's a known system identifier
	 */
	private boolean isSystemIdentifier(final String identifier) {
		return "SYSTEM_HOST".equals(identifier) ||
			   "SYSTEM_FOLDER".equals(identifier);
	}

	/**
	 * Validates if a string is a valid velocity variable name for content types.
	 * Uses the established VALID_VARIABLE_NAME_REGEX from WorkflowFactoryImpl.
	 *
	 * @param variableName the variable name to validate
	 * @return true if the variable name is valid, false otherwise
	 */
	private boolean isValidVariableName(final String variableName) {
		if (!UtilMethods.isSet(variableName)) {
			return false;
		}
		return variableName.matches(com.dotmarketing.portlets.workflows.business.WorkflowFactoryImpl.VALID_VARIABLE_NAME_REGEX);
	}

}
