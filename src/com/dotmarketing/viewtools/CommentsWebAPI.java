package com.dotmarketing.viewtools;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.lucene.queryParser.ParseException;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.RelationshipFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

public class CommentsWebAPI implements ViewTool {

	public final static String commentsVelocityStructureName = "Comments";
	private static String datePublishedFieldName = "";
	
	
	/**
	 * Holds a list of comment relationships that have already been created
	 * 
	 */
	private static final Map<String, Boolean> _commentsCreated = new HashMap<String, Boolean>();
	private com.liferay.portal.model.User user = null;
	private HttpServletRequest request;
	private Context ctx;
	private boolean respectFrontendRoles = true;
	
	public void init(Object obj) {
		ViewContext context = (ViewContext) obj;
		this.request = context.getRequest();
		ctx = context.getVelocityContext();

		if(request.getSession(false) != null)
			user = (User)request.getSession().getAttribute(WebKeys.CMS_USER);

	}
	
	private String getRelationshipName(String contentletInode) throws DotSecurityException {
		try{
			Contentlet contentlet = APILocator.getContentletAPI().find(contentletInode, APILocator.getUserAPI().getSystemUser(), true);
			return getRelationshipName(contentlet);
		}catch(DotDataException e){
			Logger.error(this, "Unable to look up contentlet with inode " + contentletInode, e);
			return "";
		}
	}
	private String getRelationshipName(Contentlet contentlet) throws DotSecurityException {
		// Comments Structure
		Structure commentsStructure = StructureCache.getStructureByVelocityVarName(commentsVelocityStructureName);
		// Get the contentlet structure
		Structure contentletStructure = StructureCache.getStructureByInode(contentlet.getStructureInode());

		String commentStructureName = commentsStructure.getName().replaceAll("\\s", "_").replaceAll("[^a-zA-Z0-9\\_]", "");
		String contentletStructureName = contentletStructure.getName().replaceAll("\\s", "_").replaceAll("[^a-zA-Z0-9\\_]", "");
		String relationshipName = contentletStructureName + "-" + commentStructureName;

		return relationshipName;
		
	}
    @Deprecated
	private String getRelationshipName(long contentletInode) {
		try {
			return getRelationshipName(String.valueOf(contentletInode));
		} catch (Exception e) {
			Logger.error(this, "Cannot find relationship for contentlet" + contentletInode, e);
		}
			return "";
	}

	private static boolean existCommentsStructure() {
		Structure structure = StructureCache.getStructureByVelocityVarName(commentsVelocityStructureName);
		boolean returnValue = false;
		if (InodeUtils.isSet(structure.getInode())) {
			returnValue = true;
		}
		return returnValue;
	}

	@Deprecated
	public List<Contentlet> getComments(long inode){
		try {
			//if(UtilMethods.isSet(inode))
				return getComments(String.valueOf(inode));
		} catch (Exception e) {
			Logger.error(this,e.getMessage(),e);
		}
		return new ArrayList<Contentlet>();
	}
	
	
	public List<Contentlet> getComments(String inode, boolean descendingOrder){
		try{
			ContentletAPI conAPI = APILocator.getContentletAPI();
			Contentlet contentlet = new Contentlet();
			try{
				contentlet = conAPI.find(inode, APILocator.getUserAPI().getSystemUser(), true);
			}catch(DotDataException e){
				Logger.debug(this, "Unable to look up contentlet with inode " + inode, e);
			}
			validateComments(contentlet);
			if(!InodeUtils.isSet(contentlet.getInode())){
				return new ArrayList<Contentlet>();
			}
			List<Contentlet> comments = null;
			if(contentlet.getStructure().getVelocityVarName().equals(commentsVelocityStructureName)){
				comments = conAPI.search("+Comments-Comments-parent:" + contentlet.getIdentifier() + " +live:true +languageid:" + contentlet.getLanguageId() + " +deleted:false", 0, -1, "Comments.datePublished " + (descendingOrder ? "desc":"asc"), APILocator.getUserAPI().getSystemUser(), true);
			}else{
				comments = conAPI.search("+" + getRelationshipName(contentlet) + ":" + contentlet.getIdentifier() + " +live:true +languageid:" + contentlet.getLanguageId() + " +deleted:false", 0, -1, "Comments.datePublished " + (descendingOrder ? "desc":"asc"), APILocator.getUserAPI().getSystemUser(), true);
			}
//			int count = 0;
//			StringBuilder bob = new StringBuilder();
//			for (Contentlet c : comments) {
//				bob.append("Comments-Comments-child:" + c.getIdentifier() + " ");
//				count++;
//				if(count > 20){
//					List<Contentlet> cons = conAPI.search("+(" + bob.toString() + ") +live:true +languageid:" + contentlet.getLanguageId() + " +deleted:false", 0, -1, "datePublished " + (descendingOrder ? "desc":"asc"), APILocator.getUserAPI().getSystemUser(), true);
//					if(cons != null){
//						comments.addAll(cons);
//					}
//					count = 0;
//					bob = new StringBuilder();
//				}
//			}
//			if(count >0){
//				List<Contentlet> cons = conAPI.search("+(" + bob.toString() + ") +live:true +languageid:" + contentlet.getLanguageId() + " +deleted:false", 0, -1, "datePublished " + (descendingOrder ? "desc":"asc"), APILocator.getUserAPI().getSystemUser(), true);
//				if(cons != null){
//					comments.addAll(cons);
//				}
//			}
//			CommentsComparator comparator = new CommentsComparator(descendingOrder);
//			Collections.sort(comments, comparator);
			return comments;
		}catch (DotSecurityException se) {
			Logger.error(this, "Unable to get comments becuase of user/security error",se);
			return new ArrayList<Contentlet>();
		} catch (DotDataException e) {
			Logger.error(CommentsWebAPI.class,e.getMessage(),e);
			return new ArrayList<Contentlet>();
		} catch (Exception e) {
			Logger.error(CommentsWebAPI.class,e.getMessage(),e);
			return new ArrayList<Contentlet>();
		}
				
	}
	
	public List<Contentlet> getComments(String inode){
		return getComments(inode, false);
	}
	
	
	private static boolean existCommentsRelation(Contentlet contentlet) {
		boolean returnValue = false;
		// Comments Structure
		Structure commentsStructure = StructureCache.getStructureByVelocityVarName(commentsVelocityStructureName);

		// Get the contentlet structure
		Structure contentletStructure = StructureCache.getStructureByInode(contentlet.getStructureInode());


		// Get the relationships of the comments structure
		List<Relationship> relationships = RelationshipFactory.getAllRelationshipsByStructure(commentsStructure);
		for (Relationship relationship : relationships) {
			Structure childStructure = relationship.getChildStructure();
			Structure parentStructure = relationship.getParentStructure();
			if (childStructure.getInode().equalsIgnoreCase(commentsStructure.getInode()) && parentStructure.getInode().equalsIgnoreCase(contentletStructure.getInode())) {
				returnValue = true;
				break;
			}
		}
		return returnValue;
	}

	private static  void validateCommentsRelation(Contentlet contentlet) throws DotHibernateException {
		if (!existCommentsRelation(contentlet)) {
			// Comments Structure
			Structure commentsStructure =  StructureCache.getStructureByVelocityVarName(commentsVelocityStructureName);
			// Get the contentlet structure
			Structure contentletStructure = contentlet.getStructure();

			String commentStructureName = commentsStructure.getName().replaceAll("\\s", "_").replaceAll("[^a-zA-Z0-9\\_]", "");
			String contentletStructureName = contentletStructure.getName().replaceAll("\\s", "_").replaceAll("[^a-zA-Z0-9\\_]", "");

			// Create the relationship
			Relationship relationship = new Relationship();
			relationship.setCardinality(0);
			relationship.setChildRelationName(commentStructureName);
			relationship.setParentRelationName(contentletStructureName);
			relationship.setChildStructureInode(commentsStructure.getInode());
			relationship.setParentStructureInode(contentletStructure.getInode());
			relationship.setRelationTypeValue(contentletStructureName + "-" + commentStructureName);
			relationship.setParentRequired(false);
			relationship.setChildRequired(false);
			relationship.setFixed(true);
			RelationshipFactory.saveRelationship(relationship);

		}
	}

	private static boolean existCommentsCommentsRelation() {
		boolean returnValue = false;
		// Comments Structure
		Structure commentsStructure =  StructureCache.getStructureByVelocityVarName(commentsVelocityStructureName);

		// Get the relationships of the comments structure
		List<Relationship> relationships = RelationshipFactory.getAllRelationshipsByStructure(commentsStructure);
		for (Relationship relationship : relationships) {
			Structure childStructure = relationship.getChildStructure();
			Structure parentStructure = relationship.getParentStructure();
			if (childStructure.getInode().equalsIgnoreCase(commentsStructure.getInode()) && parentStructure.getInode().equalsIgnoreCase(commentsStructure.getInode())) {
				returnValue = true;
				break;
			}
		}
		return returnValue;
	}

	private static void validateCommentsCommentsRelation() throws DotHibernateException {
		if (!existCommentsCommentsRelation()) {
			// Comments Structure
			Structure commentsStructure =  StructureCache.getStructureByVelocityVarName(commentsVelocityStructureName);
			// Get the contentlet structure

			String commentStructureName = commentsStructure.getName().replaceAll("\\s", "_").replaceAll("[^a-zA-Z0-9\\_]", "");

			// Create the relationship
			Relationship relationship = new Relationship();
			relationship.setCardinality(0);
			relationship.setChildRelationName("Parent Comment");
			relationship.setParentRelationName("Replies");
			relationship.setChildStructureInode(commentsStructure.getInode());
			relationship.setParentStructureInode(commentsStructure.getInode());
			relationship.setRelationTypeValue(commentStructureName + "-" + commentStructureName);
			relationship.setParentRequired(false);
			relationship.setChildRequired(false);
			RelationshipFactory.saveRelationship(relationship);
		}
	}

	private static void initStructures() {
		if (!existCommentsStructure()) {
			// Save the structure
			try {
				Structure commentsStructure = new Structure();
				commentsStructure.setDefaultStructure(false);
				commentsStructure.setDescription("Comments structure for all content");
				commentsStructure.setName(commentsVelocityStructureName);
				commentsStructure.setVelocityVarName(commentsVelocityStructureName);
				commentsStructure.setFixed(true);
				commentsStructure.setStructureType(Structure.STRUCTURE_TYPE_CONTENT);
				StructureFactory.saveStructure(commentsStructure);
				StructureCache.removeStructure(commentsStructure);
				StructureCache.addStructure(commentsStructure);
				List<Field> fields = new ArrayList<Field>();
				String commentsStructureInode = commentsStructure.getInode();

				// Save the fields

				
				// Comment Title
				Field field = new Field();
				field.setFieldName("Title");
				field.setVelocityVarName("title");
				field.setFieldContentlet("text1");
				field.setFieldType("text");
				field.setStructureInode(commentsStructureInode);
				field.setIndexed(true);
				field.setRequired(true);
				field.setListed(true);
				field.setSortOrder(0);
				field.setFixed(true);
				field.setReadOnly(true);
				FieldFactory.saveField(field);
				fields.add(field);
							

				// UserId
				field = new Field();
				field.setFieldName("UserId");
				field.setVelocityVarName("userid");
				field.setFieldContentlet("text2");
				field.setFieldType("text");
				field.setStructureInode(commentsStructureInode);
				field.setSortOrder(2);
				field.setFixed(true);
				field.setReadOnly(true);
				FieldFactory.saveField(field);
				fields.add(field);

				
				// Author
				field = new Field();
				field.setFieldName("Author");
				field.setVelocityVarName("author");
				field.setFieldContentlet("text3");
				field.setFieldType("text");
				field.setStructureInode(commentsStructureInode);
				field.setSortOrder(3);
				field.setFixed(true);
				field.setReadOnly(true);
				FieldFactory.saveField(field);
				fields.add(field);

				
				// Email
				field = new Field();
				field.setFieldName("Email");
				field.setVelocityVarName("email");
				field.setFieldContentlet("text4");
				field.setFieldType("text");
				field.setStructureInode(commentsStructureInode);
				field.setSortOrder(4);
				field.setIndexed(true);
				field.setRequired(true);
				field.setListed(true);
				field.setFixed(true);
				field.setReadOnly(true);
				FieldFactory.saveField(field);
				fields.add(field);

				
				// Website
				field = new Field();
				field.setFieldName("Website");
				field.setVelocityVarName("website");
				field.setFieldContentlet("text5");
				field.setFieldType("text");
				field.setStructureInode(commentsStructureInode);
				field.setSortOrder(5);
				field.setFixed(true);
				field.setReadOnly(true);
				FieldFactory.saveField(field);
				fields.add(field);

				// Comment
				field = new Field();
				field.setFieldName("Comment");
				field.setVelocityVarName("comment");
				field.setFieldContentlet("text_area1");
				field.setFieldType("wysiwyg");
				field.setStructureInode(commentsStructureInode);
				field.setIndexed(true);
				field.setRequired(true);
				field.setSortOrder(6);
				field.setFixed(true);
				field.setReadOnly(true);
				FieldFactory.saveField(field);
				fields.add(field);

				
				// Email Responses
				field = new Field();
				field.setFieldName("Email Response");
				field.setVelocityVarName("emailResponse");
				field.setFieldContentlet("text6");
				field.setFieldType("radio");
				field.setValues("Yes|yes\r\nNo|no");
				field.setStructureInode(commentsStructureInode);
				field.setSortOrder(7);
				field.setFixed(true);
				field.setReadOnly(true);
				FieldFactory.saveField(field);
				fields.add(field);

				
				// IP Address
				field = new Field();
				field.setFieldName("IP Address");
				field.setVelocityVarName("ipAddress");
				field.setFieldContentlet("text7");
				field.setFieldType("text");
				field.setStructureInode(commentsStructureInode);
				field.setSortOrder(8);
				field.setFixed(true);
				field.setReadOnly(true);
				FieldFactory.saveField(field);
				fields.add(field);
				
				
				// Date
				field = new Field();
				field.setFieldName("DatePublished");
				field.setVelocityVarName("datePublished");
				field.setFieldContentlet("date1");
				field.setFieldType("date_time");
				field.setStructureInode(commentsStructureInode);
				field.setIndexed(false);
				field.setRequired(false);
				field.setListed(false);
				field.setSortOrder(9);
				field.setFixed(true);
				field.setReadOnly(true);
				FieldFactory.saveField(field);
				fields.add(field);
				FieldsCache.removeFields(commentsStructure);
				FieldsCache.addFields(commentsStructure,fields);
			} catch (DotHibernateException e) {
				Logger.error(CommentsWebAPI.class, e.getMessage(), e);
			} 
		}
	}

	public void validateComments(String contentletInode) throws DotSecurityException, DotHibernateException {
		// Load the contentlet
		ContentletAPI conAPI = APILocator.getContentletAPI();
		Contentlet contentlet = new Contentlet();
		try{
			contentlet = conAPI.find(contentletInode, APILocator.getUserAPI().getSystemUser(), true);
		}catch(DotDataException e){
			Logger.debug(this, "validateComments for no inode : " + contentletInode);
		}
		validateComments(contentlet);
	}

	public void validateComments(Contentlet contentlet) throws DotSecurityException, DotHibernateException {
		if(!InodeUtils.isSet(contentlet.getInode())){
			return;
		}
		/* if we have not created the structure and relationship between structures*/
		if (_commentsCreated.get(String.valueOf(contentlet.getStructure())) == null) {
			initStructures();
			validateCommentsRelation(contentlet);
			validateCommentsCommentsRelation();
			_commentsCreated.put(String.valueOf(contentlet.getStructure()), new Boolean(true));
		}
	}
/*
	public List<Contentlet> filterComments(List<Contentlet> comments) {
		if(comments == null) return new ArrayList<Contentlet>();
		int size = comments.size();
		for (int i = size - 1; i >= 0; i--) {
			if (!comments.get(i).isLive()) {
				comments.remove(i);
			}
		}
		CommentsComparator commentsComparator = new CommentsComparator();
		Collections.sort(comments, commentsComparator);
		return comments;
	}
	*/

	public class CommentsComparator implements Comparator<com.dotmarketing.portlets.contentlet.model.Contentlet> 
	{		
		private boolean invert = false;
		
		public CommentsComparator()
		{				
		}
		
		public CommentsComparator(boolean invert)
		{
				this.invert = invert;
		}
		
		public int compare(com.dotmarketing.portlets.contentlet.model.Contentlet contentlet1, com.dotmarketing.portlets.contentlet.model.Contentlet contentlet2) {
			
			if (!UtilMethods.isSet(datePublishedFieldName)) {
				Structure commentsStructure =  StructureCache.getStructureByVelocityVarName(commentsVelocityStructureName);
				
				Field field = commentsStructure.getField("DatePublished");
				String dbField = field.getVelocityVarName();
				CommentsWebAPI.datePublishedFieldName = dbField;
			}
			

			Date contentlet1PublishDate = new Date();
			Date contentlet2PublishDate = new Date();
			try {
				contentlet1PublishDate = (Date) contentlet1.getDateProperty(datePublishedFieldName);
				contentlet2PublishDate = (Date) contentlet2.getDateProperty(datePublishedFieldName);
			} catch (Exception ex) {
			}

			int returnValue = 0;
			if (contentlet1PublishDate.before(contentlet2PublishDate)) {
				returnValue = 1;
			}
			if (contentlet1PublishDate.after(contentlet2PublishDate)) {
				returnValue = -1;
			}
			if (contentlet1PublishDate.equals(contentlet2PublishDate)) {
				returnValue = 0;
			}
			returnValue = (invert ? returnValue * -1 : returnValue);
			return returnValue;
		}
	}
	
	public int getCommentsCount(String inode) {
		int commentNumber = 0;
		try{
		    ContentletAPI conAPI = APILocator.getContentletAPI();
		    Contentlet contentlet = new Contentlet();
			contentlet = conAPI.find(inode, APILocator.getUserAPI().getSystemUser(), respectFrontendRoles);

			Structure contentletStructure = StructureCache.getStructureByInode(contentlet.getStructureInode());

    		Field field =  contentletStructure.getField("CommentsCount");
    		if (!InodeUtils.isSet(field.getInode())){
    			return 0;
    		}
    		if(field.getFieldContentlet().startsWith(Field.DataType.TEXT.toString())){
    		    commentNumber = Integer.parseInt(contentlet.getStringProperty(field.getVelocityVarName())) ;
    		    
    		}else {
    			commentNumber = new Long(contentlet.getLongProperty(field.getVelocityVarName())).intValue();
    		}
    		
        }catch(DotDataException e){
                Logger.error(this, "Unable to look up contentlet with inode " + inode, e);
        }catch (DotSecurityException se) {
            Logger.error(this, "Unable to get system user", se);
        }catch (Exception ex) {
        	Logger.error(this, "CommentsCount Method : Unable to return comments count properly", ex);
        }
		 return commentNumber;
		 
		
	}

	@Deprecated
	public int getCommentsCount(long inode) {				 
		 int returnValue = 0;
			try {
				returnValue = getCommentsCount(String.valueOf(inode));
			} catch (Exception e) {
				Logger.error(this, "Comments Count Method : Unable to parse to String " ,e);
			} finally {
			}
			return returnValue;
	}
	
	public com.liferay.portal.model.User getUser() {
		return user;
	}
	
	public void setUser(com.liferay.portal.model.User user) {
		this.user = user;
	}

	public boolean isRespectFrontendRoles() {
		return respectFrontendRoles;
	}

	public void setRespectFrontendRoles(boolean respectFrontendRoles) {
		this.respectFrontendRoles = respectFrontendRoles;
	}
}
