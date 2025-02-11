package com.dotmarketing.portlets.containers.business;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.config.DotInitializer;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.rendering.velocity.services.ContainerLoader;
import com.dotcms.system.event.local.model.Subscriber;
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
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.containers.model.SystemContainer;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.ApplicationContainerFolderListener;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.*;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import java.net.URL;
import java.util.*;
import java.util.function.Supplier;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_PUBLISH;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

import java.util.stream.Collectors;

/**
 * Implementation class of the {@link ContainerAPI}.
 * 
 * @author root
 * @version 1.0
 * @since Mar 22, 2012
 *
 */
public class ContainerAPIImpl extends BaseWebAssetAPI implements ContainerAPI, DotInitializer {

	protected PermissionAPI    permissionAPI;
	protected ContainerFactory containerFactory;
	protected HostAPI          hostAPI;
	protected FolderAPI        folderAPI;
	protected Lazy<Container> systemContainer = Lazy.of(() -> new SystemContainer());

	private static final String DEFAULT_CONTAINER_FILE_NAME = "com/dotmarketing/portlets/containers/business/default_container.vtl";

	/**
	 * Constructor
	 */
	public ContainerAPIImpl () {

        this.permissionAPI    = APILocator.getPermissionAPI();
        this.containerFactory = FactoryLocator.getContainerFactory();
        this.hostAPI          = APILocator.getHostAPI();
        this.folderAPI        = APILocator.getFolderAPI();
	}

	@Override
	public void init() {
		Logger.debug(this, ()-> "Initializing the System Container");
		this.systemContainer.get().setCode(codeFromFile());
	}

	/**
	 * Reads the Velocity code of the System Container from the appropriate {@link #DEFAULT_CONTAINER_FILE_NAME} file.
	 * This is the boilerplate that will be used to render any type of content that is added to the System Container.
	 *
	 * @return The Velocity code for the System Container.
	 */
	private String codeFromFile() {
		final ClassLoader loader = Thread.currentThread().getContextClassLoader();
		final URL resourceURL = loader.getResource(DEFAULT_CONTAINER_FILE_NAME);
		try {
			Logger.debug(this, ()-> "Reading System Template default code.");
			return IOUtils.toString(resourceURL, UtilMethods.getCharsetConfiguration());
		} catch (final Exception e) {
			Logger.error(this,
					String.format("An error occurred when reading System Container code: %s", e.getMessage()), e);
			return "<h1>$!{title}</h1>\n" +
					"#set($contentlet = $dotcontent.find($!{ContentIdentifier}))\n" +
					"#if (\"TITLE_IMAGE_NOT_FOUND\" != $!{contentlet.titleImage})\n" +
					"<img src=\"/contentAsset/raw-data/$!{ContentIdentifier}/$!{contentlet.titleImage}\">\n" +
					"#end";
		}
	}

	@Override
	public Container systemContainer() {
		return this.systemContainer.get();
	}

	@WrapInTransaction
	@Override
	public Container copy(final Container source, Host destination, final User user, final boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException {
		if (Container.SYSTEM_CONTAINER.equals(source.getIdentifier())) {
			final String errorMsg = "System Container cannot be copied.";
			Logger.error(this, errorMsg);
			throw new IllegalArgumentException(errorMsg);
		}
		if (!permissionAPI.doesUserHavePermission(source, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
			throw new DotSecurityException(
					String.format("User '%s' does not have READ permission on source Container '%s' [%s]",
							user.getUserId(), source.getName(), source.getIdentifier()));
		}

		if (!permissionAPI.doesUserHavePermission(destination, PermissionAPI.PERMISSION_WRITE, user,
				respectFrontendRoles)) {
			throw new DotSecurityException(
					String.format("User '%s' does not have WRITE permission on destination Container '%s' [%s]",
							user.getUserId(), destination.getName(), destination.getIdentifier()));
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
			final List<ContainerStructure> newContainerCS = new LinkedList<>();

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

		try {
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
		} catch (NotFoundInDbException e) {
			return Optional.empty();
		}

	}

	/**
	 *
	 * @param container
	 * @throws DotDataException
	 */
	private void save(final Container container) throws DotDataException {
		Logger.debug(this, ()-> "Saving container: " + container);
		if (Container.SYSTEM_CONTAINER.equals(container.getIdentifier())) {
			Logger.debug(this, "System Container cannot be saved.");
			return;
		}
		containerFactory.save(container);
	}

	/**
	 *
	 * @param container
	 * @param existingId
	 * @throws DotDataException
	 */
	private void save(final Container container, final String existingId) throws DotDataException {
		Logger.debug(this, ()-> "Saving container: " + container + " with existing Id " + existingId);
		if (Container.SYSTEM_CONTAINER.equals(container.getIdentifier())) {
			Logger.debug(this, "System Container cannot be saved/updated.");
			return;
		}
		containerFactory.save(container);
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
		String temp = containerTitle;
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
		if (Container.SYSTEM_CONTAINER.equals(inode)) {
			return this.systemContainer();
		}
	      final  Identifier  identifier = Try.of(()->APILocator.getIdentifierAPI().findFromInode(inode)).getOrNull();
        final Container container = this.isContainerFile(identifier) ?
				this.getWorkingContainerByFolderPath(identifier.getParentPath(), identifier.getHostId(), user, respectFrontendRoles):
                containerFactory.find(inode);

        if (container == null) {
            return null;
        }
        if (!permissionAPI.doesUserHavePermission(container, PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
			throw new DotSecurityException(
					String.format("User '%s' does not have READ permission on Container with Inode '%s'",
							user.getUserId(), inode));
        }

        return container;
    }

	@CloseDBIfOpened
	@Override
	@SuppressWarnings("unchecked")
	public Container getWorkingContainerById(final String identifierParameter, final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		if (Container.SYSTEM_CONTAINER.equals(identifierParameter)) {
			return this.systemContainer();
		}
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
	public Container getWorkingArchiveContainerByFolderPath(final String path, final Host host, final User user,
													 final boolean respectFrontEndPermissions) throws DotSecurityException, DotDataException {

		return this.containerFactory.getWorkingArchiveContainerByFolderPath(path, host, user, respectFrontEndPermissions);
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
		if (Container.SYSTEM_CONTAINER.equals(containerId)) {
			return this.systemContainer();
		}
        final  VersionInfo info = APILocator.getVersionableAPI().getVersionInfo(containerId);
        return info !=null? find(info.getWorkingInode(), user, respectFrontendRoles): null;
    }

	@Override
	@SuppressWarnings("unchecked")
	public Container getLiveContainerById(final String containerId, final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		if (Container.SYSTEM_CONTAINER.equals(containerId)) {
			return this.systemContainer();
		}
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
		if (Container.SYSTEM_CONTAINER.equals(containerId)) {
			return this.systemContainer();
		}
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
	    
		if (null != containerStructureList && !containerStructureList.isEmpty()) {
			//Get one of the relations to get the container id.
			final String containerIdentifier = containerStructureList.get(0).getContainerId();
			final String containerInode = containerStructureList.get(0).getContainerInode();
			try {
				HibernateUtil.delete("from container_structures in class com.dotmarketing.beans.ContainerStructure " +
                        "where container_id = '" + containerIdentifier + "'" +
                        "and container_inode = '" + containerInode+ "'");

				for (final ContainerStructure containerStructure : containerStructureList){
					HibernateUtil.save(containerStructure);
				}
				
				//Add the list to the cache.
                final VersionInfo info = APILocator.getVersionableAPI().getVersionInfo(containerIdentifier);
				CacheLocator.getContainerCache().remove(info);

			} catch (final DotHibernateException e) {
				final String errorMsg = String.format(
						"An error occurred when saving Container-to-Content Type associations for Container '%s': %s",
						containerIdentifier, e.getMessage());
				Logger.error(this, errorMsg, e);
				throw new DotDataException(errorMsg, e);
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
	public List<ContainerStructure> getRelatedContainerContentTypes(final Container container) throws
			DotHibernateException {
		List<ContainerStructure> relatedContainerContentTypes =
				containerFactory.getRelatedContainerContentTypes(container);
		relatedContainerContentTypes = relatedContainerContentTypes.stream().map((cs) -> {
			if (cs.getCode() == null) {
				cs.setCode(StringPool.BLANK);
			}
			return cs;
		}).collect(Collectors.toList());
		return relatedContainerContentTypes;
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
					Logger.debug(this, () -> String.format(
							"An error occurred when User '%s' tried to check information from Content Type '%s': %s",
							user.getUserId(), containerStructure.getStructureId(), container.getIdentifier()));
					continue;
				}
			}

			return contentTypeList.stream()
					.sorted(Comparator.comparing(ContentType::name))
					.collect(CollectionsUtils.toImmutableList());

		} catch (DotSecurityException e) {
			Logger.debug(this, () -> String.format(
					"An error occurred when User '%s' tried to get the Content Types from Container '%s' [%s]: %s",
					user.getUserId(), container.getName(), container.getIdentifier()));
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
	public Container save(final Container container,
			final List<ContainerStructure> containerStructureList, final Host host, final User user,
			final boolean respectFrontendRoles) throws DotDataException, DotSecurityException {
		if (Container.SYSTEM_CONTAINER.equals(container.getIdentifier())) {
			Logger.debug(this, "System Container cannot be saved/updated.");
			throw new IllegalArgumentException(
					"System Container and its associated data cannot be saved.");
		}
		if (!APILocator.getPermissionAPI()
				.doesUserHavePermission(host, PermissionAPI.PERMISSION_CAN_ADD_CHILDREN, user,
						respectFrontendRoles)
				|| !APILocator.getPermissionAPI()
				.doesUserHavePermissions(PermissionAPI.PermissionableType.CONTAINERS,
						PermissionAPI.PERMISSION_EDIT, user)) {

			Logger.info(this, "The user: " + user.getUserId()
					+ ", does not have ADD children permissions to the host: " + host.getHostname()
					+ " or add containers");
			throw new DotSecurityException(WebKeys.USER_PERMISSIONS_EXCEPTION);
		}
		Container currentContainer = null;
		List<Template> currentTemplates = null;
		Identifier identifier = null;
		boolean existingId = false;
		boolean existingInode = false;
		if (UtilMethods.isSet(container.getInode())) {
			try {
				Container existing = (Container) HibernateUtil.load(Container.class,
						container.getInode());
				existingInode = existing == null || !UtilMethods.isSet(existing.getInode());
			} catch (Exception ex) {
				existingInode = true;
			}
		}
		if (UtilMethods.isSet(container.getIdentifier())) {
			identifier = APILocator.getIdentifierAPI().find(container.getIdentifier());
			if (identifier != null && UtilMethods.isSet(identifier.getId())) {
				if (!existingInode) {
					currentContainer = getWorkingContainerById(container.getIdentifier(), user,
							respectFrontendRoles);
					currentTemplates = APILocator.getTemplateAPI()
							.findTemplatesByContainerInode(currentContainer.getInode());
				}
			} else {
				existingId = true;
				identifier = null;
			}
		}
		if ((identifier != null && !existingInode) && !permissionAPI.doesUserHavePermission(
				currentContainer, PermissionAPI.PERMISSION_WRITE, user, respectFrontendRoles)) {
			throw new DotSecurityException(
					String.format("User '%s' does not have WRITE permission on Container '%s'",
							user.getUserId(),
							container.getName()));
		}
		if (containerStructureList != null) {
			for (ContainerStructure cs : containerStructureList) {
				Structure st = CacheLocator.getContentTypeCache()
						.getStructureByInode(cs.getStructureId());
				if ((st != null && !existingInode) && !permissionAPI.doesUserHavePermission(st,
						PermissionAPI.PERMISSION_READ, user, respectFrontendRoles)) {
					throw new DotSecurityException(
							String.format(
									"User '%s' does not have WRITE permission on Content Type '%s'",
									user.getUserId(),
									st.getName()));
				}
			}
		}
		if (!permissionAPI.doesUserHavePermission(host, PermissionAPI.PERMISSION_WRITE, user,
				respectFrontendRoles)) {
			throw new DotSecurityException(
					String.format("User '%s' does not have WRITE permission on Site '%s'",
							user.getUserId(), host));
		}

		container.setModUser(user.getUserId());
		container.setModDate(new Date());

		// it saves or updates the asset
		if (identifier != null) {
			container.setIdentifier(identifier.getId());
		} else {
			Identifier ident = (existingId) ?
					APILocator.getIdentifierAPI()
							.createNew(container, host, container.getIdentifier()) :
					APILocator.getIdentifierAPI().createNew(container, host);
			container.setIdentifier(ident.getId());
		}

		if (existingInode) {
			save(container, container.getInode());
		} else {
			save(container);
		}

		APILocator.getVersionableAPI().setWorking(container);

		// Get templates of the old version so you can update the working
		// information to this new version.
		if (currentTemplates != null) {
			Iterator<Template> it = currentTemplates.iterator();
			// update templates to new version
			while (it.hasNext()) {
				Template parentInode = it.next();
				TreeFactory.saveTree(new Tree(parentInode.getInode(), container.getInode()));
			}
		}
		if (containerStructureList != null) {
			// save the container-structure relationships , issue-2093
			for (ContainerStructure cs : containerStructureList) {
				cs.setContainerId(container.getIdentifier());
				cs.setContainerInode(container.getInode());
			}
			saveContainerStructures(containerStructureList);
		}
		// saves to working folder under velocity
		new ContainerLoader().invalidate(container);

		return container;
	}

	@Override
	@WrapInTransaction
	public void deleteVersionByInode(final String inode) {
		Logger.debug(this, ()-> "Deleting container inode: " + inode);
		Try.run(()->containerFactory.deleteContainerByInode(inode)).onFailure(e -> new RuntimeException(e));
	}

	@WrapInTransaction
	@Override
	public boolean delete(final Container container, final User user, final boolean respectFrontendRoles) throws DotSecurityException, DotDataException {
		if (Container.SYSTEM_CONTAINER.equals(container.getIdentifier())) {
			Logger.debug(this, "System Container cannot be deleted.");
			throw new IllegalArgumentException("System Container cannot be deleted.");
		}
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
		if (Container.SYSTEM_CONTAINER.equals(cont.getIdentifier())) {
			return APILocator.systemHost();
		}
		return hostAPI.findParentHost(cont, user, respectFrontendRoles);
	}

	@Deprecated
	@Override
	public List<Container> findContainers(final User user, final boolean includeArchived,
			final Map<String, Object> params, final String siteId, final String inode, final String identifier, final String parent,
			final int offset, final int limit, final String orderBy) throws DotSecurityException,
			DotDataException {
		final SearchParams searchParams = SearchParams.newBuilder()
				.includeArchived(includeArchived)
				.filteringCriterion(params)
				.siteId(siteId)
				.containerInode(inode)
				.containerIdentifier(identifier)
				.contentTypeIdOrVar(parent)
				.offset(offset)
				.limit(limit)
				.orderBy(orderBy).build();
		return this.findContainers(user, searchParams);
	}

	@CloseDBIfOpened
	@Override
	public List<Container> findContainers(final User user, final SearchParams searchParams) throws DotSecurityException,
			DotDataException {
		// Include System Container only if required AND if the first page is being requested
		return searchParams.includeSystemContainer() && searchParams.offset() == 0 ?
				includeSystemContainer(this.containerFactory.findContainers(user, searchParams)) :
				this.containerFactory.findContainers(user, searchParams);
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
	public void publish(final Container container, final User user, final boolean respectAnonPerms) throws DotDataException, DotSecurityException {

		Logger.debug(this, ()-> "Doing publish of container: " + container.getIdentifier());

		//Check write Permissions over container
		if(!this.permissionAPI.doesUserHavePermission(container, PERMISSION_PUBLISH, user)) {

			Logger.error(this,"The user: " + user.getUserId() + " does not have Permissions to publish the container");
			throw new DotSecurityException("User does not have Permissions to publish the Container");
		}

		if(container instanceof FileAssetContainer) {

			final FileAssetContainer fileAssetContainer = (FileAssetContainer) container;
			final Host containerHost = fileAssetContainer.getHost();
			final Identifier idPropertiesVTL = APILocator.getIdentifierAPI().find(containerHost, fileAssetContainer.getPath() + "container.vtl");
			final Contentlet contentletVTL   = APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage(idPropertiesVTL.getId());
			APILocator.getContentletAPI().publish(contentletVTL, user, respectAnonPerms);
		} else {

			final Container containerWorkingVersion = this.getWorkingContainerById(
					container.getIdentifier(), user, respectAnonPerms);

			try {

				PublishFactory.publishAsset(container, user, respectAnonPerms);
			} catch (WebAssetException e) {

				Logger.error(this, e.getMessage(), e);
				throw new DotDataException(e);
			}

			//Remove live version from version_info
			containerWorkingVersion.setModDate(new Date());
			containerWorkingVersion.setModUser(user.getUserId());
			containerFactory.save(containerWorkingVersion);
		}

		//Clean-up the cache for this template
		CacheLocator.getContainerCache().remove(container);
		//remove template from the live directory
		new ContainerLoader().invalidate(container);
	}

	@WrapInTransaction
	@Override
	public void unpublish(final Container container, final User user, final boolean respectAnonPerms) throws DotDataException, DotSecurityException {

		Logger.debug(this, ()-> "Doing unpublish of container: " + container.getIdentifier());

		//Check write Permissions over container
		if(!this.permissionAPI.doesUserHavePermission(container, PERMISSION_WRITE, user)) {

			Logger.error(this,"The user: " + user.getUserId() + " does not have Permissions to write the container");
			throw new DotSecurityException("User does not have Permissions to write the Container");
		}

		if(container instanceof FileAssetContainer) {

			final FileAssetContainer fileAssetContainer = (FileAssetContainer) container;
			final Host containerHost = fileAssetContainer.getHost();
			final Identifier idPropertiesVTL = APILocator.getIdentifierAPI().find(containerHost, fileAssetContainer.getPath() + "container.vtl");
			final Contentlet contentletVTL   = APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage(idPropertiesVTL.getId());
			APILocator.getContentletAPI().unpublish(contentletVTL, user, respectAnonPerms);
		} else {

			final Container containerWorkingVersion = getWorkingContainerById(container.getIdentifier(), user,false);
			//Remove live version from version_info
			final String containerIdentifier = container.getIdentifier();
			APILocator.getVersionableAPI().removeLive(containerIdentifier);
			containerWorkingVersion.setModDate(new java.util.Date());
			containerWorkingVersion.setModUser(user.getUserId());
			containerFactory.save(containerWorkingVersion);
		}

		//Clean-up the cache for this template
		CacheLocator.getContainerCache().remove(container);
		//remove template from the live directory
		new ContainerLoader().invalidate(container);
	}

	@WrapInTransaction
	@Override
	public void archive(final Container container,
						final User user, final boolean respectAnonPerms) throws DotDataException, DotSecurityException {

		Logger.debug(this, ()-> "Doing archive of container: " + container.getIdentifier());

		//Check write Permissions over container
		if(!this.permissionAPI.doesUserHavePermission(container, PERMISSION_WRITE, user)){
			Logger.error(this,"The user: " + user.getUserId() + " does not have Permissions to write the container");
			throw new DotSecurityException("User does not have Permissions to write the Container");
		}

		//Check that the template is Unpublished
		if (container.isLive()) {
			Logger.error(this, "The Container: " + container.getName() + " can not be archive. "
					+ "Because it is live.");
			throw new DotStateException("Container must be unpublished before it can be archived");
		}

		if(container instanceof FileAssetContainer) {

			final FileAssetContainer fileAssetContainer = (FileAssetContainer) container;
			final Host containerHost = fileAssetContainer.getHost();
			final Identifier idPropertiesVTL = APILocator.getIdentifierAPI().find(containerHost, fileAssetContainer.getPath() + "container.vtl");
			final Contentlet contentletVTL   = APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage(idPropertiesVTL.getId());
			APILocator.getContentletAPI().archive(contentletVTL, user, respectAnonPerms);
		} else {
			archive(container, user);
		}
	}

	/**
	 * This method was extracted from {@link WebAssetFactory#archiveAsset(WebAsset, String)} }
	 * @param container
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	private void archive(final Container container, final User user) throws DotSecurityException, DotDataException {
		final Container containerLiveVersion    = getLiveContainerById(container.getIdentifier(),APILocator.systemUser(),false);
		final Container containerWorkingVersion = getWorkingContainerById(container.getIdentifier(),APILocator.systemUser(),false);
		if(containerLiveVersion!=null) {
			
			APILocator.getVersionableAPI().removeLive(container.getIdentifier());
		}
		containerWorkingVersion.setModDate(new java.util.Date());
		containerWorkingVersion.setModUser(user.getUserId());
		// sets deleted to true
		APILocator.getVersionableAPI().setDeleted(containerWorkingVersion, true);
		this.containerFactory.save(containerWorkingVersion);
	}

	@WrapInTransaction
	@Override
	public void unarchive(final Container container,
						final User user, final boolean respectAnonPerms) throws DotDataException, DotSecurityException {

		Logger.debug(this, ()-> "Doing unarchive of container: " + container.getIdentifier());

		//Check write Permissions over container
		if(!this.permissionAPI.doesUserHavePermission(container, PERMISSION_WRITE, user)){
			Logger.error(this,"The user: " + user.getUserId() + " does not have Permissions to write the container");
			throw new DotSecurityException("User does not have Permissions to write the Container");
		}

		//Check that the template is Unpublished
		if (!container.isArchived()) {
			Logger.error(this, "The Container: " + container.getName() + " can not be unarchive. "
					+ "Because it is not archived.");
			throw new DotStateException("Container must be archived before it can be unarchived");
		}

		if(container instanceof FileAssetContainer) {

			final FileAssetContainer fileAssetContainer = (FileAssetContainer) container;
			final Host containerHost = fileAssetContainer.getHost();
			final Identifier idPropertiesVTL = APILocator.getIdentifierAPI().find(containerHost, fileAssetContainer.getPath() + "container.vtl");
			// todo: find here archived
			final Contentlet contentletVTL   = APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage(idPropertiesVTL.getId(), true);
			APILocator.getContentletAPI().unarchive(contentletVTL, user, respectAnonPerms);
		} else {
			unarchive(container, user);
		}
	}

	private void unarchive(final Container container, final User user) throws DotSecurityException, DotDataException {
		final Container containerWorkingVersion = getWorkingContainerById(container.getIdentifier(),APILocator.systemUser(),false);
		containerWorkingVersion.setModDate(new java.util.Date());
		containerWorkingVersion.setModUser(user.getUserId());
		// sets deleted to false
		APILocator.getVersionableAPI().setDeleted(containerWorkingVersion, false);
		this.containerFactory.save(containerWorkingVersion);
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
			Logger.debug(ContainerAPIImpl.class,
					() -> String.format("The User %s don't have read permission in the default Host, so it is going to be excluded to resolve the follow path %s",
							user.getUserId(), inputPath));
		}

		try {
			final Host defaultHost = APILocator.getHostAPI().findDefaultHost(user, respectFrontEndEndRoles);
			hostsToFound.put(defaultHost.getInode(), defaultHost);
		} catch(DotSecurityException e) {
			Logger.debug(ContainerAPIImpl.class,
					() -> String.format("The User %s don't have read permission in the default Host, so it is going to be excluded to resolve the follow path %s",
					user.getUserId(), inputPath));
		}

		return find(relativePath, hostsToFound, user, live, respectFrontEndEndRoles);
	}

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
			} catch (NotFoundInDbException | DotSecurityException e) {
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

	@Subscriber
	public void onCopySite(final SiteCreatedEvent event)
			throws DotDataException, DotSecurityException {
		final Folder appContainerFolder = APILocator.getFolderAPI().findFolderByPath(Constants.CONTAINER_FOLDER_PATH,
				APILocator.getHostAPI().find(event.getSiteIdentifier(),APILocator.systemUser(),false),
				APILocator.systemUser(), false);

		APILocator.getFolderAPI().subscribeFolderListener(appContainerFolder, new ApplicationContainerFolderListener(),
				childName -> null != childName && childName.endsWith(Constants.VELOCITY_FILE_EXTENSION));
	}

	/**
	 * Utility method used to include the {@link SystemContainer} object as part of the set of Containers that are being
	 * returned.
	 *
	 * @param originalContainers The original list of {@link Container} objects.
	 *
	 * @return The list of Containers including the System Container object.
	 */
	private List<Container> includeSystemContainer(final List<Container> originalContainers) {
		final PaginatedArrayList<Container> containers = new PaginatedArrayList<>();
		if (originalContainers instanceof PaginatedArrayList) {
			containers.setQuery(PaginatedArrayList.class.cast(originalContainers).getQuery());
			containers.setTotalResults(PaginatedArrayList.class.cast(originalContainers).getTotalResults());
		}
		containers.add(systemContainer());
		containers.addAll(originalContainers);
		return containers;
	}

}
