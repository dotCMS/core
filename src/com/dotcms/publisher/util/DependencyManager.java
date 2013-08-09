package com.dotcms.publisher.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.PushPublisherConfig.Operation;
import com.dotcms.publishing.DotBundleException;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotIdentifierStateException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class DependencyManager {

	private DependencySet hosts;
	private DependencySet folders;
	private DependencySet htmlPages;
	private DependencySet templates;
	private DependencySet structures;
	private DependencySet containers;
	private DependencySet contents;
	private DependencySet links;
	private DependencySet relationships;

	private Set<String> hostsSet;
	private Set<String> foldersSet;
	private Set<String> htmlPagesSet;
	private Set<String> templatesSet;
	private Set<String> structuresSet;
	private Set<String> containersSet;
	private Set<String> contentsSet;
	private Set<String> linksSet;

	private User user;

	private PushPublisherConfig config;

	public DependencyManager(User user, PushPublisherConfig config) {
		this.config = config;
		// these ones store the assets that will be sent in the bundle
		boolean isPublish=config.getOperation().equals(Operation.PUBLISH);
		hosts = new DependencySet(config.getId(), "host", config.isDownloading(), isPublish);
		folders = new DependencySet(config.getId(), "folder", config.isDownloading(), isPublish);
		htmlPages = new DependencySet(config.getId(), "htmlPage", config.isDownloading(), isPublish);
		templates = new DependencySet(config.getId(), "template", config.isDownloading(), isPublish);
		structures = new DependencySet(config.getId(), "structure", config.isDownloading(), isPublish);
		containers = new DependencySet(config.getId(), "container", config.isDownloading(), isPublish);
		contents = new DependencySet(config.getId(), "content", config.isDownloading(), isPublish);
		relationships = new DependencySet(config.getId(), "relationship", config.isDownloading(), isPublish);
		links = new DependencySet(config.getId(),"links",config.isDownloading(), isPublish);

		// these ones are for being iterated over to solve the asset's dependencies
		hostsSet = new HashSet<String>();
		foldersSet = new HashSet<String>();
		htmlPagesSet = new HashSet<String>();
		templatesSet = new HashSet<String>();
		structuresSet = new HashSet<String>();
		containersSet = new HashSet<String>();
		contentsSet = new HashSet<String>();
		linksSet = new HashSet<String>();
		
		this.user = user;
	}

	public void setDependencies() throws DotDataException, DotBundleException {
		List<PublishQueueElement> assets = config.getAssets();

		for (PublishQueueElement asset : assets) {
			if(asset.getType().equals("htmlpage")) {
				try {
					HTMLPage page = APILocator.getHTMLPageAPI().loadLivePageById(asset.getAsset(), user, false);

					if(page==null) {
						page = APILocator.getHTMLPageAPI().loadWorkingPageById(asset.getAsset(), user, false);
					}

					htmlPages.add(asset.getAsset(), page.getModDate());
					htmlPagesSet.add(asset.getAsset());
				} catch (Exception e) {
					Logger.error(getClass(), "Couldn't add the HtmlPage to the Bundle. Bundle ID: " + config.getId() + ", HTMLPage ID: " + asset.getAsset(), e);
				}

			} else if(asset.getType().equals("structure")) {
				try {
					Structure st = StructureCache.getStructureByInode(asset.getAsset());
					structures.add(asset.getAsset(), st.getModDate());
					structuresSet.add(asset.getAsset());
				} catch (Exception e) {
					Logger.error(getClass(), "Couldn't add the Structure to the Bundle. Bundle ID: " + config.getId() + ", Structure ID: " + asset.getAsset(), e);
				}

			} else if(asset.getType().equals("template")) {
				try {
					Template t = APILocator.getTemplateAPI().findLiveTemplate(asset.getAsset(), user, false);

					if(t==null) {
						t = APILocator.getTemplateAPI().findWorkingTemplate(asset.getAsset(), user, false);
					}

					templates.add(asset.getAsset(), t.getModDate());
					templatesSet.add(asset.getAsset());
				} catch (Exception e) {
					Logger.error(getClass(), "Couldn't add the Template to the Bundle. Bundle ID: " + config.getId() + ", Template ID: " + asset.getAsset(), e);
				}
			} else if(asset.getType().equals("containers")) {
				try {
					Container c = APILocator.getContainerAPI().getLiveContainerById(asset.getAsset(), user, false);

					if(c==null) {
						c = APILocator.getContainerAPI().getWorkingContainerById(asset.getAsset(), user, false);
					}

					containers.add(asset.getAsset(), c.getModDate());
					containersSet.add(asset.getAsset());
				} catch (DotSecurityException e) {
					Logger.error(getClass(), "Couldn't add the Container to the Bundle. Bundle ID: " + config.getId() + ", Container ID: " + asset.getAsset(), e);
				}
			} else if(asset.getType().equals("folder")) {
				try {
					Folder f = APILocator.getFolderAPI().find(asset.getAsset(), user, false);
					folders.add(asset.getAsset(), f.getModDate());
					foldersSet.add(asset.getAsset());
				} catch (DotSecurityException e) {
					Logger.error(getClass(), "Couldn't add the Folder to the Bundle. Bundle ID: " + config.getId() + ", Folder ID: " + asset.getAsset(), e);
				}
			} else if(asset.getType().equals("host")) {
				try {
					Host h = APILocator.getHostAPI().find(asset.getAsset(), user, false);
					hosts.add(asset.getAsset(), h.getModDate());
					hostsSet.add(asset.getAsset());
				} catch (DotSecurityException e) {
					Logger.error(getClass(), "Couldn't add the Host to the Bundle. Bundle ID: " + config.getId() + ", Host ID: " + asset.getAsset(), e);
				}
			} else if(asset.getType().equals("links")) {
			    try {
                    Link link = (Link) APILocator.getVersionableAPI().findLiveVersion(asset.getAsset(), user, false);
                    if(link==null || !InodeUtils.isSet(link.getInode())) {
                        link = APILocator.getMenuLinkAPI().findWorkingLinkById(asset.getAsset(), user, false);
                    }
                    links.add(asset.getAsset(),link.getModDate());
                    linksSet.add(asset.getAsset());
                } catch (DotSecurityException e) {
                    Logger.error(getClass(), "Couldn't add the Host to the Bundle. Bundle ID: " + config.getId() + ", Host ID: " + asset.getAsset(), e);
                }
			}
		}

		if(config.getOperation().equals(Operation.PUBLISH)) {
    		setHostDependencies();
    		setFolderDependencies();
    		setHTMLPagesDependencies();
    		setTemplateDependencies();
    		setContainerDependencies();
    		setStructureDependencies();
    		setLinkDependencies();
    		setContentDependencies(config.getLuceneQueries());
		}else{
            contents.addAll( PublisherUtil.getContentIds( config.getLuceneQueries() ) );
        }


		config.setHostSet(hosts);
		config.setFolders(folders);
		config.setHTMLPages(htmlPages);
		config.setTemplates(templates);
		config.setContainers(containers);
		config.setStructures(structures);
		config.setContents(contents);
		config.setLinks(links);
		config.setRelationships(relationships);
	}
	
	private void setLinkDependencies() {
	    for(String linkId : linksSet) {
	        try {
                Identifier ident=APILocator.getIdentifierAPI().find(linkId);
                Folder ff = APILocator.getFolderAPI().findFolderByPath(ident.getParentPath(), ident.getHostId(), user, false);
                folders.add(ff.getInode(), ff.getModDate());
                foldersSet.add(ff.getInode());
                
                Host hh=APILocator.getHostAPI().find(ident.getHostId(), user, false);
                hosts.add(hh.getIdentifier(),hh.getModDate());
                hostsSet.add(hh.getIdentifier());
            } catch (Exception e) {
                Logger.error(this, "can't load menuLink deps "+linkId,e);
            }
	    }
	}

	private void setHostDependencies() {
		try {
			for (String id : hosts) {
				Host h = APILocator.getHostAPI().find(id, user, false);

				// Template dependencies
				List<Template> templateList = APILocator.getTemplateAPI().findTemplatesAssignedTo(h);
				for (Template template : templateList) {
					templates.add(template.getIdentifier(), template.getModDate());
					templatesSet.add(template.getIdentifier());
				}

				// Container dependencies
				List<Container> containerList = APILocator.getContainerAPI().findContainersUnder(h);
				for (Container container : containerList) {
					containers.add(container.getIdentifier(), container.getModDate());
					containersSet.add(container.getIdentifier());
				}

				// Content dependencies
				String luceneQuery = "+conHost:" + h.getIdentifier();

				List<Contentlet> contentList = APILocator.getContentletAPI().search(luceneQuery, 0, 0, null, user, false);
				for (Contentlet contentlet : contentList) {
					contents.add(contentlet.getIdentifier(), contentlet.getModDate());
					contentsSet.add(contentlet.getIdentifier());
				}

				// Structure dependencies
				List<Structure> structuresList = StructureFactory.getStructuresUnderHost(h, user, false);;
				for (Structure structure : structuresList) {
					structures.add(structure.getInode(), structure.getModDate());
					structuresSet.add(structure.getInode());
				}

				// Folder dependencies
				List<Folder> folderList = APILocator.getFolderAPI().findFoldersByHost(h, user, false);
				for (Folder folder : folderList) {
					folders.add(folder.getInode(), folder.getModDate());
					foldersSet.add(folder.getInode());
				}
			}

		} catch (DotSecurityException e) {

			Logger.error(this, e.getMessage(),e);
		} catch (DotDataException e) {

			Logger.error(this, e.getMessage(),e);
		}
	}

	private void setFolderDependencies() {
		try {
			List<Folder> folderList = new ArrayList<Folder>();

			for (String id : folders) {
				Folder f = APILocator.getFolderAPI().find(id, user, false);
				// Parent folder
				Folder parent = APILocator.getFolderAPI().findParentFolder(f, user, false);
				if(UtilMethods.isSet(parent)) {
					folders.add(parent.getInode(), parent.getModDate());
					foldersSet.add(parent.getInode());
				}

				folderList.add(f);
			}

			setFolderListDependencies(folderList);
		} catch (DotSecurityException e) {

			Logger.error(this, e.getMessage(),e);
		} catch (DotDataException e) {

			Logger.error(this, e.getMessage(),e);
		}
	}

	private void setFolderListDependencies(List<Folder> folderList) throws DotIdentifierStateException, DotDataException, DotSecurityException {
		for (Folder f : folderList) {

			// Add folder even if empty
			folders.add(f.getInode(), f.getModDate());
			foldersSet.add(f.getInode());

			// Host dependency
			Host h = APILocator.getHostAPI().find(f.getHostId(), user, false);
			hosts.add(f.getHostId(), h.getModDate());
			hostsSet.add(f.getHostId());

			// Content dependencies
			String luceneQuery = "+conFolder:" + f.getInode();

			List<Contentlet> contentList = APILocator.getContentletAPI().search(luceneQuery, 0, 0, null, user, false);
			for (Contentlet contentlet : contentList) {
				contents.add(contentlet.getIdentifier(), contentlet.getModDate());
				contentsSet.add(contentlet.getIdentifier());
			}

			// Menu Link dependencies

			List<Link> linkList = APILocator.getMenuLinkAPI().findFolderMenuLinks(f);
			for (Link link : linkList) {
				links.add(link.getIdentifier(), link.getModDate());
				linksSet.add(link.getIdentifier());
			}

			// Structure dependencies
			List<Structure> structureList = APILocator.getFolderAPI().getStructures(f, user, false);

			for (Structure structure : structureList) {
				structures.add(structure.getInode(), structure.getModDate());
				structuresSet.add(structure.getInode());
			}

			// HTML Page dependencies
			List<HTMLPage> pages = APILocator.getFolderAPI().getHTMLPages(f, user, false);

			for (HTMLPage p : pages) {
				htmlPages.add(p.getIdentifier(), p.getModDate());
				htmlPagesSet.add(p.getIdentifier());
			}

			setFolderListDependencies(APILocator.getFolderAPI().findSubFolders(f, user, false));
		}

	}

	private void setHTMLPagesDependencies() {
		try {

			IdentifierAPI idenAPI = APILocator.getIdentifierAPI();
			FolderAPI folderAPI = APILocator.getFolderAPI();
			List<Container> containerList = new ArrayList<Container>();

			for (String pageId : htmlPagesSet) {
				Identifier iden = idenAPI.find(pageId);


				// Host dependency
				Host h = APILocator.getHostAPI().find(iden.getHostId(), user, false);
				hosts.add(iden.getHostId(), h.getModDate());
				hostsSet.add(iden.getHostId());
				Folder folder = folderAPI.findFolderByPath(iden.getParentPath(), iden.getHostId(), user, false);
				folders.add(folder.getInode(), folder.getModDate());
				foldersSet.add(folder.getInode());
				HTMLPage workingPage = APILocator.getHTMLPageAPI().loadWorkingPageById(pageId, user, false);
				HTMLPage livePage = APILocator.getHTMLPageAPI().loadLivePageById(pageId, user, false);

				// working template working page
				Template workingTemplateWP = null;
				// live template working page
				Template liveTemplateWP = null;

				if(workingPage!=null) {
					workingTemplateWP = APILocator.getTemplateAPI().findWorkingTemplate(workingPage.getTemplateId(), user, false);
					liveTemplateWP = APILocator.getTemplateAPI().findLiveTemplate(workingPage.getTemplateId(), user, false);
					// Templates dependencies
					templates.add(workingPage.getTemplateId(), workingTemplateWP.getModDate());
					templatesSet.add(workingPage.getTemplateId());
				}

				Template liveTemplateLP = null;

				// live template live page
				if(livePage!=null) {
					liveTemplateLP = APILocator.getTemplateAPI().findLiveTemplate(livePage.getTemplateId(), user, false);
					// Templates dependencies
					templates.add(livePage.getTemplateId(), livePage.getModDate());
					templatesSet.add(livePage.getTemplateId());
				}

				// Containers dependencies
				containerList.clear();

				if(workingTemplateWP!=null && InodeUtils.isSet(workingTemplateWP.getInode()))
					containerList.addAll(APILocator.getTemplateAPI().getContainersInTemplate(workingTemplateWP, user, false));
				if(liveTemplateWP!=null && InodeUtils.isSet(liveTemplateWP.getInode()))
					containerList.addAll(APILocator.getTemplateAPI().getContainersInTemplate(liveTemplateWP, user, false));
				if(liveTemplateLP!=null && InodeUtils.isSet(liveTemplateLP.getInode()))
					containerList.addAll(APILocator.getTemplateAPI().getContainersInTemplate(liveTemplateLP, user, false));

				for (Container container : containerList) {
					containers.add(container.getIdentifier(), container.getModDate());
					containersSet.add(container.getIdentifier());
					// Structure dependencies
					List<ContainerStructure> csList = APILocator.getContainerAPI().getContainerStructures(container);

					for (ContainerStructure containerStructure : csList) {
						Structure st = StructureCache.getStructureByInode(containerStructure.getStructureId());
						structures.add(containerStructure.getStructureId(), st.getModDate());
						structuresSet.add(containerStructure.getStructureId());
					}

					List<MultiTree> treeList = MultiTreeFactory.getMultiTree(workingPage,container);

					for (MultiTree mt : treeList) {
						String contentIdentifier = mt.getChild();
						// Contents dependencies
						Contentlet content =  APILocator.getContentletAPI().findContentletByIdentifier(contentIdentifier, true, -1, user, false);
						if(content==null) {
							content = APILocator.getContentletAPI().findContentletByIdentifier(contentIdentifier, false, -1, user, false);
						}

						contents.add(contentIdentifier, content.getModDate());
						contentsSet.add(contentIdentifier);
					}
				}
			}
		} catch (DotSecurityException e) {

			Logger.error(this, e.getMessage(),e);
		} catch (DotDataException e) {

			Logger.error(this, e.getMessage(),e);
		}
	}

	private void setTemplateDependencies() {
		try {
			List<Container> containerList = new ArrayList<Container>();

			for (String id : templates) {
				Template wkT = APILocator.getTemplateAPI().findWorkingTemplate(id, user, false);
				Template lvT = APILocator.getTemplateAPI().findLiveTemplate(id, user, false);

				// Host dependency
				Host h = APILocator.getHostAPI().find(APILocator.getTemplateAPI().getTemplateHost(wkT).getIdentifier(), user, false);
				hosts.add(APILocator.getTemplateAPI().getTemplateHost(wkT).getIdentifier(), h.getModDate());
				
				containerList.clear();
				containerList.addAll(APILocator.getTemplateAPI().getContainersInTemplate(wkT, user, false));

				if(lvT!=null && InodeUtils.isSet(lvT.getInode())) {
				    containerList.addAll(APILocator.getTemplateAPI().getContainersInTemplate(lvT, user, false));
				}

				for (Container container : containerList) {
					// Container dependencies
					containers.add(container.getIdentifier(), container.getModDate());
					containersSet.add(container.getIdentifier());
				}
			}

		} catch (DotSecurityException e) {

			Logger.error(this, e.getMessage(),e);
		} catch (DotDataException e) {

			Logger.error(this, e.getMessage(),e);
		}

	}

	private void setContainerDependencies() {

		try {

			List<Container> containerList = new ArrayList<Container>();

			for (String id : containers) {
				Container c = APILocator.getContainerAPI().getWorkingContainerById(id, user, false);

				// Host Dependency
				Host h = APILocator.getContainerAPI().getParentHost(c, user, false);
				hosts.add(APILocator.getContainerAPI().getParentHost(c, user, false).getIdentifier(), h.getModDate());

				containerList.clear();
				containerList.add(APILocator.getContainerAPI().getWorkingContainerById(id, user, false));
				containerList.add(APILocator.getContainerAPI().getLiveContainerById(id, user, false));

				for (Container container : containerList) {
					// Structure dependencies
					List<ContainerStructure> csList = APILocator.getContainerAPI().getContainerStructures(container);

					for (ContainerStructure containerStructure : csList) {
						Structure st = StructureCache.getStructureByInode(containerStructure.getStructureId());
						structures.add(containerStructure.getStructureId(), st.getModDate());
						structuresSet.add(containerStructure.getStructureId());
					}
				}

			}

		} catch (DotSecurityException e) {

			Logger.error(this, e.getMessage(),e);
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage(),e);
		}

	}


	private void setStructureDependencies() {
		Set<String> s = new HashSet<String>();
		s.addAll(structures);
		for (String inode : s) {
			structureDependencyHelper(inode);
		}
	}



	private void structureDependencyHelper(String stInode){
		Structure st = StructureCache.getStructureByInode(stInode);
		hosts.add(st.getHost()); // add the host dependency
		folders.add(st.getFolder()); // add the folder dependency

		// Related structures
		List<Relationship> relations = RelationshipFactory.getAllRelationshipsByStructure(st);

		for (Relationship r : relations) {
			relationships.add(r.getInode(), r.getModDate());

			if(!structures.contains(r.getChildStructureInode())){
				Structure struct = StructureCache.getStructureByInode(r.getChildStructureInode());
				structures.add(r.getChildStructureInode(), struct.getModDate());
				structureDependencyHelper(r.getChildStructureInode());
			}
			if(!structures.contains(r.getParentStructureInode())){
				Structure struct = StructureCache.getStructureByInode(r.getChildStructureInode());
				structures.add(r.getParentStructureInode(), struct.getModDate());
				structureDependencyHelper(r.getParentStructureInode());
			}
		}
	}


	private void processList(List<Contentlet> cons) throws DotDataException, DotSecurityException {
	    Set<Contentlet> contentsToProcess = new HashSet<Contentlet>();
	    Set<Contentlet> contentsWithDependenciesToProcess = new HashSet<Contentlet>();

        //Getting all related content

        for (Contentlet con : cons) {
        	Host h = APILocator.getHostAPI().find(con.getHost(), user, false);
        	hosts.add(con.getHost(), h.getModDate()); // add the host dependency
            contentsToProcess.add(con);

            Map<Relationship, List<Contentlet>> contentRel =
                    APILocator.getContentletAPI().findContentRelationships(con, user);

            for (Relationship rel : contentRel.keySet()) {
                contentsToProcess.addAll(contentRel.get(rel));
                /**
                 * ISSUE #2222: https://github.com/dotCMS/dotCMS/issues/2222
                 *
                 * We need the relationships in which the single related content is involved.
                 *
                 */
                if(contentRel.get(rel).size()>0)
                    relationships.add(rel.getInode(), rel.getModDate());
            }
        }

        for (Contentlet con : contentsToProcess) {
        	Host h = APILocator.getHostAPI().find(con.getHost(), user, false);
        	hosts.add(con.getHost(), h.getModDate()); // add the host dependency
        	contentsWithDependenciesToProcess.add(con);
	        //Copy asset files to bundle folder keeping original folders structure
	        List<Field> fields=FieldsCache.getFieldsByStructureInode(con.getStructureInode());

	        for(Field ff : fields) {
	            if (ff.getFieldType().equals(Field.FieldType.IMAGE.toString())
	                    || ff.getFieldType().equals(Field.FieldType.FILE.toString())) {

	                try {
	                    String value = "";
	                    if(UtilMethods.isSet(APILocator.getContentletAPI().getFieldValue(con, ff))){
	                        value = APILocator.getContentletAPI().getFieldValue(con, ff).toString();
	                    }
	                    //Identifier id = (Identifier) InodeFactory.getInode(value, Identifier.class);
	                    Identifier id = APILocator.getIdentifierAPI().find(value);
	                    if (InodeUtils.isSet(id.getInode()) && id.getAssetType().equals("contentlet")) {
	                    	contentsWithDependenciesToProcess.addAll(
	                    			APILocator.getContentletAPI()
	                                .search("+identifier:"+id.getId(), 0, 0, "moddate", user, false));
	                    }
	                } catch (Exception ex) {
	                    Logger.debug(this, ex.toString());
	                    throw new DotStateException("Problem occured while publishing file");
	                }
	            }

	        }
        }

        // Adding the Contents (including related) and adding filesAsContent
        for (Contentlet con : contentsWithDependenciesToProcess) {
        	Host h = APILocator.getHostAPI().find(con.getHost(), user, false);
        	hosts.add(con.getHost(), h.getModDate()); // add the host dependency
            contents.add(con.getIdentifier(), con.getModDate()); // adding the content (including related)
            Folder f = APILocator.getFolderAPI().find(con.getFolder(), user, false);
            folders.add(con.getFolder(), f.getModDate()); // adding content folder

            try {
                if(Config.getBooleanProperty("PUSH_PUBLISHING_PUSH_ALL_FOLDER_PAGES",false)) {
                    List<HTMLPage> folderHtmlPages = APILocator.getHTMLPageAPI().findLiveHTMLPages(
                            APILocator.getFolderAPI().find(con.getFolder(), user, false));
                    folderHtmlPages.addAll(APILocator.getHTMLPageAPI().findWorkingHTMLPages(
                            APILocator.getFolderAPI().find(con.getFolder(), user, false)));
                    for(HTMLPage htmlPage: folderHtmlPages) {
                        htmlPages.add(htmlPage.getIdentifier(), htmlPage.getModDate());

                        // working template working page
                        Template workingTemplateWP = APILocator.getTemplateAPI().findWorkingTemplate(htmlPage.getTemplateId(), user, false);
                        // live template working page
                        Template liveTemplateWP = APILocator.getTemplateAPI().findLiveTemplate(htmlPage.getTemplateId(), user, false);

                        // Templates dependencies
                        templates.add(htmlPage.getTemplateId(), workingTemplateWP.getModDate());

                        // Containers dependencies
                        List<Container> containerList = new ArrayList<Container>();
                        containerList.addAll(APILocator.getTemplateAPI().getContainersInTemplate(workingTemplateWP, user, false));
                        containerList.addAll(APILocator.getTemplateAPI().getContainersInTemplate(liveTemplateWP, user, false));

                        for (Container container : containerList) {
                            containers.add(container.getIdentifier(), container.getModDate());
                            // Structure dependencies
                            List<ContainerStructure> csList = APILocator.getContainerAPI().getContainerStructures(container);

        					for (ContainerStructure containerStructure : csList) {
        						Structure struct = StructureCache.getStructureByInode(containerStructure.getStructureId());
        						structures.add(containerStructure.getStructureId(), struct.getModDate());
        					}
                        }
                    }
                }
            } catch (Exception e) {
                Logger.debug(this, e.toString());
            }

            if(Config.getBooleanProperty("PUSH_PUBLISHING_PUSH_STRUCTURES", true)) {
            	Structure struct = StructureCache.getStructureByInode(con.getStructureInode());
                structures.add(con.getStructureInode(), struct.getModDate());
            }
        }

	}

	private void setContentDependencies(List<String> luceneQueries) throws DotBundleException {
		try {
		    // we need to process contents already taken as dependency
			Set<String> cons = new HashSet<String>(contents);
            for(String id : cons){
                processList(APILocator.getContentletAPI().search("+identifier:"+id, 0, 0, "moddate", user, false));
            }

    		for(String luceneQuery: luceneQueries) {
    		    List<Contentlet> cs = APILocator.getContentletAPI().search(luceneQuery, 0, 0, "moddate", user, false);
    			processList(cs);
    		}

		} catch (Exception e) {
			throw new DotBundleException(this.getClass().getName() + " : " + "generate()"
			+ e.getMessage() + ": Unable to pull content", e);
		}

	}

}