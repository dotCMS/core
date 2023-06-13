package com.dotcms.enterprise.publishing.remote.bundler;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.publisher.business.PublishAuditHistory;
import com.dotcms.publisher.business.PublishAuditStatus;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.wrapper.PushContentWorkflowWrapper;
import com.dotcms.publisher.pusher.wrapper.PushContentWrapper;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.IBundler;
import com.dotcms.publishing.IPublisher;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PushPublishLogger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;

/**
 * This bundler will take the list of {@link Contentlet} objects that are being
 * pushed and will write them in the file system in the form of an XML file.
 * This information will be part of the bundle that will be pushed to the
 * destination server. Please notice that a Contentlet encompasses a number of 
 * different entities, such as a Content Page, a Site, etc.
 * 
 * @author Jorge Urdaneta
 * @version 1.0
 * @since Mar 7, 2013
 *
 */
public class ContentBundler implements IBundler {

	private PushPublisherConfig config;
	private User systemUser;
	private IdentifierAPI identifierAPI = null;
	private ContentletAPI conAPI = null;
	private CategoryAPI categoryAPI = null;
	private UserAPI uAPI = null;
	private PublisherAPI pubAPI = null;
	private PublishAuditAPI pubAuditAPI = PublishAuditAPI.getInstance();

	public final static String CONTENT_EXTENSION = ".content.xml" ;
	public final static String CONTENT_WORKFLOW_EXTENSION = ".contentworkflow.xml";
	public static final String CATEGORY_SEPARATOR = "sep_sep";

	@Override
	public String getName() {
		return "Content bundler";
	}

	@Override
	public void setConfig(PublisherConfig pc) {
		config = (PushPublisherConfig) pc;
		this.identifierAPI = APILocator.getIdentifierAPI();
		conAPI = APILocator.getContentletAPI();
		categoryAPI = APILocator.getCategoryAPI();
		uAPI = APILocator.getUserAPI();
		pubAPI = PublisherAPI.getInstance();

		try {
			systemUser = uAPI.getSystemUser();
		} catch (DotDataException e) {
			Logger.fatal(ContentBundler.class,e.getMessage(),e);
		}
	}

    @Override
    public void setPublisher(IPublisher publisher) {
    }

	@Override
	public void generate(File bundleRoot, BundlerStatus status)
			throws DotBundleException {
	    if(LicenseUtil.getLevel() < LicenseLevel.PROFESSIONAL.level) {
	        throw new RuntimeException("need an enterprise pro license to run this bundler");
	    }
		PublishAuditHistory currentStatusHistory = null;
		try {
            //Updating audit table
            if ( !config.isDownloading() ) {
                currentStatusHistory = pubAuditAPI.getPublishAuditStatus( config.getId() ).getStatusPojo();
                if ( currentStatusHistory == null ) {
                    currentStatusHistory = new PublishAuditHistory();
                }
                currentStatusHistory.setBundleStart( new Date() );
				PushPublishLogger.log(this.getClass(), "Status Update: Bundling.");
                pubAuditAPI.updatePublishAuditStatus( config.getId(), PublishAuditStatus.Status.BUNDLING, currentStatusHistory );
            }

            Set<String> contentsIds = config.getContentlets();

			if(UtilMethods.isSet(contentsIds) && !contentsIds.isEmpty()) { // this content set is a dependency of other assets, like htmlpages
				Set<Contentlet> contents = new HashSet<Contentlet>();
				// If push remove, include deleted contents as well
				String excludeDeleted = "";
				if (config.getOperation() == PushPublisherConfig.Operation.PUBLISH) {
					// If push or push & delete, exclude deleted contents
					excludeDeleted = " +deleted:false";
				}
				for (String contentIdentifier : contentsIds) {
					contents.addAll(conAPI.search("+identifier:"+contentIdentifier+" +live:true" + excludeDeleted, 0, -1, null, systemUser, false));
					contents.addAll(conAPI.search("+identifier:"+contentIdentifier+" +working:true" + excludeDeleted, 0, -1, null, systemUser, false));
					if(config.isSameIndexNotIncremental()){
						contents.addAll(conAPI.search("+identifier:"+contentIdentifier+" +deleted:true", 0, -1, null, systemUser, false));
					}
				}

				//Delete duplicate
				Set<ContentletUniqueWrapper> contentsToProcessFinal = new HashSet<ContentletUniqueWrapper>();
				for(Contentlet con: contents) {
					contentsToProcessFinal.add(new ContentletUniqueWrapper(con));
				}

				Iterator<ContentletUniqueWrapper> it = contentsToProcessFinal.iterator();
				for (int ii = 0; it.hasNext(); ii++) {
					Contentlet con = it.next().getContentlet();
					if (con.isHTMLPage()) {
						// Temporarily add the URI to the contentlet properties
						// The URI is now stored ONLY in the page Identifier
						String pageUri = this.identifierAPI.find(
								con.getIdentifier()).getAssetName();
						con.getMap().put(HTMLPageAssetAPI.URL_FIELD, pageUri);
					}
					writeFileToDisk(bundleRoot, con, ii);
					status.addCount();
				}
			}

            if ( currentStatusHistory != null && !config.isDownloading() ) {
                //Updating audit table
                currentStatusHistory = pubAuditAPI.getPublishAuditStatus( config.getId() ).getStatusPojo();

                currentStatusHistory.setBundleEnd( new Date() );
				PushPublishLogger.log(this.getClass(), "Status Update: Bundling.");
                pubAuditAPI.updatePublishAuditStatus( config.getId(), PublishAuditStatus.Status.BUNDLING, currentStatusHistory );
            }

        } catch (Exception e) {
            try {
                if ( currentStatusHistory != null && !config.isDownloading() ) {
					PushPublishLogger.log(this.getClass(), "Status Update: Failed to bundle");
                    pubAuditAPI.updatePublishAuditStatus( config.getId(), PublishAuditStatus.Status.FAILED_TO_BUNDLE, currentStatusHistory );
                }
            } catch ( DotPublisherException e1 ) {
                Logger.warn( this.getClass(), "Unable to update Publish Audit Status for failed bundle: " + config.getId(), e1 );
            }

            status.addFailure();

			throw new DotBundleException(this.getClass().getName() + " : " + "generate()"
			+ e.getMessage() + ": Unable to pull content", e);
		}
	}

	/**
	 * Writes the properties of a {@link Contentlet} object to the file system,
	 * so that it can be bundled and pushed to the destination server.
	 * 
	 * @param bundleRoot
	 *            - The root location of the bundle in the file system.
	 * @param con
	 *            - The {@link Rule} object to write.
	 * @param countOrder
	 *            - The order in which a Contentlet must be processed.
	 * @throws IOException
	 *             An error occurred when writing the rule to the file system.
	 * @throws DotDataException
	 *             An error occurred reading information from the database.
	 * @throws DotSecurityException
	 *             The current user does not have the required permissions to
	 *             perform this action.
	 * @throws DotPublisherException
	 *             An error occurred when retrieving the content matrix.
	 */
	private void writeFileToDisk(File bundleRoot, Contentlet con, int countOrder)
			throws IOException, DotDataException,
				DotSecurityException, DotPublisherException
	{

		Calendar cal = Calendar.getInstance();
		File pushContentFile = null;
		Host h = null;

		//Populate wrapper
		ContentletVersionInfo info = APILocator.getVersionableAPI().getContentletVersionInfo(con.getIdentifier(), con.getLanguageId());
		h = APILocator.getHostAPI().find(con.getHost(), APILocator.getUserAPI().getSystemUser(), true);

		PushContentWrapper wrapper=new PushContentWrapper();
	    wrapper.setContent(con);
		wrapper.setInfo(info);
		wrapper.setId(APILocator.getIdentifierAPI().find(con.getIdentifier()));
		wrapper.setTags(APILocator.getTagAPI().getTagsByInode(con.getInode()));
		wrapper.setOperation(config.getOperation());
		wrapper.setLanguage(APILocator.getLanguageAPI().getLanguage(con.getLanguageId()));


        //Find Tree
        List<Map<String, Object>> contentTreeMatrix = pubAPI.getContentTreeMatrix( con.getIdentifier() );
        wrapper.setTree( contentTreeMatrix );

        //Now add the categories, we will find categories by inode NOT by identifier
        List<Map<String, Object>> categoryTrees = pubAPI.getContentTreeMatrix( con.getInode() );
        Collections.sort(categoryTrees, (lhs, rhs) ->
        	Integer.parseInt(lhs.get("tree_order").toString()) - Integer.parseInt(rhs.get("tree_order").toString())
        );

        List<String> categories = new ArrayList<String>();
        for(Map<String, Object> categoryTree : categoryTrees) {
        	Category cat = categoryAPI.find((String) categoryTree.get("parent"), systemUser, true);

        	if (cat == null) continue;

    		List<Category> categoryPath = categoryAPI.getCategoryTreeUp(cat, systemUser, true);
    		categoryPath.remove(0);

    		final String[] categoryPathNames = categoryPath.stream().map( category -> category.getKey() ).toArray(String[]::new);
        	final String categoryQualifiedName = StringUtils.join(categoryPathNames, CATEGORY_SEPARATOR);

        	categories.add(categoryQualifiedName);
        }
        wrapper.setCategories(categories);

        //Verify if this contentlet is a HTMLPage
        if ( con.getStructure().getStructureType() == Structure.STRUCTURE_TYPE_HTMLPAGE ) {

            List<Map<String, Object>> multiTreesList = new ArrayList<Map<String, Object>>();
            //Find the MultiTree records for this html page and add them to the wrapper
            List<MultiTree> multiTrees = APILocator.getMultiTreeAPI().getMultiTrees( con.getIdentifier() );
            for ( MultiTree multiTree : multiTrees ) {
                Map<String, Object> multiTreeMap = new HashMap<String, Object>();
                multiTreeMap.put( "parent1", multiTree.getParent1() );
                multiTreeMap.put( "parent2", multiTree.getParent2() );
                multiTreeMap.put( "child", multiTree.getChild() );
                multiTreeMap.put( "relation_type", multiTree.getRelationType() );
                multiTreeMap.put( "tree_order", multiTree.getTreeOrder() );
                multiTreesList.add( multiTreeMap );
            }
            wrapper.setMultiTree( multiTreesList );
        }

		//Copy asset files to bundle folder keeping original folders structure
		List<Field> fields=FieldsCache.getFieldsByStructureInode(con.getStructureInode());
		File assetFolder = new File(bundleRoot.getPath()+File.separator+"assets");
		String inode=con.getInode();
		Map<String, List<Tag>> contentTags = new HashMap<>();

		for(Field ff : fields) {
			if(ff.getFieldType().toString().equals(Field.FieldType.BINARY.toString())) {
				File sourceFile = con.getBinary( ff.getVelocityVarName());

				if(sourceFile != null && sourceFile.exists()) {
					if(!assetFolder.exists())
						assetFolder.mkdir();

					String folderTree = inode.charAt(0)+File.separator+inode.charAt(1)+File.separator+
					        inode+File.separator+ff.getVelocityVarName()+File.separator+sourceFile.getName();

					File destFile = new File(assetFolder, folderTree);
		            destFile.getParentFile().mkdirs();
		            FileUtil.copyFile(sourceFile, destFile);
				}
			} else if ( ff.getFieldType().toString().equals(Field.FieldType.TAG.toString()) ) {
				List<Tag> tagsByFieldVarName = APILocator.getTagAPI().getTagsByInodeAndFieldVarName(con.getInode(), ff.getVelocityVarName());
				contentTags.put(ff.getVelocityVarName(), tagsByFieldVarName);
			}

		}

		wrapper.setContentTags(contentTags);

		String liveworking = con.isLive() ? "live" :  "working";

		String uri = APILocator.getIdentifierAPI().find(con).getURI().replace("/", File.separator);
		if(!uri.endsWith(CONTENT_EXTENSION)){
			uri.replace(CONTENT_EXTENSION, "");
			uri.trim();
			uri += CONTENT_EXTENSION;
		}
		uri = uri.replace(uri.substring(uri.lastIndexOf(File.separator)+1, uri.length()), countOrder +"-"+ uri.substring(uri.lastIndexOf(File.separator)+1, uri.length()));

		String assetName = APILocator.getFileAssetAPI().isFileAsset(con)?(File.separator + countOrder +"-" +con.getInode() + CONTENT_EXTENSION):uri;

		String myFileUrl = bundleRoot.getPath() + File.separator
				+liveworking + File.separator
				+ h.getHostname() + File.separator +
				+ con.getLanguageId() + assetName;

		pushContentFile = new File(myFileUrl);
		pushContentFile.mkdirs();

		BundlerUtil.objectToXML(wrapper, pushContentFile, true);
		pushContentFile.setLastModified(cal.getTimeInMillis());

		if(Config.getBooleanProperty("PUSH_PUBLISHING_LOG_DEPENDENCIES", false)) {
			PushPublishLogger.log(getClass(), "Content bundled for pushing. Operation: "+config.getOperation()+", Identifier: "+ con.getIdentifier(), config.getId());
		}

		WorkflowStep step = APILocator.getWorkflowAPI().findStepByContentlet(con);
		WorkflowScheme scheme = null;
		if( null != step) {
			scheme = APILocator.getWorkflowAPI().findScheme(step.getSchemeId());
		}

		if(config.getOperation().equals(Operation.PUBLISH) && null != scheme) {
		    // send step information if we're publishing and the structure has a scheme other than the default
            WorkflowTask task = APILocator.getWorkflowAPI().findTaskByContentlet( con );
            if ( task != null &&  null != task.getId()) {

                Role assignedToRole = APILocator.getRoleAPI().loadRoleById( task.getAssignedTo() );
                if ( (assignedToRole != null && UtilMethods.isSet( assignedToRole.getRoleKey() ))
                        && assignedToRole.getRoleKey().equalsIgnoreCase( "system" ) ) {

                    // user role for system user might be different.
                    HibernateUtil.evict( task );
                    WorkflowTask orig = task;
                    task = new WorkflowTask();
                    try {
                        BeanUtils.copyProperties( task, orig );
                    } catch ( Exception e ) {
                        Logger.error( this, "can't copy properties from original task to a new with system user role mark", e );
                    }
                    task.setAssignedTo( "__SYSTEM_USER_ROLE__" );
                }

                PushContentWorkflowWrapper w=new PushContentWorkflowWrapper();
    		    w.setTask(task);
    		    w.setHistory(APILocator.getWorkflowAPI().findWorkflowHistory(task));
    		    w.setComments(APILocator.getWorkflowAPI().findWorkFlowComments(task));
    		    File stepFile=new File(bundleRoot,liveworking+File.separator+h.getHostname()+File.separator+
    		            con.getLanguageId()+File.separator+con.getIdentifier()+CONTENT_WORKFLOW_EXTENSION);
    		    BundlerUtil.objectToXML(w, stepFile, true);
    		    stepFile.setLastModified(cal.getTimeInMillis());
		    }
		}
		// If content is a Content Page, add dependent Rules, if any
		if (con.isHTMLPage()) {
			List<Rule> ruleList = APILocator.getRulesAPI().getAllRulesByParent(con, systemUser, false);
			Set<String> ruleIds = new HashSet<>();
			if (!ruleList.isEmpty()) {
				for (Rule rule : ruleList) {
					ruleIds.add(rule.getId());
				}
				this.config.getRules().addAll(ruleIds);
			}
		}
	}

	@Override
	public FileFilter getFileFilter(){
		return new ContentBundlerFilter();
	}

	/**
	 * A simple file filter that looks for contentlet data files inside a
	 * bundle.
	 * 
	 * @author Jorge Urdaneta
	 * @version 1.0
	 * @since Mar 7, 2013
	 *
	 */
	public class ContentBundlerFilter implements FileFilter{

		@Override
		public boolean accept(File pathname) {

			return (pathname.isDirectory() || pathname.getName().endsWith(CONTENT_EXTENSION));
		}

	}

}

/**
 * 
 * @author Jorge Urdaneta
 * @version 1.0
 * @since Mar 7, 2013
 *
 */
class ContentletUniqueWrapper {
	private Contentlet contentlet;

	public ContentletUniqueWrapper (Contentlet contentlet) {
		this.contentlet = contentlet;
	}

	public Contentlet getContentlet() {
		return contentlet;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((contentlet == null) ? 0 : contentlet.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ContentletUniqueWrapper other = (ContentletUniqueWrapper) obj;
		if (contentlet == null) {
			if (other.contentlet != null)
				return false;
		} else if (!contentlet.getInode().equals(other.contentlet.getInode()))
			return false;
		return true;
	}

}
