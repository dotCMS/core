package com.dotmarketing.cms.comment.struts;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.ValidatorForm;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.CaptchaUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.octo.captcha.service.CaptchaServiceException;

/**
 * This class manage the comments beans
 * @author Salvador Di Nardo
 * @version 1.5
 * @since 1.0
 */
public class CommentsForm extends ValidatorForm
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String dispatch;
	private String commentTitle;
	private String name;
	private String email;
	private String website;
	private boolean notify;	
	private String comment;
	private boolean accept;
	private String contentInode;
	private String userId;
	private String referrer;
	private String captcha;
	private String audioCaptcha;

	//Configuration Variables
	private boolean commentAutoPublish;
	private boolean commentUseCaptcha;
	private boolean commentStripHtml;
	private boolean commentForceLogin;
	private boolean commentUseAudioCaptcha;
	
	/**
	 * @return the commentUseAudioCaptcha
	 */
	public boolean isCommentUseAudioCaptcha() {
		return commentUseAudioCaptcha;
	}

	/**
	 * @param commentUseAudioCaptcha the commentUseAudioCaptcha to set
	 */
	public void setCommentUseAudioCaptcha(boolean commentUseAudioCaptcha) {
		this.commentUseAudioCaptcha = commentUseAudioCaptcha;
	}

	/*
	 * This field was added to create workflow task over the
	 * new comments
	 */
	private String commentsModeration;
	
	/*
	 * This field keeps the id of the last active div in listcomments.html so that when an error happens in posting
	 * the form, the form that needs to be corrected can be opened in the right place
	 */
	
	private String activeDiv;
	
	public void reset()
	{
		name = "";
		email = "";
		website = "";
		notify = false;
		comment = "";
		accept = false;		
		captcha = "";
		commentsModeration="";
		audioCaptcha = "";
	}

	public boolean isAccept() {
		return accept;
	}
	public void setAccept(boolean accept) {
		this.accept = accept;
	}
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public boolean isNotify() {
		return notify;
	}
	public void setNotify(boolean notify) {
		this.notify = notify;
	}
	public String getContentInode() {
		return contentInode;
	}
	public void setContentInode(String contentInode) {
		this.contentInode = contentInode;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public void setCommentTitle(String commentTitle){
		this.commentTitle = commentTitle;
	}


	public String getReferrer() {
		return referrer;
	}
	public void setReferrer(String referrer) {
		this.referrer = referrer;
	}
	public String getCaptcha() {
		return captcha;
	}
	public void setCaptcha(String captcha) {
		this.captcha = captcha;
	}

	public String getWebsite() {
		return website;
	}
	public void setWebsite(String website) {
		this.website = website;
	}
	
	@SuppressWarnings("deprecation")
	public ActionErrors validate(ActionMapping arg0, HttpServletRequest request) 
	{
		ContentletAPI conAPI = APILocator.getContentletAPI();
		ActionErrors errors = new ActionErrors(); 
		Contentlet parentContentlet = new Contentlet();	
		HttpSession session = request.getSession();
		String captchaSession =  (String) session.getAttribute(nl.captcha.servlet.Constants.SIMPLE_CAPCHA_SESSION_KEY);
		try{
			parentContentlet = conAPI.find(contentInode, APILocator.getUserAPI().getSystemUser(), true);
		}catch(DotDataException e){
			Logger.error(this, "Unable to look up contentlet with inode " + contentInode, e);
		}catch (DotSecurityException dse) {
			Logger.error(this, "Unable to look up contentlet with inode " + contentInode + " because of security issue", dse);
		}
		
		if(!InodeUtils.isSet(parentContentlet.getInode())){
			errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("message.contentlet.required","Contentlet Inode"));    		
			return errors;
		}
		if (!UtilMethods.isSet(name)) 
		{
			errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("message.contentlet.required","Name"));    		
		}
		if (!UtilMethods.isSet(email) || ! UtilMethods.isValidEmail(email)) 
		{    		
			errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("message.contentlet.required","Email"));    		
		}
		/*	Try to find a title if we don't have one.  */
		if (!UtilMethods.isSet(commentTitle))
    	{
			Structure s = StructureCache.getStructureByInode(parentContentlet.getStructureInode());
			List<Field> lf = s.getFields();
			for(Field f : lf){
				if("text".equals(f.getFieldType()) && f.isIndexed() && f.isListed()){
					try{
						commentTitle = "re: " + conAPI.getFieldValue(parentContentlet, f);
					}catch (Exception e) {
						Logger.error(CommentsForm.class, "Unable to set comment title", e);
					}
					break;
				}
			}
    	}
		if (!UtilMethods.isSet(comment))
		{
			errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("message.contentlet.required","Comment"));
		}
		if (UtilMethods.isSet(accept) && accept == false)
		{
			errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("message.contentlet.required","Accept"));
		}

		if(commentUseCaptcha)
		{
			if(!UtilMethods.isSet(captcha) || !UtilMethods.isSet(captchaSession) || !captcha.equals(captchaSession))
			{
				errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("message.contentlet.required","Validation Image"));
			}
		}
		
		if(commentUseAudioCaptcha && !UtilMethods.isSet(captchaSession)){
         
			Boolean isResponseCorrect =Boolean.FALSE;
			String captchaId = request.getSession().getId();  
			if(UtilMethods.isSet(audioCaptcha) && UtilMethods.isSet(captchaId)){
				try {
//					isResponseCorrect = CaptchaServiceSingleton.getInstance().validateResponseForID(captchaId,
//							audioCaptcha);
					
					isResponseCorrect = CaptchaUtil.isValidAudioCaptcha(request);
				} catch (CaptchaServiceException e) {
					Logger.error(CommentsForm.class, "An error ocurred trying to validate audio captcha", e);
				}
			 }
		
			if(!isResponseCorrect){
				errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("message.contentlet.required","Validation Sound"));
			}

		}
		return errors;
	}

	public String getCommentTitle() {
		return commentTitle;
	}


	public String getDispatch() {
		return dispatch;
	}

	public void setDispatch(String dispatch) {
		this.dispatch = dispatch;
	}

	public boolean isCommentAutoPublish() {
		return commentAutoPublish;
	}

	public void setCommentAutoPublish(boolean commentAutoPublish) {
		this.commentAutoPublish = commentAutoPublish;
	}

	public boolean isCommentForceLogin() {
		return commentForceLogin;
	}

	public void setCommentForceLogin(boolean commentForceLogin) {
		this.commentForceLogin = commentForceLogin;
	}

	public boolean isCommentStripHtml() {
		return commentStripHtml;
	}

	public void setCommentStripHtml(boolean commentStripHtml) {
		this.commentStripHtml = commentStripHtml;
	}

	public boolean isCommentUseCaptcha() {
		return commentUseCaptcha;
	}

	public void setCommentUseCaptcha(boolean commentUseCaptcha) {
		this.commentUseCaptcha = commentUseCaptcha;
	}
	
	/**
	 * Get the role name of the role to assign the comment workflow
	 * @return String
	 * @author Oswaldo Gallango
	 * @since 1.5
	 * @version 1.0
	 */
	public String getCommentsModeration() {
		return commentsModeration;
	}

	/**
	 * Set the role name of the role to assign the comment workflow
	 * @param commentsModeration The role name
	 * @author Oswaldo Gallango
	 * @since 1.5
	 * @version 1.0
	 */
	public void setCommentsModeration(String commentsModeration) {
		this.commentsModeration = commentsModeration;
	}

	public String getAudioCaptcha() {
		return audioCaptcha;
	}

	public void setAudioCaptcha(String audioCaptcha) {
		this.audioCaptcha = audioCaptcha;
	}
	
	public String getActiveDiv(){
		return activeDiv;
	}
	
	public void setActiveDiv(String activeDiv){
		this.activeDiv = activeDiv;
	}
}
