package com.dotmarketing.portlets.containers.business;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.rendering.velocity.services.ContainerLoader;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.transform.TransformerLocator;
import com.dotmarketing.beans.*;
import com.dotmarketing.business.*;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Constants;
import com.dotmarketing.util.HostUtil;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.function.Supplier;

import static com.dotcms.util.CollectionsUtils.list;

/**
 * Implementation class of the {@link ContainerAPI}.
 * 
 * @author root
 * @version 1.0
 * @since Mar 22, 2012
 *
 */
public class ContainerAPIImpl extends BaseWebAssetAPI implements ContainerAPI {
	protected PermissionAPI    permissionAPI;
	protected ContainerFactory containerFactory;
	protected HostAPI          hostAPI;
	protected FolderAPI        folderAPI;

	/**
	 * Constructor
	 */
	public ContainerAPIImpl () {

        this.permissionAPI    = APILocator.getPermissionAPI();
        this.containerFactory = FactoryLocator.getContainerFactory();
        this.hostAPI          = APILocator.getHostAPI();
        this.folderAPI        = APILocator.getFolderAPI();
	}

	@WrapInTransaction
	@Override
	public Container copy(final Container source, Host destination, final User user, final boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {

		if (!permissionAPI.doesUserHavePermission(source, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to read the source container.");
		}

		if (!permissionAPI.doesUserHavePermission(destination, PermissionAPI.PERMISSION_WRITE, user,
				respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to wirte in the destination folder.");
		}

		//gets the new information for the template from the request object
		final Container newContainer = new Container();

		newContainer.copy(source);

		final String appendToName = getAppendToContainerTitle(source.getTitle(), destination);
       	newContainer.setFriendlyName(source.getFriendlyName() + appendToName);
       	newContainer.setTitle(source.getTitle() + appendToName);

        //creates new identifier for this webasset and persists it
		final Identifier newIdentifier = APILocator.getIdentifierAPI().createNew(newContainer, destination);

        newContainer.setIdentifier(newIdentifier.getId());
		//persists the webasset
		save(newContainer);

		if(source.isWorking()){
			APILocator.getVersionableAPI().setWorking(newContainer);
		}
		if(source.isLive()){
			APILocator.getVersionableAPI().setLive(newContainer);
		}

		// issue-2093 Copying multiple structures per container
		if(source.getMaxContentlets()>0) {

			final List<ContainerStructure> sourceCS = getContainerStructures(source);
			final List<ContainerStructure> newContainerCS = new LinkedList<ContainerStructure>();

			for (final ContainerStructure oldCS : sourceCS) {

				final ContainerStructure newCS = new ContainerStructure();
				newCS.setContainerId(newContainer.getIdentifier());
                newCS.setContainerInode(newContainer.getInode());
				newCS.setStructureId(oldCS.getStructureId());
				newCS.setCode(oldCS.getCode());
				newContainerCS.add(newCS);
			}

			saveContainerStructures(newContainerCS);

		}

		//Copy permissions
		permissionAPI.copyPermissions(source, newContainer);

		//saves to working folder under velocity
		new ContainerLoader().invalidate(newContainer);

		return newContainer;
	}

	@Override
	public Optional<Container> findContainer(final String idOrPath, final User user, final boolean live,
								   final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		final Container container;

		if (FileAssetContainerUtil.getInstance().isFolderAssetContainerId(idOrPath)) {

			final ResolvedPath hostAndRelativeFromPath = resolvePath(idOrPath, user, live, respectFrontendRoles);
			container = hostAndRelativeFromPath.container;
		} else {
			if (live) {
				container = this.getLiveContainerById(idOrPath, user, respectFrontendRoles);
			} else {
				container = this.getWorkingContainerById(idOrPath, user, respectFrontendRoles);
			}
		}

		return container != null ? Optional.of(container) : Optional.empty();
	}

	/**
	 * 
	 * @param container
	 * @throws DotDataException
	 */
	private void save(final Container container) throws DotDataException {
		containerFactory.save(container);
	}

	/**
	 * 
	 * @param container
	 * @param existingId
	 * @throws DotDataException
	 */
	private void save(final Container container, final String existingId) throws DotDataException {
		containerFactory.save(container, existingId);
	}

	@WrapInTransaction
	@Override
	protected void save(final WebAsset webAsset) throws DotDataException {
		save((Container) webAsset);
	}

	/**
	 * Save an existing container as a web asset
	 * @param webAsset {@link WebAsset}
	 * @param existingId {@link String}
	 * @throws DotDataException
	 */
	@WrapInTransaction
	protected void save(final WebAsset webAsset, final String existingId) throws DotDataException {
		save((Container) webAsset, existingId);
	}

	/**
	 * Appends the name of the container
	 * @param containerTitle {@link String}
	 * @param destination    {@link Host}
	 * @return String
	 * @throws DotDataException
	 */
	@CloseDBIfOpened
	@SuppressWarnings("unchecked")
	private String getAppendToContainerTitle(final String containerTitle, final Host destination)
			throws DotDataException {

		List<Container> containers;
		String temp = new String(containerTitle);
		String result = "";
		final DotConnect dc = new DotConnect();
		String sql = "SELECT " + Inode.Type.CONTAINERS.getTableName() + ".*, dot_containers_1_.* from "
				+ Inode.Type.CONTAINERS.getTableName()
				+ ", inode dot_containers_1_, identifier ident, container_version_info vv " +
				"where vv.identifier=ident.id and vv.working_inode=" + Inode.Type.CONTAINERS
				.getTableName() + ".inode and " + Inode.Type.CONTAINERS.getTableName()
				+ ".inode = dot_containers_1_.inode and " +
				Inode.Type.CONTAINERS.getTableName()
				+ ".identifier = ident.id and host_inode = ? order by title ";
		dc.setSQL(sql);
		dc.addParam(destination.getIdentifier());

		containers = TransformerLocator.createContainerTransformer(dc.loadObjectResults()).asList();

		boolean isContainerTitle = false;

		for (; !isContainerTitle; ) {
			isContainerTitle = true;
			temp += result;

			for (Container container : containers) {
				if (container.getTitle().equals(temp)) {
					isContainerTitle = false;
					break;
				}
			}

			if (!isContainerTitle) {
				result += " (COPY)";
			}
		}

		return result;
	}

	@CloseDBIfOpened
    @Override
    @SuppressWarnings("unchecked")
    public Container find(final String inode, final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

	      final  Identifier  identifier = Try.of(()->APILocator.getIdentifierAPI().findFromInode(inode)).getOrNull();
        final Container container = this.isContainerFile(identifier)?
				this.getWorkingContainerByFolderPath(identifier.getParentPath(), identifier.getHostId(), user, respectFrontendRoles):
                containerFactory.find(inode);

        if (container == null) {
            return null;
        }
        if (!permissionAPI.doesUserHavePermission(container, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
            throw new DotSecurityException("You don't have permission to read the source file.");
        }

        return container;
    }

	@CloseDBIfOpened
	@Override
	@SuppressWarnings("unchecked")
	public Container getWorkingContainerById(final String identifierParameter, final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

        final  Identifier  identifier = APILocator.getIdentifierAPI().find(identifierParameter);

        if (null != identifier && UtilMethods.isSet(identifier.getId())) {

			return this.isContainerFile(identifier) ?
					this.getWorkingContainerByFolderPath(identifier.getParentPath(), identifier.getHostId(), user, respectFrontendRoles) :
					this.getWorkingVersionInfoContainerById(identifierParameter, user, respectFrontendRoles);
		}

        return null;
	}

    /**
     * If the container asset name is container.vtl, returns true
     * @param identifier {@link Identifier}
     * @return boolean
     */
    private boolean isContainerFile(final Identifier identifier) {

        return null != identifier && Constants.CONTAINER_META_INFO_FILE_NAME.equals(identifier.getAssetName());
    }

    /**
     * Gets the working version of the container based on a folder path
     * @param path    {@link String}
     * @param hostId  {@link String}
     * @param user    {@link User}
     * @param respectFrontEndPermissions {@link Boolean}
     * @return Container
     * @throws DotSecurityException
     * @throws DotDataException
     */
	private Container getWorkingContainerByFolderPath(final String path, final String hostId, final User user,
													 final boolean respectFrontEndPermissions) throws DotSecurityException, DotDataException {

		final Host host = this.hostAPI.find(hostId, user, respectFrontEndPermissions);
		return this.getWorkingContainerByFolderPath(path, host, user, respectFrontEndPermissions);
	}

	@CloseDBIfOpened
	@Override
	public Container getWorkingContainerByFolderPath(final String fullContainerPathWithHost, final User user,
                                                     final boolean respectFrontEndPermissions, final Supplier<Host> resourceHost) throws DotSecurityException, DotDataException {

		final Tuple2<String, Host> pathAndHostTuple = this.getContainerPathHost(fullContainerPathWithHost, user, resourceHost);
			return this.getWorkingContainerByFolderPath(pathAndHostTuple._1, pathAndHostTuple._2, user, respectFrontEndPermissions);
	}

	private Tuple2<String, Host> getContainerPathHost(final String containerIdOrPath, final User user,
                                                      final Supplier<Host> resourceHost) throws DotSecurityException, DotDataException {

		final ResolvedPath resolvedPath = this.resolvePath(containerIdOrPath, user, false, false);
		return Tuple.of(resolvedPath.relativePath, resolvedPath.host);
	}

    @CloseDBIfOpened
    @Override
    public Container getWorkingContainerByFolderPath(final String path, final Host host, final User user,
                                                     final boolean respectFrontEndPermissions) throws DotSecurityException, DotDataException {

	    return this.containerFactory.getWorkingContainerByFolderPath(path, host, user, respectFrontEndPermissions);
    }

	@CloseDBIfOpened
    @Override
    public Container getContainerByFolder(final Folder folder, final User user, final boolean showLive) throws DotSecurityException, DotDataException {

		final Host host = this.hostAPI.find(folder.getHostId(), user, false);
	    return this.getContainerByFolder(folder, host, user, showLive);
    }

	@CloseDBIfOpened
	@Override
	public Container getContainerByFolder(final Folder folder, final Host host, final User user, final boolean showLive) throws DotSecurityException, DotDataException {

		final String folderHostId           = folder.getHostId();
		final Optional<Host> currentHostOpt = HostUtil.tryToFindCurrentHost(user);
		boolean includeHostOnPath           = false;

		if (currentHostOpt.isPresent()) {

			includeHostOnPath = !folderHostId.equals(currentHostOpt.get().getIdentifier());
		}

		return this.containerFactory.getContainerByFolder(host, folder, user, showLive, includeHostOnPath);
	}

    /**
     * Get the container data base, base on the {@link VersionableAPI}
     * @param containerId {@link String}
     * @param user {@link User}
     * @param respectFrontendRoles boolean
     * @return Container
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private Container getWorkingVersionInfoContainerById(final String containerId, final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

        final  VersionInfo info = APILocator.getVersionableAPI().getVersionInfo(containerId);
        return info !=null? find(info.getWorkingInode(), user, respectFrontendRoles): null;
    }

	@Override
	@SuppressWarnings("unchecked")
	public Container getLiveContainerById(final String containerId, final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		final  Identifier  identifier = APILocator.getIdentifierAPI().find(containerId);

		if (null != identifier && UtilMethods.isSet(identifier.getId())) {
			return this.isContainerFile(identifier) ?
					this.getLiveContainerByFolderPath(identifier.getParentPath(),
							this.hostAPI.find(identifier.getHostId(), user, respectFrontendRoles), user, respectFrontendRoles) :
					this.getLiveVersionInfoContainerById(containerId, user, respectFrontendRoles);
		}

		return null;
	}

	@CloseDBIfOpened
	@Override
	public Container getLiveContainerByFolderPath(final String path, final Host host, final User user,
													 final boolean respectFrontEndPermissions) throws DotSecurityException, DotDataException {

		return this.containerFactory.getLiveContainerByFolderPath(path, host, user, respectFrontEndPermissions);
	}

    @CloseDBIfOpened
    @Override
    public Container getLiveContainerByFolderPath(final String fullContainerPathWithHost, final User user,
                                                  final boolean respectFrontEndPermissions, final Supplier<Host> resourceHost) throws DotSecurityException, DotDataException {

        final Tuple2<String, Host> pathAndHostTuple = this.getContainerPathHost(fullContainerPathWithHost, user, resourceHost);
        return this.getLiveContainerByFolderPath(pathAndHostTuple._1, pathAndHostTuple._2, user, respectFrontEndPermissions);
    }

    /**
     * Get the lie version of the container
     * @param containerId {@link String}
     * @param user        {@link User}
     * @param respectFrontendRoles {@link Boolean}
     * @return Container
     * @throws DotDataException
     * @throws DotSecurityException
     */
	private Container getLiveVersionInfoContainerById(final String containerId, final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		final VersionInfo info = APILocator.getVersionableAPI().getVersionInfo(containerId);
		return (info !=null && UtilMethods.isSet(info.getLiveInode())) ? find(info.getLiveInode(), user, respectFrontendRoles) : null;
	}


    @CloseDBIfOpened
    @Override
    @SuppressWarnings("unchecked")
    public List<Container> getContainersOnPage(final IHTMLPage page)
            throws DotStateException, DotDataException, DotSecurityException {

        final List<Container> containers  = new ArrayList<>();
        final DotConnect dotConnect = new DotConnect()
				.setSQL("select * from identifier where id in (select distinct(parent2) as containers from multi_tree where parent1=?)")
        		.addParam(page.getIdentifier());

        final List<Identifier> identifiers   = TransformerLocator.createIdentifierTransformer(dotConnect.loadObjectResults()).asList();
        final List<Container> pageContainers = new ArrayList<>();

        for (final Identifier id : identifiers) {

            final Container container =
					this.getWorkingVersionInfoContainerById (id.getId(), APILocator.getUserAPI().getSystemUser(), false);
            pageContainers.add(container);
        }

        return containers;
    }
    
	@WrapInTransaction
	@Override
	public void saveContainerStructures(final List<ContainerStructure> containerStructureList) throws DotStateException, DotDataException, DotSecurityException  {
	    
		if(!containerStructureList.isEmpty()) {

			try {

				//Get one of the relations to get the container id.
				String containerIdentifier = containerStructureList.get(0).getContainerId();
                String containerInode = containerStructureList.get(0).getContainerInode();

				HibernateUtil.delete("from container_structures in class com.dotmarketing.beans.ContainerStructure " +
                        "where container_id = '" + containerIdentifier + "'" +
                        "and container_inode = '" + containerInode+ "'");

				for(ContainerStructure containerStructure : containerStructureList){
					HibernateUtil.save(containerStructure);
				}
				
				//Add the list to the cache.
                final VersionInfo info = APILocator.getVersionableAPI().getVersionInfo(containerIdentifier);
				CacheLocator.getContainerCache().remove(info);

			} catch(DotHibernateException e){
				throw new DotDataException(e.getMessage(),e);

			}
		}
		
	}

	@CloseDBIfOpened
	@Override
	@SuppressWarnings({ "unchecked" })

	public List<ContainerStructure> getContainerStructures(final Container container) throws DotStateException, DotDataException, DotSecurityException  {

		final ContainerStructureFinderStrategyResolver resolver   =
				new ContainerStructureFinderStrategyResolver();

		final Optional<ContainerStructureFinderStrategy> strategy =
				resolver.get(container);

		final List<ContainerStructure> containerStructures = strategy.isPresent()? strategy.get().apply(container):
				resolver.getDefaultStrategy().apply(container);


		return containerStructures;
	}
	
    @CloseDBIfOpened
    @Override
    public List<ContentType> getContentTypesInContainer(final Container container)
			throws DotStateException, DotDataException, DotSecurityException {

		final List<ContainerStructure>  containerList = getContainerStructures(container);

		final List<ContentType> contentTypeList = new ArrayList<>();
		final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());

		for (final ContainerStructure containerStructure : containerList) {
			try {
				final ContentType type = contentTypeAPI.find(containerStructure.getStructureId());

				if(type == null) {
					continue;
				}

				contentTypeList .add(type);
			} catch (DotSecurityException e) {
				continue;
			}
		}

		return contentTypeList.stream()
				.sorted(Comparator.comparing(ContentType::name))
				.collect(CollectionsUtils.toImmutableList());
    }

	/**
	 * Return the {@link ContentType} that can be add by user into a specific {@link Container}
	 *
	 * @param user
	 * @param container
	 * @return
	 * @throws DotStateException
	 * @throws DotDataException
	 */
	@CloseDBIfOpened
	@Override
	public List<ContentType> getContentTypesInContainer(final User user, final Container container) throws
			DotStateException, DotDataException {

		try {
			final List<ContainerStructure>  containerStructureList = getContainerStructures(container);
			final Set<ContentType> contentTypeList =
                    new TreeSet<>(Comparator.comparing(ContentType::id));
			ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);
			PermissionAPI permissionAPI = APILocator.getPermissionAPI();

			for (final ContainerStructure containerStructure : containerStructureList) {
				try {
					final ContentType type = contentTypeAPI.find(containerStructure.getStructureId());

					if(type == null) {
						continue;
					}

					final boolean hasPermission = permissionAPI.doesUserHavePermission(type, PermissionAPI.PERMISSION_READ, user, false);

					if (!hasPermission){
						continue;
					}

					contentTypeList.add(type);
				} catch (DotSecurityException e) {
					continue;
				}
			}

			return contentTypeList.stream()
					.sorted(Comparator.comparing(ContentType::name))
					.collect(CollectionsUtils.toImmutableList());

		} catch (DotSecurityException e) {
			return Collections.EMPTY_LIST;
		}
	}

    @Deprecated
	public List<Structure> getStructuresInContainer(Container container) throws DotStateException, DotDataException, DotSecurityException  {
        return new StructureTransformer(getContentTypesInContainer(container)).asStructureList();

	}

	@CloseDBIfOpened
	@Override
	public List<Container> findContainersUnder(Host parentPermissionable) throws DotDataException {
		return containerFactory.findContainersUnder(parentPermissionable);
	}

	@WrapInTransaction
	@Override
	@SuppressWarnings("unchecked")
	public Container save(Container container, List<ContainerStructure> containerStructureList, Host host, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		Container currentContainer = null;
		List<Template> currentTemplates = null;
		Identifier identifier = null;
		boolean existingId=false;
		boolean existingInode=false;

		if(UtilMethods.isSet(container.getInode())) {
            try {
                Container existing=(Container) HibernateUtil.load(Container.class, container.getInode());
                existingInode = existing==null || !UtilMethods.isSet(existing.getInode());
            }
            catch(Exception ex) {
                existingInode=true;
            }
        }

		if (UtilMethods.isSet(container.getIdentifier())) {
		    identifier = APILocator.getIdentifierAPI().find(container.getIdentifier());
		    if(identifier!=null && UtilMethods.isSet(identifier.getId())) {
		        if(!existingInode) {
        		    currentContainer = getWorkingContainerById(container.getIdentifier(), user, respectFrontendRoles);
        			currentTemplates = InodeFactory.getChildrenClass(currentContainer, Template.class);
		        }
		    }
		    else {
		        existingId=true;
		        identifier=null;
		    }
		}

		if ((identifier != null && !existingInode)  && !permissionAPI.doesUserHavePermission(currentContainer, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to write the container.");
		}

		for (ContainerStructure cs : containerStructureList) {
			Structure st = CacheLocator.getContentTypeCache().getStructureByInode(cs.getStructureId());
			if((st != null && !existingInode) && !permissionAPI.doesUserHavePermission(st, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
				throw new DotSecurityException("You don't have permission to use the structure. Structure Name: " + st.getName());
			}
		}


		if(!permissionAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles)) {
			throw new DotSecurityException("You don't have permission to write on the given host.");
		}

		String userId = user.getUserId();
		container.setModUser(user.getUserId());
		container.setModDate(new Date());

		// it saves or updates the asset
		if (identifier != null) {
			container.setIdentifier(identifier.getId());
		} else {
		    Identifier ident= (existingId) ?
		           APILocator.getIdentifierAPI().createNew(container, host, container.getIdentifier()) :
			       APILocator.getIdentifierAPI().createNew(container, host);
			container.setIdentifier(ident.getId());
		}
		if(existingInode){
            save(container, container.getInode());
		}
        else{
            save(container);
        }

		APILocator.getVersionableAPI().setWorking(container);

		// Get templates of the old version so you can update the working
		// information to this new version.
		if (currentTemplates != null) {
			Iterator<Template> it = currentTemplates.iterator();

			// update templates to new version
			while (it.hasNext()) {
				Template parentInode = (Template) it.next();
				TreeFactory.saveTree(new Tree(parentInode.getInode(), container.getInode()));
			}
		}

		// save the container-structure relationships , issue-2093
		for (ContainerStructure cs : containerStructureList) {
			cs.setContainerId(container.getIdentifier());
            cs.setContainerInode(container.getInode());
		}
		saveContainerStructures(containerStructureList);
		// saves to working folder under velocity
		new ContainerLoader().invalidate(container);

		return container;
	}

	@WrapInTransaction
	@Override
	public boolean delete(final Container container, final User user, final boolean respectFrontendRoles) throws DotSecurityException, DotDataException {
		if(permissionAPI.doesUserHavePermission(container, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles)) {
			deleteContainerStructuresByContainer(container);
			return deleteAsset(container);
		} else {
			throw new DotSecurityException(WebKeys.USER_PERMISSIONS_EXCEPTION);
		}

	}

	@CloseDBIfOpened
	@Override
	public List<Container> findAllContainers(final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		return this.findAllContainers(APILocator.getHostAPI().findDefaultHost(user, respectFrontendRoles), user, respectFrontendRoles);
	}


	@CloseDBIfOpened
	@Override
	public List<Container> findAllContainers(final Host currentHost, final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		final RoleAPI roleAPI = APILocator.getRoleAPI();
		return user != null
				&& roleAPI.doesUserHaveRole(user, roleAPI.loadCMSAdminRole())?
				this.containerFactory.findAllContainers(currentHost):
				this.containerFactory.findContainers(user, false, null, null, null,null, null, 0, -1, "title ASC");
	}

	@CloseDBIfOpened
	@Override
	public Host getParentHost(final Container cont, final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		return hostAPI.findParentHost(cont, user, respectFrontendRoles);
	}

	@CloseDBIfOpened
	@Override
	public List<Container> findContainers(final User user, final boolean includeArchived,
			final Map<String, Object> params, final String hostId, final String inode, final String identifier, final String parent,
			final int offset, final int limit, final String orderBy) throws DotSecurityException,
			DotDataException {
		return containerFactory.findContainers(user, includeArchived, params, hostId, inode, identifier, parent, offset, limit, orderBy);
	}



	@CloseDBIfOpened
	@Override
	public List<Container> findContainersForStructure(final String structureInode,
			final boolean workingOrLiveOnly) throws DotDataException {
		return containerFactory.findContainersForStructure(structureInode, workingOrLiveOnly);
	}

	@Override
    public int deleteOldVersions(final Date assetsOlderThan) throws DotStateException, DotDataException {
        return deleteOldVersions(assetsOlderThan, Inode.Type.CONTAINERS.getValue());
    }

    @WrapInTransaction
    @Override
    public void deleteContainerStructureByContentType(final ContentType type)
            throws DotDataException {
            

      new DotConnect()
        .setSQL("DELETE FROM container_structures WHERE structure_id = ?")
        .addParam(type.id())
        .loadResult();
    }

    @CloseDBIfOpened
    @Override
    public List<Container> findContainersForStructure(final String structureInode) throws DotDataException {
        return containerFactory.findContainersForStructure(structureInode);
    }
    
    @WrapInTransaction
	@Override
	public void deleteContainerStructuresByContainer(final Container container)
			throws DotStateException, DotDataException, DotSecurityException {

		if(container != null && UtilMethods.isSet(container.getIdentifier()) && UtilMethods.isSet(container.getInode())){
			HibernateUtil.delete("from container_structures in class com.dotmarketing.beans.ContainerStructure " +
                    "where container_id = '" + container.getIdentifier() + "'");
			
			//Remove the list from cache.
			CacheLocator.getContentTypeCache().removeContainerStructures(container.getIdentifier(), container.getInode());
		}
	}

	@WrapInTransaction
	@Override
	public void deleteContainerContentTypesByContainerInode(final Container container) throws DotStateException, DotDataException {
		if (container != null && UtilMethods.isSet(container.getIdentifier()) && UtilMethods.isSet(container.getInode())) {
			DotConnect dc = new DotConnect();
			dc.setSQL("DELETE FROM container_structures WHERE container_inode = ? AND container_id = ?");
			dc.addParam(container.getInode());
			dc.addParam(container.getIdentifier());
			dc.loadResult();
			CacheLocator.getContentTypeCache().removeContainerStructures(container.getIdentifier(), container.getInode());
		}
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
	@WrapInTransaction
	public void updateUserReferences(final String userId, final String replacementUserId)throws DotDataException, DotSecurityException{
		containerFactory.updateUserReferences(userId, replacementUserId);
	}

	private  ResolvedPath resolvePath(final String inputPath,
									  final User user,
									  final boolean live,
									  final boolean respectFrontEndEndRoles) throws DotSecurityException, DotDataException {


		final FileAssetContainerUtil fileAssetContainerUtil = FileAssetContainerUtil.getInstance();
		final String relativePath;

		final Map<String, Host> hostsToFound = new LinkedHashMap<>();

		if (fileAssetContainerUtil.isFullPath(inputPath)) {
			final String hostName = fileAssetContainerUtil.getHostName(inputPath);
			relativePath = fileAssetContainerUtil.getPathFromFullPath(hostName, inputPath);
			final Host host = APILocator.getHostAPI().findByName(hostName, user, respectFrontEndEndRoles);

			if (host != null) {
				hostsToFound.put(host.getInode(), host);
			}
		} else {
			relativePath = inputPath;
		}

		try {
			final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();

			if (request != null) {
				final Host currentHost = WebAPILocator.getHostWebAPI().getCurrentHost(request, user);
				hostsToFound.put(currentHost.getInode(), currentHost);
			}
		} catch(DotSecurityException e) {

		}

		final Host defaultHost = APILocator.getHostAPI().findDefaultHost(user, respectFrontEndEndRoles);
		hostsToFound.put(defaultHost.getInode(), defaultHost);

		return find(relativePath, hostsToFound, user, live, respectFrontEndEndRoles);
	}

	@NotNull
	private ResolvedPath find(
			final String relativePath,
			final Map<String, Host> hostsToFound,
			final User user,
			boolean live,
			boolean respectFrontEndEndRoles) throws DotSecurityException, DotDataException {

		Container container;
		for (final Host host : hostsToFound.values()) {
			try {
				container = containerFactory.getContainerByFolderPath(relativePath, host, user, live, respectFrontEndEndRoles);

				if (container != null) {
					return  new ResolvedPath(host, relativePath, container);
				}
			} catch (NotFoundInDbException e) {
				continue;
			}
		}

		throw new NotFoundInDbException(String.format("File Container %s not found", relativePath));
	}

	private static class ResolvedPath {
		Host host;
		String relativePath;
		Container container;

		public ResolvedPath(Host host, String relativePath, Container container) {
			this.host = host;
			this.relativePath = relativePath;
			this.container = container;
		}
	}
}
