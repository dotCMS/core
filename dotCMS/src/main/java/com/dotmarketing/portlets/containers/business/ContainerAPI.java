package com.dotmarketing.portlets.containers.business;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rest.api.v1.container.ContainerForm;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Provides access to the information of {@link Container} objects in dotCMS.
 * <p>
 * Containers allow users to specify the different places of a template where
 * content authors can add information inside an HTML page. Containers define
 * the list of Content Types that they are able to display.
 * 
 * @author root
 * @version 1.0
 * @since Mar 22, 2012
 *
 */
public interface ContainerAPI {

	String CODE					= FileAssetContainerUtil.CODE;
	String PRE_LOOP             = FileAssetContainerUtil.PRE_LOOP;
	String POST_LOOP            = FileAssetContainerUtil.POST_LOOP;
	String CONTAINER_META_INFO  = FileAssetContainerUtil.CONTAINER_META_INFO;
	String DEFAULT_CONTAINER_LAYOUT = FileAssetContainerUtil.DEFAULT_CONTAINER_LAYOUT;

	/**
	 * Returns the System Container object. This is a special kind of Container that is meant to render any sort of
	 * Content Type that exists in the dotCMS repository. It allows Users and/or Content Authors to easily add
	 * information to a page without having to set up all the required structures for correctly putting an HTML Page
	 * together.
	 *
	 * @return The System {@link Container} instance.
	 */
	Container systemContainer();

	/**
	 * Copies container to the specified host
	 *
	 * @param source
	 * @param destination
	 * @param user
	 * @param respectFrontendRoles
	 * @return Container
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public Container copy(Container source, Host destination, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Finds a container by identifier or path
	 * In the case it is a path, will try to resolve the host by 1) the host in the path if any.
	 * 2) the current host (if could retrieve it)
	 * 3) the default host
	 * @param idOrPath
	 * @param user
	 * @param live
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public Optional<Container> findContainer(String idOrPath, User user, boolean live, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException;

	/**
	 * Returns the working container by the id
	 *
	 * @param identifier
	 * @param user
	 * @param respectFrontendRoles
	 * @return Container
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public Container getWorkingContainerById(String identifier, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Returns the Container based on the folder; this method is mostly used when the container is file asset based.
	 *
	 * @param folder
	 * @param showLive true if wants the live, false if wants the working
	 * @return Container
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	Container getContainerByFolder(final Folder folder, final User user, final boolean showLive) throws DotSecurityException, DotDataException;

	/**
	 * Returns the Container based on the folder and host; this method is mostly used when the container is file asset based.
	 * @param folder
	 * @param host
	 * @param user
	 * @param showLive
	 * @return
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	Container getContainerByFolder(final Folder folder, final Host host, final User user, final boolean showLive) throws DotSecurityException, DotDataException;

	/**
	 * Returns the working container by path and host; this method is mostly used when the container is file asset based.
	 * @param path
	 * @param host
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return Container
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	Container getWorkingContainerByFolderPath(final String path, final Host host, final User user, final boolean respectFrontEndPermissions) throws DotSecurityException, DotDataException;

	/**
	 * Returns the working (including archive) container by path and host; this method is mostly used when the container is file asset based.
	 * @param path
	 * @param host
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return Container
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	Container getWorkingArchiveContainerByFolderPath(final String path, final Host host, final User user, final boolean respectFrontEndPermissions) throws DotSecurityException, DotDataException;

	/***
	 * Similar to the {@link #getWorkingContainerByFolderPath(String, Host, User, boolean)} but the host will be figured out from the path, it is particular useful when you
	 * have the  full path such as //demo.dotcms.com/application/containers/large-column/
	 * @param fullContainerPathWithHost String for instance //demo.dotcms.com/application/containers/large-column/
	 * @param user
	 * @param respectFrontEndPermissions
	 * @param resourceHost {@link Supplier} this supplier will be called in case the fullContainerPathWithHost hasn't a host, if null will use the default one, for instance if fullContainerPathWithHost is (/application/containers/large-column/) will call the supplier
	 * @return Container
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	Container getWorkingContainerByFolderPath(final String fullContainerPathWithHost, final User user, final boolean respectFrontEndPermissions, final Supplier<Host> resourceHost) throws DotSecurityException, DotDataException;

	/**
	 * Returns the live container by path and host; this method is mostly used when the container is file asset based.
	 * @param path
	 * @param host
	 * @param user
	 * @param respectFrontEndPermissions
	 * @return Container
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	Container getLiveContainerByFolderPath(final String path, final Host host, final User user, final boolean respectFrontEndPermissions) throws DotSecurityException, DotDataException;

	/**
	 * Similar to the {@link #getLiveContainerByFolderPath(String, Host, User, boolean)} but the host will be figured out from the path, it is particular useful when you
	 * 	 * have the  full path such as //demo.dotcms.com/application/containers/large-column/
	 * 	 * @param fullContainerPathWithHost String for instance //demo.dotcms.com/application/containers/large-column/
	 * @param path
	 * @param host
	 * @param user
	 * @param respectFrontEndPermissions
	 * resourceHost {@link Supplier} this supplier will be called in case the fullContainerPathWithHost hasn't a host, if null will use the default one, for instance if fullContainerPathWithHost is (/application/containers/large-column/) will call the supplier
	 * @return Container
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	Container getLiveContainerByFolderPath(final String fullContainerPathWithHost, final User user, final boolean respectFrontEndPermissions, final Supplier<Host> resourceHost) throws DotSecurityException, DotDataException;


	/**
	 * Returns the live container by the id
	 *
	 * @param identifier
	 * @param user
	 * @param respectFrontendRoles
	 * @return Container
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	Container getLiveContainerById(String identifier, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;


	/**
	 *
	 * Retrieves a list of container-structure relationships by container
	 *
	 * @param container
	 * @return List of ContainerStructure
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotStateException
	 *
	 */
	List<ContainerStructure> getContainerStructures(Container container) throws DotStateException, DotDataException, DotSecurityException;

	/**
	 * Returns the list of associated Container-to-Content-Type relationships for a given Container. This list
	 * determines what Contentlets or a specific type can be added and rendered by a Container.
	 *
	 * @param container The {@link Container} whose associated Content Types will be determined.
	 *
	 * @return the list of {@link ContainerStructure} objects.
	 */
	public List<ContainerStructure> getRelatedContainerContentTypes(final Container container) throws
			DotHibernateException;

	/**
	 *
	 * Retrieves the list of structures related to the given container
	 *
	 * @param container
	 * @return List of Structure
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotStateException
	 *
	 */
	List<Structure> getStructuresInContainer(Container container) throws DotStateException, DotDataException, DotSecurityException;

	/**
	 *
	 * saves a list of container-structure relationships
	 *
	 * @param containerStructureList
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotStateException
	 *
	 */
	void saveContainerStructures(List<ContainerStructure> containerStructureList) throws DotStateException, DotDataException, DotSecurityException;

	/**
	 *
	 * Deletes the container-structure relationships for the given container identifier. Inode does not matter.
	 *
	 * @param container
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotStateException
	 *
	 */
	void deleteContainerStructuresByContainer(Container container) throws DotStateException, DotDataException, DotSecurityException;

	/**
	 *
	 * Deletes the Container-Content Type relationships for the given container
	 * Inode.
	 *
	 * @param container
	 *            - The {@link Container} whose Content Type relationships will
	 *            be deleted.
	 * @throws DotDataException
	 *             An error occurred when deleting the data.
	 * @throws DotStateException
	 *             A system error occurred.
	 */
	void deleteContainerContentTypesByContainerInode(final Container container) throws DotStateException,
			DotDataException;

	/**
	 * Retrieves all the containers attached to the given host
	 * @param parentHost
	 * @author David H Torres
	 * @return
	 * @throws DotDataException
	 *
	 */
	List<Container> findContainersUnder(Host parentHost) throws DotDataException;

	/**
	 * Retrieves the list of all containers in the system
	 * The fs container on the default host will return the path as a relative, the rest of them will include absolute path with the host appended
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	List<Container> findAllContainers(User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Retrieves the list of all containers in the system
	 * The fs container on the current host will return the path as a relative, the rest of them will include absolute path with the host appended
	 * @param currentHost {@link Host} this is the current host, all fs containers will use relative path for them
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	List<Container> findAllContainers(final Host currentHost, final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Save container
	 *
	 * @param container
	 * @param containerStructureList
	 * @param host
	 * @param user
	 * @param respectFrontendRoles
	 * @return Container
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	Container save(final Container container,final List<ContainerStructure> containerStructureList,final Host host,final User user,final boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Deletes the template version by inode
	 * @param inode String
	 */
	void deleteVersionByInode(String inode);

	/**
	 * Delete the specified container
	 *
	 * @param container
	 * @param user
	 * @param respectFrontendRoles
	 * @return boolean
	 * @throws DotSecurityException
	 * @throws Exception
	 */
	boolean delete(Container container, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException;

	/**
	 * Retrieves the parent host of a container
	 * @throws DotSecurityException
	 */
	Host getParentHost(Container cont, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * @param user
	 * @param includeArchived
	 * @param params
	 * @param hostId
	 * @param inode
	 * @param identifier
	 * @param parent
	 * @param offset
	 * @param limit
	 * @param orderBy
	 *
	 * @return
	 *
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @deprecated Use method {@link #findContainers(User, SearchParams)} instead, which allows you to set the same
	 * query parameters in the method's signature via the {@link SearchParams} object more easily.
	 */
	@Deprecated
	List<Container> findContainers(User user, boolean includeArchived, Map<String,Object> params, String hostId, String inode, String identifier, String parent, int offset, int limit, String orderBy) throws DotSecurityException, DotDataException;

    /**
     * 
     * @param assetsOlderThan
     * @return
     * @throws DotStateException
     * @throws DotDataException
     */
    public int deleteOldVersions(Date assetsOlderThan) throws DotStateException, DotDataException;

    /**
	 * Method will replace user references of the given userId in containers 
	 * with the replacement user id 
	 * @param userId User Identifier
	 * @param replacementUserId The user id of the replacement user
	 * @throws DotDataException There is a data inconsistency
	 * @throws DotStateException There is a data inconsistency
	 * @throws DotSecurityException 
	 */
	void updateUserReferences(String userId, String replacementUserId)throws DotDataException, DotSecurityException;

    void deleteContainerStructureByContentType(ContentType type) throws DotDataException;
    
	Container find(String inode, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	List<Container> getContainersOnPage(IHTMLPage page) throws DotStateException, DotDataException, DotSecurityException;

    List<ContentType> getContentTypesInContainer(Container container)
            throws DotStateException, DotDataException, DotSecurityException;

	/**
	 * Publish a container
	 * @param container {@link Container}
	 * @param user {@link User}
	 * @param respectAnonPerms {@link Boolean}
	 */
	void publish(Container container, User user, boolean respectAnonPerms) throws DotDataException, DotSecurityException;

	/**
	 * Unpublish a container
	 * @param container {@link Container}
	 * @param user {@link User}
	 * @param respectAnonPerms {@link Boolean}
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	 void unpublish(final Container container, final User user, final boolean respectAnonPerms) throws DotDataException, DotSecurityException;

	/**
	 * Archive a container
	 * @param container {@link Container}
	 * @param user {@link User}
	 * @param respectAnonPerms {@link Boolean}
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
    void archive(Container container, User user, boolean respectAnonPerms) throws DotDataException, DotSecurityException;

	/**
	 * Unarchive a container
	 * @param container {@link Container}
	 * @param user {@link User}
	 * @param respectAnonPerms  {@link Boolean}
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	void unarchive(Container container, User user, boolean respectAnonPerms) throws DotDataException, DotSecurityException;

	/**
	 * Returns a list of Containers in the system, based on the specified search criteria.
	 *
	 * @param user        The {@link User} executing this action.
	 * @param queryParams The {@link SearchParams} object containing the different combinations of search terms.
	 *
	 * @return The list of {@link Container} objects that match the search criteria.
	 *
	 * @throws DotSecurityException The specified user cannot perform this action.
	 * @throws DotDataException     An error occurred when interacting with the data source.
	 */
	List<Container> findContainers(final User user, final SearchParams queryParams) throws DotSecurityException, DotDataException;

	/**
	 * Retrieves containers using the specified structure
	 *
	 * @param structureInode
	 * @return
	 * @throws DotDataException
	 */
	List<Container> findContainersForStructure(String structureInode)
			throws DotDataException;

	/**
	 * Retrieves containers using the specified structure
	 *
	 * @param structureInode
	 * @param workingOrLiveOnly
	 * @return
	 * @throws DotDataException
	 */
	List<Container> findContainersForStructure(String structureInode, boolean workingOrLiveOnly)
			throws DotDataException;


	/**
	 * Return the {@link ContentType} into a {@link Container} for a specific {@link User}, return a empty List if the
	 * user don't have READ permission in any {@link ContentType} into the specific {@link Container}
	 *
	 * @param user
	 * @param container
	 * @return
	 * @throws DotStateException
	 * @throws DotDataException
	 */
	List<ContentType> getContentTypesInContainer(User user, Container container) throws DotStateException, DotDataException;

	/**
	 * Utility class that allows users to specify different filtering parameters when retrieving Containers from the
	 * dotCMS content repository. This keeps API methods from having extremely long signatures, which makes it simpler
	 * and cleaner as users just need to add the ones they really need.
	 *
	 * @author Jose Castro
	 * @since Apr 5th, 2022
	 */
	class SearchParams {

		private final String containerIdentifier;
		private final String containerInode;
		private final String siteId;

		private final boolean includeArchived;
		private final boolean includeSystemContainer;

		private final Map<String, Object> filteringCriteria;
		private final String contentTypeIdOrVar;
		private final int offset;
		private final int limit;
		private final String orderBy;

		/**
		 * Private class constructor which creates an instance of this class based on information from its Builder
		 * class
		 *
		 * @param builder The {@link Builder} object containing the search criteria for Containers.
		 */
		private SearchParams(final Builder builder) {
			this.containerIdentifier = builder.containerIdentifier;
			this.containerInode = builder.containerInode;
			this.siteId = builder.siteId;
			this.includeArchived = builder.includeArchived;
			this.includeSystemContainer = builder.includeSystemContainer;
			this.filteringCriteria = builder.filteringCriteria;
			this.contentTypeIdOrVar = builder.contentTypeIdOrVar;
			this.offset = builder.offset;
			this.limit = builder.limit;
			this.orderBy = builder.orderBy;
		}

		/**
		 * Finds information for a specific Container based on its Identifier.
		 *
		 * @return The ID of the Container you need to retrieve information from.
		 */
		public String containerIdentifier() {
			return this.containerIdentifier;
		}

		/**
		 * Finds information for a specific Container based on its Inode.
		 *
		 * @return The Inode of the Container you need to retrieve information from.
		 */
		public String containerInode() {
			return this.containerInode;
		}

		/**
		 * The Identifier of the Site where the API will search for Containers. If not specified, Containers from any
		 * Site will be returned.
		 *
		 * @return The ID of the Site.
		 */
		public String siteId() {
			return this.siteId;
		}

		/**
		 * Determines whether archived Containers must be included in the result set or not.
		 *
		 * @return If archived Containers must be retrieved, set to {@code true}. Otherwise, set to {@code false}.
		 */
		public boolean includeArchived() {
			return this.includeArchived;
		}

		/**
		 * Determines whether the System Container must be included in the result set or not.
		 *
		 * @return If the System Container must be retrieved, set to {@code true}. Otherwise, set to {@code false}.
		 */
		public boolean includeSystemContainer() {
			return this.includeSystemContainer;
		}

		/**
		 * Finds information based on one or more fields of a Container. If a given criterion is an Identifier, Inode,
		 * or any String, a LIKE-type clause will be used to look for matches.
		 *
		 * @return A {@link Map} with the different optional filtering criteria.
		 */
		public Map<String, Object> filteringCriteria() {
			return this.filteringCriteria;
		}

		/**
		 * Finds information based on one specific field in a Container. If a given criterion is an Identifier, Inode,
		 * or any String, a LIKE-type clause will be used to look for matches.
		 *
		 * @param key The name of the filtering parameter -- usually the database column name
		 *
		 * @return The value of such a filtering parameter.
		 */
		public <T> T filteringCriterion(final String key) {
			return (T) this.filteringCriteria.get(key);
		}

		/**
		 * Finds information for Containers that are related to a specific Content Type.
		 *
		 * @return The ID or Velocity Variable Name of the Content Type that must be associated to the Containers that
		 * will be returned.
		 */
		public String contentTypeIdOrVar() {
			return this.contentTypeIdOrVar;
		}

		/**
		 * The result set offset -- for pagination purposes. Defaults to zero.
		 * @return The result set's offset.
		 */
		public int offset() {
			return this.offset;
		}

		/**
		 * the maximum number of results that will be returned -- usually for pagination purposes. Defaults to 500.
		 *
		 * @return The result set's limit.
		 */
		public int limit() {
			return this.limit;
		}

		/**
		 * The ordering criterion for the results. Defaults to {@code "mod_date desc"}.
		 *
		 * @return The order-by clause for the result set.
		 */
		public String orderBy() {
			return this.orderBy;
		}

		/**
		 * Creates a new instance of the Builder object that will instantiate the {@link SearchParams} class.
		 *
		 * @return The Builder instance.
		 */
		public static Builder newBuilder() {
			return new Builder();
		}

		/**
		 * Builder class used to set up user-specified search criteria for Containers in the system.
		 */
		public static final class Builder {

			private String containerIdentifier = StringPool.BLANK;
			private String containerInode = StringPool.BLANK;
			private String siteId = StringPool.BLANK;

			private boolean includeArchived = Boolean.FALSE;
			private boolean includeSystemContainer = Boolean.FALSE;

			private Map<String, Object> filteringCriteria;
			private String contentTypeIdOrVar = StringPool.BLANK;
			private int offset = 0;
			private int limit = 500;
			private String orderBy = "mod_date desc";

			private Builder() {

			}

			/**
			 * Creates an instance of the Query Params object with user-specified criteria that will be used to retrieve
			 * Containers from the content repository.
			 *
			 * @return An instance of the {@link SearchParams} class.
			 */
			public SearchParams build() {
				return new SearchParams(this);
			}

			/**
			 * Finds information for a specific Container based on its Identifier.
			 *
			 * @param containerIdentifier The ID of the Container you need to retrieve information from.
			 *
			 * @return The {@link SearchParams.Builder} object.
			 */
			public Builder containerIdentifier(final String containerIdentifier) {
				this.containerIdentifier = containerIdentifier;
				return this;
			}

			/**
			 * Finds information for a specific Container based on its Inode.
			 *
			 * @param containerInode The Inode of the Container you need to retrieve information from.
			 *
			 * @return The {@link SearchParams.Builder} object.
			 */
			public Builder containerInode(final String containerInode) {
				this.containerInode = containerInode;
				return this;
			}

			/**
			 * The Identifier of the Site where the API will search for Containers. If not specified, Containers from
			 * any Site will be returned.
			 *
			 * @param siteId The ID of the Site.
			 *
			 * @return The {@link SearchParams.Builder} object.
			 */
			public Builder siteId(final String siteId) {
				this.siteId = siteId;
				return this;
			}

			/**
			 * Determines whether archived Containers must be included in the result set or not.
			 *
			 * @param includeArchived If archived Containers must be retrieved, set to {@code true}. Otherwise, set to
			 *                        {@code false}.
			 *
			 * @return The {@link SearchParams.Builder} object.
			 */
			public Builder includeArchived(final boolean includeArchived) {
				this.includeArchived = includeArchived;
				return this;
			}

			/**
			 * Determines whether the System Container must be included in the result set or not.
			 *
			 * @param includeSystemContainer If the System Container must be retrieved, set to {@code true}. Otherwise,
			 *                               set to {@code false}.
			 *
			 * @return The {@link SearchParams.Builder} object.
			 */
			public Builder includeSystemContainer(final boolean includeSystemContainer) {
				this.includeSystemContainer = includeSystemContainer;
				return this;
			}

			/**
			 * Finds information based on one or more fields of a Container. If a given criterion is an Identifier,
			 * Inode, or any String, a LIKE-type clause will be used to look for matches.
			 *
			 * @param filteringCriteria A {@link Map} with the different optional filtering criteria.
			 *
			 * @return The {@link SearchParams.Builder} object.
			 */
			public Builder filteringCriterion(final Map<String, Object> filteringCriteria) {
				this.filteringCriteria = filteringCriteria;
				return this;
			}

			/**
			 * Finds information based on one specific field in a Container. If a given criterion is an Identifier,
			 * Inode, or any String, a LIKE-type clause will be used to look for matches.
			 *
			 * @param key   The name of the filtering parameter -- usually the database column name
			 * @param value The value of the filtering parameter.
			 *
			 * @return The {@link SearchParams.Builder} object.
			 */
			public Builder filteringCriteria(final String key, final Object value) {
				if (null == this.filteringCriteria) {
					this.filteringCriteria = new HashMap<>();
				}
				this.filteringCriteria.put(key, value);
				return this;
			}

			/**
			 * Finds information for Containers that are related to a specific Content Type.
			 *
			 * @param contentTypeIdOrVar The ID or Velocity Variable Name of the Content Type that must be associated to
			 *                           the Containers that will be returned.
			 *
			 * @return The {@link SearchParams.Builder} object.
			 */
			public Builder contentTypeIdOrVar(final String contentTypeIdOrVar) {
				this.contentTypeIdOrVar = contentTypeIdOrVar;
				return this;
			}

			/**
			 * Sets the result offset -- for pagination purposes. Defaults to zero.
			 *
			 * @param offset The result set's offset.
			 *
			 * @return The {@link SearchParams.Builder} object.
			 */
			public Builder offset(final int offset) {
				this.offset = offset;
				return this;
			}

			/**
			 * Sets the maximum number of results that will be returned -- usually for pagination purposes. Defaults to
			 * 500.
			 *
			 * @param limit The result set's limit.
			 *
			 * @return The {@link SearchParams.Builder} object.
			 */
			public Builder limit(final int limit) {
				this.limit = limit;
				return this;
			}

			/**
			 * The ordering criterion for the results. Defaults to {@code "mod_date desc"}.
			 *
			 * @param orderBy The order-by clause for the result set.
			 *
			 * @return The {@link SearchParams.Builder} object.
			 */
			public Builder orderBy(final String orderBy) {
				this.orderBy = orderBy;
				return this;
			}

		}

	}
	//
}
