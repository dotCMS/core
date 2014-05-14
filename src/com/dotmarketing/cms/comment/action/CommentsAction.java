package com.dotmarketing.cms.comment.action;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.actions.DispatchAction;
import org.owasp.esapi.ESAPI;

import com.dotcms.util.SecurityUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.cms.comment.struts.CommentsForm;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.factories.EmailFactory;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.model.WorkflowComment;
import com.dotmarketing.portlets.workflows.model.WorkflowHistory;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilHTML;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.util.WebKeys.WorkflowStatuses;
import com.dotmarketing.viewtools.CommentsWebAPI;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.util.Html;



/**
 * This class manage the content comments
 * @author Salvador Di Nardo
 * @version 1.6
 * @since 1.0
 */
public class CommentsAction extends DispatchAction {

	private ContentletAPI conAPI = APILocator.getContentletAPI();
	private CategoryAPI catAPI = APILocator.getCategoryAPI();
	private HostWebAPI hostWebAPI = WebAPILocator.getHostWebAPI();

	public ActionForward unspecified(ActionMapping mapping, ActionForm lf, HttpServletRequest request, HttpServletResponse response) {
		ActionForward forward = new ActionForward("/");
		forward.setRedirect(false);
		return forward;
	}

	public ActionForward saveComments(ActionMapping mapping, ActionForm lf, HttpServletRequest request, HttpServletResponse response) {

		CommentsForm commentsForm = (CommentsForm) lf;

		request.getSession().setAttribute("commentsForm", commentsForm);
		ActionErrors ae = commentsForm.validate(mapping, request);

		HashMap<String,String> commentsOptions = (HashMap<String,String>) request.getSession().getAttribute("commentsOptions");


		try {
			HibernateUtil.startTransaction();
			if ((ae != null) && (ae.size() > 0)) {
				String referrer = UtilMethods.isSet(commentsOptions.get("referrer"))?commentsOptions.get("referrer"):"";
				if(referrer != null){
					referrer=referrer.replaceAll("#comments", "#comments");
					if(referrer.indexOf("?") > -1){
						referrer = referrer + "&dotcache=no";
					}else{
						referrer = referrer + "?dotcache=no";
					}

				}
				referrer = (referrer.indexOf("#comments") == -1 ? referrer + "#comments" : referrer);




				saveMessages(request, ae);
				saveMessages(request.getSession(), ae);
				ActionForward forward = new ActionForward(SecurityUtils.stripReferer(request, referrer));
				forward.setRedirect(true);
				return forward;
			}

			User user = APILocator.getUserAPI().getSystemUser();
			if (request.getSession().getAttribute(WebKeys.CMS_USER) != null) {
				user = (User) request.getSession().getAttribute(WebKeys.CMS_USER);
			}
			String userId = user.getUserId();
			// Contentlet Data
			Contentlet contentlet = new Contentlet();
			try{
				contentlet = conAPI.find(commentsOptions.get("contentInode"), user, true);
			}catch(DotDataException e){
				Logger.error(this, "Unable to look up comment with inode " + commentsOptions.get("contentInode"), e);
			}

			Structure contentletStructure = StructureCache.getStructureByInode(contentlet.getStructureInode());
			Identifier contentletIdentifier = APILocator.getIdentifierAPI().find(contentlet);

			/*make sure we have a structure in place before saving */
			CommentsWebAPI cAPI = new CommentsWebAPI();
			cAPI.validateComments(contentlet.getInode());

			Structure commentsStructure = StructureCache.getStructureByVelocityVarName(CommentsWebAPI.commentsVelocityStructureName);

			Contentlet contentletComment = new Contentlet();

			// set the default language
			com.dotmarketing.portlets.contentlet.business.Contentlet beanContentlet = (com.dotmarketing.portlets.contentlet.business.Contentlet) InodeFactory.getInode(contentlet.getInode(), com.dotmarketing.portlets.contentlet.business.Contentlet.class);

			contentletComment.setLanguageId(beanContentlet.getLanguageId());

			// Add the default fields
			contentletComment.setStructureInode(commentsStructure.getInode());

			Field field;

			/* Set the title if we have one*/
			if(UtilMethods.isSet(commentsOptions.get("commentTitle"))){

				field = commentsStructure.getFieldVar("title");

				conAPI.setContentletProperty(contentletComment, field, commentsOptions.get("commentTitle"));
			}

			/* Validate if a CommentsCount field exists in the contentlet structure
			   if not, then create it and populate it.*/

			if (!InodeUtils.isSet(contentletStructure.getFieldVar("commentscount").getInode())) {
				List<Field> fields = new ArrayList<Field>();
			    field = new Field("CommentsCount", Field.FieldType.TEXT, Field.DataType.INTEGER, contentletStructure,
						          false, false, true, Integer.MAX_VALUE, "0", "0", "",true, true, true);
				FieldFactory.saveField(field);
				for(Field structureField: contentletStructure.getFields()){
					fields.add(structureField);
				}
				fields.add(field);
				FieldsCache.removeFields(contentletStructure);
				FieldsCache.addFields(contentletStructure,fields);
			}

			/* Get the  value from the CommentsCount field for this contentlet, if the value
			 * is null, then the contentlet has no comments, otherwise increment its value by one
			 * and set it to the contentlet.
			 */
			field = contentletStructure.getFieldVar("commentscount");
			String velVar = field.getVelocityVarName();

			int comentsCount = -1;
			try {
				Long countValue = contentlet.getLongProperty(velVar);
				comentsCount  = countValue.intValue();
			} catch (Exception e) {
				Logger.debug(this, e.toString());
			}

			if (comentsCount == -1) {
				try {
					String countValue = (contentlet.getStringProperty(velVar) ==  null) ? field.getDefaultValue() : contentlet.getStringProperty(velVar);
					comentsCount  = countValue.equals("") ? 0 : new Integer(countValue).intValue();
				} catch (Exception e) {
					Logger.debug(this, e.toString());
				}
			}

			++comentsCount;
			conAPI.setContentletProperty(contentlet, field, comentsCount);
			//Update the contentlet with the new comment count
			/*List<Category> cats = catAPI.getParents(contentlet, user, true);
			Map<Relationship, List<Contentlet>> contentRelationships = new HashMap<Relationship, List<Contentlet>>();

			List<Relationship> rels = RelationshipFactory.getAllRelationshipsByStructure(contentlet.getStructure());
			for (Relationship r : rels) {
				if(!contentRelationships.containsKey(r)){
					contentRelationships.put(r, new ArrayList<Contentlet>());
				}
				List<Contentlet> cons = conAPI.getRelatedContent(contentlet, r, user, true);
				for (Contentlet co : cons) {
					List<Contentlet> l2 = contentRelationships.get(r);
					l2.add(co);
				}
			}
			conAPI.checkinWithoutVersioning(contentlet, contentRelationships, cats, APILocator.getPermissionAPI().getPermissions(contentlet), user, true);
            */
			// Date
			field = commentsStructure.getFieldVar("datePublished");
			conAPI.setContentletProperty(contentletComment, field, new Date());

			// User Id
			field = commentsStructure.getFieldVar("userid");
			conAPI.setContentletProperty(contentletComment, field, userId);

			// Author
			field = commentsStructure.getFieldVar("author");
			conAPI.setContentletProperty(contentletComment, field, VelocityUtil.cleanVelocity(commentsForm.getName()));

			// Email
			field = commentsStructure.getFieldVar("email");
			conAPI.setContentletProperty(contentletComment, field, VelocityUtil.cleanVelocity(commentsForm.getEmail()));

			// WebSite
			field = commentsStructure.getFieldVar("website");
			conAPI.setContentletProperty(contentletComment, field, VelocityUtil.cleanVelocity(commentsForm.getWebsite()));

			// EmailResponse
			field = commentsStructure.getFieldVar("emailResponse");
			conAPI.setContentletProperty(contentletComment, field, (commentsForm.isNotify()?"yes":"no"));

			// IP Address
			field = commentsStructure.getFieldVar("ipAddress");
			conAPI.setContentletProperty(contentletComment, field, request.getRemoteAddr());

			// Comment
			field = commentsStructure.getFieldVar("comment");
			String comment = commentsForm.getComment();
			comment=VelocityUtil.cleanVelocity(comment);


			if (UtilMethods.isSet(commentsOptions.get("commentStripHtml")) && commentsOptions.get("commentStripHtml").equalsIgnoreCase("true")) {
				comment = Html.stripHtml(comment);
			}

			conAPI.setContentletProperty(contentletComment, field, comment);

			// Add the permission
			PermissionAPI perAPI = APILocator.getPermissionAPI();
			List<Permission> pers = perAPI.getPermissions(commentsStructure);




			// new workflows
			if(UtilMethods.isSet(commentsOptions.get("commentsModeration"))){
				if(!UtilMethods.isSet(contentletComment.getStringProperty(Contentlet.WORKFLOW_ACTION_KEY)))
						contentletComment.setStringProperty(Contentlet.WORKFLOW_ACTION_KEY, APILocator.getWorkflowAPI().findEntryAction(contentletComment, user).getId());
				contentletComment.setStringProperty(Contentlet.WORKFLOW_COMMENTS_KEY, commentsForm.getComment());
			}






			// Save the comment
			contentletComment = conAPI.checkin(contentletComment, new HashMap<Relationship, List<Contentlet>>(), new ArrayList<Category>(), pers, user, true);

            // If live I have to publish the asset
            if (UtilMethods.isSet(commentsOptions.get("commentAutoPublish")) && commentsOptions.get("commentAutoPublish").equalsIgnoreCase("true")) {
                APILocator.getVersionableAPI().setLive(contentletComment);
            }

			// Set the relation between the content and the comments
			Identifier contentletCommentIdentifier = APILocator.getIdentifierAPI().find(contentletComment);
			String commentRelationStructureName = commentsStructure.getName().replaceAll("\\s", "_").replaceAll("[^a-zA-Z0-9\\_]", "");
			String contentletRelationStructureName = contentletStructure.getName().replaceAll("\\s", "_").replaceAll("[^a-zA-Z0-9\\_]", "");
			String relationName = contentletRelationStructureName + "-" + commentRelationStructureName;


			/* get the next in the order */
			int order  = RelationshipFactory.getMaxInSortOrder(contentletIdentifier.getInode(), relationName);


			contentletIdentifier.addChild(contentletCommentIdentifier, relationName, ++order);

			ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.comment.success"));

			/* If the comment has been sennt successfully, activeDiv is set to its initial value*/
			commentsForm.setActiveDiv("");









			beanContentlet = (com.dotmarketing.portlets.contentlet.business.Contentlet) InodeFactory.getInode(contentletComment.getInode(), com.dotmarketing.portlets.contentlet.business.Contentlet.class);
			beanContentlet = conAPI.convertContentletToFatContentlet(contentletComment, beanContentlet);
			HibernateUtil.saveOrUpdate(beanContentlet);

			if (UtilMethods.isSet(commentsOptions.get("commentAutoPublish")) && commentsOptions.get("commentAutoPublish").equalsIgnoreCase("true"))
			{
				conAPI.publish(contentletComment, user, true);
			}


			saveMessages(request, ae);
			saveMessages(request.getSession(), ae);
			String referrer = UtilMethods.isSet(commentsOptions.get("referrer"))?commentsOptions.get("referrer"):"";
			referrer = (referrer.indexOf("#comments") == -1 ? referrer + "#comments" : referrer);

			String commentQuestion = commentsOptions.get("commentTitle");

			//The send comment emails should be send when there is a new comment (ALWAYS)
			String userName = commentsForm.getName();
			String userEmail = commentsForm.getEmail();
			String userComment = commentsForm.getComment();
			HibernateUtil.commitTransaction();
			if(!conAPI.isInodeIndexed(contentletComment.getInode())){
				Logger.error(this, "Problem indexing comment content");
			}
			sendCommentEmails(contentletIdentifier, relationName, request, referrer,userName,userEmail,userComment,commentQuestion, commentsOptions);

			ActionForward forward = new ActionForward(referrer);
			forward.setRedirect(true);

			/* reset the form and reset the captcha */
			commentsForm.reset();

			request.setAttribute("commentsForm", commentsForm);
			return forward;
		} catch (Exception ex) {
			try {
				HibernateUtil.rollbackTransaction();
			} catch (DotHibernateException e) {
				Logger.error(CommentsAction.class,e.getMessage(),e);
			}
			Logger.error(this, ex.toString(), ex);

			ae.add(Globals.ERROR_KEY, new ActionMessage("message.comment.failure"));
			saveMessages(request, ae);
			saveMessages(request.getSession(), ae);
			String referrer = UtilMethods.isSet(commentsOptions.get("referrer"))?commentsOptions.get("referrer"):"";

			referrer = (referrer.indexOf("#comments") == -1 ? referrer + "#comments" : referrer);
			ActionForward forward = new ActionForward(referrer);
			forward.setRedirect(true);
			return forward;
		}
	}

	private void sendCommentEmails(Identifier contentletIdentifier, String relationName, HttpServletRequest request,
			String referrer,String userName,String userEmail,String userComment,String commentQuestion, HashMap<String, String> commentsOptions)
	{

		//This needs to be updated to use a lucene fix
		// Condition
		Structure commentsStructure = StructureCache.getStructureByVelocityVarName(CommentsWebAPI.commentsVelocityStructureName);
		Field field = commentsStructure.getFieldVar("emailResponse");
		String responseField = field.getFieldContentlet();

		//Get the Email Field
		field = commentsStructure.getFieldVar("email");
		String emailField = field.getVelocityVarName();

		//Get the Date Published Field
		field = commentsStructure.getFieldVar("datePublished");
		String dateField = field.getVelocityVarName();

		//Get the Email Response Field
		field = commentsStructure.getFieldVar("emailResponse");
		String emailResponseField = field.getVelocityVarName();

		// Order
		String order = "";

		// Gather all comment no matter the value of the Email Response
		List<Contentlet> comments = RelationshipFactory.getRelatedContentByParent(contentletIdentifier.getInode(), relationName, true, order);

		//Cycle for all comments to get the most recent comment from each email
		HashMap<String,CommentDate> commentDates = new HashMap<String, CommentDate>();
		for(Contentlet comment : comments)
		{
			try
			{
				//Get the email of the comment
				String email = comment.getStringProperty(emailField);
				//Get the date of the comment
				Date date = comment.getDateProperty(dateField);
				//Get the value of the comment
				String emailResponseString = comment.getStringProperty(emailResponseField);
				boolean emailResponse = (emailResponseString.equals("yes") ? true : false);

				//If there is another comment from the same email I update it
				if(commentDates.containsKey(email))
				{
					CommentDate commentDate = commentDates.get(email);
					if(commentDate.getDate().before(date))
					{
						commentDate.setDate(date);
						commentDate.setSend(emailResponse);
					}
				}
				//If this is the first comment for the email I add it to the hash
				else
				{
					commentDates.put(email,new CommentDate(email,date,emailResponse));
				}
			}
			catch(Exception ex)
			{}
		}

		//Cycle for all emails, check if they has set the send email to true and add it to the list
		String emails = new String();
		Set<String> keys = commentDates.keySet();
		for(String key : keys)
		{
			try
			{
				CommentDate commentDate = commentDates.get(key);
				if(commentDate.isSend())
				{
					emails += commentDate.getEmail() + ";";
				}
			}
			catch(Exception ex)
			{}
		}
		//Remove the last ; from the list
		if(UtilMethods.isSet(emails) && emails.indexOf(";") > 0)
		{
			emails = emails.substring(0,emails.lastIndexOf(";"));
		}

		String emailTemplate = commentsOptions.get("emailTemplate");

		Company liferay = PublicCompanyFactory.getDefaultCompany();
		String from = liferay.getEmailAddress();
		String to  =  commentsOptions.get("email");
		String subject = Config.getStringProperty("commentSubject");
		subject = subject.replaceAll("\\$\\{commentQuestion}", commentQuestion);
		String URL = "http://" + request.getServerName() + referrer;

		Map<String, Object> parameters = new HashMap<String, Object> ();
		//I set a dummy TO email address to send the real emails using BCC
		//parameters.put("to", to);
		String emailTo = Config.getStringProperty("EMAIL_TO");
		if(UtilMethods.isSet(emailTo)){
			parameters.put("to", emailTo);
		}
		parameters.put("from", from);
		parameters.put("userName", userName);
		parameters.put("userEmail", userEmail);
		parameters.put("userComment", userComment);
		parameters.put("commentQuestion", commentQuestion);
		parameters.put("url", URL);
		parameters.put("subject", subject);
		parameters.put("emailTemplate", emailTemplate);
		parameters.put("bcc", emails);


		try {
			Host host = hostWebAPI.getCurrentHost(request);
			EmailFactory.sendParameterizedEmail(parameters, new HashSet<String>(), host, null);
		} catch (Exception e) {
			Logger.error(this, "An error as ocurred trying to send the comment notification");
		}


	}
	/***
	 * This is an helper class to store the last comment from any account and their email response value
	 * @author Salvador
	 *
	 */
	private class CommentDate{

		//The email that post the comment
		private String email;
		//The date of the comment
		private Date date;
		//The value of the email response
		private boolean send;

		//Default Constructor
		public CommentDate()
		{

		}

		public CommentDate(String email,Date date,boolean send)
		{
			this.email = email;
			this.date = date;
			this.send = send;
		}

		public String getEmail() {
			return email;
		}
		public void setEmail(String email) {
			this.email = email;
		}
		public Date getDate() {
			return date;
		}
		public void setDate(Date date) {
			this.date = date;
		}
		public boolean isSend() {
			return send;
		}
		public void setSend(boolean send) {
			this.send = send;
		}
	}
}
