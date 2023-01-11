package com.dotmarketing.portlets.templates.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.model.User;

import java.util.List;
import java.util.Map;

public interface TemplateFactory {


	List<Template> findTemplatesAssignedTo(Host parentHost, final boolean includeArchived) throws DotDataException;
		
	List<Template> findTemplatesUserCanUse(User user, String hostId, String query,boolean searchHost, int offset, int limit) throws DotDataException, DotSecurityException ;

//	void delete(Template template) throws DotDataException;
	
	/**
	 * Save template into a persistent repository.
	 * 
	 * @param template
	 * @throws DotDataException
	 */
	void save(Template template) throws DotDataException;
	void save(Template template, String inode)throws DotDataException;
	
	/**
	 * Delete template from cache.
	 * 
	 * @param template
	 * @throws DotDataException
	 */
	void deleteFromCache(Template template) throws DotDataException;

	Template findWorkingTemplateByName(String name, Host host) throws DotDataException;
	
	List<Template> findTemplates(User user, boolean includeArchived, Map<String,Object> params, String hostId, String inode, String identifier, String parent, int offset, int limit, String orderBy) throws DotSecurityException, DotDataException;
	
	Template find(String inode) throws DotStateException, DotDataException;
	
	List<Container> getContainersInTemplate(Template template, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Parse a html with #parseContainer(id, uuid)
	 * The id could be an identifier or path
	 * @param templateBody String
	 * @return List
	 */
	List<ContainerUUID> getContainerUUIDFromHTML(final String templateBody);

	Template copyTemplate(Template currentTemplate, Host host) throws DotDataException, DotSecurityException;
	
	/**
	 *
	 * Updates the template's theme without creating new version.
	 * @param templateInode
	 * @param theme
	 *
	 */
	void updateThemeWithoutVersioning(String templateInode, String theme) throws DotDataException;

   /**
	 * Method will replace user references of the given userId in templates
	 * with the replacement user Id  
	 * @param userId User Identifier
	 * @param replacementUserId The user id of the replacement user
	 * @throws DotDataException There is a data inconsistency
	 * @throws DotStateException There is a data inconsistency
	 * @throws DotSecurityException 
	 */
	 void updateUserReferences(String userId, String replacementUserId)throws DotDataException, DotSecurityException;

	/**
	 * Brings the versions of a specific template, if bringOldVersions is true brings all the versions,
	 * if is set to false, only brings the working and the live version
	 * @param identifier id of the template
	 * @param bringOldVersions true = all versions, false = only live and working
	 * @return
	 * @throws DotDataException
	 */
	List<Template> findAllVersions(final Identifier identifier, final boolean bringOldVersions)
			throws DotDataException;

	/**
	 * Deletes a template by inode
	 * @param templateInode templateInode to be deleted
	 * @throws DotDataException
	 */
	void deleteTemplateByInode(final String templateInode) throws DotDataException;

	/**
	 * Finds the templates where the containerInode is set as a parent in the tree table.
	 * Was created to recreate InodeFactory.getChildrenClass(Inode p, Class c) since it uses Hibernate
	 * and Templates were remove from the hbm files.
	 * @param containerInode
	 * @return
	 * @throws DotDataException
	 */
	List<Template> findTemplatesByContainerInode(final String containerInode) throws DotDataException;

	/**
	 * Get a template based on a folder (non-db)
	 * A folder could be considered as a template if:
	 * 1) Is under /application/templates
	 * 2) Has a file called properties.vtl
	 * @param site site where the folder lives
	 * @param folder folder that should be a template
	 * @param user
	 * @param showLive
	 * @return Template
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	Template getTemplateByFolder(final Host site, final Folder folder, final User user, final boolean showLive) throws DotDataException,DotSecurityException;

	/**
	 * Return the List of Page's Version tha used the {@link Template}
	 * In case that the Template is not used by any Page then it returns an empty List
	 * If the Template is used by more than one version of a Page then it returns all the
	 * Inodes of the different versions.
	 *
	 * @param templateId Template's ID that we are looking for
	 * @return
	 */
	List<HTMLPageVersion> getPages(final String templateId) throws DotDataException, DotSecurityException;

	class HTMLPageVersion {
		private String identifier;
		private String inode;
		private String variantName;
		private long languageId;

		private HTMLPageVersion(final String identifier, final String inode, final String variantName,
				final long languageId) {
			this.identifier = identifier;
			this.inode = inode;
			this.variantName = variantName;
			this.languageId = languageId;
		}

		public String getIdentifier() {
			return identifier;
		}

		public String getInode() {
			return inode;
		}

		public String getVariantName() {
			return variantName;
		}

		public long getLanguageId() {
			return languageId;
		}

		static class Builder {
			private String identifier;
			private String inode;
			private String variantName;
			private long languageId;

			public Builder identifier(final String identifier) {
				this.identifier = identifier;
				return this;
			}

			public Builder inode(final String inode) {
				this.inode = inode;
				return this;
			}

			public Builder variantName(final String variantName) {
				this.variantName = variantName;
				return this;
			}

			public Builder languageId(final long languageId) {
				this.languageId = languageId;
				return this;
			}

			public HTMLPageVersion build(){
				return new HTMLPageVersion(identifier, inode, variantName, languageId);
			}
		}
	}
}
