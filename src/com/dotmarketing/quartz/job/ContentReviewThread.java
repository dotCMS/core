package com.dotmarketing.quartz.job;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.RoleFactory;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.portlets.workflows.util.WorkflowEmailUtil;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilHTML;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
public class ContentReviewThread implements Runnable, Job {

	private ContentletAPI conAPI = APILocator.getContentletAPI();
	private UserAPI userAPI = APILocator.getUserAPI();
	private WorkflowAPI wapi = APILocator.getWorkflowAPI();
	private RoleAPI roleAPI = APILocator.getRoleAPI();
	private CategoryAPI catAPI = APILocator.getCategoryAPI();
	private PermissionAPI permAPI = APILocator.getPermissionAPI();

	public ContentReviewThread() {
    }

    public void run() {
        try {
            Logger.debug(this, "Starting ContentsReview");
            HibernateUtil.startTransaction();
            
            HibernateUtil dh = new HibernateUtil(com.dotmarketing.portlets.contentlet.business.Contentlet.class);
            dh.setSQLQuery("select {contentlet.*} from contentlet, inode contentlet_1_, structure, contentlet_lang_version_info "
                    + "where contentlet.inode = contentlet_1_.inode and " 
                    + "? >= contentlet.next_review and "
                    + "contentlet.review_interval is not null and contentlet.review_interval <> '' and "
                    + "contentlet.structure_inode = structure.inode and "
                    + "structure.reviewer_role is not null and structure.reviewer_role <> '' and " 
                    + "contentlet_lang_version_info.working_inode = contentlet.inode ");
            dh.setParam(new Date());
            //dh.setParam(true);
            List<com.dotmarketing.portlets.contentlet.business.Contentlet> fatContentlets = dh.list();
            List<Contentlet> contentlets = new ArrayList<Contentlet>();
            for(com.dotmarketing.portlets.contentlet.business.Contentlet fatCont : fatContentlets){
            	contentlets.add(conAPI.convertFatContentletToContentlet(fatCont));
            }
            for (Contentlet cont : contentlets) {
                try {
                	WorkflowTask task = wapi.findTaskByContentlet(cont);
                	User systemUser = userAPI.getSystemUser();
                	
                	if(UtilMethods.isSet(task.getAssignedTo())){
                		// If a task exists for this content, placing a comment and an email to review the content.
                		WorkflowProcessor processor = new WorkflowProcessor(cont);
    					WorkflowComment comment = new WorkflowComment();
    					String assignedTo = task.getAssignedTo();
    					
    					// add the user if assign is a user
    					Set<String> recipients = new HashSet<String>();
    					try {
    						recipients.add(userAPI
    								.loadUserById(assignedTo, userAPI.getSystemUser(), false).getEmailAddress());
    					} catch (Exception e) {

    					}

    					// add the user if assign is a role
    					try {
    						List<User> users = roleAPI.findUsersForRole(roleAPI.loadRoleById(assignedTo), false);
    						for(User u : users){
    							recipients.add(u.getEmailAddress());
    						}
    					} catch (Exception e) {

    					}
    					
    					String[] to = (String[]) recipients.toArray(new String[recipients.size()]);

    					// Commenting on task to review
    					comment.setComment(LanguageUtil.get(PublicCompanyFactory.getDefaultCompany(), "Please-review-this-content-comment"));    					
    					comment.setWorkflowtaskId(task.getId());
    					comment.setCreationDate(new Date());
    					comment.setPostedBy(systemUser.getUserId());
    					wapi.saveComment(comment);
    					
    					// Sending Email to review the content
    					WorkflowEmailUtil.sendWorkflowEmail(processor, to, LanguageUtil.get(PublicCompanyFactory.getDefaultCompany(), "Please-review-this-content-comment"), LanguageUtil.get(PublicCompanyFactory.getDefaultCompany(), "Please-review-this-content-email"), true);
                	}else{
                		// Creating a Content Review Task
                		cont.setStringProperty(Contentlet.WORKFLOW_ACTION_KEY, wapi.findEntryAction(cont, systemUser).getId());
                		if(UtilMethods.isSet(cont.getStructure().getReviewerRole()) && !cont.getStructure().getReviewerRole().equalsIgnoreCase("0"))
                			cont.setStringProperty(Contentlet.WORKFLOW_ASSIGN_KEY, cont.getStructure().getReviewerRole());
                		cont.setStringProperty(Contentlet.WORKFLOW_COMMENTS_KEY, "Content \"" + UtilHTML.escapeHTMLSpecialChars(cont.getTitle().trim()) + 
            					"\" need to be reviewed ");
                	}
                	
                	// updating content NextReview date, in order not to over load the Task if CONTENT_REVIEW_THREAD interval is less than content reviewInterval
                	cont.setNextReview(conAPI.getNextReview(cont, systemUser, false));
                	Map<Relationship, List<Contentlet>> contentRelationships = conAPI.findContentRelationships(cont, systemUser);
                	List<Category> cats = catAPI.getParents(cont, systemUser, false);
                	List<Permission> permissions = permAPI.getPermissions(cont);
                	conAPI.checkinWithoutVersioning(cont, contentRelationships, cats, permissions, systemUser, false);

                } catch (Exception e) {
                    Logger.error(this, "Error ocurred trying to create the review task for contenlet: "
                            + cont.getInode(), e);
                }
            }

        } catch (Exception e) {
            Logger.error(this, "Error ocurred trying to review contents.", e);
        } finally {
            try {
				HibernateUtil.commitTransaction();
			} catch (DotHibernateException e) {
				Logger.error(this.getClass(), e.getMessage(), e);
			}
        }
    }

    private String _buildWorkflowEmailBody (WorkflowTask task, String change) throws DotDataException, DotSecurityException {

        Host host = APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), false);
        String ref = "http://" + host.getHostname() + Config.getStringProperty("WORKFLOWS_URL") + "&inode=" + String.valueOf(task.getInode());

        StringBuffer buffer = new StringBuffer ();

        try {
            String roleName = APILocator.getRoleAPI().loadRoleById(task.getBelongsTo()).getName();
            
            buffer.append(
                  "<table align=\"center\" border=\"1\" width=\"50%\">" +
                  "    <tr>" +
                  "        <td align=\"center\" colspan=\"2\"><b>" + change + "</b></td>" +
                  "    </tr>" +
                  "    <tr>" +
                  "        <td width=\"15%\" nowrap><b>Task</b></td><td><a href=\"" + ref + "\">" + task.getTitle() + "</a></td>" +
                  "    </tr>" +
                  "    <tr>" +
                  "        <td  nowrap><b>Created</b></td><td>" + UtilMethods.dateToHTMLDate(task.getCreationDate()) + "</td>" +
                  "    </tr>" +
                  "    <tr>" +
                  "        <td  nowrap><b>Author</b></td><td>" + UtilMethods.getUserFullName(task.getCreatedBy()) + "</td>" +
                  "    </tr>" +
                  "    <tr>" +
                  "        <td  nowrap><b>Assignee Group</b></td><td>" + roleName + "</td>" +
                  "    </tr>" +
                  "</table>"  
            );
        } catch (Exception e) {
            Logger.warn(RoleFactory.class, "_buildWorkflowEmailBody: Error getting role", e);
        }
        
        return buffer.toString();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Thread#destroy()
     */
    public void destroy() {
        try {
			HibernateUtil.closeSession();
		} catch (DotHibernateException e) {
			Logger.error(this.getClass(), e.getMessage(), e);
		}
    }
    
    public void execute(JobExecutionContext context) throws JobExecutionException {
    	Logger.debug(this, "Running ContentReviewThread - " + new Date());

    	try {
			run();
		} catch (Exception e) {
			Logger.info(this, e.toString());
		} finally {
			try {
				HibernateUtil.closeSession();
			} catch (DotHibernateException e) {
				Logger.error(this.getClass(), e.getMessage(), e);
			}
		}
	}
}
