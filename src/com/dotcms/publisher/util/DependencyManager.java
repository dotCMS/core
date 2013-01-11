package com.dotcms.publisher.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.model.User;

public class DependencyManager {
	
	private Set<String> folders;
	private Set<String> htmlPages;
	private Set<String> templates;
	private Set<String> structures;
	private Set<String> containers;
	private Set<String> contents;
	private User user;
	
	public DependencyManager(User user) {
		folders = new HashSet<String>();
		htmlPages = new HashSet<String>();
		templates = new HashSet<String>();
		structures = new HashSet<String>();
		containers = new HashSet<String>();
		contents = new HashSet<String>();
		this.user = user;
	}
	
	public void setDependencies(PushPublisherConfig config) throws DotDataException {
		List<PublishQueueElement> assets = config.getAssets();
		
		for (PublishQueueElement asset : assets) {
			if(asset.getType().equals("htmlpage")) {
				htmlPages.add(asset.getAsset());
			} else if(asset.getType().equals("structure")) {
				structures.add(asset.getAsset());
			}
		}
		
		setHTMLPagesDependencies();
		setStructureDependencies();
		
		config.setFolders(folders);
		config.setHTMLPages(htmlPages);
		config.setTemplates(templates);
		config.setContainers(containers);
		config.setStructures(structures);
		config.setContents(contents);
	}
	
	public void setStructureDependencies() {
			
			for (String inode : structures) {
				Structure st = StructureCache.getStructureByInode(inode);
				contents.add(st.getHost()); // add the host dependency
				folders.add(st.getFolder()); // add the folder dependency
			}
			
	}
	
	public void setHTMLPagesDependencies() {
		try {
			
			IdentifierAPI idenAPI = APILocator.getIdentifierAPI();
			FolderAPI folderAPI = APILocator.getFolderAPI();
			
			for (String pageId : htmlPages) {
				Identifier iden = idenAPI.find(pageId);
				// Host dependency
				contents.add(iden.getHostId());
				// Templates dependencies are added inside HTMLPageBundler, not here
				Folder folder = folderAPI.findFolderByPath(iden.getParentPath(), iden.getHostId(), user, false);
				folders.add(folder.getInode());
				HTMLPage workingPage = APILocator.getHTMLPageAPI().loadWorkingPageById(pageId, user, false);
				HTMLPage livePage = APILocator.getHTMLPageAPI().loadLivePageById(pageId, user, false);
				// working template working page
				Template workingTemplateWP = APILocator.getTemplateAPI().findWorkingTemplate(workingPage.getTemplateId(), user, false);
				// live template working page
				Template liveTemplateWP = APILocator.getTemplateAPI().findLiveTemplate(workingPage.getTemplateId(), user, false);
				// live template live page
				Template liveTemplateLP = APILocator.getTemplateAPI().findLiveTemplate(livePage.getTemplateId(), user, false);
				// Containers dependencies 
				List<Container> containerList = new ArrayList<Container>();
				containerList.addAll(APILocator.getTemplateAPI().getContainersInTemplate(workingTemplateWP, user, false));
				containerList.addAll(APILocator.getTemplateAPI().getContainersInTemplate(liveTemplateWP, user, false));
				containerList.addAll(APILocator.getTemplateAPI().getContainersInTemplate(liveTemplateLP, user, false));
				
				for (Container container : containerList) {
					containers.add(container.getInode());
					// Structure dependencies
					structures.add(container.getStructureInode());
					List<MultiTree> treeList = MultiTreeFactory.getMultiTree(container.getIdentifier());
					
					for (MultiTree mt : treeList) {
						String contentIdentifier = mt.getChild();
						// Contents dependencies
						contents.add(contentIdentifier);
					}
				}
			}
		} catch (DotSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DotDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

}
