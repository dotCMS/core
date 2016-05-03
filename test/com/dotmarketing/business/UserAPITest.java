package com.dotmarketing.business;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.DwrAuthenticationUtil;
import com.dotcms.TestBase;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.ajax.RoleAjax;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPIImpl;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.InodeUtils;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

/**
 * 
 * @author Oswaldo Gallango
 *
 */
public class UserAPITest extends TestBase{

	private static User systemUser;
	private static DwrAuthenticationUtil dwrAuthentication = null;

	@BeforeClass
	public static void prepare () throws DotSecurityException, DotDataException {

		//Setting the test user
		systemUser = APILocator.getUserAPI().getSystemUser();

		// User authentication through DWR is required for RoleAjax class
		Map<String, Object> sessionAttrs = new HashMap<String, Object>();
		sessionAttrs.put("USER_ID", "dotcms.org.1");
		dwrAuthentication = new DwrAuthenticationUtil();
		dwrAuthentication.setupWebContext(null, sessionAttrs);

	}

	/**
	 * Testing {@link UserAPI#delete(User, User, User, boolean)}
	 *
	 * @param userToDelete User to delete 
	 * @param replacementUser User to replace the db reference of the user to delete
	 * @param user User requesting the delete user
	 * @param respectFrontEndRoles
	 * @throws DotDataException If the user to delete or the replacement user are not set
	 * @throws DotSecurityException If the user requesting the delete doesn't have permission
	 * @throws SystemException 
	 * @throws PortalException 
	 * @throws WebAssetException 
	 */
	@Test
	public void delete() throws DotDataException,DotSecurityException, PortalException, SystemException, WebAssetException{

		UserAPI userAPI = APILocator.getUserAPI();
		RoleAPI roleAPI = APILocator.getRoleAPI();
		PermissionAPI perAPI = APILocator.getPermissionAPI();
		HostAPI hostAPI = APILocator.getHostAPI();
		TemplateAPI templateAPI = APILocator.getTemplateAPI();
		ContainerAPI containerAPI = APILocator.getContainerAPI();
		ContentletAPI conAPI = APILocator.getContentletAPI();
		LayoutAPI layoutAPI = APILocator.getLayoutAPI();
		VersionableAPI versionableAPI = APILocator.getVersionableAPI();
		HTMLPageAssetAPI htmlPageAssetAPI = APILocator.getHTMLPageAssetAPI();
		FolderAPI folderAPI = APILocator.getFolderAPI();
		IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();
		long langId =APILocator.getLanguageAPI().getDefaultLanguage().getId();
		String id = String.valueOf( new Date().getTime());
		//Host host = hostAPI.findByName("demo.dotcms.com", systemUser, false);
		//add host
		Host host = new Host();
		host.setHostname("test"+id+".demo.dotcms.com");
		host=APILocator.getHostAPI().save(host, systemUser, false);

		/*add role*/
		Role newRole = new Role();
		String newRoleName = "role"+id;
		newRole.setName(newRoleName);
		newRole.setRoleKey(newRoleName);
		newRole.setEditLayouts(true);
		newRole.setEditPermissions(true);
		newRole.setEditUsers(true);
		newRole.setSystem(false);
		newRole = roleAPI.save(newRole);

		//set permission to role
		Map<String,String> mm=new HashMap<String,String>();
		mm.put("individual",Integer.toString(PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN | PermissionAPI.PERMISSION_EDIT_PERMISSIONS));
		mm.put("hosts", "0");
		mm.put("structures", Integer.toString(PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_PUBLISH));
		mm.put("content", Integer.toString(PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_PUBLISH));
		mm.put("containers", Integer.toString(PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_PUBLISH));
		mm.put("links", Integer.toString(PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_PUBLISH));
		mm.put("files", Integer.toString(PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_PUBLISH));
		mm.put("templates", Integer.toString(PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_PUBLISH));
		mm.put("templateLayouts", Integer.toString(PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_PUBLISH));
		mm.put("pages", Integer.toString(PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_PUBLISH));
		mm.put("folders", Integer.toString(PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE | PermissionAPI.PERMISSION_CAN_ADD_CHILDREN));
		mm.put("rules", "0");
		mm.put("categories", "0");
		new RoleAjax().saveRolePermission(newRole.getId(), host.getIdentifier(), mm, false);

		Structure pageStructure = StructureFactory.getStructureByVelocityVarName(HTMLPageAssetAPIImpl.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_VARNAME);
		perAPI.permissionIndividually(host, pageStructure, systemUser, false);

		List<Permission> permissions = new ArrayList<Permission>();
		Permission p1=new Permission();
		p1.setPermission(PermissionAPI.PERMISSION_READ);
		p1.setRoleId(newRole.getId());
		p1.setInode(pageStructure.getInode());
		permissions.add(p1);

		p1=new Permission();
		p1.setPermission(PermissionAPI.PERMISSION_WRITE);
		p1.setRoleId(newRole.getId());
		p1.setInode(pageStructure.getInode());
		permissions.add(p1);

		p1=new Permission();
		p1.setPermission(PermissionAPI.PERMISSION_PUBLISH);
		p1.setRoleId(newRole.getId());
		p1.setInode(pageStructure.getInode());
		permissions.add(p1);

		perAPI.assignPermissions(permissions, pageStructure, systemUser, false);

		//add layout
		List<Layout> layouts = layoutAPI.findAllLayouts();
		for(Layout l : layouts){
			if(l.getName().equals("Site Browser") || l.getName().equals("Content") || l.getName().equals("Content Types")){
				roleAPI.addLayoutToRole(l, newRole);
			}
		}

		//add users with role
		String newUserName = "user"+id;
		User newUser = userAPI.createUser( newUserName + "@test.com", newUserName + "@test.com" );
		newUser.setFirstName( newUserName );
		newUser.setLastName( "TestUser" );
		userAPI.save( newUser, systemUser, false );

		roleAPI.addRoleToUser(newRole, newUser);

		String replacementUserName = "replacementuser"+id;
		User replacementUser = userAPI.createUser( replacementUserName + "@test.com", replacementUserName + "@test.com" );
		replacementUser.setFirstName( replacementUserName );
		replacementUser.setLastName( "TestUser" );
		userAPI.save( replacementUser, systemUser, false );

		roleAPI.addRoleToUser(newRole, replacementUser);

		//login as newUser
		dwrAuthentication.shutdownWebContext();
		Map<String, Object> sessionAttrs = new HashMap<String, Object>();
		sessionAttrs.put("USER_ID", newUser.getUserId());
		dwrAuthentication = new DwrAuthenticationUtil();
		dwrAuthentication.setupWebContext(null, sessionAttrs);
		//add folder
		Folder ftest = folderAPI.createFolders("/folderTest"+id, host, newUser, false);
		ftest.setOwner(newUser.getUserId());
		folderAPI.save(ftest, newUser, false);

		//add structure
		Structure st = new Structure();
		st.setHost(host.getIdentifier());
		st.setFolder(ftest.getInode());
		st.setName("structure"+id);
		st.setStructureType(Structure.STRUCTURE_TYPE_CONTENT);
		st.setOwner(newUser.getUserId());
		st.setVelocityVarName("structure"+id);
		StructureFactory.saveStructure(st);
		CacheLocator.getContentTypeCache().add(st);

		Field field1 = new Field("title", Field.FieldType.TEXT, Field.DataType.TEXT, st,true, true, true, 3, "", "", "", true, false, true);
		field1.setVelocityVarName("title");
		field1.setListed(true);
		FieldFactory.saveField(field1);
		FieldsCache.addField(field1);

		Field field2 = new Field("body", Field.FieldType.TEXT_AREA, Field.DataType.LONG_TEXT, st,true, true, true, 4, "", "", "", true, false, true);
		field2.setVelocityVarName("body");
		FieldFactory.saveField(field2);
		FieldsCache.addField(field2);

		//add container
		Container container = new Container();
		String containerName="container"+id;
		container.setFriendlyName(containerName);
		container.setTitle(containerName);
		container.setOwner(newUser.getUserId());
		container.setMaxContentlets(5);
		container.setPreLoop("preloop code");
		container.setCode("<div><h3>content $!{title}</h3><p>$!{body}</p></div>");
		container.setPostLoop("postloop code");

		List<ContainerStructure> csList = new ArrayList<ContainerStructure>();
		ContainerStructure cs = new ContainerStructure();
		cs.setStructureId(st.getInode());
		cs.setCode(container.getCode());
		csList.add(cs);
		container = containerAPI.save(container, csList, host, newUser, false);
		PublishFactory.publishAsset(container,newUser, false, false);

		//add template
		String templateBody="<html><body> #parseContainer('"+container.getIdentifier()+"') </body></html>";
		String templateTitle="template"+id;

		Template template=new Template();
		template.setTitle(templateTitle);
		template.setBody(templateBody);
		template.setOwner(newUser.getUserId());
		template = templateAPI.saveTemplate(template, host, newUser, false);
		PublishFactory.publishAsset(template, newUser, false, false);
		//add page
		String page0Str ="page"+id;

		Contentlet contentAsset=new Contentlet();
		contentAsset.setStructureInode(HTMLPageAssetAPIImpl.DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
		contentAsset.setHost(host.getIdentifier());
		contentAsset.setProperty(HTMLPageAssetAPIImpl.FRIENDLY_NAME_FIELD, page0Str);
		contentAsset.setProperty(HTMLPageAssetAPIImpl.URL_FIELD, page0Str);
		contentAsset.setProperty(HTMLPageAssetAPIImpl.TITLE_FIELD, page0Str);
		contentAsset.setProperty(HTMLPageAssetAPIImpl.CACHE_TTL_FIELD, "0");
		contentAsset.setProperty(HTMLPageAssetAPIImpl.TEMPLATE_FIELD, template.getIdentifier());
		contentAsset.setLanguageId(langId);
		contentAsset.setFolder(ftest.getInode());
		contentAsset=conAPI.checkin(contentAsset, newUser, false);
		conAPI.publish(contentAsset, newUser, false);

		//add content
		String title ="content"+id;
		Contentlet contentAsset2=new Contentlet();
		contentAsset2.setStructureInode(st.getInode());
		contentAsset2.setHost(host.getIdentifier());
		contentAsset2.setProperty("title", title);
		contentAsset2.setLanguageId(langId);
		contentAsset2.setProperty("body", title);
		contentAsset2.setFolder(ftest.getInode());
		contentAsset2=conAPI.checkin(contentAsset2, newUser, false);
		conAPI.publish(contentAsset2, newUser, false);

		/*Relate content to page*/
		MultiTree m = new MultiTree(contentAsset.getIdentifier(), container.getIdentifier(), contentAsset2.getIdentifier());
		MultiTreeFactory.saveMultiTree(m);

		//add menu link
		String linkStr="link"+id;
		Link link = new Link();
		link.setTitle(linkStr);
		link.setFriendlyName(linkStr);
		link.setParent(ftest.getInode());
		link.setTarget("_blank");
		link.setOwner(newUser.getUserId());
		link.setModUser(newUser.getUserId());
		IHTMLPage page =htmlPageAssetAPI.getPageByPath(ftest.getPath()+page0Str, host, langId, true);

		Identifier internalLinkIdentifier = identifierAPI.findFromInode(page.getIdentifier());
		link.setLinkType(Link.LinkType.INTERNAL.toString());
		link.setInternalLinkIdentifier(internalLinkIdentifier.getId());
		link.setProtocal("http://");

		StringBuffer myURL = new StringBuffer();
		if(InodeUtils.isSet(internalLinkIdentifier.getHostId())) {
			myURL.append(host.getHostname());
		}
		myURL.append(internalLinkIdentifier.getURI());
		link.setUrl(myURL.toString());
		WebAssetFactory.createAsset(link, newUser.getUserId(), ftest);
		versionableAPI.setLive(link);

		//login as admin
		dwrAuthentication.shutdownWebContext();
		sessionAttrs = new HashMap<String, Object>();
		sessionAttrs.put("USER_ID", "dotcms.org.1");
		dwrAuthentication = new DwrAuthenticationUtil();
		dwrAuthentication.setupWebContext(null, sessionAttrs);

		//validation
		assertTrue(userAPI.userExistsWithEmail(newUser.getEmailAddress()));
		assertNotNull(roleAPI.loadRoleByKey(newUser.getUserId()));

		assertTrue(link.getOwner().equals(newUser.getUserId()));
		assertTrue(link.getModUser().equals(newUser.getUserId()));

		assertTrue(contentAsset2.getOwner().equals(newUser.getUserId()));
		assertTrue(contentAsset2.getModUser().equals(newUser.getUserId()));

		assertTrue(contentAsset.getOwner().equals(newUser.getUserId()));
		assertTrue(contentAsset.getModUser().equals(newUser.getUserId()));

		assertTrue(container.getOwner().equals(newUser.getUserId()));
		assertTrue(container.getModUser().equals(newUser.getUserId()));

		assertTrue(template.getOwner().equals(newUser.getUserId()));
		assertTrue(template.getModUser().equals(newUser.getUserId()));

		assertTrue(ftest.getOwner().equals(newUser.getUserId()));

		//delete user
		userAPI.delete(newUser, replacementUser, systemUser,false);

		//validate references deleted user and references updated
		try {
			assertNull(userAPI.loadByUserByEmail(newUser.getEmailAddress(),systemUser,false));			
		}catch(com.dotmarketing.business.NoSuchUserException e){
			//enter here if the user doesn't exist as is expected
		}
		assertNull(roleAPI.loadRoleByKey(newUser.getUserId()));

		assertNotNull(userAPI.loadByUserByEmail(replacementUser.getEmailAddress(),systemUser,false));

		link = APILocator.getMenuLinkAPI().find(link.getInode(), systemUser, false);
		assertTrue(link.getOwner().equals(replacementUser.getUserId()));
		assertTrue(link.getModUser().equals(replacementUser.getUserId()));

		page =htmlPageAssetAPI.getPageByPath(ftest.getPath()+page0Str, host, langId, true);
		assertTrue(page.getOwner().equals(replacementUser.getUserId()));
		assertTrue(page.getModUser().equals(replacementUser.getUserId()));

		List<Contentlet> contentAssets = conAPI.findByStructure(st, systemUser, false, 100,0);
		for(Contentlet content: contentAssets){
			assertTrue(content.getOwner().equals(replacementUser.getUserId()));
			assertTrue(content.getModUser().equals(replacementUser.getUserId()));
		}
		
		container = containerAPI.getLiveContainerById(container.getIdentifier(), systemUser, false);
		assertTrue(container.getOwner().equals(replacementUser.getUserId()));
		assertTrue(container.getModUser().equals(replacementUser.getUserId()));

		template = templateAPI.find(template.getInode(), systemUser, false);
		assertTrue(template.getOwner().equals(replacementUser.getUserId()));
		assertTrue(template.getModUser().equals(replacementUser.getUserId()));

		ftest = folderAPI.findFolderByPath(ftest.getPath(), host, systemUser, false);
		assertTrue(ftest.getOwner().equals(replacementUser.getUserId()));

		dwrAuthentication.shutdownWebContext();
	}


}
